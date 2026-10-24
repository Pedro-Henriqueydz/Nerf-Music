package com.nerf.music.data.repository

import android.content.Context
import android.util.Log
import com.nerf.music.data.model.HomeSection
import com.nerf.music.data.model.Track
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

/**
 * Interface para persistência do histórico do ouvinte, preferências de gosto musical,
 * histórico de pesquisas e cache offline do feed da Home.
 */
interface UserHistoryRepository {
    fun getRecentlyPlayed(): List<Track>
    fun recordTrackPlayed(track: Track)
    fun getTopArtists(limit: Int = 5): List<String>
    fun getFavoriteSeedVideoId(): String?
    fun getCachedHomeFeed(): List<HomeSection>
    fun saveCachedHomeFeed(sections: List<HomeSection>)
    fun getTracksByArtist(artistName: String): List<Track>
    fun getSearchHistory(): List<String>
    fun addSearchQuery(query: String)
    fun removeSearchQuery(query: String)
    fun clearSearchHistory()
    fun clearHistory()
    fun getLastSeenVersion(): String?
    fun setLastSeenVersion(version: String)
    fun updateTrackDuration(trackId: String, durationText: String, durationSeconds: Long)
}

/**
 * Implementação local do histórico baseada em SharedPreferences e JSON serializado.
 * Garante que os itens de "Ouvir novamente", buscas recentes, artistas favoritos e cache nunca sejam zerados
 * ao fechar ou reabrir o app.
 */
class UserHistoryRepositoryImpl(
    context: Context
) : UserHistoryRepository {

    companion object {
        private const val TAG = "UserHistoryRepository"
        private const val PREFS_NAME = "nerf_user_history_prefs"
        private const val KEY_RECENTLY_PLAYED = "recently_played_tracks"
        private const val KEY_SEARCH_HISTORY = "search_history_queries"
        private const val KEY_ARTIST_COUNTS = "artist_play_counts"
        private const val KEY_LAST_SEED_VIDEO_ID = "last_seed_video_id"
        private const val KEY_CACHED_HOME = "cached_home_feed"
        private const val KEY_LAST_SEEN_VERSION = "last_seen_app_version"
        private const val MAX_RECENT_TRACKS = 40
        private const val MAX_SEARCH_QUERIES = 25
    }

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val artistCounts = ConcurrentHashMap<String, Int>()

    init {
        loadArtistCounts()
    }

    override fun getRecentlyPlayed(): List<Track> {
        val rawJson = prefs.getString(KEY_RECENTLY_PLAYED, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<Track>>(rawJson)
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao deserializar histórico recente: ${e.message}")
            emptyList()
        }
    }

    override fun recordTrackPlayed(track: Track) {
        if (track.id.isBlank() || track.isAlbumOrPlaylist) return

        try {
            // 1. Atualiza lista de ouvidas recentemente (deduplica, preserva duracao valida e insere no topo)
            val existing = getRecentlyPlayed().firstOrNull { it.id == track.id }
            val mergedTrack = if ((track.durationText == "--:--" || track.durationSeconds <= 0L) && existing != null && existing.durationText != "--:--") {
                track.copy(durationText = existing.durationText, durationSeconds = existing.durationSeconds)
            } else {
                track
            }
            val current = getRecentlyPlayed().filterNot { it.id == track.id }.toMutableList()
            current.add(0, mergedTrack)
            val trimmed = current.take(MAX_RECENT_TRACKS)
            prefs.edit().putString(KEY_RECENTLY_PLAYED, json.encodeToString(trimmed)).apply()

            // 2. Incrementa contador de reprodução do artista
            val artist = track.artist.trim()
            if (artist.isNotBlank() && artist != "--") {
                val newCount = (artistCounts[artist] ?: 0) + 1
                artistCounts[artist] = newCount
                saveArtistCounts()
            }

            // 3. Salva a faixa como semente dinâmica (seed) para rádio do usuário
            prefs.edit().putString(KEY_LAST_SEED_VIDEO_ID, track.id).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao registrar faixa ouvida: ${e.message}", e)
        }
    }

    override fun getTracksByArtist(artistName: String): List<Track> {
        val cleanName = artistName.trim().lowercase()
        if (cleanName.isBlank()) return emptyList()
        return getRecentlyPlayed().filter {
            it.artist.trim().lowercase().contains(cleanName) || cleanName.contains(it.artist.trim().lowercase())
        }
    }

    override fun getTopArtists(limit: Int): List<String> {
        if (artistCounts.isEmpty()) {
            loadArtistCounts()
        }
        return artistCounts.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key }
    }

    override fun getFavoriteSeedVideoId(): String? {
        val lastSeed = prefs.getString(KEY_LAST_SEED_VIDEO_ID, null)
        if (!lastSeed.isNullOrBlank()) return lastSeed
        return getRecentlyPlayed().firstOrNull { it.id.isNotBlank() && !it.isAlbumOrPlaylist }?.id
    }

    override fun getCachedHomeFeed(): List<HomeSection> {
        val rawJson = prefs.getString(KEY_CACHED_HOME, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<HomeSection>>(rawJson)
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao carregar cache da Home: ${e.message}")
            emptyList()
        }
    }

    override fun saveCachedHomeFeed(sections: List<HomeSection>) {
        if (sections.isEmpty()) return
        try {
            prefs.edit().putString(KEY_CACHED_HOME, json.encodeToString(sections)).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao salvar cache da Home: ${e.message}", e)
        }
    }

    override fun getSearchHistory(): List<String> {
        val rawJson = prefs.getString(KEY_SEARCH_HISTORY, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<String>>(rawJson)
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao deserializar histórico de buscas: ${e.message}")
            emptyList()
        }
    }

    override fun addSearchQuery(query: String) {
        val clean = query.trim()
        if (clean.isBlank()) return
        try {
            val current = getSearchHistory().filterNot { it.equals(clean, ignoreCase = true) }.toMutableList()
            current.add(0, clean)
            val trimmed = current.take(MAX_SEARCH_QUERIES)
            prefs.edit().putString(KEY_SEARCH_HISTORY, json.encodeToString(trimmed)).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao salvar busca recente: ${e.message}", e)
        }
    }

    override fun removeSearchQuery(query: String) {
        try {
            val updated = getSearchHistory().filterNot { it.equals(query, ignoreCase = true) }
            prefs.edit().putString(KEY_SEARCH_HISTORY, json.encodeToString(updated)).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao remover busca recente: ${e.message}", e)
        }
    }

    override fun clearSearchHistory() {
        prefs.edit().remove(KEY_SEARCH_HISTORY).apply()
    }

    override fun clearHistory() {
        artistCounts.clear()
        prefs.edit().clear().apply()
    }

    override fun getLastSeenVersion(): String? {
        return prefs.getString(KEY_LAST_SEEN_VERSION, null)
    }

    override fun setLastSeenVersion(version: String) {
        prefs.edit().putString(KEY_LAST_SEEN_VERSION, version).apply()
    }

    private fun loadArtistCounts() {
        val rawJson = prefs.getString(KEY_ARTIST_COUNTS, null) ?: return
        try {
            val map = json.decodeFromString<Map<String, Int>>(rawJson)
            artistCounts.clear()
            artistCounts.putAll(map)
        } catch (e: Exception) {
            Log.w(TAG, "Erro ao carregar contadores de artistas: ${e.message}")
        }
    }

    private fun saveArtistCounts() {
        try {
            val serialized = json.encodeToString(artistCounts.toMap())
            prefs.edit().putString(KEY_ARTIST_COUNTS, serialized).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao salvar contadores de artistas: ${e.message}")
        }
    }
    override fun updateTrackDuration(trackId: String, durationText: String, durationSeconds: Long) {
        if (trackId.isBlank() || durationText.isBlank() || durationText == "--:--") return
        try {
            val current = getRecentlyPlayed().toMutableList()
            var modified = false
            for (i in 0 until current.size) {
                if (current[i].id == trackId && (current[i].durationText == "--:--" || current[i].durationSeconds <= 0L)) {
                    current[i] = current[i].copy(durationText = durationText, durationSeconds = durationSeconds)
                    modified = true
                }
            }
            if (modified) {
                prefs.edit().putString(KEY_RECENTLY_PLAYED, json.encodeToString(current)).apply()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Erro ao atualizar duracao da faixa $trackId: ${e.message}")
        }
    }
}