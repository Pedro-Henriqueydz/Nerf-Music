package com.nerf.music.data.network

import com.nerf.music.data.model.Artist
import com.nerf.music.data.model.SearchFilter
import com.nerf.music.data.model.SearchResult
import com.nerf.music.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Cliente de acesso ao catálogo InnerTube (YouTube Music Web Remix).
 *
 * Realiza buscas e extração estruturada de Músicas e Artistas em português brasileiro (pt-BR)
 * de forma anônima, sem requisição de login ou envio de credenciais.
 */
class InnerTubeCatalogClient(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {

    companion object {
        private const val SEARCH_URL = "https://music.youtube.com/youtubei/v1/search"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        private val DURATION_REGEX = Pattern.compile("^\\d{1,2}:\\d{2}$")
    }

    /**
     * Executa a pesquisa no catálogo e mapeia para [SearchResult], separando músicas e artistas.
     */
    suspend fun search(
        query: String,
        filter: SearchFilter = SearchFilter.ALL
    ): Result<SearchResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) {
            return@withContext Result.success(SearchResult())
        }

        try {
            val payload = buildSearchPayload(query, filter)
            val request = Request.Builder()
                .url(SEARCH_URL)
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .header("User-Agent", USER_AGENT)
                .header("Origin", "https://music.youtube.com")
                .header("Referer", "https://music.youtube.com/")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    IOException("Servidor respondeu com código HTTP ${response.code}")
                )
            }

            val body = response.body?.string()
                ?: return@withContext Result.failure(IOException("Resposta vazia do servidor"))

            val searchResult = parseSearchResponse(body)
            Result.success(searchResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildSearchPayload(query: String, filter: SearchFilter): JSONObject {
        val root = JSONObject()
        val context = JSONObject()
        val client = JSONObject()

        client.put("clientName", "WEB_REMIX")
        client.put("clientVersion", "1.20231204.01.00")
        client.put("hl", "pt-BR")
        client.put("gl", "BR")

        context.put("client", client)
        root.put("context", context)
        root.put("query", query)
        if (filter.paramValue != null) {
            root.put("params", filter.paramValue)
        }
        return root
    }

    /**
     * Analisa o JSON de resposta do InnerTube e extrai músicas e artistas de forma independente.
     * Visibilidade pública/interna para testes unitários com fixtures.
     */
    fun parseSearchResponse(jsonString: String): SearchResult {
        if (jsonString.isBlank()) return SearchResult()

        return try {
            val root = JSONObject(jsonString)
            val contents = root.optJSONObject("contents") ?: return SearchResult()
            val tabbed = contents.optJSONObject("tabbedSearchResultsRenderer") ?: return SearchResult()
            val tabs = tabbed.optJSONArray("tabs") ?: return SearchResult()
            if (tabs.length() == 0) return SearchResult()

            val tabContent = tabs.optJSONObject(0)
                ?.optJSONObject("tabRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("sectionListRenderer")
                ?.optJSONArray("contents") ?: return SearchResult()

            val songs = mutableListOf<Track>()
            val artists = mutableListOf<Artist>()
            var topArtist: Artist? = null

            for (i in 0 until tabContent.length()) {
                val section = tabContent.optJSONObject(i) ?: continue

                // 1. Destaque Principal (musicCardShelfRenderer)
                val cardShelf = section.optJSONObject("musicCardShelfRenderer")
                if (cardShelf != null) {
                    val (cardTrack, cardArtist) = parseCardShelf(cardShelf)
                    if (cardArtist != null && topArtist == null) {
                        topArtist = cardArtist
                    }
                    if (cardTrack != null && songs.none { it.id == cardTrack.id }) {
                        songs.add(cardTrack)
                    }
                }

                // 2. Itens envelopados em itemSectionRenderer (Padrão moderno do Web Remix)
                val itemSection = section.optJSONObject("itemSectionRenderer")
                if (itemSection != null) {
                    val innerContents = itemSection.optJSONArray("contents") ?: JSONArray()
                    for (j in 0 until innerContents.length()) {
                        val innerItem = innerContents.optJSONObject(j) ?: continue
                        
                        val listItem = innerItem.optJSONObject("musicResponsiveListItemRenderer")
                        if (listItem != null) {
                            parseResponsiveListItem(listItem, songs, artists)
                        }

                        val shelf = innerItem.optJSONObject("musicShelfRenderer")
                        if (shelf != null) {
                            parseShelfContents(shelf, songs, artists)
                        }
                    }
                }

                // 3. Estante direta de músicas (musicShelfRenderer)
                val directShelf = section.optJSONObject("musicShelfRenderer")
                if (directShelf != null) {
                    parseShelfContents(directShelf, songs, artists)
                }
            }

            SearchResult(
                songs = songs,
                artists = artists,
                topArtist = topArtist
            )
        } catch (e: Exception) {
            // Em caso de JSON malformado ou corrompido, retorna resultado vazio seguro
            SearchResult()
        }
    }

    private fun parseShelfContents(
        shelf: JSONObject,
        songs: MutableList<Track>,
        artists: MutableList<Artist>
    ) {
        val items = shelf.optJSONArray("contents") ?: return
        for (i in 0 until items.length()) {
            val item = items.optJSONObject(i) ?: continue
            val listItem = item.optJSONObject("musicResponsiveListItemRenderer") ?: continue
            parseResponsiveListItem(listItem, songs, artists)
        }
    }

    private fun parseCardShelf(card: JSONObject): Pair<Track?, Artist?> {
        val titleRuns = card.optJSONObject("title")?.optJSONArray("runs") ?: return Pair(null, null)
        val title = titleRuns.optJSONObject(0)?.optString("text") ?: return Pair(null, null)

        val subtitleRuns = card.optJSONObject("subtitle")?.optJSONArray("runs") ?: JSONArray()
        val subtitleParts = mutableListOf<String>()
        for (i in 0 until subtitleRuns.length()) {
            val text = subtitleRuns.optJSONObject(i)?.optString("text")?.trim() ?: ""
            if (text.isNotEmpty() && text != "•") {
                subtitleParts.add(text)
            }
        }
        val fullSubtitle = subtitleParts.joinToString(" • ")

        val thumbnailUrl = getBestThumbnailUrl(
            card.optJSONObject("thumbnail")
                ?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails")
        )

        // Se o subtítulo indica Artista, preserva estritamente como Artist
        if (fullSubtitle.contains("Artista", ignoreCase = true) || fullSubtitle.contains("Artist", ignoreCase = true)) {
            val artist = Artist(
                id = "card_${title.hashCode()}",
                name = title,
                subtitle = fullSubtitle,
                thumbnailUrl = thumbnailUrl
            )
            return Pair(null, artist)
        }

        // Caso seja uma música em destaque (ex: busca direta pelo nome de faixa)
        var videoId = ""
        val onTap = card.optJSONObject("onTap")
        if (onTap != null) {
            videoId = onTap.optJSONObject("watchEndpoint")?.optString("videoId") ?: ""
        }
        if (videoId.isEmpty()) {
            val buttons = card.optJSONArray("buttons") ?: JSONArray()
            for (b in 0 until buttons.length()) {
                val btn = buttons.optJSONObject(b)?.optJSONObject("buttonRenderer")
                val cmd = btn?.optJSONObject("command")?.optJSONObject("watchEndpoint")
                val vid = cmd?.optString("videoId") ?: ""
                if (vid.isNotEmpty()) {
                    videoId = vid
                    break
                }
            }
        }

        var artistName = "Artista Desconhecido"
        var duration = "--:--"
        for (part in subtitleParts) {
            if (DURATION_REGEX.matcher(part).matches()) {
                duration = part
            } else if (!part.contains("Música", ignoreCase = true) &&
                !part.contains("Song", ignoreCase = true) &&
                !part.contains("Vídeo", ignoreCase = true) &&
                !part.contains("visualiza", ignoreCase = true) &&
                artistName == "Artista Desconhecido"
            ) {
                artistName = part
            }
        }

        val track = Track(
            id = if (videoId.isNotEmpty()) videoId else "card_track_${title.hashCode()}",
            title = title,
            artist = artistName,
            thumbnailUrl = thumbnailUrl,
            durationText = duration,
            sourceIdentifier = "INNERTUBE_WEB_REMIX"
        )
        return Pair(track, null)
    }

    private fun parseResponsiveListItem(
        item: JSONObject,
        songs: MutableList<Track>,
        artists: MutableList<Artist>
    ) {
        val flexCols = item.optJSONArray("flexColumns") ?: return
        if (flexCols.length() == 0) return

        // Coluna 0: Título principal
        val col0Runs = flexCols.optJSONObject(0)
            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.optJSONObject("text")
            ?.optJSONArray("runs") ?: return

        val title = col0Runs.optJSONObject(0)?.optString("text")?.trim() ?: return
        if (title.isEmpty()) return

        // Extrai todas as partes de texto da Coluna 1
        val col1Runs = if (flexCols.length() > 1) {
            flexCols.optJSONObject(1)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")
                ?.optJSONArray("runs") ?: JSONArray()
        } else {
            JSONArray()
        }

        val col1Parts = mutableListOf<String>()
        for (i in 0 until col1Runs.length()) {
            val runText = col1Runs.optJSONObject(i)?.optString("text")?.trim() ?: ""
            if (runText.isNotEmpty() && runText != "•") {
                col1Parts.add(runText)
            }
        }
        val col1FullText = col1Parts.joinToString(" • ")

        val thumbnailUrl = getBestThumbnailUrl(
            item.optJSONObject("thumbnail")
                ?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails")
        )

        // 1. Extrair videoId do Item (usado para Músicas/Vídeos)
        var videoId = ""
        // Tentativa via botão de play no overlay
        val overlayPlay = item.optJSONObject("overlay")
            ?.optJSONObject("musicItemThumbnailOverlayRenderer")
            ?.optJSONObject("content")
            ?.optJSONObject("musicPlayButtonRenderer")
            ?.optJSONObject("playNavigationEndpoint")
            ?.optJSONObject("watchEndpoint")
            ?.optString("videoId") ?: ""

        if (overlayPlay.isNotEmpty()) {
            videoId = overlayPlay
        } else {
            // Tentativa via navigationEndpoint da Coluna 0
            val col0Watch = col0Runs.optJSONObject(0)
                ?.optJSONObject("navigationEndpoint")
                ?.optJSONObject("watchEndpoint")
                ?.optString("videoId") ?: ""

            if (col0Watch.isNotEmpty()) {
                videoId = col0Watch
            } else {
                videoId = item.optJSONObject("playlistItemData")?.optString("videoId") ?: ""
            }
        }

        // 2. Extrair Duração
        var durationText = "--:--"
        // Procura em todas as colunas flexíveis por padrão MM:SS
        for (c in 0 until flexCols.length()) {
            val cRuns = flexCols.optJSONObject(c)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")
                ?.optJSONArray("runs") ?: continue
            for (r in 0 until cRuns.length()) {
                val t = cRuns.optJSONObject(r)?.optString("text")?.trim() ?: ""
                if (DURATION_REGEX.matcher(t).matches()) {
                    durationText = t
                    break
                }
            }
            if (durationText != "--:--") break
        }

        // 3. Checagem de marcadores explícitos
        val hasSongMarker = col1Parts.any { part ->
            part.equals("Música", ignoreCase = true) ||
            part.equals("Musica", ignoreCase = true) ||
            part.equals("Song", ignoreCase = true) ||
            part.equals("Faixa", ignoreCase = true)
        }

        val hasArtistMarker = col1Parts.any { part ->
            part.equals("Artista", ignoreCase = true) ||
            part.equals("Artist", ignoreCase = true)
        }

        val isMusic = hasSongMarker || videoId.isNotEmpty() || durationText != "--:--"

        if (isMusic) {
            // 4. Identificar Artista e Álbum para Faixa Musical
            var artistName = "Desconhecido"
            var albumName: String? = null

            val filteredMeta = col1Parts.filter { part ->
                !part.equals("Música", ignoreCase = true) &&
                !part.equals("Musica", ignoreCase = true) &&
                !part.equals("Song", ignoreCase = true) &&
                !part.equals("Vídeo", ignoreCase = true) &&
                !part.equals("Video", ignoreCase = true) &&
                !part.contains("visualiza", ignoreCase = true) &&
                !DURATION_REGEX.matcher(part).matches()
            }

            if (filteredMeta.isNotEmpty()) {
                artistName = filteredMeta[0]
                if (filteredMeta.size > 1) {
                    albumName = filteredMeta[1]
                }
            }

            val trackId = if (videoId.isNotEmpty()) videoId else "track_${title.hashCode()}"
            if (songs.none { it.id == trackId }) {
                songs.add(
                    Track(
                        id = trackId,
                        title = title,
                        artist = artistName,
                        album = albumName,
                        thumbnailUrl = thumbnailUrl,
                        durationText = durationText,
                        sourceIdentifier = "INNERTUBE_WEB_REMIX"
                    )
                )
            }
        } else if (hasArtistMarker) {
            // 5. Identificar Artista
            val browseId = item.optJSONObject("navigationEndpoint")
                ?.optJSONObject("browseEndpoint")
                ?.optString("browseId") ?: "artist_${title.hashCode()}"

            if (artists.none { it.name.equals(title, ignoreCase = true) }) {
                artists.add(
                    Artist(
                        id = browseId,
                        name = title,
                        subtitle = if (col1FullText.isNotEmpty()) col1FullText else "Artista",
                        thumbnailUrl = thumbnailUrl
                    )
                )
            }
        }
    }

    private fun getBestThumbnailUrl(thumbnails: JSONArray?): String? {
        if (thumbnails == null || thumbnails.length() == 0) return null
        val last = thumbnails.optJSONObject(thumbnails.length() - 1)
        val url = last?.optString("url") ?: return null
        return if (url.startsWith("//")) "https:$url" else url
    }
}
