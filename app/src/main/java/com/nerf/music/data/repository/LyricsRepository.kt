package com.nerf.music.data.repository

import android.util.Log
import com.nerf.music.data.model.LyricsData
import com.nerf.music.data.model.SyncedLyricsLine
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

interface LyricsRepository {
    suspend fun getLyrics(trackId: String, title: String, artist: String, durationSeconds: Int): Result<LyricsData?>
}

class LyricsRepositoryImpl(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()
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

            // 1. Tenta consulta direta no lrclib.net (/api/get)
            var lyricsData = fetchExactLrclib(cleanTitle, cleanArtist, durationSeconds, trackId)

            // 2. Se não encontrar, tenta busca geral (/api/search?q=...)
            if (lyricsData == null) {
                lyricsData = searchLrclib("$cleanTitle $cleanArtist", trackId)
            }
            Log.i(TAG, "Resultado da busca de letras: isSynced=${lyricsData?.isSynced}, linhas=${lyricsData?.syncedLines?.size}")

            if (lyricsData != null) {
                cache[trackId] = lyricsData
            } else {
                negativeCache[trackId] = true
            }
            Result.success(lyricsData)
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

    private fun searchLrclib(query: String, trackId: String): LyricsData? {
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

                // Procura primeiro por um que tenha letras sincronizadas
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    val parsed = parseLrclibJson(item, trackId)
                    if (parsed != null && parsed.isSynced) {
                        return parsed
                    }
                }
                // Senão retorna o primeiro disponível
                parseLrclibJson(array.getJSONObject(0), trackId)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Exceção em searchLrclib: ${e.message}")
            null
        }
    }

    private fun parseLrclibJson(obj: JSONObject, trackId: String): LyricsData? {
        val syncedLyrics = obj.optString("syncedLyrics", "").trim()
        val plainLyrics = obj.optString("plainLyrics", "").trim()

        if (syncedLyrics.isNotEmpty()) {
            val lines = parseLrc(syncedLyrics)
            if (lines.isNotEmpty()) {
                return LyricsData(
                    trackId = trackId,
                    isSynced = true,
                    plainLyrics = plainLyrics.ifEmpty { null },
                    syncedLines = lines
                )
            }
        }

        if (plainLyrics.isNotEmpty()) {
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
        val list = mutableListOf<SyncedLyricsLine>()
        lrcText.lines().forEach { line ->
            val trimmed = line.trim()
            val matcher = lrcPattern.matcher(trimmed)
            if (matcher.matches()) {
                val min = matcher.group(1)?.toLongOrNull() ?: 0L
                val sec = matcher.group(2)?.toLongOrNull() ?: 0L
                val millisRaw = matcher.group(3) ?: "0"
                val ms = if (millisRaw.length == 2) {
                    millisRaw.toLong() * 10L
                } else {
                    millisRaw.toLongOrNull() ?: 0L
                }
                val totalMs = (min * 60 * 1000L) + (sec * 1000L) + ms
                val text = matcher.group(4)?.trim().orEmpty()
                if (text.isNotBlank()) {
                    list.add(SyncedLyricsLine(timeMs = totalMs, text = text))
                }
            }
        }
        return list.sortedBy { it.timeMs }
    }

    private fun cleanTrackTitle(title: String): String {
        return title
            .replace(Regex("""\s*[\(\[](?:Official Video|Video Oficial|Audio Oficial|Oficial|Ao Vivo|Lyric Video|Clip Oficial|part\.[^\)\]]+|feat\.[^\)\]]+|ft\.[^\)\]]+)[\)\]]""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*-\s*(?:Official Video|Video Oficial|Ao Vivo|Oficial).*""", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    private fun cleanArtistName(artist: String): String {
        return artist
            .split("•")[0]
            .replace(Regex("""\s*-\s*Tópico""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*-\s*Topic""", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    companion object {
        private const val TAG = "LyricsRepository"
    }
}
