package com.example.cycletracker.db

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface TrackDao {

    @Insert
    suspend fun insertTrack(track: TrackEntity): Long

    @Update
    suspend fun updateTrack(track: TrackEntity)

    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun getTrack(id: Long): TrackEntity?

    @Query("SELECT * FROM tracks ORDER BY startedAt DESC")
    fun getAllTracksLive(): LiveData<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE synced = 0 AND state = :finished")
    suspend fun getUnsyncedTracks(finished: String = TrackEntity.STATE_FINISHED): List<TrackEntity>

    @Insert
    suspend fun insertPoint(point: TrackPointEntity): Long

    @Insert
    suspend fun insertPoints(points: List<TrackPointEntity>)

    @Query("SELECT * FROM track_points WHERE trackId = :trackId ORDER BY timestamp ASC")
    suspend fun getPointsForTrack(trackId: Long): List<TrackPointEntity>

    @Query("SELECT * FROM track_points WHERE trackId = :trackId ORDER BY timestamp ASC")
    fun getPointsForTrackLive(trackId: Long): LiveData<List<TrackPointEntity>>

    @Query("SELECT * FROM track_points WHERE synced = 0 ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getUnsyncedPoints(limit: Int = 500): List<TrackPointEntity>

    @Query("UPDATE track_points SET synced = 1 WHERE id IN (:ids)")
    suspend fun markPointsSynced(ids: List<Long>)

    @Query("UPDATE tracks SET synced = 1, remoteId = :remoteId WHERE id = :trackId")
    suspend fun markTrackSynced(trackId: Long, remoteId: Long)
}
