package com.nerf.music.data.network

import com.nerf.music.data.model.Artist
import com.nerf.music.data.model.QueueFilterChip
import com.nerf.music.data.model.SearchFilter
import com.nerf.music.data.model.SearchResult
import com.nerf.music.data.model.Track
import com.nerf.music.data.model.UpNextResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Cliente de acesso ao catálogo InnerTube (YouTube Music Web Remix).
 *
 * Realiza buscas e extração estruturada de Músicas, Artistas e Rádio Dinâmica ("Up Next")
 * em português brasileiro (pt-BR) de forma anônima, sem requisição de login.
 */
class InnerTubeCatalogClient(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {

    // Mapa auxiliar que associa o `params` de um Chip ao seu `playlistId` específico (ex: RDAT...)
    private val chipPlaylistIdMap = ConcurrentHashMap<String, String>()

    companion object {
        private const val SEARCH_URL = "https://music.youtube.com/youtubei/v1/search"
        private const val NEXT_URL = "https://music.youtube.com/youtubei/v1/next"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

        private val DURATION_REGEX = Pattern.compile("^(?:(\\d{1,2}):)?(\\d{1,2}):(\\d{2})$")
        private val DURATION_INLINE_REGEX = Pattern.compile("\\b(?:(\\d{1,2}):)?(\\d{1,2}):(\\d{2})\\b")
        private val YEAR_REGEX = Pattern.compile("^\\d{4}$")
        private val ACCESSIBILITY_DURATION_REGEX = Pattern.compile(
            "(\\d{1,2})\\s*minutos?\\s*e\\s*(\\d{1,2})\\s*segundos?",
            Pattern.CASE_INSENSITIVE
        )

        private val SONG_MARKERS = setOf("música", "musica", "song", "faixa")
        private val ARTIST_MARKERS = setOf("artista", "artist")
        private val NON_SONG_MARKERS = setOf(
            "artista", "artist",
            "álbum", "album",
            "single", "ep",
            "playlist",
            "podcast", "episódio", "episodio", "episode",
            "perfil", "profile",
            "vídeo", "video"
        )

        /**
         * Normaliza textos de duração (ex: "3:45" -> "03:45", "1:04:05" -> "1:04:05").
         */
        fun normalizeDurationText(raw: String): String {
            val trimmed = raw.trim()
            val matcher = DURATION_REGEX.matcher(trimmed)
            if (!matcher.matches()) return trimmed
            val hoursGroup = matcher.group(1)
            val minutes = matcher.group(2)?.toIntOrNull() ?: return trimmed
            val seconds = matcher.group(3)?.toIntOrNull() ?: return trimmed
            return if (!hoursGroup.isNullOrEmpty()) {
                val hours = hoursGroup.toIntOrNull() ?: 0
                String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format(Locale.US, "%02d:%02d", minutes, seconds)
            }
        }
    }

    /**
     * Executa a pesquisa no catálogo e mapeia para [SearchResult], separando músicas e artistas.
     *
     * Na aba "Todos" ([SearchFilter.ALL]) ou "Músicas" ([SearchFilter.SONGS]), combina o Top Result
     * (`musicCardShelfRenderer`) com a estante dedicada de músicas para garantir que todas as faixas
     * possuam a duração exata (ex: "03:45") e priorizem o destaque principal.
     */
    suspend fun search(
        query: String,
        filter: SearchFilter = SearchFilter.ALL
    ): Result<SearchResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) {
            return@withContext Result.success(SearchResult())
        }

        try {
            when (filter) {
                SearchFilter.ALL -> coroutineScope {
                    val allDeferred = async { executeSearchRequest(query, SearchFilter.ALL) }
                    val songsDeferred = async { executeSearchRequest(query, SearchFilter.SONGS) }

                    val allRes = allDeferred.await()
                    val songsRes = songsDeferred.await()

                    val primary = allRes.getOrElse { return@coroutineScope Result.failure(it) }
                    val secondary = songsRes.getOrNull()

                    if (secondary == null) {
                        Result.success(primary)
                    } else {
                        Result.success(mergeAllAndSongsResults(primary, secondary))
                    }
                }
                SearchFilter.SONGS -> coroutineScope {
                    val songsDeferred = async { executeSearchRequest(query, SearchFilter.SONGS) }
                    val allDeferred = async { executeSearchRequest(query, SearchFilter.ALL) }

                    val songsRes = songsDeferred.await()
                    val allRes = allDeferred.await()

                    val primary = songsRes.getOrElse { return@coroutineScope Result.failure(it) }
                    val allData = allRes.getOrNull()

                    if (allData == null) {
                        Result.success(primary)
                    } else {
                        val mergedSongs = mutableListOf<Track>()
                        val songsById = primary.songs.associateBy { it.id }
                        for (track in allData.songs) {
                            val enriched = songsById[track.id] ?: track
                            if (enriched.durationText != "--:--" && mergedSongs.none { it.id == enriched.id }) {
                                mergedSongs.add(enriched)
                            }
                        }
                        for (track in primary.songs) {
                            if (mergedSongs.none { it.id == track.id }) {
                                mergedSongs.add(track)
                            }
                        }
                        Result.success(primary.copy(songs = mergedSongs))
                    }
                }
                else -> executeSearchRequest(query, filter)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtém a Rádio Dinâmica ("Up Next") e os Chips de Filtro via endpoint `/youtubei/v1/next`.
     *
     * @param videoId ID da música inicial para gerar a rádio ("RDAMVM" + videoId).
     * @param params Parâmetro opcional de filtro de chip (ex: Descobrir, Familiar, Popular).
     */
    suspend fun getUpNextRadio(
        videoId: String,
        params: String? = null
    ): Result<UpNextResult> = withContext(Dispatchers.IO) {
        if (videoId.isBlank()) {
            return@withContext Result.success(UpNextResult())
        }

        try {
            val payload = buildNextPayload(videoId, params)
            val request = Request.Builder()
                .url(NEXT_URL)
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

            val upNextResult = parseNextResponse(body)
            Result.success(upNextResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    internal fun buildNextPayload(videoId: String, params: String? = null): JSONObject {
        val root = JSONObject()
        val context = JSONObject()
        val client = JSONObject()

        client.put("clientName", "WEB_REMIX")
        client.put("clientVersion", "1.20231204.01.00")
        client.put("hl", "pt-BR")
        client.put("gl", "BR")

        context.put("client", client)
        root.put("context", context)
        root.put("videoId", videoId)

        val resolvedPlaylistId = if (!params.isNullOrBlank()) {
            chipPlaylistIdMap[params] ?: "RDAMVM$videoId"
        } else {
            "RDAMVM$videoId"
        }
        root.put("playlistId", resolvedPlaylistId)
        root.put("isAudioOnly", true)
        root.put("enablePersistentPlaylistPanel", true)

        if (!params.isNullOrBlank()) {
            root.put("params", params)
        }
        return root
    }

    /**
     * Faz o parse da resposta do endpoint `/youtubei/v1/next`, extraindo:
     * - A lista de próximas músicas de `playlistPanelRenderer`.
     * - Os chips de filtro de `relatedChipCloudRenderer` / `subHeaderChipCloud` (`chipCloudRenderer`).
     * - O título de origem da fila (ex: "Mix de Stay").
     */
    fun parseNextResponse(jsonString: String): UpNextResult {
        if (jsonString.isBlank()) return UpNextResult()

        return try {
            val root = JSONObject(jsonString)

            val tabs = root.optJSONObject("contents")
                ?.optJSONObject("singleColumnMusicWatchNextResultsRenderer")
                ?.optJSONObject("tabbedRenderer")
                ?.optJSONObject("watchNextTabbedResultsRenderer")
                ?.optJSONArray("tabs")

            val musicQueueRenderer = tabs?.optJSONObject(0)
                ?.optJSONObject("tabRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("musicQueueRenderer")
                ?: root.optJSONObject("musicQueueRenderer")

            val playlistPanel = musicQueueRenderer
                ?.optJSONObject("content")
                ?.optJSONObject("playlistPanelRenderer")
                ?: root.optJSONObject("playlistPanelRenderer")
                ?: root.optJSONObject("continuationContents")
                    ?.optJSONObject("playlistPanelContinuation")

            // 1. Extrair título de origem da fila (ex: "Mix de Stay")
            val headerSubtitleRuns = musicQueueRenderer
                ?.optJSONObject("header")
                ?.optJSONObject("musicQueueHeaderRenderer")
                ?.optJSONObject("subtitle")
                ?.optJSONArray("runs")
            val queueSourceTitle = extractSegmentsFromRuns(headerSubtitleRuns).firstOrNull()
                ?: playlistPanel?.optString("title")?.takeIf { it.isNotBlank() }

            // 2. Extrair lista de próximas músicas de playlistPanelRenderer
            val tracks = mutableListOf<Track>()
            val panelContents = playlistPanel?.optJSONArray("contents") ?: JSONArray()
            for (i in 0 until panelContents.length()) {
                val itemObj = panelContents.optJSONObject(i) ?: continue
                val videoRenderer = itemObj.optJSONObject("playlistPanelVideoRenderer")
                    ?: itemObj.optJSONObject("playlistPanelVideoWrapperRenderer")
                        ?.optJSONObject("primaryRenderer")
                        ?.optJSONObject("playlistPanelVideoRenderer")
                    ?: continue

                val parsedTrack = parsePlaylistPanelVideoRenderer(videoRenderer)
                if (parsedTrack != null && tracks.none { it.id == parsedTrack.id }) {
                    tracks.add(parsedTrack)
                }
            }

            // 3. Extrair Chips de filtro de relatedChipCloudRenderer / subHeaderChipCloud
            val chips = mutableListOf<QueueFilterChip>()
            val chipCandidates = listOfNotNull(
                musicQueueRenderer?.optJSONObject("relatedChipCloudRenderer"),
                musicQueueRenderer?.optJSONObject("subHeaderChipCloud"),
                musicQueueRenderer?.optJSONObject("header")
                    ?.optJSONObject("musicQueueHeaderRenderer")
                    ?.optJSONObject("relatedChipCloudRenderer"),
                musicQueueRenderer?.optJSONObject("header")
                    ?.optJSONObject("musicQueueHeaderRenderer")
                    ?.optJSONObject("subHeaderChipCloud"),
                playlistPanel?.optJSONObject("relatedChipCloudRenderer"),
                playlistPanel?.optJSONObject("subHeaderChipCloud"),
                root.optJSONObject("relatedChipCloudRenderer"),
                root.optJSONObject("subHeaderChipCloud")
            )

            for (candidate in chipCandidates) {
                parseChipsFromContainer(candidate, chips)
            }

            UpNextResult(
                tracks = tracks,
                chips = chips,
                queueSourceTitle = queueSourceTitle
            )
        } catch (e: Exception) {
            UpNextResult()
        }
    }

    private fun parsePlaylistPanelVideoRenderer(renderer: JSONObject): Track? {
        val titleRuns = renderer.optJSONObject("title")?.optJSONArray("runs") ?: return null
        val title = extractSegmentsFromRuns(titleRuns).joinToString(" • ").trim()
        if (title.isEmpty()) return null

        val videoId = renderer.optString("videoId").ifEmpty {
            renderer.optJSONObject("navigationEndpoint")
                ?.optJSONObject("watchEndpoint")
                ?.optString("videoId") ?: ""
        }
        if (videoId.isEmpty()) return null

        val bylineRuns = renderer.optJSONObject("longBylineText")?.optJSONArray("runs")
            ?: renderer.optJSONObject("shortBylineText")?.optJSONArray("runs")
            ?: JSONArray()
        val bylineSegments = extractSegmentsFromRuns(bylineRuns)

        val filteredByline = bylineSegments.filter { segment ->
            val lower = segment.lowercase(Locale.ROOT)
            lower !in SONG_MARKERS &&
                lower !in NON_SONG_MARKERS &&
                !isPlayOrViewCountSegment(segment) &&
                !YEAR_REGEX.matcher(segment).matches() &&
                extractDurationFromSegment(segment) == null
        }

        val artistName = filteredByline.firstOrNull() ?: "Artista Desconhecido"
        val albumName = filteredByline.getOrNull(1)

        val lengthRuns = renderer.optJSONObject("lengthText")?.optJSONArray("runs")
        val rawLength = extractSegmentsFromRuns(lengthRuns).firstOrNull() ?: ""
        val durationText = extractDurationFromSegment(rawLength) ?: "--:--"

        val thumbnailUrl = getBestThumbnailUrl(
            renderer.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
        )

        return Track(
            id = videoId,
            title = title,
            artist = artistName,
            album = albumName,
            thumbnailUrl = thumbnailUrl,
            durationText = durationText,
            sourceIdentifier = "INNERTUBE_NEXT_RADIO"
        )
    }

    private fun parseChipsFromContainer(
        container: JSONObject,
        outChips: MutableList<QueueFilterChip>
    ) {
        val chipCloud = container.optJSONObject("chipCloudRenderer")
            ?: container.optJSONObject("relatedChipCloudRenderer")?.optJSONObject("chipCloudRenderer")
            ?: container.optJSONObject("relatedChipCloudRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("chipCloudRenderer")
            ?: container.optJSONObject("content")?.optJSONObject("chipCloudRenderer")
            ?: container

        val chipsArray = chipCloud.optJSONArray("chips") ?: return
        for (i in 0 until chipsArray.length()) {
            val chipItem = chipsArray.optJSONObject(i) ?: continue
            val chipRenderer = chipItem.optJSONObject("chipCloudChipRenderer") ?: chipItem

            val textRuns = chipRenderer.optJSONObject("text")?.optJSONArray("runs")
            val chipTitle = extractSegmentsFromRuns(textRuns).firstOrNull()
                ?: chipRenderer.optJSONObject("accessibilityData")
                    ?.optJSONObject("accessibilityData")
                    ?.optString("label")
                    ?.trim()
                ?: ""

            if (chipTitle.isEmpty()) continue

            val navEndpoint = chipRenderer.optJSONObject("navigationEndpoint")
                ?: chipRenderer.optJSONObject("onTap")
            val watchEndpoint = navEndpoint
                ?.optJSONObject("queueUpdateCommand")
                ?.optJSONObject("fetchContentsCommand")
                ?.optJSONObject("watchEndpoint")
                ?: navEndpoint?.optJSONObject("watchEndpoint")

            val chipParams = watchEndpoint?.optString("params")?.takeIf { it.isNotEmpty() }
                ?: chipRenderer.optString("params").takeIf { it.isNotEmpty() }
                ?: continue

            val chipPlaylistId = watchEndpoint?.optString("playlistId")?.takeIf { it.isNotEmpty() }
            if (chipPlaylistId != null) {
                chipPlaylistIdMap[chipParams] = chipPlaylistId
            }

            val isSelected = chipRenderer.optBoolean("isSelected", false)

            if (outChips.none { it.params == chipParams || it.title.equals(chipTitle, ignoreCase = true) }) {
                outChips.add(
                    QueueFilterChip(
                        title = chipTitle,
                        params = chipParams,
                        playlistId = chipPlaylistId,
                        isSelected = isSelected
                    )
                )
            }
        }
    }

    private fun executeSearchRequest(
        query: String,
        filter: SearchFilter
    ): Result<SearchResult> {
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
            return Result.failure(
                IOException("Servidor respondeu com código HTTP ${response.code}")
            )
        }

        val body = response.body?.string()
            ?: return Result.failure(IOException("Resposta vazia do servidor"))

        val searchResult = parseSearchResponse(body)
        return Result.success(searchResult)
    }

    /**
     * Mescla o resultado geral (que contém o Top Result `musicCardShelfRenderer` e artistas)
     * com a estante de músicas dedicada (onde 100% das faixas trazem duração `MM:SS`).
     */
    private fun mergeAllAndSongsResults(
        allResult: SearchResult,
        songsResult: SearchResult
    ): SearchResult {
        val songsById = songsResult.songs.associateBy { it.id }
        val mergedSongs = mutableListOf<Track>()

        for (track in allResult.songs) {
            val fromSongsTab = songsById[track.id]
            val resolvedTrack = if (track.durationText == "--:--" && fromSongsTab != null) {
                fromSongsTab
            } else {
                track
            }
            if (mergedSongs.none { it.id == resolvedTrack.id }) {
                mergedSongs.add(resolvedTrack)
            }
        }

        for (track in songsResult.songs) {
            if (mergedSongs.none { it.id == track.id }) {
                mergedSongs.add(track)
            }
        }

        val hasValidDurations = mergedSongs.any { it.durationText != "--:--" }
        val finalSongs = if (hasValidDurations) {
            mergedSongs.filter { it.durationText != "--:--" }
        } else {
            mergedSongs
        }

        return SearchResult(
            songs = finalSongs,
            artists = allResult.artists,
            topArtist = allResult.topArtist
        )
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

                // 1. Destaque Principal (Top Result / musicCardShelfRenderer) — priorizado no topo
                val cardShelf = section.optJSONObject("musicCardShelfRenderer")
                if (cardShelf != null) {
                    val (cardTrack, cardArtist) = parseCardShelf(cardShelf)
                    if (cardArtist != null && topArtist == null) {
                        topArtist = cardArtist
                    }
                    if (cardTrack != null && songs.none { it.id == cardTrack.id }) {
                        songs.add(0, cardTrack)
                    }

                    // Extrai também as músicas de destaque aninhadas em musicCardShelfRenderer.contents
                    val cardContents = cardShelf.optJSONArray("contents")
                    if (cardContents != null) {
                        for (cIdx in 0 until cardContents.length()) {
                            val cardItem = cardContents.optJSONObject(cIdx) ?: continue
                            val listItem = cardItem.optJSONObject("musicResponsiveListItemRenderer") ?: continue
                            parseResponsiveListItem(
                                item = listItem,
                                songs = songs,
                                artists = artists,
                                fallbackArtist = cardArtist?.name
                            )
                        }
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
        val title = titleRuns.optJSONObject(0)?.optString("text")?.trim() ?: return Pair(null, null)

        val subtitleRuns = card.optJSONObject("subtitle")?.optJSONArray("runs") ?: JSONArray()
        val subtitleParts = extractSegmentsFromRuns(subtitleRuns)
        val fullSubtitle = subtitleParts.joinToString(" • ")

        val thumbnailUrl = getBestThumbnailUrl(
            card.optJSONObject("thumbnail")
                ?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails")
        )

        if (subtitleParts.any { it.lowercase(Locale.ROOT) in ARTIST_MARKERS } ||
            fullSubtitle.contains("Artista", ignoreCase = true) ||
            fullSubtitle.contains("Artist", ignoreCase = true)
        ) {
            val artist = Artist(
                id = "card_${title.hashCode()}",
                name = title,
                subtitle = fullSubtitle,
                thumbnailUrl = thumbnailUrl
            )
            return Pair(null, artist)
        }

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
            val extractedDuration = extractDurationFromSegment(part)
            if (extractedDuration != null) {
                duration = extractedDuration
            } else if (
                part.lowercase(Locale.ROOT) !in NON_SONG_MARKERS &&
                part.lowercase(Locale.ROOT) !in SONG_MARKERS &&
                !isPlayOrViewCountSegment(part) &&
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
        artists: MutableList<Artist>,
        fallbackArtist: String? = null
    ) {
        val flexCols = item.optJSONArray("flexColumns") ?: return
        if (flexCols.length() == 0) return

        val col0Runs = flexCols.optJSONObject(0)
            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.optJSONObject("text")
            ?.optJSONArray("runs") ?: return

        val title = col0Runs.optJSONObject(0)?.optString("text")?.trim() ?: return
        if (title.isEmpty()) return

        val col1TextObj = if (flexCols.length() > 1) {
            flexCols.optJSONObject(1)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")
        } else {
            null
        }
        val col1Runs = col1TextObj?.optJSONArray("runs") ?: JSONArray()
        val col1Segments = extractSegmentsFromRuns(col1Runs)
        val col1FullText = col1Segments.joinToString(" • ")

        val thumbnailUrl = getBestThumbnailUrl(
            item.optJSONObject("thumbnail")
                ?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails")
        )

        var videoId = ""
        val overlayPlayRenderer = item.optJSONObject("overlay")
            ?.optJSONObject("musicItemThumbnailOverlayRenderer")
            ?.optJSONObject("content")
            ?.optJSONObject("musicPlayButtonRenderer")

        val overlayPlay = overlayPlayRenderer
            ?.optJSONObject("playNavigationEndpoint")
            ?.optJSONObject("watchEndpoint")
            ?.optString("videoId") ?: ""

        if (overlayPlay.isNotEmpty()) {
            videoId = overlayPlay
        } else {
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

        val durationText = extractDurationFromItem(item, flexCols)

        val firstSegmentLower = col1Segments.firstOrNull()?.lowercase(Locale.ROOT) ?: ""
        val hasSongMarker = col1Segments.any { it.lowercase(Locale.ROOT) in SONG_MARKERS }
        val hasArtistMarker = col1Segments.any { it.lowercase(Locale.ROOT) in ARTIST_MARKERS }
        val hasExplicitNonSongMarker = firstSegmentLower in NON_SONG_MARKERS

        val isMusic = !hasExplicitNonSongMarker && (
            hasSongMarker || videoId.isNotEmpty() || durationText != "--:--"
        )

        if (isMusic) {
            val filteredMeta = col1Segments.filter { segment ->
                val lower = segment.lowercase(Locale.ROOT)
                lower !in SONG_MARKERS &&
                    lower !in NON_SONG_MARKERS &&
                    !isPlayOrViewCountSegment(segment) &&
                    extractDurationFromSegment(segment) == null
            }

            val artistName = when {
                filteredMeta.isNotEmpty() -> filteredMeta[0]
                !fallbackArtist.isNullOrBlank() -> fallbackArtist
                else -> extractArtistFromAccessibility(overlayPlayRenderer, title) ?: "Desconhecido"
            }
            val albumName: String? = if (filteredMeta.size > 1) filteredMeta[1] else null

            val trackId = if (videoId.isNotEmpty()) videoId else "track_${title.hashCode()}"
            val existingIndex = songs.indexOfFirst { it.id == trackId }
            val newTrack = Track(
                id = trackId,
                title = title,
                artist = artistName,
                album = albumName,
                thumbnailUrl = thumbnailUrl,
                durationText = durationText,
                sourceIdentifier = "INNERTUBE_WEB_REMIX"
            )

            if (existingIndex == -1) {
                songs.add(newTrack)
            } else if (songs[existingIndex].durationText == "--:--" && durationText != "--:--") {
                songs[existingIndex] = newTrack
            }
        } else if (hasArtistMarker) {
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

    private fun extractSegmentsFromRuns(runs: JSONArray?): List<String> {
        if (runs == null || runs.length() == 0) return emptyList()
        val fullText = buildString {
            for (i in 0 until runs.length()) {
                append(runs.optJSONObject(i)?.optString("text") ?: "")
            }
        }
        return fullText
            .split("•")
            .map { it.replace('\u00A0', ' ').trim() }
            .filter { it.isNotEmpty() }
    }

    private fun extractDurationFromItem(item: JSONObject, flexCols: JSONArray): String {
        for (c in 0 until flexCols.length()) {
            val textObj = flexCols.optJSONObject(c)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text") ?: continue
            val runs = textObj.optJSONArray("runs") ?: continue

            val segments = extractSegmentsFromRuns(runs)
            for (segment in segments) {
                val matched = extractDurationFromSegment(segment)
                if (matched != null) return matched
            }

            for (r in 0 until runs.length()) {
                val runText = runs.optJSONObject(r)?.optString("text")?.trim() ?: ""
                val matched = extractDurationFromSegment(runText)
                if (matched != null) return matched
            }
        }

        val fixedCols = item.optJSONArray("fixedColumns")
        if (fixedCols != null) {
            for (c in 0 until fixedCols.length()) {
                val runs = fixedCols.optJSONObject(c)
                    ?.optJSONObject("musicResponsiveListItemFixedColumnRenderer")
                    ?.optJSONObject("text")
                    ?.optJSONArray("runs") ?: continue
                val segments = extractSegmentsFromRuns(runs)
                for (segment in segments) {
                    val matched = extractDurationFromSegment(segment)
                    if (matched != null) return matched
                }
            }
        }

        for (c in 0 until flexCols.length()) {
            val label = flexCols.optJSONObject(c)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")
                ?.optJSONObject("accessibility")
                ?.optJSONObject("accessibilityData")
                ?.optString("label") ?: continue
            val matcher = ACCESSIBILITY_DURATION_REGEX.matcher(label)
            if (matcher.find()) {
                val min = matcher.group(1)?.toIntOrNull() ?: 0
                val sec = matcher.group(2)?.toIntOrNull() ?: 0
                return String.format(Locale.US, "%02d:%02d", min, sec)
            }
        }

        return "--:--"
    }

    private fun extractDurationFromSegment(segment: String): String? {
        val cleaned = segment.replace('\u00A0', ' ').trim()
        if (DURATION_REGEX.matcher(cleaned).matches()) {
            return normalizeDurationText(cleaned)
        }
        val inlineMatcher = DURATION_INLINE_REGEX.matcher(cleaned)
        if (inlineMatcher.matches()) {
            return normalizeDurationText(cleaned)
        }
        return null
    }

    private fun isPlayOrViewCountSegment(segment: String): Boolean {
        val lower = segment.lowercase(Locale.ROOT)
        return lower.contains("visualiza") ||
            lower.contains("tocou ") ||
            lower.contains("vezes") ||
            lower.contains("ouvintes") ||
            lower.contains("inscritos") ||
            lower.contains("marcações") ||
            lower.contains("gostei") ||
            lower.contains("views") ||
            lower.contains("plays") ||
            lower.contains("likes")
    }

    private fun extractArtistFromAccessibility(
        playButtonRenderer: JSONObject?,
        trackTitle: String
    ): String? {
        val label = playButtonRenderer
            ?.optJSONObject("accessibilityPlayData")
            ?.optJSONObject("accessibilityData")
            ?.optString("label")
            ?.trim() ?: return null
        val prefixRemoved = label
            .removePrefix("Reproduzir ")
            .removePrefix("Play ")
            .trim()
        val dashPrefix = "$trackTitle - "
        if (prefixRemoved.startsWith(dashPrefix, ignoreCase = true)) {
            val extracted = prefixRemoved.substring(dashPrefix.length).trim()
            if (extracted.isNotEmpty()) return extracted
        }
        return null
    }

    private fun getBestThumbnailUrl(thumbnails: JSONArray?): String? {
        if (thumbnails == null || thumbnails.length() == 0) return null
        val last = thumbnails.optJSONObject(thumbnails.length() - 1)
        val url = last?.optString("url") ?: return null
        return if (url.startsWith("//")) "https:$url" else url
    }
}
