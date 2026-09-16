package com.example.cycletracker.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Zaznam o stazene offline oblasti mapy. Samotne dlazdice se ukladaji do
 * standardni osmdroid disk cache (viz CacheManager) - tahle tabulka je jen
 * evidence "co uzivatel stahl", aby appka mohla ukazat seznam a umoznit mazani.
 */
@Entity(tableName = "offline_regions")
data class OfflineRegionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double,
    val zoomMin: Int,
    val zoomMax: Int,
    val downloadedAt: Long,
    val tileCount: Int = 0
)
