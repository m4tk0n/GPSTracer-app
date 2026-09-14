package com.example.cycletracker.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.cycletracker.R
import com.example.cycletracker.TrackingService
import com.example.cycletracker.db.AppDatabase
import com.example.cycletracker.db.TrackEntity
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

class MainActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var routeLine: Polyline
    private lateinit var positionMarker: Marker

    private val requiredPermissions = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val fineGranted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (fineGranted) requestBackgroundLocationIfNeeded()
    }

    private val backgroundPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* uzivatel rozhodl, pokracujeme bez ohledu - bez "Vzdy" pojede jen na popredi */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        Configuration.getInstance().userAgentValue = packageName
        super.onCreate(savedInstanceState)

        if (!com.example.cycletracker.SessionManager.isLoggedIn(this)) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        setupMap()
        ensurePermissions()
        wireButtons()
        observeService()
    }

    private fun setupMap() {
        mapView = findViewById(R.id.mapView)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(15.0)
        mapView.controller.setCenter(GeoPoint(50.0755, 14.4378)) // Praha jako vychozi bod

        routeLine = Polyline(mapView)
        mapView.overlays.add(routeLine)

        positionMarker = Marker(mapView)
        positionMarker.title = "Ted jsi tady"
        mapView.overlays.add(positionMarker)
    }

    private fun ensurePermissions() {
        val missing = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        } else {
            requestBackgroundLocationIfNeeded()
        }
    }

    private fun requestBackgroundLocationIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                // Na Androidu se toto MUSI zeptat samostatne, az po udeleni "behem pouzivani"
                backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        }
    }

    private fun wireButtons() {
        findViewById<android.widget.Button>(R.id.btnStart).setOnClickListener {
            startService(Intent(this, TrackingService::class.java).setAction(TrackingService.ACTION_START))
        }
        findViewById<android.widget.Button>(R.id.btnPause).setOnClickListener { btn ->
            val isPaused = TrackingService.liveState.value == TrackEntity.STATE_PAUSED
            val action = if (isPaused) TrackingService.ACTION_RESUME else TrackingService.ACTION_PAUSE
            startService(Intent(this, TrackingService::class.java).setAction(action))
        }
        findViewById<android.widget.Button>(R.id.btnStop).setOnClickListener {
            startService(Intent(this, TrackingService::class.java).setAction(TrackingService.ACTION_STOP))
        }
        findViewById<android.widget.Button>(R.id.btnHistory).setOnClickListener {
            startActivity(Intent(this, TrackListActivity::class.java))
        }
    }

    private fun observeService() {
        TrackingService.liveState.observe(this) { state ->
            val running = state == TrackEntity.STATE_RUNNING
            val paused = state == TrackEntity.STATE_PAUSED
            findViewById<android.widget.Button>(R.id.btnStart).isEnabled = state == TrackEntity.STATE_FINISHED
            findViewById<android.widget.Button>(R.id.btnPause).isEnabled = running || paused
            findViewById<android.widget.Button>(R.id.btnPause).text = if (paused) "Pokracovat" else "Pauza"
            findViewById<android.widget.Button>(R.id.btnStop).isEnabled = running || paused
        }

        TrackingService.liveDistanceM.observe(this) {
            findViewById<android.widget.TextView>(R.id.tvDistance).text = "%.2f km".format(it / 1000.0)
        }
        TrackingService.liveElevationM.observe(this) {
            findViewById<android.widget.TextView>(R.id.tvElevation).text = "%.0f m".format(it)
        }
        TrackingService.liveAvgSpeedKmh.observe(this) {
            findViewById<android.widget.TextView>(R.id.tvAvgSpeed).text = "%.1f km/h".format(it)
        }
        TrackingService.liveLastLocation.observe(this) { loc ->
            if (loc == null) return@observe
            val point = GeoPoint(loc.latitude, loc.longitude)
            positionMarker.position = point
            mapView.controller.animateTo(point)
            mapView.invalidate()
        }
        TrackingService.liveTrackId.observe(this) { trackId ->
            if (trackId == null) return@observe
            observeRoutePoints(trackId)
        }
    }

    private fun observeRoutePoints(trackId: Long) {
        val dao = AppDatabase.getInstance(this).trackDao()
        dao.getPointsForTrackLive(trackId).observe(this) { points ->
            routeLine.setPoints(points.map { GeoPoint(it.latitude, it.longitude) })
            mapView.invalidate()
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
}
