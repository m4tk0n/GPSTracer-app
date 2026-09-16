package com.example.cycletracker.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Jeden zaznam polohy. Uklada se lokalne hned - i kdyz je spatny signal
 * internetu (na GPS signalu nezavisi, jen na pripojeni site pro sync).
 * "synced" rika, jestli uz byl odeslan na server.
 */
@Entity(tableName = "track_points")
data class TrackPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: Long,
    val timestamp: Long,      // epoch millis
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,     // metry n.m., muze byt 0/nepresne bez barometru
    val speedMs: Float,       // m/s z GPS (0 pokud nedostupne)
    val accuracyM: Float,
    var synced: Boolean = false
)
