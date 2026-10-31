package com.nerf.music.data.repository

import android.content.Context
import android.util.Log
import com.nerf.music.data.model.Track
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.Normalizer
import java.util.concurrent.ConcurrentHashMap

private fun artistMatchKey(value: String): String = Normalizer
    .normalize(value.trim(), Normalizer.Form.NFD)
    .replace("\\p{M}+".toRegex(), "")
    .lowercase(java.util.Locale.ROOT)
    .filter(Char::isLetterOrDigit)

/**
 * Repositório inteligente de afinidade musical local do ouvinte.
 *
 * Constrói um grafo de gostos baseado no comportamento real de audição:
 * - Músicas ouvidas até o fim ou repetidas ganham alta pontuação positiva.
 * - Músicas puladas rapidamente (Skip Penalty) perdem pontos e são filtradas das rádios.
 * - Músicas favoritadas recebem o maior bônus de afinidade.
 * - Artistas recebem pontuações agregadas para guiar recomendações contextualizadas.
 */
interface UserAffinityRepository {
    fun recordTrackStarted(track: Track)
    fun recordTrackEnded(track: Track, positionMs: Long, durationMs: Long, wasSkipped: Boolean)
    fun recordTrackLiked(track: Track)
    fun recordTrackUnliked(track: Track)

    fun getTrackAffinity(trackId: String): Int
    fun getArtistAffinity(artistName: String): Int
    fun isTrackPenalized(trackId: String): Boolean
    fun getTopAffinitiveArtists(limit: Int = 10): List<String>
    fun getPenalizedArtists(): Set<String>
    fun getExcludedArtists(): Set<String>
    fun setArtistExcluded(artistName: String, excluded: Boolean)
    fun clearArtistExclusions()
    fun isArtistPenalized(artistName: String): Boolean {
        val blocked = getPenalizedArtists().map(::artistMatchKey).toSet()
        val explicitlyExcluded = getExcludedArtists().map(::artistMatchKey).toSet()
        val name = artistMatchKey(artistName)
        val contributors = com.nerf.music.data.model.ReleaseRecommendations.extractContributors(artistName)
            .map(::artistMatchKey)
        return name in blocked || name in explicitlyExcluded || contributors.any { it in blocked || it in explicitlyExcluded }
    }
    fun recordInitialArtistPreferences(artists: List<String>)
}

class UserAffinityRepositoryImpl(context: Context) : UserAffinityRepository {

    companion object {
        private const val TAG = "UserAffinityRepo"
        private const val PREFS_NAME = "nerf_user_affinity_prefs"
        private const val KEY_TRACK_AFFINITY = "key_track_affinity_map"
        private const val KEY_ARTIST_AFFINITY = "key_artist_affinity_map"
        private const val KEY_EXCLUDED_ARTISTS = "key_excluded_artists"

        // Limiares de pontuação
        private const val SCORE_COMPLETED = 4
        private const val SCORE_PARTIAL_LISTEN = 1
        private const val SCORE_QUICK_SKIP = -4
        private const val SCORE_LIKED = 12
        private const val SCORE_UNLIKED = -5
        private const val PENALTY_THRESHOLD = -4
    }

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    private val trackAffinityMap = ConcurrentHashMap<String, Int>()
    private val artistAffinityMap = ConcurrentHashMap<String, Int>()
    private val excludedArtists = ConcurrentHashMap.newKeySet<String>()

    init {
        loadData()
    }

    private fun loadData() {
        try {
            val rawTracks = prefs.getString(KEY_TRACK_AFFINITY, null)
            if (!rawTracks.isNullOrBlank()) {
                val map = json.decodeFromString<Map<String, Int>>(rawTracks)
                trackAffinityMap.putAll(map)
            }

            val rawArtists = prefs.getString(KEY_ARTIST_AFFINITY, null)
            if (!rawArtists.isNullOrBlank()) {
                val map = json.decodeFromString<Map<String, Int>>(rawArtists)
                artistAffinityMap.putAll(map)
            }
            excludedArtists.addAll(prefs.getStringSet(KEY_EXCLUDED_ARTISTS, emptySet()).orEmpty())
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao carregar afinidade inicial: ${e.message}")
        }
    }

    private fun persistData() {
        try {
            prefs.edit()
                .putString(KEY_TRACK_AFFINITY, json.encodeToString(trackAffinityMap.toMap()))
                .putString(KEY_ARTIST_AFFINITY, json.encodeToString(artistAffinityMap.toMap()))
                .putStringSet(KEY_EXCLUDED_ARTISTS, excludedArtists.toSet())
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao salvar afinidade: ${e.message}")
        }
    }

    override fun recordTrackStarted(track: Track) {
        // Marcador inicial
    }

    override fun recordTrackEnded(track: Track, positionMs: Long, durationMs: Long, wasSkipped: Boolean) {
        if (track.id.isBlank() || track.isAlbumOrPlaylist) return

        val duration = if (durationMs > 0) durationMs else 180_000L
        val fraction = (positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        val listenedSeconds = positionMs / 1000

        var delta = 0
        when {
            // Pulo rápido (menos de 25 segundos ou menos de 15% da música)
            wasSkipped && (listenedSeconds < 25 || fraction < 0.15f) -> {
                delta = SCORE_QUICK_SKIP
                Log.d(TAG, "Skip Penalty aplicado em '${track.title}': $delta")
            }
            // Audição completa (> 80% ou concluída sem skip)
            !wasSkipped || fraction >= 0.80f -> {
                delta = SCORE_COMPLETED
                Log.d(TAG, "Completion Boost em '${track.title}': +$delta")
            }
            // Audição parcial relevante (> 40%)
            fraction >= 0.40f -> {
                delta = SCORE_PARTIAL_LISTEN
            }
        }

        if (delta != 0) {
            val currentTrackScore = trackAffinityMap[track.id] ?: 0
            val newTrackScore = (currentTrackScore + delta).coerceIn(-20, 50)
            trackAffinityMap[track.id] = newTrackScore

            val cleanArtist = track.artist.trim()
            if (cleanArtist.isNotBlank() && cleanArtist != "--") {
                val currentArtistScore = artistAffinityMap[cleanArtist] ?: 0
                val artistDelta = if (delta > 0) 1 else -1
                artistAffinityMap[cleanArtist] = (currentArtistScore + artistDelta).coerceIn(-30, 100)
            }

            persistData()
        }
    }

    override fun recordTrackLiked(track: Track) {
        if (track.id.isBlank()) return
        val currentScore = trackAffinityMap[track.id] ?: 0
        trackAffinityMap[track.id] = (currentScore + SCORE_LIKED).coerceIn(-20, 50)

        val cleanArtist = track.artist.trim()
        if (cleanArtist.isNotBlank()) {
            val currentArtistScore = artistAffinityMap[cleanArtist] ?: 0
            artistAffinityMap[cleanArtist] = (currentArtistScore + 3).coerceIn(-30, 100)
        }
        persistData()
    }

    override fun recordTrackUnliked(track: Track) {
        if (track.id.isBlank()) return
        val currentScore = trackAffinityMap[track.id] ?: 0
        trackAffinityMap[track.id] = (currentScore + SCORE_UNLIKED).coerceIn(-20, 50)
        persistData()
    }

    override fun getTrackAffinity(trackId: String): Int {
        return trackAffinityMap[trackId] ?: 0
    }

    override fun getArtistAffinity(artistName: String): Int {
        val clean = artistName.trim()
        return artistAffinityMap[clean] ?: 0
    }

    override fun isTrackPenalized(trackId: String): Boolean {
        return (trackAffinityMap[trackId] ?: 0) <= PENALTY_THRESHOLD
    }

    override fun recordInitialArtistPreferences(artists: List<String>) {
        if (artists.isEmpty()) return
        artists.forEach { artistName ->
            val clean = artistName.trim()
            if (clean.isNotBlank()) {
                if (artistAffinityMap.keys.none { it.equals(clean, ignoreCase = true) }) {
                    artistAffinityMap.putIfAbsent(clean, 100)
                }
            }
        }
        persistData()
        Log.d(TAG, "Preferências iniciais de artistas registradas: $artists")
    }

    override fun getTopAffinitiveArtists(limit: Int): List<String> {
        return artistAffinityMap.entries
            .filter { it.value > 0 }
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key }
    }

    override fun getPenalizedArtists(): Set<String> {
        return artistAffinityMap.entries
            .filter { it.value <= -3 }
            .map { it.key }
            .toSet()
    }

    override fun isArtistPenalized(artistName: String): Boolean {
        val excludedNow = prefs.getStringSet(KEY_EXCLUDED_ARTISTS, emptySet()).orEmpty()
        val name = artistMatchKey(artistName)
        val excludedNames = excludedNow.map(::artistMatchKey).toSet()
        val penalizedNames = getPenalizedArtists().map(::artistMatchKey).toSet()
        val contributors = com.nerf.music.data.model.ReleaseRecommendations.extractContributors(artistName)
            .map(::artistMatchKey)
        return name in excludedNames || name in penalizedNames ||
            contributors.any { it in excludedNames || it in penalizedNames }
    }

    override fun getExcludedArtists(): Set<String> = excludedArtists
        .distinctBy(::artistMatchKey)
        .toSet()

    override fun setArtistExcluded(artistName: String, excluded: Boolean) {
        val clean = artistName.trim()
        if (clean.isBlank()) return
        if (excluded) {
            if (excludedArtists.none { artistMatchKey(it) == artistMatchKey(clean) }) excludedArtists.add(clean)
        } else {
            excludedArtists.removeIf { artistMatchKey(it) == artistMatchKey(clean) }
        }
        persistData()
    }

    override fun clearArtistExclusions() {
        excludedArtists.clear()
        persistData()
    }
}
