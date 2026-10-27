package com.nerf.music.data.repository

import android.util.Log
import com.nerf.music.data.model.LyricsData
import com.nerf.music.data.model.SyncedLyricsLine
import com.nerf.music.data.network.InnerTubeCatalogClient
import com.nerf.music.player.KaraokeLyricsEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

fun JSONObject.optCleanString(key: String): String? {
    if (isNull(key)) return null
    val v = optString(key).trim()
    return if (v.isEmpty() || v.equals("null", ignoreCase = true)) null else v
}

interface LyricsRepository {
    suspend fun getLyrics(trackId: String, title: String, artist: String, durationSeconds: Int): Result<LyricsData?>
}

class LyricsRepositoryImpl(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build(),
    private val catalogClient: InnerTubeCatalogClient = InnerTubeCatalogClient()
) : LyricsRepository {

    private val cache = ConcurrentHashMap<String, LyricsData>()
    private val negativeCache = ConcurrentHashMap<String, Boolean>()
    private val lrcPattern = Pattern.compile("""^\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)$""")

    override suspend fun getLyrics(
        trackId: String,
        title: String,
        artist: String,
        durationSeconds: Int
    ): Result<LyricsData?> = withContext(Dispatchers.IO) {
        if (cache.containsKey(trackId)) {
            return@withContext Result.success(cache[trackId])
        }
        if (negativeCache.containsKey(trackId)) {
            return@withContext Result.success(null)
        }

        try {
            val cleanTitle = cleanTrackTitle(title)
            val cleanArtist = cleanArtistName(artist)
            Log.i(TAG, "Iniciando busca de letras para: title='$cleanTitle', artist='$cleanArtist', duration=$durationSeconds")

            val primaryArtist = extractPrimaryArtist(artist)
            Log.i(TAG, "Artista primario extraido: '$primaryArtist' (original: '$cleanArtist')")

            // 1. Tenta consulta direta no lrclib.net (/api/get) com o artista completo
            var lyricsData = fetchExactLrclib(cleanTitle, cleanArtist, durationSeconds, trackId)

            // 2. Se não encontrar e o artista primário for diferente, tenta consulta exata com o artista primário
            if (lyricsData == null && primaryArtist.isNotBlank() && !primaryArtist.equals(cleanArtist, ignoreCase = true)) {
                Log.d(TAG, "Tentando fetchExactLrclib com artista primario: '$cleanTitle' - '$primaryArtist'")
                lyricsData = fetchExactLrclib(cleanTitle, primaryArtist, durationSeconds, trackId)
            }

            // 3. Busca geral com Título + Artista Primário (evita erro 503 e encontra colaborações)
            if (lyricsData == null) {
                val queryPrimary = if (primaryArtist.isNotBlank()) "$cleanTitle $primaryArtist" else cleanTitle
                lyricsData = searchLrclib(queryPrimary, trackId, cleanTitle, cleanArtist, primaryArtist, durationSeconds)
            }

            // 4. Se ainda não encontrou e o artista original tem mais detalhes, tenta busca com o artista completo
            if (lyricsData == null && !cleanArtist.equals(primaryArtist, ignoreCase = true)) {
                lyricsData = searchLrclib("$cleanTitle $cleanArtist", trackId, cleanTitle, cleanArtist, primaryArtist, durationSeconds)
            }

            // 5. Fallback sem sufixos entre parênteses (ex: 'Spaceship (edit)' -> 'Spaceship')
            val strippedTitle = stripParentheses(cleanTitle)
            if (lyricsData == null && strippedTitle.isNotBlank() && !strippedTitle.equals(cleanTitle, ignoreCase = true)) {
                val fallbackQuery = if (primaryArtist.isNotBlank()) "$strippedTitle $primaryArtist" else strippedTitle
                Log.d(TAG, "Tentando busca secundaria sem parenteses: '$fallbackQuery'")
                lyricsData = searchLrclib(fallbackQuery, trackId, strippedTitle, cleanArtist, primaryArtist, durationSeconds)
            }

            // 6. Último recurso: busca pelo título SOMENTE SE validar que o artista ou duração batem com precisão
            if (lyricsData == null && cleanTitle.length >= 3) {
                Log.d(TAG, "Tentando busca apenas pelo titulo com validacao estrita: '$cleanTitle'")
                lyricsData = searchLrclib(cleanTitle, trackId, cleanTitle, cleanArtist, primaryArtist, durationSeconds)
            }

            // 7. Fallback Oficial: Se o LRCLIB não retornou letras válidas, consulta o YouTube Music (Musixmatch)
            if (lyricsData == null && trackId.isNotBlank() && !trackId.startsWith("playlist_")) {
                Log.i(TAG, "Tentando busca de letras via fallback do YouTube Music (Musixmatch) para trackId='$trackId'")
                lyricsData = catalogClient.getTrackLyrics(videoId = trackId).getOrNull()
            }
            Log.i(TAG, "Resultado da busca de letras: isSynced=${lyricsData?.isSynced}, linhas=${lyricsData?.syncedLines?.size}")

            if (lyricsData != null && lyricsData.hasValidLyrics) {
                cache[trackId] = lyricsData
                Result.success(lyricsData)
            } else {
                negativeCache[trackId] = true
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar letras para $title - $artist", e)
            Result.failure(e)
        }
    }

    private fun fetchExactLrclib(
        title: String,
        artist: String,
        durationSeconds: Int,
        trackId: String
    ): LyricsData? {
        return try {
            val encodedTitle = URLEncoder.encode(title, "UTF-8")
            val encodedArtist = URLEncoder.encode(artist, "UTF-8")
            val url = if (durationSeconds > 0) {
                "https://lrclib.net/api/get?artist_name=$encodedArtist&track_name=$encodedTitle&duration=$durationSeconds"
            } else {
                "https://lrclib.net/api/get?artist_name=$encodedArtist&track_name=$encodedTitle"
            }

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "NerfMusicApp/1.0 (Android)")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val bodyStr = response.body?.string() ?: return null
                parseLrclibJson(JSONObject(bodyStr), trackId)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Exceção em fetchExactLrclib: ${e.message}")
            null
        }
    }

    private fun searchLrclib(
        query: String,
        trackId: String,
        expectedTitle: String,
        expectedArtist: String,
        primaryArtist: String,
        expectedDurationSeconds: Int
    ): LyricsData? {
        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://lrclib.net/api/search?q=$encodedQuery"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "NerfMusicApp/1.0 (Android)")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "searchLrclib HTTP status: ${response.code}")
                    return null
                }
                val bodyStr = response.body?.string() ?: return null
                val array = JSONArray(bodyStr)
                Log.i(TAG, "searchLrclib retornou ${array.length()} resultados para '$query'")
                if (array.length() == 0) return null

                // Encontra os candidatos válidos (valida se o artista OU duração são compatíveis para evitar músicas aleatórias)
                var bestCandidate: JSONObject? = null

                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    val candTrack = item.optString("trackName", "").trim()
                    val candArtist = item.optString("artistName", "").trim()
                    val candDuration = item.optDouble("duration", 0.0).toInt()
                    val hasSynced = item.optCleanString("syncedLyrics") != null
                    val hasPlain = item.optCleanString("plainLyrics") != null
                    if (!hasSynced && !hasPlain) continue

                    val artistMatches = isArtistMatch(candArtist, expectedArtist, primaryArtist)
                    val titleMatches = isTitleMatch(candTrack, expectedTitle)
                    val durationMatches = expectedDurationSeconds <= 0 || candDuration <= 0 ||
                            kotlin.math.abs(candDuration - expectedDurationSeconds) <= 6

                    // Validação estrita: se for resultado de busca, DEVE bater o artista, ou bater título e duração com precisão
                    val isValidCandidate = when {
                        artistMatches && titleMatches -> true
                        artistMatches -> true
                        titleMatches && durationMatches && isArtistPartiallyRelated(candArtist, expectedArtist) -> true
                        else -> false
                    }

                    if (isValidCandidate) {
                        if (hasSynced) {
                            return parseLrclibJson(item, trackId)
                        } else if (bestCandidate == null) {
                            bestCandidate = item
                        }
                    }
                }

                bestCandidate?.let { parseLrclibJson(it, trackId) }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Exceção em searchLrclib: ${e.message}")
            null
        }
    }

    private fun isArtistMatch(candArtist: String, expectedArtist: String, primaryArtist: String): Boolean {
        if (candArtist.isBlank() || expectedArtist.isBlank()) return false
        val c = candArtist.lowercase()
        val e = expectedArtist.lowercase()
        val p = primaryArtist.lowercase()
        if (c == e || (p.isNotBlank() && c == p)) return true
        if (c.contains(e) || e.contains(c)) return true
        if (p.isNotBlank() && (c.contains(p) || p.contains(c))) return true
        return false
    }

    private fun isTitleMatch(candTitle: String, expectedTitle: String): Boolean {
        if (candTitle.isBlank() || expectedTitle.isBlank()) return false
        val c = cleanTrackTitle(candTitle).lowercase()
        val e = cleanTrackTitle(expectedTitle).lowercase()
        return c == e || c.contains(e) || e.contains(c)
    }

    private fun isArtistPartiallyRelated(candArtist: String, expectedArtist: String): Boolean {
        val candTokens = candArtist.lowercase().split(Regex("[\\s,&/]+")).filter { it.length > 2 }
        val expTokens = expectedArtist.lowercase().split(Regex("[\\s,&/]+")).filter { it.length > 2 }
        return candTokens.any { it in expTokens }
    }

    private fun parseLrclibJson(obj: JSONObject, trackId: String): LyricsData? {
        val syncedLyrics = obj.optCleanString("syncedLyrics")
        val plainLyrics = obj.optCleanString("plainLyrics")

        if (!syncedLyrics.isNullOrEmpty()) {
            val lines = parseLrc(syncedLyrics)
            if (lines.isNotEmpty()) {
                return LyricsData(
                    trackId = trackId,
                    isSynced = true,
                    plainLyrics = plainLyrics,
                    syncedLines = lines,
                    hasPronunciation = lines.any { !it.pronunciationText.isNullOrBlank() }
                )
            }
        }

        if (!plainLyrics.isNullOrEmpty()) {
            return LyricsData(
                trackId = trackId,
                isSynced = false,
                plainLyrics = plainLyrics,
                syncedLines = emptyList()
            )
        }

        return null
    }

    private fun parseLrc(lrcText: String): List<SyncedLyricsLine> {
        return KaraokeLyricsEngine.parseLrcWithKaraokeTiming(lrcText)
    }

    private fun cleanTrackTitle(title: String): String {
        return title
            .replace(Regex("""\s*[\(\[](?:Official Video|Video Oficial|Audio Oficial|Oficial|Ao Vivo|Lyric Video|Clip Oficial|part\.[^\)\]]+|feat\.[^\)\]]+|ft\.[^\)\]]+)[\)\]]""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*-\s*(?:Official Video|Video Oficial|Ao Vivo|Oficial).*""", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    private fun stripParentheses(title: String): String {
        return title
            .replace(Regex("""\s*[\(\[].*?[\)\]]"""), "")
            .trim()
    }

    private fun cleanArtistName(artist: String): String {
        return artist
            .split("•")[0]
            .replace(Regex("""\s*-\s*(?:Tópico|Topic)""", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    private fun extractPrimaryArtist(artist: String): String {
        val base = cleanArtistName(artist)
        return base
            .split(Regex("""\s+(?:e|&|,|x|vs\.?|and|feat\.?|ft\.?|/)\s+""", RegexOption.IGNORE_CASE))[0]
            .split(",")[0]
            .trim()
    }

    companion object {
        private const val TAG = "LyricsRepository"
    }
}
