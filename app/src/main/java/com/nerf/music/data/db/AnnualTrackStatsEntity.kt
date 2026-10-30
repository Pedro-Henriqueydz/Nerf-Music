package com.nerf.music.data.db

import androidx.room.Entity
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "annual_track_stats", primaryKeys = ["trackId", "year"])
data class AnnualTrackStatsEntity(
    val trackId: String,
    val year: Int,
    val playCount: Int,
    val totalListenMs: Long,
    val lastPlayedAtEpochMs: Long?
)
