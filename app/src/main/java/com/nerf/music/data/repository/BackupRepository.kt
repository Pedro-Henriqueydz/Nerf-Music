package com.nerf.music.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.room.withTransaction
import com.nerf.music.BuildConfig
import com.nerf.music.data.db.AppDatabase
import com.nerf.music.data.db.AnnualTrackStatsEntity
import com.nerf.music.data.db.LibraryTrackEntity
import com.nerf.music.data.model.Track
import com.nerf.music.data.model.UserPlaylist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class BackupPreferenceEntry(
    val key: String,
    val type: String,
    val value: String
)

@Serializable
data class NerfMusicBackup(
    val format: String = "nerf-music-backup",
    val schemaVersion: Int = 1,
    val appVersion: String,
    val exportedAtEpochMs: Long,
    val preferenceGroups: Map<String, List<BackupPreferenceEntry>>,
    val listeningLibrary: List<LibraryTrackEntity>,
    val annualListeningStats: List<AnnualTrackStatsEntity> = emptyList()
)

data class BackupRestoreSummary(
    val preferenceCount: Int,
    val trackCount: Int
)

data class BackupPreview(
    val appVersion: String,
    val exportedAtEpochMs: Long,
    val favoriteCount: Int,
    val playlistCount: Int,
    val recentTrackCount: Int,
    val listeningTrackCount: Int,
    val annualStatCount: Int
)

enum class BackupRestoreMode {
    REPLACE,
    MERGE
}

data class BackupPreviewState(
    val isLoading: Boolean = false,
    val preview: BackupPreview? = null,
    val errorMessage: String? = null
)

class BackupRepository(private val context: Context) {
    private val appContext = context.applicationContext
    private val database = AppDatabase.getInstance(appContext)
    private val json = Json { prettyPrint = true; encodeDefaults = true; ignoreUnknownKeys = true }

    private val allowedPreferenceGroups = listOf(
        "nerf_user_settings_prefs",
        "nerf_user_history_prefs",
        "nerf_user_affinity_prefs",
        "nerf_library_prefs"
    )

    suspend fun exportTo(uri: Uri): BackupRestoreSummary = withContext(Dispatchers.IO) {
        val groups = allowedPreferenceGroups.associateWith { name ->
            val excluded = if (name == "nerf_library_prefs") setOf("downloaded_tracks_json")
                else if (name == "nerf_user_history_prefs") setOf("cached_home_feed", "cached_explore_feed")
                else emptySet()
            snapshotPreferences(appContext.getSharedPreferences(name, Context.MODE_PRIVATE), excluded)
        }
        val tracks = database.libraryTrackDao().getAll()
        val annualStats = database.libraryTrackDao().getAllAnnualStats()
        val backup = NerfMusicBackup(
            appVersion = BuildConfig.VERSION_NAME,
            exportedAtEpochMs = System.currentTimeMillis(),
            preferenceGroups = groups,
            listeningLibrary = tracks,
            annualListeningStats = annualStats
        )
        appContext.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use {
            it.write(json.encodeToString(backup))
        } ?: error("Não foi possível abrir o arquivo de destino.")
        BackupRestoreSummary(groups.values.sumOf { it.size }, tracks.size)
    }

    suspend fun inspect(uri: Uri): BackupPreview = withContext(Dispatchers.IO) {
        val backup = readAndValidate(uri)
        val preferences = backup.preferenceGroups.values.flatten().associateBy { it.key }
        BackupPreview(
            appVersion = backup.appVersion,
            exportedAtEpochMs = backup.exportedAtEpochMs,
            favoriteCount = decodeTrackList(preferences[KEY_LIKED_TRACKS]?.value).size,
            playlistCount = decodePlaylistList(preferences[KEY_PLAYLISTS]?.value).size,
            recentTrackCount = decodeTrackList(preferences[KEY_RECENTLY_PLAYED]?.value).size,
            listeningTrackCount = backup.listeningLibrary.size,
            annualStatCount = backup.annualListeningStats.size.takeIf { it > 0 }
                ?: backup.listeningLibrary.count { it.playCount > 0 && it.totalListenMs > it.playCount * 30_000L }
        )
    }

    suspend fun restoreFrom(
        uri: Uri,
        mode: BackupRestoreMode = BackupRestoreMode.REPLACE
    ): BackupRestoreSummary = withContext(Dispatchers.IO) {
        val backup = readAndValidate(uri)
        val previousGroups = allowedPreferenceGroups.associateWith { name ->
            snapshotPreferences(appContext.getSharedPreferences(name, Context.MODE_PRIVATE))
        }
        val restoreGroups = if (mode == BackupRestoreMode.MERGE) {
            backup.preferenceGroups.mapValues { (name, entries) ->
                if (name == SETTINGS_PREFS_NAME) previousGroups[name].orEmpty()
                else mergePreferenceEntries(previousGroups[name].orEmpty(), entries, name)
            }
        } else backup.preferenceGroups

        val previousTracks = database.libraryTrackDao().getAll()
        val previousAnnualStats = database.libraryTrackDao().getAllAnnualStats()
        val backupAnnualStats = backup.annualListeningStats.ifEmpty {
            backup.listeningLibrary
                .filter { it.playCount > 0 && it.totalListenMs > it.playCount * 30_000L }
                .map {
                    AnnualTrackStatsEntity(
                        trackId = it.id,
                        year = it.statsYear,
                        playCount = it.playCount,
                        totalListenMs = it.totalListenMs,
                        lastPlayedAtEpochMs = it.lastPlayedAtEpochMs
                    )
                }
        }
        val restoredTracks = if (mode == BackupRestoreMode.MERGE) {
            mergeLibraryTracks(previousTracks, backup.listeningLibrary)
        } else backup.listeningLibrary
        val restoredAnnualStats = if (mode == BackupRestoreMode.MERGE) {
            mergeAnnualStats(previousAnnualStats, backupAnnualStats)
        } else backupAnnualStats

        try {
            database.withTransaction {
                if (mode == BackupRestoreMode.REPLACE) {
                    database.libraryTrackDao().clearAll()
                    database.libraryTrackDao().clearAllAnnualStats()
                }
                database.libraryTrackDao().upsertAll(restoredTracks)
                database.libraryTrackDao().upsertAnnualStatsAll(restoredAnnualStats)
            }
            restoreGroups.forEach { (name, entries) ->
                restorePreferences(
                    prefs = appContext.getSharedPreferences(name, Context.MODE_PRIVATE),
                    entries = entries,
                    preservedKeys = portableBackupExclusions[name].orEmpty()
                )
            }
        } catch (error: Throwable) {
            database.withTransaction {
                database.libraryTrackDao().clearAll()
                database.libraryTrackDao().clearAllAnnualStats()
                database.libraryTrackDao().upsertAll(previousTracks)
                database.libraryTrackDao().upsertAnnualStatsAll(previousAnnualStats)
            }
            previousGroups.forEach { (name, entries) ->
                restorePreferences(appContext.getSharedPreferences(name, Context.MODE_PRIVATE), entries)
            }
            throw error
        }
        BackupRestoreSummary(restoreGroups.values.sumOf { it.size }, restoredTracks.size)
    }

    private suspend fun readAndValidate(uri: Uri): NerfMusicBackup {
        val raw = appContext.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: error("Nao foi possivel ler o arquivo selecionado.")
        val backup = json.decodeFromString<NerfMusicBackup>(raw)
        require(backup.format == "nerf-music-backup") { "Este arquivo nao e um backup do NERF Music." }
        require(backup.schemaVersion == 1) { "Versao de backup ainda nao suportada: ${backup.schemaVersion}." }
        require(backup.preferenceGroups.keys.all { it in allowedPreferenceGroups }) { "O backup contem grupos de preferencias desconhecidos." }
        require(backup.listeningLibrary.none { it.id.isBlank() }) { "O backup contem faixas sem identificador." }
        require(backup.annualListeningStats.none { it.trackId.isBlank() || it.year <= 0 }) {
            "O backup contem estatisticas de escuta invalidas."
        }
        backup.preferenceGroups.values.flatten().forEach(::validatePreferenceEntry)
        return backup
    }

    private fun validatePreferenceEntry(item: BackupPreferenceEntry) {
        when (item.type) {
            "string" -> Unit
            "boolean" -> item.value.toBooleanStrict()
            "int" -> item.value.toInt()
            "long" -> item.value.toLong()
            "float" -> item.value.toFloat()
            "stringSet" -> Unit
            else -> error("Tipo de preferencia desconhecido: ${item.type}")
        }
        if (item.type != "string") return
        when (item.key) {
            KEY_LIKED_TRACKS, KEY_RECENTLY_PLAYED -> decodeTrackList(item.value)
            KEY_PLAYLISTS -> decodePlaylistList(item.value)
            KEY_SEARCH_HISTORY -> json.decodeFromString<List<String>>(item.value)
            KEY_ARTIST_COUNTS, KEY_TRACK_AFFINITY, KEY_ARTIST_AFFINITY -> json.decodeFromString<Map<String, Int>>(item.value)
        }
    }

    private fun decodeTrackList(raw: String?): List<Track> = raw
        ?.takeIf { it.isNotBlank() }
        ?.let { json.decodeFromString<List<Track>>(it) }
        .orEmpty()

    private fun decodePlaylistList(raw: String?): List<UserPlaylist> = raw
        ?.takeIf { it.isNotBlank() }
        ?.let { json.decodeFromString<List<UserPlaylist>>(it) }
        .orEmpty()

    private fun mergePreferenceEntries(
        currentEntries: List<BackupPreferenceEntry>,
        backupEntries: List<BackupPreferenceEntry>,
        group: String
    ): List<BackupPreferenceEntry> {
        val merged = currentEntries.associateBy { it.key }.toMutableMap()
        val excluded = portableBackupExclusions[group].orEmpty()
        backupEntries.forEach { incoming ->
            if (incoming.key in excluded) return@forEach
            val current = merged[incoming.key]
            if (current == null) {
                merged[incoming.key] = incoming
            } else if (current.type == "string" && incoming.type == "string") {
                val value = when (incoming.key) {
                    KEY_LIKED_TRACKS -> mergeTrackLists(current.value, incoming.value)
                    KEY_RECENTLY_PLAYED -> mergeTrackLists(current.value, incoming.value, MAX_MERGED_HISTORY)
                    KEY_PLAYLISTS -> mergePlaylists(current.value, incoming.value)
                    KEY_SEARCH_HISTORY -> mergeSearchHistory(current.value, incoming.value)
                    KEY_ARTIST_COUNTS, KEY_TRACK_AFFINITY, KEY_ARTIST_AFFINITY ->
                        mergeIntMaps(current.value, incoming.value)
                    else -> current.value
                }
                merged[incoming.key] = current.copy(value = value)
            }
        }
        return merged.values.sortedBy { it.key }
    }

    private fun mergeTrackLists(currentRaw: String, backupRaw: String, limit: Int? = null): String {
        val merged = (decodeTrackList(currentRaw) + decodeTrackList(backupRaw))
            .filter { it.id.isNotBlank() }
            .distinctBy { it.id }
        val limited = limit?.let(merged::take) ?: merged
        return json.encodeToString(limited)
    }

    private fun mergePlaylists(currentRaw: String, backupRaw: String): String {
        val current = decodePlaylistList(currentRaw)
        val backup = decodePlaylistList(backupRaw)
        val backupById = backup.associateBy { it.id }
        val mergedCurrent = current.map { existing ->
            val incoming = backupById[existing.id] ?: return@map existing
            existing.copy(
                tracks = (existing.tracks + incoming.tracks)
                    .filter { it.id.isNotBlank() }
                    .distinctBy { it.id },
                title = existing.title.ifBlank { incoming.title },
                description = existing.description.ifBlank { incoming.description },
                createdAtEpochMs = minOf(existing.createdAtEpochMs, incoming.createdAtEpochMs),
                customThumbnailUrl = existing.customThumbnailUrl ?: incoming.customThumbnailUrl
            )
        }
        val currentIds = current.map { it.id }.toSet()
        val newBackupPlaylists = backup.filter { it.id.isNotBlank() && it.id !in currentIds }.distinctBy { it.id }
        return json.encodeToString(mergedCurrent + newBackupPlaylists)
    }

    private fun mergeSearchHistory(currentRaw: String, backupRaw: String): String {
        val current = json.decodeFromString<List<String>>(currentRaw)
        val backup = json.decodeFromString<List<String>>(backupRaw)
        return json.encodeToString(
            (current + backup).distinctBy { it.trim().lowercase(java.util.Locale.ROOT) }
                .take(MAX_MERGED_SEARCH_HISTORY)
        )
    }

    private fun mergeIntMaps(currentRaw: String, backupRaw: String): String {
        val current = json.decodeFromString<Map<String, Int>>(currentRaw)
        val backup = json.decodeFromString<Map<String, Int>>(backupRaw)
        return json.encodeToString(current.toMutableMap().apply {
            backup.forEach { (key, value) -> merge(key, value) { left, right -> maxOf(left, right) } }
        })
    }

    private fun mergeLibraryTracks(
        current: List<LibraryTrackEntity>,
        backup: List<LibraryTrackEntity>
    ): List<LibraryTrackEntity> {
        val merged = current.associateBy { it.id }.toMutableMap()
        backup.forEach { incoming ->
            val existing = merged[incoming.id]
            merged[incoming.id] = if (existing == null) incoming else existing.copy(
                playCount = maxOf(existing.playCount, incoming.playCount),
                totalListenMs = maxOf(existing.totalListenMs, incoming.totalListenMs),
                statsYear = maxOf(existing.statsYear, incoming.statsYear),
                lastPlayedAtEpochMs = listOfNotNull(existing.lastPlayedAtEpochMs, incoming.lastPlayedAtEpochMs).maxOrNull(),
                libraryAddedAtEpochMs = listOfNotNull(existing.libraryAddedAtEpochMs, incoming.libraryAddedAtEpochMs).minOrNull(),
                likedAtEpochMs = listOfNotNull(existing.likedAtEpochMs, incoming.likedAtEpochMs).minOrNull()
            )
        }
        return merged.values.toList()
    }

    private fun mergeAnnualStats(
        current: List<AnnualTrackStatsEntity>,
        backup: List<AnnualTrackStatsEntity>
    ): List<AnnualTrackStatsEntity> {
        val merged = current.associateBy { it.trackId to it.year }.toMutableMap()
        backup.forEach { incoming ->
            val key = incoming.trackId to incoming.year
            val existing = merged[key]
            merged[key] = if (existing == null) incoming else existing.copy(
                playCount = maxOf(existing.playCount, incoming.playCount),
                totalListenMs = maxOf(existing.totalListenMs, incoming.totalListenMs),
                lastPlayedAtEpochMs = listOfNotNull(existing.lastPlayedAtEpochMs, incoming.lastPlayedAtEpochMs).maxOrNull()
            )
        }
        return merged.values.toList()
    }

    private fun snapshotPreferences(
        prefs: SharedPreferences,
        excludedKeys: Set<String> = emptySet()
    ): List<BackupPreferenceEntry> = prefs.all.entries
        .filterNot { it.key in excludedKeys }
        .mapNotNull { (key, raw) ->
            when (raw) {
                is String -> BackupPreferenceEntry(key, "string", raw)
                is Boolean -> BackupPreferenceEntry(key, "boolean", raw.toString())
                is Int -> BackupPreferenceEntry(key, "int", raw.toString())
                is Long -> BackupPreferenceEntry(key, "long", raw.toString())
                is Float -> BackupPreferenceEntry(key, "float", raw.toString())
                is Set<*> -> BackupPreferenceEntry(key, "stringSet", raw.filterIsInstance<String>().joinToString("\u001f"))
                else -> null
            }
        }
        .sortedBy { it.key }

    private fun restorePreferences(
        prefs: SharedPreferences,
        entries: List<BackupPreferenceEntry>,
        preservedKeys: Set<String> = emptySet()
    ) {
        val preservedEntries = snapshotPreferences(prefs).filter { it.key in preservedKeys }
        val editor = prefs.edit().clear()
        (entries + preservedEntries).distinctBy { it.key }.forEach { item ->
            when (item.type) {
                "string" -> editor.putString(item.key, item.value)
                "boolean" -> editor.putBoolean(item.key, item.value.toBooleanStrict())
                "int" -> editor.putInt(item.key, item.value.toInt())
                "long" -> editor.putLong(item.key, item.value.toLong())
                "float" -> editor.putFloat(item.key, item.value.toFloat())
                "stringSet" -> editor.putStringSet(item.key, item.value.split("\u001f").filter { it.isNotEmpty() }.toSet())
                else -> error("Tipo de preferência desconhecido: ${item.type}")
            }
        }
        check(editor.commit()) { "Falha ao restaurar preferências locais." }
    }

    private val portableBackupExclusions = mapOf(
        "nerf_library_prefs" to setOf("downloaded_tracks_json"),
        "nerf_user_history_prefs" to setOf("cached_home_feed", "cached_explore_feed")
    )

    private companion object {
        const val KEY_LIKED_TRACKS = "liked_tracks_json"
        const val KEY_PLAYLISTS = "user_playlists_json"
        const val KEY_RECENTLY_PLAYED = "recently_played_tracks"
        const val KEY_SEARCH_HISTORY = "search_history_queries"
        const val KEY_ARTIST_COUNTS = "artist_play_counts"
        const val KEY_TRACK_AFFINITY = "key_track_affinity_map"
        const val KEY_ARTIST_AFFINITY = "key_artist_affinity_map"
        const val SETTINGS_PREFS_NAME = "nerf_user_settings_prefs"
        const val MAX_MERGED_HISTORY = 40
        const val MAX_MERGED_SEARCH_HISTORY = 25
    }
}
