package com.example.cycletracker.sync

import android.content.Context
import androidx.work.*
import com.example.cycletracker.db.AppDatabase
import com.example.cycletracker.network.PointDto
import com.example.cycletracker.network.RetrofitClient
import com.example.cycletracker.network.TrackDto
import com.example.cycletracker.network.UploadPointsRequest
import java.util.concurrent.TimeUnit

/**
 * Odesila lokalne ulozena data na server, kdyz je pripojeni k internetu.
 * Pri spatnem signalu proste selze a WorkManager to podle "Constraints"
 * sam zkusi znovu, az sit bude dostupna - nic se neztrati, protoze body
 * uz jsou bezpecne ulozene v lokalni Room databazi.
 */
class SyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(applicationContext)
        val dao = db.trackDao()
        val api = RetrofitClient.api

        return try {
            // 1) Nejdriv dokoncene, jeste nesynchronizovane trasy -> zalozit na serveru
            val unsyncedTracks = dao.getUnsyncedTracks()
            for (track in unsyncedTracks) {
                val dto = TrackDto(
                    deviceTrackId = track.id,
                    startedAt = track.startedAt,
                    endedAt = track.endedAt,
                    state = track.state,
                    distanceMeters = track.distanceMeters,
                    elevationGainMeters = track.elevationGainMeters,
                    movingTimeMillis = track.movingTimeMillis,
                    avgSpeedKmh = track.avgSpeedKmh
                )
                val response = api.uploadTrack(dto)
                if (response.isSuccessful) {
                    val remoteId = response.body()?.remoteId ?: continue
                    dao.markTrackSynced(track.id, remoteId)
                }
            }

            // 2) Body po davkach (aby jedna zprava nebyla obri pri spatnem signalu)
            var batch = dao.getUnsyncedPoints()
            while (batch.isNotEmpty()) {
                val dtos = batch.map {
                    PointDto(
                        deviceTrackId = it.trackId,
                        timestamp = it.timestamp,
                        latitude = it.latitude,
                        longitude = it.longitude,
                        altitude = it.altitude,
                        speedMs = it.speedMs
                    )
                }
                val response = api.uploadPoints(UploadPointsRequest(dtos))
                if (!response.isSuccessful) return Result.retry()
                dao.markPointsSynced(batch.map { it.id })
                batch = dao.getUnsyncedPoints()
            }

            Result.success()
        } catch (e: Exception) {
            // Typicky vypadek site / spatny signal - zkus to pozdeji znovu
            Result.retry()
        }
    }

    companion object {
        /** Zavolej jednou napr. v Application.onCreate() pro pravidelnou synchronizaci. */
        fun schedulePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "sync_tracks", ExistingPeriodicWorkPolicy.KEEP, request
            )
        }
    }
}
