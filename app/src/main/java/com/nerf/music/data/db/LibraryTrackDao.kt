package com.nerf.music.data.db

import androidx.room.Dao
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryTrackDao {
    @Query("SELECT * FROM library_tracks")
    fun allTracksFlow(): Flow<List<LibraryTrackEntity>>

    @Query(
        """SELECT library_tracks.*,
                  annual_track_stats.playCount AS recapPlayCount,
                  annual_track_stats.totalListenMs AS recapTotalListenMs,
                  annual_track_stats.lastPlayedAtEpochMs AS recapLastPlayedAtEpochMs
           FROM annual_track_stats
           INNER JOIN library_tracks ON library_tracks.id = annual_track_stats.trackId
           WHERE annual_track_stats.year = :year"""
    )
    fun annualTracksFlow(year: Int): Flow<List<AnnualTrackStatsWithTrack>>

    @Query("SELECT * FROM annual_track_stats")
    suspend fun getAllAnnualStats(): List<AnnualTrackStatsEntity>

    @Query("DELETE FROM annual_track_stats WHERE year < :minimumYear")
    suspend fun pruneAnnualStatsBefore(minimumYear: Int)

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

    @Upsert
    suspend fun upsertAnnualStatsAll(entities: List<AnnualTrackStatsEntity>)

    @Query("DELETE FROM library_tracks")
    suspend fun clearAll()

    @Query("DELETE FROM annual_track_stats")
    suspend fun clearAllAnnualStats()

    @Query("SELECT * FROM annual_track_stats WHERE trackId = :trackId AND year = :year LIMIT 1")
    suspend fun getAnnualStats(trackId: String, year: Int): AnnualTrackStatsEntity?

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
        val annual = getAnnualStats(entity.id, entity.statsYear)
        upsertAnnualStatsAll(
            listOf(
                AnnualTrackStatsEntity(
                    trackId = entity.id,
                    year = entity.statsYear,
                    playCount = (annual?.playCount ?: 0) + 1,
                    totalListenMs = (annual?.totalListenMs ?: 0L) + listenedMs.coerceAtLeast(0L),
                    lastPlayedAtEpochMs = playedAt
                )
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

data class AnnualTrackStatsWithTrack(
    @Embedded val track: LibraryTrackEntity,
    @ColumnInfo(name = "recapPlayCount") val playCount: Int,
    @ColumnInfo(name = "recapTotalListenMs") val totalListenMs: Long,
    @ColumnInfo(name = "recapLastPlayedAtEpochMs") val lastPlayedAtEpochMs: Long?
)
