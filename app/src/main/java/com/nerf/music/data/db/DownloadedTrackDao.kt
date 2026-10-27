package com.nerf.music.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadedTrackDao {
    @Query("SELECT * FROM downloaded_tracks ORDER BY downloadedAtEpochMs DESC")
    fun getAllTracksFlow(): Flow<List<DownloadedTrackEntity>>

    @Query("SELECT * FROM downloaded_tracks ORDER BY downloadedAtEpochMs DESC")
    suspend fun getAllTracks(): List<DownloadedTrackEntity>

    @Query("SELECT * FROM downloaded_tracks WHERE id = :trackId LIMIT 1")
    suspend fun getTrackById(trackId: String): DownloadedTrackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(track: DownloadedTrackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tracks: List<DownloadedTrackEntity>)

    @Update
    suspend fun update(track: DownloadedTrackEntity)

    @Delete
    suspend fun delete(track: DownloadedTrackEntity)

    @Query("DELETE FROM downloaded_tracks WHERE id = :trackId")
    suspend fun deleteById(trackId: String)

    @Query("DELETE FROM downloaded_tracks WHERE id NOT IN (:validIds)")
    suspend fun deleteExcept(validIds: List<String>)

    @Query("DELETE FROM downloaded_tracks")
    suspend fun clearAll()
}
