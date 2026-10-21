package com.nerf.music.data.repository

import android.content.Context
import android.util.Log
import com.nerf.music.data.model.HomeSection
import com.nerf.music.data.model.Track
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

/**
 * Interface para persistência do histórico do ouvinte, preferências de gosto musical
 * e cache offline do feed da Home.
 */
interface UserHistoryRepository {
    fun getRecentlyPlayed(): List<Track>
    fun recordTrackPlayed(track: Track)
    fun getTopArtists(limit: Int = 5): List<String>
    fun getFavoriteSeedVideoId(): String?
    fun getCachedHomeFeed(): List<HomeSection>
    fun saveCachedHomeFeed(sections: List<HomeSection>)
    fun clearHistory()
}

/**
 * Implementação local do histórico baseada em SharedPreferences e JSON serializado.
 * Garante que os itens de "Ouvir novamente", artistas favoritos e cache nunca sejam zerados
 * ao fechar ou reabrir o app.
 */
class UserHistoryRepositoryImpl(
    context: Context
) : UserHistoryRepository {

    companion object {
        private const val TAG = "UserHistoryRepository"
        private const val PREFS_NAME = "nerf_user_history_prefs"
        private const val KEY_RECENTLY_PLAYED = "recently_played_tracks"
        private const val KEY_ARTIST_COUNTS = "artist_play_counts"
        private const val KEY_LAST_SEED_VIDEO_ID = "last_seed_video_id"
        private const val KEY_CACHED_HOME = "cached_home_feed"
        private const val MAX_RECENT_TRACKS = 40
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
        // Ignora cards que não sejam faixas individuais reproduzíveis
        if (track.id.isBlank() || track.isAlbumOrPlaylist) return

        try {
            // 1. Atualiza lista de ouvidas recentemente (deduplica e insere no topo)
            val current = getRecentlyPlayed().filterNot { it.id == track.id }.toMutableList()
            current.add(0, track)
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

        // Fallback: primeira faixa válida do histórico
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

    override fun clearHistory() {
        artistCounts.clear()
        prefs.edit().clear().apply()
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
}
