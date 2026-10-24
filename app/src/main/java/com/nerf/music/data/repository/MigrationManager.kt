package com.nerf.music.data.repository

import android.content.Context
import android.util.Log
import com.nerf.music.data.model.SearchFilter
import com.nerf.music.data.model.Track
import com.nerf.music.data.model.UserPlaylist
import com.nerf.music.data.network.InnerTubeCatalogClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Gerenciador da Central de Migração e Importação de Streaming do NERF Music.
 *
 * Suporta 2 modalidades completas com paridade funcional:
 * 1. Modo Anônimo (Sem Login):
 *    - Importação de links públicos do Spotify (via Embed público sem chaves de API).
 *    - Importação de links de playlists do YouTube Music (via InnerTube).
 *    - Importação de arquivos CSV, JSON ou texto puro (Soundiiz, TuneMyMusic ou backup).
 *    - Motor de Fuzzy Matching com normalização acústica para encontrar versões originais de estúdio.
 * 2. Modo Conexão Direta (Com Login Seguro):
 *    - Sessão oficial do YouTube Music / Google para sincronização de Músicas Curtidas e Playlists.
 *    - Sessão oficial do Spotify para importação de Liked Songs e Playlists.
 *
 * Em ambos os modos, a importação injeta automaticamente os dados no DNA acústico do ouvinte
 * (artistas mais ouvidos e sementes favoritas), transformando a Home instantaneamente.
 */
class MigrationManager(
    private val context: Context,
    private val catalogClient: InnerTubeCatalogClient = InnerTubeCatalogClient(),
    private val libraryRepository: LibraryRepository,
    private val userHistoryRepository: UserHistoryRepository,
    private val userAffinityRepository: UserAffinityRepository = UserAffinityRepositoryImpl(context)
) {
    companion object {
        private const val TAG = "MigrationManager"
        private const val PREFS_NAME = "nerf_migration_prefs"
        private const val KEY_YTM_COOKIES = "ytm_auth_cookies"
        private const val KEY_SPOTIFY_TOKEN = "spotify_auth_token"
        private const val KEY_YTM_ACCOUNT_NAME = "ytm_account_name"
        private const val KEY_SPOTIFY_ACCOUNT_NAME = "spotify_account_name"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    // ---------------------------------------------------------------------------------------------
    // ESTADO DE CONTAS CONECTADAS
    // ---------------------------------------------------------------------------------------------

    fun isYouTubeMusicConnected(): Boolean = !prefs.getString(KEY_YTM_COOKIES, null).isNullOrBlank()

    fun getYouTubeMusicAccountName(): String = prefs.getString(KEY_YTM_ACCOUNT_NAME, "Conta Google Conectada") ?: "Conta Google Conectada"

    fun saveYouTubeMusicSession(cookies: String, accountName: String = "Conta Google Conectada") {
        prefs.edit()
            .putString(KEY_YTM_COOKIES, cookies)
            .putString(KEY_YTM_ACCOUNT_NAME, accountName)
            .apply()
        Log.i(TAG, "Sessão do YouTube Music salva com sucesso.")
    }

    fun disconnectYouTubeMusic() {
        prefs.edit()
            .remove(KEY_YTM_COOKIES)
            .remove(KEY_YTM_ACCOUNT_NAME)
            .apply()
        Log.i(TAG, "Sessão do YouTube Music desconectada.")
    }

    fun isSpotifyConnected(): Boolean = !prefs.getString(KEY_SPOTIFY_TOKEN, null).isNullOrBlank()

    fun getSpotifyAccountName(): String = prefs.getString(KEY_SPOTIFY_ACCOUNT_NAME, "Conta Spotify Conectada") ?: "Conta Spotify Conectada"

    fun saveSpotifySession(tokenOrCookies: String, accountName: String = "Conta Spotify Conectada") {
        prefs.edit()
            .putString(KEY_SPOTIFY_TOKEN, tokenOrCookies)
            .putString(KEY_SPOTIFY_ACCOUNT_NAME, accountName)
            .apply()
        Log.i(TAG, "Sessão do Spotify salva com sucesso.")
    }

    fun disconnectSpotify() {
        prefs.edit()
            .remove(KEY_SPOTIFY_TOKEN)
            .remove(KEY_SPOTIFY_ACCOUNT_NAME)
            .apply()
        Log.i(TAG, "Sessão do Spotify desconectada.")
    }

    // ---------------------------------------------------------------------------------------------
    // 1. SINCRONIZAÇÃO VIA CONEXÃO DIRETA: YOUTUBE MUSIC
    // ---------------------------------------------------------------------------------------------

    suspend fun syncYouTubeMusicAccount(
        onProgress: (current: Int, total: Int, trackTitle: String) -> Unit
    ): Result<Int> = withContext(Dispatchers.IO) {
        val cookies = prefs.getString(KEY_YTM_COOKIES, null)
            ?: return@withContext Result.failure(IllegalStateException("Nenhuma sessão do YouTube Music encontrada. Faça login primeiro."))

        try {
            var totalSynced = 0
            val sapisid = extractCookieValue(cookies, "SAPISID")
                ?: extractCookieValue(cookies, "__Secure-3PAPISID")
                ?: extractCookieValue(cookies, "__Secure-1PAPISID")
                ?: ""

            val origin = "https://music.youtube.com"
            val now = System.currentTimeMillis() / 1000
            val sha1Hash = computeSha1("$now $sapisid $origin")
            val authHeader = "SAPISIDHASH ${now}_$sha1Hash"

            // 1. Sincroniza Músicas Curtidas (FEmusic_liked_videos)
            onProgress(0, 1, "Buscando Músicas Curtidas do YouTube Music...")
            val likedPayload = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", "1.20240909.01.00")
                        put("hl", "pt")
                        put("gl", "BR")
                    })
                })
                put("browseId", "FEmusic_liked_videos")
            }

            val likedRequest = Request.Builder()
                .url("https://music.youtube.com/youtubei/v1/browse?prettyPrint=false")
                .post(likedPayload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
                .header("Cookie", cookies)
                .header("Authorization", authHeader)
                .header("X-Origin", origin)
                .header("Origin", origin)
                .build()

            val likedResp = httpClient.newCall(likedRequest).execute()
            val likedTracks = mutableListOf<Track>()
            if (likedResp.isSuccessful) {
                val bodyStr = likedResp.body?.string().orEmpty()
                likedTracks.addAll(parseYtmLikedTracksJson(bodyStr))
            }

            for ((idx, track) in likedTracks.withIndex()) {
                onProgress(idx + 1, likedTracks.size, "Curtidas: ${track.artist} - ${track.title}")
                if (!libraryRepository.isFavorite(track.id)) {
                    libraryRepository.toggleFavorite(track)
                }
                totalSynced++
            }

            if (likedTracks.isNotEmpty()) {
                injectImportedPreferences(likedTracks)
            }

            // 2. Sincroniza Playlists do Usuário (FEmusic_liked_playlists)
            onProgress(0, 1, "Buscando Playlists do YouTube Music...")
            val playlistsPayload = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", "1.20240909.01.00")
                        put("hl", "pt")
                        put("gl", "BR")
                    })
                })
                put("browseId", "FEmusic_liked_playlists")
            }

            val plRequest = Request.Builder()
                .url("https://music.youtube.com/youtubei/v1/browse?prettyPrint=false")
                .post(playlistsPayload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
                .header("Cookie", cookies)
                .header("Authorization", authHeader)
                .header("X-Origin", origin)
                .header("Origin", origin)
                .build()

            val plResp = httpClient.newCall(plRequest).execute()
            if (plResp.isSuccessful) {
                val plBodyStr = plResp.body?.string().orEmpty()
                val playlistSummaries = parseYtmPlaylistsSummary(plBodyStr)
                for (summary in playlistSummaries) {
                    if (summary.browseId.isBlank() || summary.browseId == "VLLM") continue
                    onProgress(1, 1, "Sincronizando: ${summary.title}")
                    val plTracks = fetchYtmPlaylistTracks(summary.browseId, cookies, authHeader, origin)
                    if (plTracks.isNotEmpty()) {
                        val created = libraryRepository.createPlaylist(
                            title = summary.title,
                            description = "Sincronizada do YouTube Music • ${plTracks.size} faixas"
                        )
                        for (tr in plTracks) {
                            libraryRepository.addTrackToPlaylist(created.id, tr)
                            totalSynced++
                        }
                    }
                }
            }

            Result.success(totalSynced)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao sincronizar YouTube Music: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // 2. SINCRONIZAÇÃO VIA CONEXÃO DIRETA: SPOTIFY
    // ---------------------------------------------------------------------------------------------

    suspend fun syncSpotifyAccount(
        onProgress: (current: Int, total: Int, trackTitle: String) -> Unit
    ): Result<Int> = withContext(Dispatchers.IO) {
        val tokenOrCookies = prefs.getString(KEY_SPOTIFY_TOKEN, null)
            ?: return@withContext Result.failure(IllegalStateException("Nenhuma sessão do Spotify encontrada. Faça login primeiro."))

        try {
            var totalSynced = 0
            val accessToken = resolveSpotifyAccessToken(tokenOrCookies)
                ?: return@withContext Result.failure(IllegalStateException("Não foi possível autenticar a sessão do Spotify."))

            // 1. Músicas Curtidas (Liked Songs)
            onProgress(0, 1, "Buscando Liked Songs do Spotify...")
            val likedReq = Request.Builder()
                .url("https://api.spotify.com/v1/me/tracks?limit=50")
                .header("Authorization", "Bearer $accessToken")
                .header("User-Agent", "Mozilla/5.0")
                .build()

            val likedResp = httpClient.newCall(likedReq).execute()
            val rawItems = mutableListOf<RawImportedItem>()
            if (likedResp.isSuccessful) {
                val json = JSONObject(likedResp.body?.string().orEmpty())
                val items = json.optJSONArray("items")
                if (items != null) {
                    for (i in 0 until items.length()) {
                        val tObj = items.optJSONObject(i)?.optJSONObject("track") ?: continue
                        val title = tObj.optString("name", "")
                        val artist = tObj.optJSONArray("artists")?.optJSONObject(0)?.optString("name", "") ?: ""
                        if (title.isNotBlank()) {
                            rawItems.add(RawImportedItem(title, artist))
                        }
                    }
                }
            }

            if (rawItems.isNotEmpty()) {
                val matched = matchRawItemsToCatalog(rawItems, onProgress)
                for (tr in matched) {
                    if (!libraryRepository.isFavorite(tr.id)) {
                        libraryRepository.toggleFavorite(tr)
                    }
                    totalSynced++
                }
                injectImportedPreferences(matched)
            }

            // 2. Playlists do Usuário
            onProgress(0, 1, "Buscando Playlists do Spotify...")
            val plReq = Request.Builder()
                .url("https://api.spotify.com/v1/me/playlists?limit=50")
                .header("Authorization", "Bearer $accessToken")
                .header("User-Agent", "Mozilla/5.0")
                .build()

            val plResp = httpClient.newCall(plReq).execute()
            if (plResp.isSuccessful) {
                val json = JSONObject(plResp.body?.string().orEmpty())
                val items = json.optJSONArray("items")
                if (items != null) {
                    for (i in 0 until items.length()) {
                        val pObj = items.optJSONObject(i) ?: continue
                        val name = pObj.optString("name", "Playlist Spotify")
                        val pId = pObj.optString("id", "")
                        if (pId.isNotBlank()) {
                            onProgress(i + 1, items.length(), "Sincronizando: $name")
                            val embedUrl = "https://open.spotify.com/embed/playlist/$pId"
                            val emReq = Request.Builder().url(embedUrl).build()
                            val emResp = httpClient.newCall(emReq).execute()
                            if (emResp.isSuccessful) {
                                val (_, pRawItems) = parseSpotifyEmbedHtml(emResp.body?.string().orEmpty())
                                if (pRawItems.isNotEmpty()) {
                                    val matchedPl = matchRawItemsToCatalog(pRawItems, onProgress)
                                    val created = libraryRepository.createPlaylist(name, "Sincronizada do Spotify • ${matchedPl.size} faixas")
                                    for (tr in matchedPl) {
                                        libraryRepository.addTrackToPlaylist(created.id, tr)
                                        totalSynced++
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Result.success(totalSynced)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao sincronizar Spotify: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun resolveSpotifyAccessToken(tokenOrCookies: String): String? {
        if (!tokenOrCookies.contains("sp_dc=")) {
            return tokenOrCookies.trim()
        }
        val spDc = extractCookieValue(tokenOrCookies, "sp_dc") ?: return null
        val tokenReq = Request.Builder()
            .url("https://open.spotify.com/get_access_token?reason=transport&productType=web_player")
            .header("Cookie", "sp_dc=$spDc")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()
        val tokenResp = httpClient.newCall(tokenReq).execute()
        if (tokenResp.isSuccessful) {
            val json = JSONObject(tokenResp.body?.string().orEmpty())
            val token = json.optString("accessToken", "")
            return if (token.isNotBlank()) token else null
        }
        return null
    }

    // ---------------------------------------------------------------------------------------------
    // 3. IMPORTAÇÃO POR LINK (SPOTIFY) - SEM LOGIN
    // ---------------------------------------------------------------------------------------------

    data class RawImportedItem(val title: String, val artist: String, val album: String? = null)

    suspend fun importFromSpotifyUrl(
        spotifyUrl: String,
        onProgress: (current: Int, total: Int, trackTitle: String) -> Unit
    ): Result<UserPlaylist> = withContext(Dispatchers.IO) {
        try {
            val playlistId = extractSpotifyPlaylistId(spotifyUrl)
                ?: return@withContext Result.failure(IllegalArgumentException("Link do Spotify inválido. Certifique-se de que é um link de playlist pública."))

            val embedUrl = "https://open.spotify.com/embed/playlist/$playlistId"
            val request = Request.Builder()
                .url(embedUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(IllegalStateException("Não foi possível acessar a playlist do Spotify (Código ${response.code})."))
            }

            val html = response.body?.string().orEmpty()
            val (playlistTitle, rawItems) = parseSpotifyEmbedHtml(html)

            if (rawItems.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("Nenhuma música encontrada na playlist pública do Spotify."))
            }

            val matchedTracks = matchRawItemsToCatalog(rawItems, onProgress)
            if (matchedTracks.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("Não foi possível localizar as faixas no catálogo."))
            }

            val finalTitle = playlistTitle.ifBlank { "Playlist do Spotify" }
            val createdPlaylist = libraryRepository.createPlaylist(
                title = finalTitle,
                description = "Importada do Spotify • ${matchedTracks.size} faixas"
            )

            for (track in matchedTracks) {
                libraryRepository.addTrackToPlaylist(createdPlaylist.id, track)
            }

            injectImportedPreferences(matchedTracks)

            Result.success(createdPlaylist)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao importar playlist do Spotify: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun extractSpotifyPlaylistId(url: String): String? {
        val pattern = Pattern.compile("playlist/([a-zA-Z0-9]+)")
        val matcher = pattern.matcher(url)
        return if (matcher.find()) matcher.group(1) else null
    }

    private fun parseSpotifyEmbedHtml(html: String): Pair<String, List<RawImportedItem>> {
        val nextDataPattern = Pattern.compile("<script id=\"__NEXT_DATA__\" type=\"application/json\">(.*?)</script>")
        val matcher = nextDataPattern.matcher(html)
        if (matcher.find()) {
            val jsonStr = matcher.group(1) ?: return Pair("", emptyList())
            val root = JSONObject(jsonStr)
            val props = root.optJSONObject("props")?.optJSONObject("pageProps")
            val entity = props?.optJSONObject("state")?.optJSONObject("data")?.optJSONObject("entity")
            val name = entity?.optString("name", "Playlist Importada") ?: "Playlist Importada"
            val trackList = entity?.optJSONArray("trackList")

            val items = mutableListOf<RawImportedItem>()
            if (trackList != null) {
                for (i in 0 until trackList.length()) {
                    val obj = trackList.optJSONObject(i) ?: continue
                    val title = obj.optString("title", "").trim()
                    val subtitle = obj.optString("subtitle", "").trim()
                    if (title.isNotBlank()) {
                        items.add(RawImportedItem(title = title, artist = subtitle))
                    }
                }
            }
            return Pair(name, items)
        }
        return Pair("Playlist Importada", emptyList())
    }

    // ---------------------------------------------------------------------------------------------
    // 4. IMPORTAÇÃO POR LINK (YOUTUBE MUSIC) - SEM LOGIN
    // ---------------------------------------------------------------------------------------------

    suspend fun importFromYouTubeMusicUrl(
        ytmUrl: String,
        onProgress: (current: Int, total: Int, trackTitle: String) -> Unit
    ): Result<UserPlaylist> = withContext(Dispatchers.IO) {
        try {
            val listId = extractYouTubeListId(ytmUrl)
                ?: return@withContext Result.failure(IllegalArgumentException("Link do YouTube Music inválido. O link deve conter 'list=ID'."))

            val browseId = if (listId.startsWith("VL")) listId else "VL$listId"
            val albumResult = catalogClient.getAlbumOrPlaylist(browseId).getOrNull()
                ?: return@withContext Result.failure(IllegalStateException("Não foi possível carregar a playlist do YouTube Music."))

            val playlistTitle = albumResult.title.ifBlank { "Playlist do YouTube Music" }
            val tracks = albumResult.tracks

            if (tracks.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("A playlist do YouTube Music está vazia ou privada."))
            }

            val createdPlaylist = libraryRepository.createPlaylist(
                title = playlistTitle,
                description = "Importada do YouTube Music • ${tracks.size} faixas"
            )

            for ((index, track) in tracks.withIndex()) {
                onProgress(index + 1, tracks.size, track.title)
                libraryRepository.addTrackToPlaylist(createdPlaylist.id, track)
            }

            injectImportedPreferences(tracks)

            Result.success(createdPlaylist)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao importar playlist do YouTube Music: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun extractYouTubeListId(url: String): String? {
        val pattern = Pattern.compile("[?&]list=([a-zA-Z0-9_-]+)")
        val matcher = pattern.matcher(url)
        return if (matcher.find()) matcher.group(1) else null
    }

    // ---------------------------------------------------------------------------------------------
    // 5. IMPORTAÇÃO DE ARQUIVO (CSV / JSON / TEXTO PURO)
    // ---------------------------------------------------------------------------------------------

    suspend fun importFromTextOrCsv(
        content: String,
        playlistTitle: String = "Playlist Importada",
        onProgress: (current: Int, total: Int, trackTitle: String) -> Unit
    ): Result<UserPlaylist> = withContext(Dispatchers.IO) {
        try {
            val rawItems = parseCsvOrTextContent(content)
            if (rawItems.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("Nenhuma música detectada no arquivo."))
            }

            val matchedTracks = matchRawItemsToCatalog(rawItems, onProgress)
            if (matchedTracks.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("Nenhuma faixa pôde ser localizada no catálogo."))
            }

            val finalTitle = playlistTitle.ifBlank { "Playlist Importada" }
            val createdPlaylist = libraryRepository.createPlaylist(
                title = finalTitle,
                description = "Importada via arquivo • ${matchedTracks.size} faixas"
            )

            for (track in matchedTracks) {
                libraryRepository.addTrackToPlaylist(createdPlaylist.id, track)
            }

            injectImportedPreferences(matchedTracks)

            Result.success(createdPlaylist)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao importar arquivo: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun parseCsvOrTextContent(content: String): List<RawImportedItem> {
        val items = mutableListOf<RawImportedItem>()
        val lines = content.lines().map { it.trim() }.filter { it.isNotBlank() }

        var isCsv = false
        var titleCol = -1
        var artistCol = -1

        for (line in lines) {
            val lower = line.lowercase()
            if (!isCsv && (lower.contains("title") || lower.contains("track") || lower.contains("name")) &&
                (lower.contains("artist") || lower.contains("performer"))
            ) {
                isCsv = true
                val cols = line.split(",").map { it.trim().replace("\"", "").lowercase() }
                titleCol = cols.indexOfFirst { it.contains("title") || it.contains("track") || it.contains("name") }
                artistCol = cols.indexOfFirst { it.contains("artist") || it.contains("performer") }
                continue
            }

            if (isCsv && titleCol != -1 && artistCol != -1) {
                val cols = line.split(",").map { it.trim().replace("\"", "") }
                if (cols.size > maxOf(titleCol, artistCol)) {
                    val title = cols[titleCol]
                    val artist = cols[artistCol]
                    if (title.isNotBlank()) {
                        items.add(RawImportedItem(title = title, artist = artist))
                    }
                }
            } else {
                if (line.contains(" - ")) {
                    val parts = line.split(" - ")
                    if (parts.size >= 2) {
                        items.add(RawImportedItem(title = parts[1].trim(), artist = parts[0].trim()))
                    }
                } else if (line.contains(",")) {
                    val parts = line.split(",")
                    if (parts.size >= 2) {
                        items.add(RawImportedItem(title = parts[0].trim(), artist = parts[1].trim()))
                    }
                } else {
                    items.add(RawImportedItem(title = line, artist = ""))
                }
            }
        }
        return items
    }

    // ---------------------------------------------------------------------------------------------
    // 6. MOTOR DE FUZZY MATCHING (Alta Precisão com Busca Aberta e Fallback)
    // ---------------------------------------------------------------------------------------------

    private suspend fun matchRawItemsToCatalog(
        rawItems: List<RawImportedItem>,
        onProgress: (current: Int, total: Int, trackTitle: String) -> Unit
    ): List<Track> {
        val matchedTracks = mutableListOf<Track>()
        val seenTrackIds = mutableSetOf<String>()

        for ((index, item) in rawItems.withIndex()) {
            onProgress(index + 1, rawItems.size, "${item.artist} - ${item.title}".trim(' ', '-'))

            try {
                val cleanTitle = cleanTrackString(item.title)
                val primaryArtist = item.artist
                    .split(",", "&", "feat", "ft", "/", ";", "•")
                    .firstOrNull()?.trim().orEmpty()
                val cleanArtist = cleanTrackString(primaryArtist)

                // Utiliza SearchFilter.ALL para cobrir faixas recentes e "Melhor Resultado"
                val query = if (cleanArtist.isNotBlank()) "$cleanTitle $cleanArtist" else cleanTitle
                var songs = catalogClient.search(query, SearchFilter.ALL).getOrNull()?.songs.orEmpty()

                if (songs.isEmpty() && cleanArtist.isNotBlank()) {
                    songs = catalogClient.search(cleanTitle, SearchFilter.ALL).getOrNull()?.songs.orEmpty()
                }

                if (songs.isNotEmpty()) {
                    val bestMatch = songs.firstOrNull { candidate ->
                        calculateSimilarity(item.title, candidate.title) > 0.40
                    } ?: songs.first()

                    if (seenTrackIds.add(bestMatch.id)) {
                        matchedTracks.add(bestMatch)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Falha ao associar faixa ${item.title}: ${e.message}")
            }
        }

        return matchedTracks
    }

    private fun cleanTrackString(text: String): String {
        return text.replace(Regex("(?i)\\(.*?(feat|ft|remix|version|official|lyric).*?\\)"), "")
            .replace(Regex("(?i)\\[.*?\\]"), "")
            .replace(Regex("[^a-zA-Z0-9À-ÿ\\s]"), " ")
            .trim()
    }

    private fun calculateSimilarity(s1: String, s2: String): Double {
        val clean1 = s1.lowercase().trim()
        val clean2 = s2.lowercase().trim()
        if (clean1 == clean2) return 1.0
        val longer = if (clean1.length > clean2.length) clean1 else clean2
        val shorter = if (clean1.length > clean2.length) clean2 else clean1
        if (longer.isEmpty()) return 1.0
        return (longer.length - levenshteinDistance(longer, shorter)) / longer.length.toDouble()
    }

    private fun levenshteinDistance(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[a.length][b.length]
    }

    // ---------------------------------------------------------------------------------------------
    // 7. INJEÇÃO DE PREFERÊNCIAS NO DNA ACÚSTICO
    // ---------------------------------------------------------------------------------------------

    private fun injectImportedPreferences(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        val topPicks = tracks.take(25)
        for (track in topPicks) {
            userHistoryRepository.recordTrackPlayed(track)
        }
        Log.i(TAG, "DNA acústico atualizado com sucesso com ${topPicks.size} faixas importadas.")
    }

    // ---------------------------------------------------------------------------------------------
    // 8. UTILITÁRIOS E PARSERS INTERNOS DO YOUTUBE MUSIC
    // ---------------------------------------------------------------------------------------------

    private fun extractCookieValue(cookies: String, key: String): String? {
        val pattern = Pattern.compile("(?:^|;\\s*)$key=([^;]+)")
        val matcher = pattern.matcher(cookies)
        return if (matcher.find()) matcher.group(1)?.trim() else null
    }

    private fun computeSha1(input: String): String {
        val md = MessageDigest.getInstance("SHA-1")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun parseYtmLikedTracksJson(jsonStr: String): List<Track> {
        val list = mutableListOf<Track>()
        try {
            val root = JSONObject(jsonStr)
            val tabs = root.optJSONObject("contents")
                ?.optJSONObject("singleColumnBrowseResultsRenderer")
                ?.optJSONArray("tabs") ?: return emptyList()

            for (t in 0 until tabs.length()) {
                val tab = tabs.optJSONObject(t) ?: continue
                val secContents = tab.optJSONObject("tabRenderer")
                    ?.optJSONObject("content")
                    ?.optJSONObject("sectionListRenderer")
                    ?.optJSONArray("contents") ?: continue

                for (s in 0 until secContents.length()) {
                    val sec = secContents.optJSONObject(s) ?: continue
                    val shelf = sec.optJSONObject("musicShelfRenderer")
                        ?: sec.optJSONObject("musicPlaylistShelfRenderer") ?: continue

                    val contents = shelf.optJSONArray("contents") ?: continue
                    for (c in 0 until contents.length()) {
                        val mfr = contents.optJSONObject(c)?.optJSONObject("musicResponsiveListItemRenderer") ?: continue
                        val flexCols = mfr.optJSONArray("flexColumns") ?: continue
                        if (flexCols.length() == 0) continue

                        val col0Runs = flexCols.optJSONObject(0)
                            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                            ?.optJSONObject("text")
                            ?.optJSONArray("runs") ?: continue
                        val title = col0Runs.optJSONObject(0)?.optString("text")?.trim().orEmpty()
                        if (title.isBlank() || title.contains("aleatório", ignoreCase = true)) continue

                        val col1Runs = if (flexCols.length() > 1) {
                            flexCols.optJSONObject(1)
                                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                                ?.optJSONObject("text")
                                ?.optJSONArray("runs")
                        } else null
                        val artist = col1Runs?.optJSONObject(0)?.optString("text")?.trim().orEmpty().ifBlank { "YouTube Music" }

                        var videoId = mfr.optJSONObject("overlay")
                            ?.optJSONObject("musicItemThumbnailOverlayRenderer")
                            ?.optJSONObject("content")
                            ?.optJSONObject("musicPlayButtonRenderer")
                            ?.optJSONObject("playNavigationEndpoint")
                            ?.optJSONObject("watchEndpoint")
                            ?.optString("videoId").orEmpty()

                        if (videoId.isBlank() && col0Runs.length() > 0) {
                            videoId = col0Runs.optJSONObject(0)
                                ?.optJSONObject("navigationEndpoint")
                                ?.optJSONObject("watchEndpoint")
                                ?.optString("videoId").orEmpty()
                        }

                        if (videoId.isNotBlank()) {
                            list.add(
                                Track(
                                    id = videoId,
                                    title = title,
                                    artist = artist,
                                    thumbnailUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
                                    durationText = "--:--",
                                    sourceIdentifier = "INNERTUBE_WEB_REMIX"
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao analisar JSON de faixas curtidas do YTM: ${e.message}", e)
        }
        return list
    }

    data class PlaylistSummary(val title: String, val browseId: String)

    private fun parseYtmPlaylistsSummary(jsonStr: String): List<PlaylistSummary> {
        val list = mutableListOf<PlaylistSummary>()
        try {
            val root = JSONObject(jsonStr)
            val tabs = root.optJSONObject("contents")
                ?.optJSONObject("singleColumnBrowseResultsRenderer")
                ?.optJSONArray("tabs") ?: return emptyList()

            for (t in 0 until tabs.length()) {
                val tab = tabs.optJSONObject(t) ?: continue
                val secContents = tab.optJSONObject("tabRenderer")
                    ?.optJSONObject("content")
                    ?.optJSONObject("sectionListRenderer")
                    ?.optJSONArray("contents") ?: continue

                for (s in 0 until secContents.length()) {
                    val sec = secContents.optJSONObject(s) ?: continue
                    val gridItems = sec.optJSONObject("gridRenderer")?.optJSONArray("items") ?: continue
                    for (g in 0 until gridItems.length()) {
                        val item = gridItems.optJSONObject(g)?.optJSONObject("musicTwoRowItemRenderer") ?: continue
                        val titleRuns = item.optJSONObject("title")?.optJSONArray("runs") ?: continue
                        val title = titleRuns.optJSONObject(0)?.optString("text")?.trim().orEmpty()
                        val browseId = item.optJSONObject("navigationEndpoint")
                            ?.optJSONObject("browseEndpoint")
                            ?.optString("browseId").orEmpty()
                        if (title.isNotBlank() && browseId.isNotBlank()) {
                            list.add(PlaylistSummary(title, browseId))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao analisar resumos de playlists do YTM: ${e.message}", e)
        }
        return list
    }

    private fun fetchYtmPlaylistTracks(
        browseId: String,
        cookies: String,
        authHeader: String,
        origin: String
    ): List<Track> {
        try {
            val payload = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", "1.20240909.01.00")
                        put("hl", "pt")
                        put("gl", "BR")
                    })
                })
                put("browseId", browseId)
            }

            val request = Request.Builder()
                .url("https://music.youtube.com/youtubei/v1/browse?prettyPrint=false")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
                .header("Cookie", cookies)
                .header("Authorization", authHeader)
                .header("X-Origin", origin)
                .header("Origin", origin)
                .build()

            val resp = httpClient.newCall(request).execute()
            if (resp.isSuccessful) {
                return parseYtmLikedTracksJson(resp.body?.string().orEmpty())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Erro ao carregar faixas da playlist $browseId: ${e.message}")
        }
        return emptyList()
    }
}
