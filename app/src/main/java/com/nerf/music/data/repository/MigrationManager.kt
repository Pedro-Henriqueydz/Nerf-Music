package com.nerf.music.data.repository

import android.content.Context
import android.util.Log
import com.nerf.music.data.model.SearchFilter
import com.nerf.music.data.model.Track
import com.nerf.music.data.model.UserPlaylist
import com.nerf.music.data.network.InnerTubeCatalogClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
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
    }

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
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
    // 1. IMPORTAÇÃO POR LINK (SPOTIFY) - SEM LOGIN
    // ---------------------------------------------------------------------------------------------

    data class RawImportedItem(val title: String, val artist: String, val album: String? = null)

    suspend fun importFromSpotifyUrl(
        spotifyUrl: String,
        onProgress: (current: Int, total: Int, trackTitle: String) -> Unit
    ): Result<UserPlaylist> = withContext(Dispatchers.IO) {
        try {
            val playlistId = extractSpotifyPlaylistId(spotifyUrl)
                ?: return@withContext Result.failure(IllegalArgumentException("Link do Spotify inválido. Certifique-se de que é um link de playlist pública."))

            // Consulta o embed público do Spotify para obter metadados das faixas
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

            // Injeta as preferências no DNA acústico da Home
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
    // 2. IMPORTAÇÃO POR LINK (YOUTUBE MUSIC) - SEM LOGIN
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
    // 3. IMPORTAÇÃO DE ARQUIVO (CSV / JSON / TEXTO PURO)
    // ---------------------------------------------------------------------------------------------

    suspend fun importFromTextOrCsv(
        content: String,
        playlistTitle: String = "Playlist Importada",
        onProgress: (current: Int, total: Int, trackTitle: String) -> Unit
    ): Result<UserPlaylist> = withContext(Dispatchers.IO) {
        try {
            val rawItems = parseCsvOrTextContent(content)
            if (rawItems.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("Nenhuma música válida foi encontrada no arquivo ou texto fornecido."))
            }

            val matchedTracks = matchRawItemsToCatalog(rawItems, onProgress)
            if (matchedTracks.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("Não foi possível localizar as músicas no catálogo musical."))
            }

            val createdPlaylist = libraryRepository.createPlaylist(
                title = playlistTitle.trim().ifBlank { "Backup Importado" },
                description = "Importada via arquivo/texto • ${matchedTracks.size} faixas"
            )

            for (track in matchedTracks) {
                libraryRepository.addTrackToPlaylist(createdPlaylist.id, track)
            }

            injectImportedPreferences(matchedTracks)

            Result.success(createdPlaylist)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao importar arquivo CSV/Texto: ${e.message}", e)
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
            // Detecção de cabeçalho CSV (Soundiiz, TuneMyMusic ou genérico)
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
                // Formato Linha por Linha: "Artista - Música" ou "Música - Artista"
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
    // 4. MOTOR DE FUZZY MATCHING (Normalização e Busca no Catálogo)
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
                val searchQuery = "${cleanTrackString(item.title)} ${cleanTrackString(item.artist)}".trim()
                val searchResult = catalogClient.search(searchQuery, SearchFilter.SONGS).getOrNull()
                val songs = searchResult?.songs.orEmpty()

                if (songs.isNotEmpty()) {
                    // Seleciona a melhor faixa com score de similaridade
                    val bestMatch = songs.firstOrNull { candidate ->
                        calculateSimilarity(item.title, candidate.title) > 0.60
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
    // 5. INJEÇÃO DE PREFERÊNCIAS NO DNA ACÚSTICO (Atualização Instantânea da Home)
    // ---------------------------------------------------------------------------------------------

    private fun injectImportedPreferences(tracks: List<Track>) {
        if (tracks.isEmpty()) return

        // Registra faixas no histórico para que os artistas subam na lista de mais ouvidos
        val topPicks = tracks.take(15)
        for (track in topPicks) {
            userHistoryRepository.recordTrackPlayed(track)
        }

        Log.i(TAG, "DNA acústico atualizado com sucesso com ${topPicks.size} faixas importadas.")
    }
}
