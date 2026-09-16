package com.example.cycletracker.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface OfflineRegionDao {

    @Insert
    suspend fun insert(region: OfflineRegionEntity): Long

    @Delete
    suspend fun delete(region: OfflineRegionEntity)

    @Query("SELECT * FROM offline_regions ORDER BY name ASC")
    fun getAllLive(): LiveData<List<OfflineRegionEntity>>

    @Query("SELECT * FROM offline_regions WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): OfflineRegionEntity?
}
