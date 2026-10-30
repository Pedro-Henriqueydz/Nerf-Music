package com.nerf.music.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.room.withTransaction
import com.nerf.music.BuildConfig
import com.nerf.music.data.db.AppDatabase
import com.nerf.music.data.db.LibraryTrackEntity
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
    val listeningLibrary: List<LibraryTrackEntity>
)

data class BackupRestoreSummary(
    val preferenceCount: Int,
    val trackCount: Int
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
        val backup = NerfMusicBackup(
            appVersion = BuildConfig.VERSION_NAME,
            exportedAtEpochMs = System.currentTimeMillis(),
            preferenceGroups = groups,
            listeningLibrary = tracks
        )
        appContext.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use {
            it.write(json.encodeToString(backup))
        } ?: error("Não foi possível abrir o arquivo de destino.")
        BackupRestoreSummary(groups.values.sumOf { it.size }, tracks.size)
    }

    suspend fun restoreFrom(uri: Uri): BackupRestoreSummary = withContext(Dispatchers.IO) {
        val raw = appContext.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: error("Não foi possível ler o arquivo selecionado.")
        val backup = json.decodeFromString<NerfMusicBackup>(raw)
        require(backup.format == "nerf-music-backup") { "Este arquivo não é um backup do NERF Music." }
        require(backup.schemaVersion == 1) { "Versão de backup ainda não suportada: ${backup.schemaVersion}." }
        require(backup.preferenceGroups.keys.all { it in allowedPreferenceGroups }) { "O backup contém grupos não permitidos." }

        val previousGroups = allowedPreferenceGroups.associateWith { name ->
            snapshotPreferences(appContext.getSharedPreferences(name, Context.MODE_PRIVATE))
        }
        val previousTracks = database.libraryTrackDao().getAll()
        try {
            database.withTransaction {
                database.libraryTrackDao().clearAll()
                database.libraryTrackDao().upsertAll(backup.listeningLibrary)
            }
            backup.preferenceGroups.forEach { (name, entries) ->
                restorePreferences(appContext.getSharedPreferences(name, Context.MODE_PRIVATE), entries)
            }
        } catch (error: Throwable) {
            database.withTransaction {
                database.libraryTrackDao().clearAll()
                database.libraryTrackDao().upsertAll(previousTracks)
            }
            previousGroups.forEach { (name, entries) ->
                restorePreferences(appContext.getSharedPreferences(name, Context.MODE_PRIVATE), entries)
            }
            throw error
        }
        BackupRestoreSummary(backup.preferenceGroups.values.sumOf { it.size }, backup.listeningLibrary.size)
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

    private fun restorePreferences(prefs: SharedPreferences, entries: List<BackupPreferenceEntry>) {
        val editor = prefs.edit().clear()
        entries.forEach { item ->
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
}
