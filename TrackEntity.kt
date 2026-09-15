package com.example.cycletracker.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Jedna ulozena trasa (jizda). Vzdalenost, prevyseni a prumerna rychlost
 * se prubezne dopocitavaji v TrackingService a ukladaji sem, aby se
 * nemusely pocitat znovu pri kazdem zobrazeni seznamu.
 */
@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remoteId: Long? = null,       // ID na serveru, az se synchronizuje
    val startedAt: Long,              // epoch millis
    var endedAt: Long? = null,
    var state: String = STATE_RUNNING, // RUNNING, PAUSED, FINISHED
    var distanceMeters: Double = 0.0,
    var elevationGainMeters: Double = 0.0,
    var movingTimeMillis: Long = 0L,  // cas bez pauz - pro prumernou rychlost
    var avgSpeedKmh: Double = 0.0,
    var synced: Boolean = false
) {
    companion object {
        const val STATE_RUNNING = "RUNNING"
        const val STATE_PAUSED = "PAUSED"
        const val STATE_FINISHED = "FINISHED"
    }
}
