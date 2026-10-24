package com.nerf.music.data.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

interface SpotifyCanvasRepository {
    suspend fun getCanvasUrl(title: String, artist: String): Result<String?>
    fun getCachedCanvasUrl(title: String, artist: String): String?
}

class SpotifyCanvasRepositoryImpl(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) : SpotifyCanvasRepository {

    private val tokenMutex = Mutex()
    private var cachedAccessToken: String? = null
    private var accessTokenExpiresAt: Long = 0L

    private var cachedClientToken: String? = null
    private var clientTokenExpiresAt: Long = 0L

    // Cache em memória (ConcurrentHashMap não aceita valores null)
    private val canvasCache = ConcurrentHashMap<String, String>()
    private val negativeCache = ConcurrentHashMap<String, Boolean>()

    override fun getCachedCanvasUrl(title: String, artist: String): String? {
        val cleanTitle = cleanTrackTitle(title)
        val cleanArtist = cleanArtistName(artist)
        val cacheKey = "$cleanTitle|$cleanArtist".lowercase()
        return canvasCache[cacheKey]
    }

    override suspend fun getCanvasUrl(title: String, artist: String): Result<String?> = withContext(Dispatchers.IO) {
        val cleanTitle = cleanTrackTitle(title)
        val cleanArtist = cleanArtistName(artist)
        val cacheKey = "$cleanTitle|$cleanArtist".lowercase()

        canvasCache[cacheKey]?.let { cachedUrl ->
            Log.d(TAG, "Canvas retornado do cache positivo para '$cleanTitle' - '$cleanArtist'")
            return@withContext Result.success(cachedUrl)
        }
        if (negativeCache.containsKey(cacheKey)) {
            Log.d(TAG, "Faixa '$cleanTitle' - '$cleanArtist' está no cache negativo (sem Canvas)")
            return@withContext Result.success(null)
        }

        try {
            Log.i(TAG, "Iniciando resolução de Spotify Canvas para: '$cleanTitle' por '$cleanArtist'")

            val (accessToken, clientToken) = ensureTokens()
            if (accessToken == null || clientToken == null) {
                Log.w(TAG, "Falha temporária ao obter tokens do Spotify Web Player (access=${accessToken != null}, client=${clientToken != null})")
                return@withContext Result.success(null)
            }

            // 1. Pesquisa o trackId na API interna Pathfinder do Spotify
            var trackId = searchSpotifyTrackId(cleanTitle, cleanArtist, accessToken, clientToken)
            var fetchResult = trackId?.let { fetchCanvasWithStatus(it) }

            // 2. Fallback: se o título tiver sufixos entre parênteses como "(extended mix)" e não achou Canvas, tenta o título base
            val strippedTitle = stripParentheses(cleanTitle)
            if ((fetchResult == null || fetchResult is CanvasStatus.NotFound) && strippedTitle.isNotBlank() && !strippedTitle.equals(cleanTitle, ignoreCase = true)) {
                Log.i(TAG, "Tentando busca secundária sem parênteses: '$strippedTitle' por '$cleanArtist'")
                val fallbackTrackId = searchSpotifyTrackId(strippedTitle, cleanArtist, accessToken, clientToken)
                if (!fallbackTrackId.isNullOrBlank() && fallbackTrackId != trackId) {
                    trackId = fallbackTrackId
                    fetchResult = fetchCanvasWithStatus(fallbackTrackId)
                }
            }

            when (fetchResult) {
                is CanvasStatus.Success -> {
                    canvasCache[cacheKey] = fetchResult.url
                    Log.i(TAG, "Canvas MP4 obtido com sucesso para '$cleanTitle' (trackId=$trackId): ${fetchResult.url}")
                    Result.success(fetchResult.url)
                }
                is CanvasStatus.NotFound -> {
                    negativeCache[cacheKey] = true
                    Log.i(TAG, "Faixa '$cleanTitle' (trackId=$trackId) confirmada sem Canvas no Spotify.")
                    Result.success(null)
                }
                is CanvasStatus.ServerError -> {
                    Log.w(TAG, "Falha de servidor ao resolver Canvas para '$cleanTitle' (trackId=$trackId): ${fetchResult.code}. Não adicionado ao cache negativo.")
                    Result.success(null)
                }
                null -> {
                    Log.i(TAG, "Faixa '$cleanTitle' não localizada no catálogo do Spotify.")
                    negativeCache[cacheKey] = true
                    Result.success(null)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar Canvas para $cleanTitle - $cleanArtist: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun ensureTokens(): Pair<String?, String?> = tokenMutex.withLock {
        val now = System.currentTimeMillis()

        // 1. Access Token
        if (cachedAccessToken == null || now >= accessTokenExpiresAt - 60_000L) {
            val tokenData = fetchAnonymousAccessToken()
            if (tokenData != null) {
                cachedAccessToken = tokenData.first
                accessTokenExpiresAt = tokenData.second
            }
        }

        // 2. Client Token
        if (cachedClientToken == null || now >= clientTokenExpiresAt - 60_000L) {
            val clientTokenData = fetchClientToken()
            if (clientTokenData != null) {
                cachedClientToken = clientTokenData.first
                clientTokenExpiresAt = clientTokenData.second
            }
        }

        Pair(cachedAccessToken, cachedClientToken)
    }

    private fun fetchAnonymousAccessToken(): Pair<String, Long>? {
        return try {
            val embedUrl = "https://open.spotify.com/embed/track/0VjIjW4GlUZAMYd2vXMi3b"
            val request = Request.Builder()
                .url(embedUrl)
                .header("User-Agent", USER_AGENT)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "fetchAnonymousAccessToken HTTP ${response.code}")
                    return null
                }
                val html = response.body?.string() ?: return null

                val pattern = Pattern.compile("""<script[^>]*id=["']__NEXT_DATA__["'][^>]*>(.*?)</script>""", Pattern.DOTALL)
                val matcher = pattern.matcher(html)
                if (matcher.find()) {
                    val jsonStr = matcher.group(1) ?: return null
                    val json = JSONObject(jsonStr)
                    val session = json.getJSONObject("props")
                        .getJSONObject("pageProps")
                        .getJSONObject("state")
                        .getJSONObject("settings")
                        .getJSONObject("session")

                    val token = session.getString("accessToken")
                    val expiresAt = session.optLong("accessTokenExpirationTimestampMs", System.currentTimeMillis() + 3600_000L)
                    Pair(token, expiresAt)
                } else {
                    Log.w(TAG, "__NEXT_DATA__ não encontrado no embed HTML")
                    null
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Erro ao buscar anonymous accessToken: ${e.message}")
            null
        }
    }

    private fun fetchClientToken(): Pair<String, Long>? {
        return try {
            val randomBytes = ByteArray(16)
            SecureRandom().nextBytes(randomBytes)
            val deviceId = randomBytes.joinToString("") { "%02x".format(it) }

            val bodyJson = JSONObject().apply {
                put("client_data", JSONObject().apply {
                    put("client_version", "1.2.42.432.g3121")
                    put("client_id", "d8a5ed958d274c2e8ee717e6a4b0971d")
                    put("js_sdk_data", JSONObject().apply {
                        put("device_brand", "Apple")
                        put("device_model", "MacBookPro")
                        put("os", "Mac OS")
                        put("os_version", "10.15.7")
                        put("device_id", deviceId)
                        put("device_type", "computer")
                    })
                })
            }

            // Importante: converter para ByteArray sem MediaType para evitar que o OkHttp anexe '; charset=utf-8'
            // (o servidor clienttoken.spotify.com rejeita charset=utf-8 com HTTP 400)
            val bodyBytes = bodyJson.toString().toByteArray(Charsets.UTF_8)
            val request = Request.Builder()
                .url("https://clienttoken.spotify.com/v1/clienttoken")
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("User-Agent", USER_AGENT)
                .post(bodyBytes.toRequestBody(null))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "fetchClientToken HTTP ${response.code}")
                    return null
                }
                val bodyStr = response.body?.string() ?: return null
                val json = JSONObject(bodyStr)
                val grantedToken = json.getJSONObject("granted_token")
                val token = grantedToken.getString("token")
                val expiresInSec = grantedToken.optLong("expires_after_seconds", 3600L)
                val expiresAt = System.currentTimeMillis() + (expiresInSec * 1000L)
                Pair(token, expiresAt)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Erro ao buscar clientToken: ${e.message}")
            null
        }
    }

    private fun searchSpotifyTrackId(
        title: String,
        artist: String,
        accessToken: String,
        clientToken: String
    ): String? {
        return try {
            val queryStr = "$title $artist".trim()
            val gqlBody = JSONObject().apply {
                put("variables", JSONObject().apply {
                    put("query", queryStr)
                    put("limit", 8)
                    put("numberOfTopResults", 8)
                    put("offset", 0)
                    put("includeAuthors", false)
                    put("includeAlbumPreReleases", false)
                    put("includeEpisodeContentRatingsV2", false)
                })
                put("operationName", "searchSuggestions")
                put("extensions", JSONObject().apply {
                    put("persistedQuery", JSONObject().apply {
                        put("version", 1)
                        put("sha256Hash", "f244254b94c0e824d458ac216e0f8406a73f2f69a9ff52831a2f78fa774ff6ce")
                    })
                })
            }

            val bodyBytes = gqlBody.toString().toByteArray(Charsets.UTF_8)
            val request = Request.Builder()
                .url("https://api-partner.spotify.com/pathfinder/v2/query")
                .header("Authorization", "Bearer $accessToken")
                .header("client-token", clientToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT)
                .post(bodyBytes.toRequestBody(null))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "searchSpotifyTrackId HTTP ${response.code}")
                    return null
                }
                val bodyStr = response.body?.string() ?: return null
                val json = JSONObject(bodyStr)
                val items = json.optJSONObject("data")
                    ?.optJSONObject("searchV2")
                    ?.optJSONObject("topResultsV2")
                    ?.optJSONArray("itemsV2") ?: return null

                for (i in 0 until items.length()) {
                    val itemWrapper = items.getJSONObject(i)
                    val itemData = itemWrapper.optJSONObject("item")?.optJSONObject("data") ?: continue
                    val uri = itemData.optString("uri", "")
                    if (uri.startsWith("spotify:track:")) {
                        return uri.removePrefix("spotify:track:")
                    }
                }
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Erro ao buscar trackId no Pathfinder: ${e.message}")
            null
        }
    }

    private sealed class CanvasStatus {
        data class Success(val url: String) : CanvasStatus()
        object NotFound : CanvasStatus()
        data class ServerError(val code: Int) : CanvasStatus()
    }

    private fun fetchCanvasWithStatus(trackId: String): CanvasStatus {
        var lastError: CanvasStatus = CanvasStatus.NotFound

        for (endpointBase in CANVAS_ENDPOINTS) {
            val url = "$endpointBase$trackId"
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", USER_AGENT)
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyStr = response.body?.string()
                        if (!bodyStr.isNullOrBlank()) {
                            val json = JSONObject(bodyStr)
                            val canvasesList = json.optJSONArray("canvasesList")
                            if (canvasesList != null && canvasesList.length() > 0) {
                                val first = canvasesList.getJSONObject(0)
                                val canvasUrl = first.optString("canvasUrl", "")
                                if (canvasUrl.isNotBlank() && canvasUrl.startsWith("http")) {
                                    return CanvasStatus.Success(canvasUrl)
                                }
                            }
                        }
                        return CanvasStatus.NotFound
                    } else if (response.code == 404) {
                        lastError = CanvasStatus.NotFound
                    } else {
                        Log.w(TAG, "Endpoint $url retornou HTTP ${response.code}. Tentando fallback...")
                        lastError = CanvasStatus.ServerError(response.code)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Falha de conexão com $url: ${e.message}. Tentando fallback...")
                lastError = CanvasStatus.ServerError(-1)
            }
        }

        return lastError
    }

    private fun cleanTrackTitle(title: String): String {
        return title
            .replace(Regex("""\s*[\(\[](?:Official Video|Video Oficial|Audio Oficial|Oficial|Ao Vivo|Lyric Video|Clip Oficial|Visualizer|part\.[^\)\]]+|feat\.[^\)\]]+|ft\.[^\)\]]+)[\)\]]""", RegexOption.IGNORE_CASE), "")
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
            .replace(Regex("""\s*-\s*(?:Tópico|Topic)""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+(?:e|&|,|x|vs\.?)\s+""", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    companion object {
        private const val TAG = "SpotifyCanvasRepository"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
        private val CANVAS_ENDPOINTS = listOf(
            "https://nerf-canvas-api.vercel.app/api/canvas?trackId=",
            "https://spotify-canva.vercel.app/api/canvas?trackId="
        )
    }
}
