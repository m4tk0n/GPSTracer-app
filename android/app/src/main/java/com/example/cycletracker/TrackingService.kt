package com.example.cycletracker

import android.app.*
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.cycletracker.db.AppDatabase
import com.example.cycletracker.db.TrackEntity
import com.example.cycletracker.db.TrackPointEntity
import com.example.cycletracker.sync.SyncWorker
import com.google.android.gms.location.*
import kotlinx.coroutines.launch

/**
 * Foreground Service, ktera bezi i pri vypnute obrazovce a se zamcenym
 * telefonem. Zapina se pozadavkem START_TRACKING, PAUSE_TRACKING nebo
 * STOP_TRACKING poslanym jako Intent action (viz companion funkce nize).
 *
 * Klic k tomu, aby to fungovalo na pozadi:
 *  - bezi jako foreground service s typem "location" (viditelna notifikace)
 *  - drzi si PARTIAL_WAKE_LOCK po dobu aktivniho zaznamu
 *  - pozadavek na polohu ma PRIORITY_HIGH_ACCURACY, interval cca 5s
 *  - vyzaduje ACCESS_BACKGROUND_LOCATION (uzivatel musi povolit "Vzdy" v nastaveni)
 */
class TrackingService : LifecycleService() {

    companion object {
        const val ACTION_START = "com.example.cycletracker.action.START"
        const val ACTION_PAUSE = "com.example.cycletracker.action.PAUSE"
        const val ACTION_RESUME = "com.example.cycletracker.action.RESUME"
        const val ACTION_STOP = "com.example.cycletracker.action.STOP"

        private const val NOTIF_CHANNEL_ID = "tracking_channel"
        private const val NOTIF_ID = 1001

        private const val LOCATION_INTERVAL_MS = 5000L
        private const val MIN_ELEVATION_DELTA_M = 1.0 // filtr GPS sumu pro prevyseni
        private const val MIN_ACCURACY_M = 25f        // zahazuj velmi nepresne body

        // Jednoduchy zpusob, jak UI sleduje aktualni stav - staci na rozsah teto appky.
        val liveTrackId = MutableLiveData<Long?>(null)
        val liveState = MutableLiveData(TrackEntity.STATE_FINISHED)
        val liveDistanceM = MutableLiveData(0.0)
        val liveElevationM = MutableLiveData(0.0)
        val liveAvgSpeedKmh = MutableLiveData(0.0)
        val liveLastLocation = MutableLiveData<Location?>(null)
    }

    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var db: AppDatabase
    private var wakeLock: PowerManager.WakeLock? = null

    private var currentTrackId: Long = -1
    private var lastPoint: TrackPointEntity? = null
    private var movingTimeMillis = 0L
    private var lastLocationTime = 0L

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            if (location.accuracy > MIN_ACCURACY_M) return
            onNewLocation(location)
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        db = AppDatabase.getInstance(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START -> startNewTrack()
            ACTION_PAUSE -> pauseTrack()
            ACTION_RESUME -> resumeTrack()
            ACTION_STOP -> stopTrack()
        }
        return START_STICKY
    }

    private fun startNewTrack() {
        lifecycleScope.launch {
            val track = TrackEntity(startedAt = System.currentTimeMillis(), state = TrackEntity.STATE_RUNNING)
            currentTrackId = db.trackDao().insertTrack(track)
            lastPoint = null
            movingTimeMillis = 0L
            lastLocationTime = System.currentTimeMillis()

            liveTrackId.postValue(currentTrackId)
            liveState.postValue(TrackEntity.STATE_RUNNING)
            liveDistanceM.postValue(0.0)
            liveElevationM.postValue(0.0)
            liveAvgSpeedKmh.postValue(0.0)

            startForeground(NOTIF_ID, buildNotification("Zaznamenavam trasu...", 0.0))
            acquireWakeLock()
            startLocationUpdates()
        }
    }

    private fun pauseTrack() {
        liveState.postValue(TrackEntity.STATE_PAUSED)
        fusedClient.removeLocationUpdates(locationCallback)
        updateNotification("Pauza", liveDistanceM.value ?: 0.0)
        lifecycleScope.launch { updateTrackState(TrackEntity.STATE_PAUSED) }
    }

    private fun resumeTrack() {
        liveState.postValue(TrackEntity.STATE_RUNNING)
        lastLocationTime = System.currentTimeMillis()
        startLocationUpdates()
        lifecycleScope.launch { updateTrackState(TrackEntity.STATE_RUNNING) }
    }

    private fun stopTrack() {
        fusedClient.removeLocationUpdates(locationCallback)
        val trackId = currentTrackId
        lifecycleScope.launch {
            val track = db.trackDao().getTrack(trackId)
            if (track != null) {
                track.state = TrackEntity.STATE_FINISHED
                track.endedAt = System.currentTimeMillis()
                track.movingTimeMillis = movingTimeMillis
                track.avgSpeedKmh = computeAvgSpeedKmh(track.distanceMeters, movingTimeMillis)
                db.trackDao().updateTrack(track)
            }
            // Rovnou zkus synchronizovat na server (WorkManager si poradi i pri spatnem signalu)
            WorkManager.getInstance(applicationContext)
                .enqueue(OneTimeWorkRequestBuilder<SyncWorker>().build())

            liveState.postValue(TrackEntity.STATE_FINISHED)
            liveTrackId.postValue(null)
            releaseWakeLock()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun onNewLocation(location: Location) {
        liveLastLocation.postValue(location)
        if (liveState.value != TrackEntity.STATE_RUNNING) return

        val now = System.currentTimeMillis()
        lifecycleScope.launch {
            val point = TrackPointEntity(
                trackId = currentTrackId,
                timestamp = now,
                latitude = location.latitude,
                longitude = location.longitude,
                altitude = location.altitude,
                speedMs = if (location.hasSpeed()) location.speed else 0f,
                accuracyM = location.accuracy
            )
            db.trackDao().insertPoint(point)

            val prev = lastPoint
            var distanceDelta = 0.0
            var elevationDelta = 0.0
            if (prev != null) {
                distanceDelta = haversineMeters(prev.latitude, prev.longitude, point.latitude, point.longitude)
                val altDelta = point.altitude - prev.altitude
                if (altDelta > MIN_ELEVATION_DELTA_M) elevationDelta = altDelta
                movingTimeMillis += (now - lastLocationTime)
            }
            lastPoint = point
            lastLocationTime = now

            val track = db.trackDao().getTrack(currentTrackId) ?: return@launch
            track.distanceMeters += distanceDelta
            track.elevationGainMeters += elevationDelta
            track.movingTimeMillis = movingTimeMillis
            track.avgSpeedKmh = computeAvgSpeedKmh(track.distanceMeters, movingTimeMillis)
            db.trackDao().updateTrack(track)

            liveDistanceM.postValue(track.distanceMeters)
            liveElevationM.postValue(track.elevationGainMeters)
            liveAvgSpeedKmh.postValue(track.avgSpeedKmh)
            updateNotification("Zaznamenavam trasu...", track.distanceMeters)

            // Best-effort "kde jsem ted" pro web - pri spatnem signalu proste selze,
            // nic se tim neztrati (souradnice uz jsou v lokalni DB a pojedou pres SyncWorker).
            try {
                com.example.cycletracker.network.RetrofitClient.api.pingLocation(
                    com.example.cycletracker.network.LivePositionPing(point.latitude, point.longitude, now)
                )
            } catch (e: Exception) { /* offline - ignorovat, neni kriticke */ }
        }
    }

    private suspend fun updateTrackState(state: String) {
        val track = db.trackDao().getTrack(currentTrackId) ?: return
        track.state = state
        db.trackDao().updateTrack(track)
    }

    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_INTERVAL_MS)
            .setMinUpdateIntervalMillis(LOCATION_INTERVAL_MS)
            .build()
        try {
            fusedClient.requestLocationUpdates(request, locationCallback, mainLooper)
        } catch (e: SecurityException) {
            // Opravneni k poloze chybi - UI by to melo osetrit pred spustenim service.
        }
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CycleTracker::TrackingWakeLock")
        wakeLock?.acquire(12 * 60 * 60 * 1000L) // max 12h pojistka
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) wakeLock?.release()
        wakeLock = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIF_CHANNEL_ID, "Zaznam trasy", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String, distanceM: Double): Notification {
        val km = distanceM / 1000.0
        return NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
            .setContentTitle("Cycle Tracker")
            .setContentText("$text  ${"%.2f".format(km)} km")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String, distanceM: Double) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIF_ID, buildNotification(text, distanceM))
    }

    private fun computeAvgSpeedKmh(distanceM: Double, movingMs: Long): Double {
        if (movingMs <= 0) return 0.0
        val hours = movingMs / 3_600_000.0
        return (distanceM / 1000.0) / hours
    }

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}
