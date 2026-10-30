package com.nerf.music.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryTrackDao {
    @Query("SELECT * FROM library_tracks")
    fun allTracksFlow(): Flow<List<LibraryTrackEntity>>

    @Query("SELECT * FROM library_tracks WHERE playCount >= :minimumPlays ORDER BY playCount DESC, totalListenMs DESC, lastPlayedAtEpochMs DESC LIMIT :limit")
    fun mostPlayedFlow(minimumPlays: Int = 3, limit: Int = 100): Flow<List<LibraryTrackEntity>>

    @Query("SELECT * FROM library_tracks WHERE libraryAddedAtEpochMs IS NOT NULL ORDER BY libraryAddedAtEpochMs DESC LIMIT :limit")
    fun recentlyAddedFlow(limit: Int = 100): Flow<List<LibraryTrackEntity>>

    @Query("SELECT * FROM library_tracks WHERE likedAtEpochMs IS NOT NULL AND lastPlayedAtEpochMs IS NOT NULL AND lastPlayedAtEpochMs < :beforeEpochMs ORDER BY playCount DESC, lastPlayedAtEpochMs ASC LIMIT :limit")
    fun forgottenFavoritesFlow(beforeEpochMs: Long, limit: Int = 20): Flow<List<LibraryTrackEntity>>

    @Query("SELECT * FROM library_tracks")
    suspend fun getAll(): List<LibraryTrackEntity>

    @Query("SELECT * FROM library_tracks WHERE id = :trackId LIMIT 1")
    suspend fun getById(trackId: String): LibraryTrackEntity?

    @Upsert
    suspend fun upsert(entity: LibraryTrackEntity)

    @Upsert
    suspend fun upsertAll(entities: List<LibraryTrackEntity>)

    @Query("DELETE FROM library_tracks")
    suspend fun clearAll()

    @Transaction
    suspend fun recordPlay(entity: LibraryTrackEntity, listenedMs: Long, playedAt: Long) {
        val existing = getById(entity.id)
        val sameYear = existing?.statsYear == entity.statsYear
        upsert(
            entity.copy(
                playCount = (if (sameYear) existing?.playCount ?: 0 else 0) + 1,
                totalListenMs = (if (sameYear) existing?.totalListenMs ?: 0L else 0L) + listenedMs.coerceAtLeast(0L),
                lastPlayedAtEpochMs = playedAt,
                libraryAddedAtEpochMs = existing?.libraryAddedAtEpochMs,
                likedAtEpochMs = existing?.likedAtEpochMs
            )
        )
    }

    @Transaction
    suspend fun markAdded(entity: LibraryTrackEntity, addedAt: Long, liked: Boolean) {
        val existing = getById(entity.id)
        upsert(
            entity.copy(
                playCount = existing?.playCount ?: 0,
                totalListenMs = existing?.totalListenMs ?: 0L,
                statsYear = existing?.statsYear ?: entity.statsYear,
                lastPlayedAtEpochMs = existing?.lastPlayedAtEpochMs,
                libraryAddedAtEpochMs = existing?.libraryAddedAtEpochMs ?: addedAt,
                likedAtEpochMs = if (liked) addedAt else existing?.likedAtEpochMs
            )
        )
    }

    @Query("UPDATE library_tracks SET likedAtEpochMs = NULL WHERE id = :trackId")
    suspend fun clearLiked(trackId: String)
}
