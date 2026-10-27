package com.nerf.music.data.network

import com.nerf.music.data.model.AlbumDetail
import com.nerf.music.data.model.ArtistDetail
import com.nerf.music.data.model.Artist
import com.nerf.music.data.model.HomeChip
import com.nerf.music.data.model.HomeFeedData
import com.nerf.music.data.model.HomeSection
import com.nerf.music.data.model.HomeSectionStyle
import com.nerf.music.data.model.QueueFilterChip
import com.nerf.music.data.model.SearchFilter
import com.nerf.music.data.model.SearchResult
import com.nerf.music.data.model.Track
import com.nerf.music.data.model.UpNextResult
import com.nerf.music.data.model.TrackRelatedData
import com.nerf.music.data.model.RelatedSection
import com.nerf.music.data.model.LyricsData
import com.nerf.music.data.model.SyncedLyricsLine
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
        private const val BROWSE_URL = "https://music.youtube.com/youtubei/v1/browse"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

        private val DURATION_REGEX = Pattern.compile("^(?:(\\d{1,2}):)?(\\d{1,2}):(\\d{2})$")
        private val DURATION_INLINE_REGEX = Pattern.compile("\\b(?:(\\d{1,2}):)?(\\d{1,2}):(\\d{2})\\b")
        private val YEAR_REGEX = Pattern.compile("^\\d{4}$")
        private val ACCESSIBILITY_DURATION_REGEX = Pattern.compile(
            "(\\d{1,2})\\s*(?:minutos?|minutes?|mins?)(?:\\s*(?:e|and|,)?\\s*(\\d{1,2})\\s*(?:segundos?|seconds?|secs?))?",
            Pattern.CASE_INSENSITIVE
        )
        private val ACCESSIBILITY_SECONDS_ONLY_REGEX = Pattern.compile(
            "(\\d{1,2})\\s*(?:segundos?|seconds?|secs?)",
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
                            if (mergedSongs.none { it.id == enriched.id }) {
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

    /**
     * Obtém o feed principal da Home via endpoint `/youtubei/v1/browse` com browseId "FEmusic_home".
     */
    /**
     * Obtém os detalhes completos de um álbum ou playlist via endpoint `/youtubei/v1/browse`.
     */
    /**
     * Obtém as recomendações e dados relacionados a uma faixa (Related / "Relacionados")
     * via [relatedBrowseId] retornado pelo endpoint /youtubei/v1/next.
     */
    /**
     * Obtém as letras oficiais via YouTube Music (parceria com Musixmatch) usando [lyricsBrowseId]
     * ou resolvendo dinamicamente via [videoId].
     */
    suspend fun getTrackLyrics(
        videoId: String,
        lyricsBrowseId: String? = null
    ): Result<LyricsData?> = withContext(Dispatchers.IO) {
        if (videoId.isBlank()) return@withContext Result.success(null)
        try {
            var targetBrowseId = lyricsBrowseId
            if (targetBrowseId.isNullOrBlank()) {
                val nextResult = getUpNextRadio(videoId)
                targetBrowseId = nextResult.getOrNull()?.lyricsBrowseId
            }
            if (targetBrowseId.isNullOrBlank()) {
                return@withContext Result.success(null)
            }
            val body = executeBrowseRequest(targetBrowseId, null).getOrNull()
                ?: return@withContext Result.success(null)

            val parsed = parseLyricsResponse(body, videoId)
            Result.success(parsed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun parseLyricsResponse(jsonString: String, videoId: String): LyricsData? {
        if (jsonString.isBlank()) return null
        return try {
            val root = JSONObject(jsonString)
            val contents = root.optJSONObject("contents") ?: return null

            // Se for messageRenderer (ex: "Letra não disponível")
            if (contents.has("messageRenderer")) {
                return null
            }

            val sectionList = contents.optJSONObject("sectionListRenderer")?.optJSONArray("contents")
            if (sectionList != null) {
                for (i in 0 until sectionList.length()) {
                    val item = sectionList.optJSONObject(i) ?: continue
                    val shelf = item.optJSONObject("musicDescriptionShelfRenderer") ?: continue
                    val runs = shelf.optJSONObject("description")?.optJSONArray("runs") ?: continue
                    val textBuilder = StringBuilder()
                    for (r in 0 until runs.length()) {
                        val runObj = runs.optJSONObject(r)
                        textBuilder.append(runObj?.optString("text").orEmpty())
                    }
                    val fullText = textBuilder.toString().trim()
                    if (fullText.isNotBlank()) {
                        val footerRuns = shelf.optJSONObject("footer")?.optJSONArray("runs")
                        val footerBuilder = StringBuilder()
                        if (footerRuns != null) {
                            for (f in 0 until footerRuns.length()) {
                                footerBuilder.append(footerRuns.optJSONObject(f)?.optString("text").orEmpty())
                            }
                        }
                        val footer = footerBuilder.toString().trim()
                        val sourceName = if (footer.isNotBlank()) "YouTube Music ($footer)" else "YouTube Music"

                        return LyricsData(
                            trackId = videoId,
                            isSynced = false,
                            plainLyrics = fullText,
                            syncedLines = emptyList(),
                            source = sourceName
                        )
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getTrackRelated(relatedBrowseId: String): Result<TrackRelatedData> = withContext(Dispatchers.IO) {
        if (relatedBrowseId.isBlank()) {
            return@withContext Result.success(TrackRelatedData())
        }
        try {
            val body = executeBrowseRequest(relatedBrowseId, null).getOrThrow()
            val parsed = parseRelatedResponse(body)
            Result.success(parsed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun parseRelatedResponse(jsonString: String): TrackRelatedData {
        if (jsonString.isBlank()) return TrackRelatedData()
        return try {
            val root = JSONObject(jsonString)
            val contentsObj = root.optJSONObject("contents")
            val sectionContents = extractSectionListContents(contentsObj)

            val sections = mutableListOf<RelatedSection>()
            var aboutHeader: String? = null
            var aboutDesc: String? = null

            for (i in 0 until sectionContents.length()) {
                val sectionObj = sectionContents.optJSONObject(i) ?: continue

                // 1. Descrição sobre o artista
                val descShelf = sectionObj.optJSONObject("musicDescriptionShelfRenderer")
                if (descShelf != null) {
                    val hRuns = descShelf.optJSONObject("header")?.optJSONArray("runs")
                    aboutHeader = extractSegmentsFromRuns(hRuns).firstOrNull() ?: "Sobre o artista"
                    val dRuns = descShelf.optJSONObject("description")?.optJSONArray("runs")
                    aboutDesc = extractSegmentsFromRuns(dRuns).joinToString(" ")
                    continue
                }

                // 2. Carrossel de músicas / playlists / artistas
                val carousel = sectionObj.optJSONObject("musicCarouselShelfRenderer") ?: continue
                val header = carousel.optJSONObject("header")
                    ?.optJSONObject("musicCarouselShelfBasicHeaderRenderer")
                val title = header?.optJSONObject("title")
                    ?.optJSONArray("runs")
                    ?.optJSONObject(0)
                    ?.optString("text")
                    ?.trim()
                    ?: continue

                val numItemsPerColumn = carousel.optString("numItemsPerColumn", "1").toIntOrNull() ?: 1
                val itemsArray = carousel.optJSONArray("contents") ?: continue
                val sectionSongs = mutableListOf<Track>()
                val dummyArtists = mutableListOf<Artist>()
                var hasResponsiveItems = false

                for (j in 0 until itemsArray.length()) {
                    val itemWrapper = itemsArray.optJSONObject(j) ?: continue
                    val responsiveItem = itemWrapper.optJSONObject("musicResponsiveListItemRenderer")
                    if (responsiveItem != null) {
                        hasResponsiveItems = true
                        parseResponsiveListItem(responsiveItem, sectionSongs, dummyArtists)
                    } else {
                        val twoRowItem = itemWrapper.optJSONObject("musicTwoRowItemRenderer")
                        if (twoRowItem != null) {
                            val track = parseTwoRowItem(twoRowItem)
                            if (track != null && sectionSongs.none { it.id == track.id }) {
                                sectionSongs.add(track)
                            }
                        }
                    }
                }

                if (sectionSongs.isNotEmpty()) {
                    val style = if (numItemsPerColumn >= 4 || hasResponsiveItems) {
                        HomeSectionStyle.QUICK_PICKS_GRID
                    } else {
                        HomeSectionStyle.TWO_ROW_CAROUSEL
                    }
                    sections.add(
                        RelatedSection(
                            title = title,
                            items = sectionSongs,
                            style = style
                        )
                    )
                }
            }

            TrackRelatedData(
                sections = sections,
                aboutArtistHeader = aboutHeader,
                aboutArtistDescription = aboutDesc
            )
        } catch (e: Exception) {
            TrackRelatedData()
        }
    }

    private fun extractSectionListContents(contentsObj: JSONObject?): JSONArray {
        if (contentsObj == null) return JSONArray()
        // 1. Direct sectionListRenderer
        val direct = contentsObj.optJSONObject("sectionListRenderer")?.optJSONArray("contents")
        if (direct != null && direct.length() > 0) return direct

        // 2. singleColumnBrowseResultsRenderer
        val singleCol = contentsObj.optJSONObject("singleColumnBrowseResultsRenderer")
        if (singleCol != null) {
            val tabs = singleCol.optJSONArray("tabs")
            if (tabs != null) {
                for (i in 0 until tabs.length()) {
                    val tabContent = tabs.optJSONObject(i)?.optJSONObject("tabRenderer")?.optJSONObject("content")
                    val sec = tabContent?.optJSONObject("sectionListRenderer")?.optJSONArray("contents")
                    if (sec != null && sec.length() > 0) return sec
                }
            }
        }

        // 3. twoColumnBrowseResultsRenderer
        val twoCol = contentsObj.optJSONObject("twoColumnBrowseResultsRenderer")
        if (twoCol != null) {
            val secContents = twoCol.optJSONObject("secondaryContents")?.optJSONObject("sectionListRenderer")?.optJSONArray("contents")
            if (secContents != null && secContents.length() > 0) return secContents

            val tabs = twoCol.optJSONArray("tabs")
            if (tabs != null) {
                for (i in 0 until tabs.length()) {
                    val tabContent = tabs.optJSONObject(i)?.optJSONObject("tabRenderer")?.optJSONObject("content")
                    val sec = tabContent?.optJSONObject("sectionListRenderer")?.optJSONArray("contents")
                    if (sec != null && sec.length() > 0) return sec
                }
            }
        }

        return direct ?: JSONArray()
    }

    suspend fun getAlbumOrPlaylist(browseId: String): Result<AlbumDetail> = withContext(Dispatchers.IO) {
        try {
            val body = executeBrowseRequest(browseId, null).getOrThrow()
            val album = parseAlbumResponse(body, browseId)
            Result.success(album)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtém os detalhes completos do perfil de um artista via endpoint `/youtubei/v1/browse`.
     */
    suspend fun getArtist(browseId: String): Result<ArtistDetail> = withContext(Dispatchers.IO) {
        try {
            var targetBrowseId = browseId
            if (!targetBrowseId.startsWith("UC") && !targetBrowseId.startsWith("FE")) {
                val query = targetBrowseId.removePrefix("card_").removePrefix("artist_")
                val searchRes = search(query).getOrNull()
                val foundArtist = searchRes?.topArtist ?: searchRes?.artists?.firstOrNull()
                if (foundArtist != null && (foundArtist.id.startsWith("UC") || foundArtist.id.startsWith("FE"))) {
                    targetBrowseId = foundArtist.id
                }
            }
            val body = executeBrowseRequest(targetBrowseId, null).getOrThrow()
            val artist = parseArtistResponse(body, targetBrowseId)
            Result.success(artist)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtém o feed da Home personalizado de acordo com as faixas e artistas mais ouvidos pelo usuário.
     */
    suspend fun getPersonalizedHomeFeedData(
        seedVideoId: String? = null,
        topArtists: List<String> = emptyList(),
        params: String? = null
    ): Result<HomeFeedData> = withContext(Dispatchers.IO) {
        try {
            val baseFeedResult = getHomeFeedData(params)
            val baseFeed = baseFeedResult.getOrThrow()
            val chips = baseFeed.chips
            val sections = baseFeed.sections.toMutableList()

            // 1. Personalização da "Escolha a dedo" (Quick Picks) usando a rádio do favorito do usuário
            if (!seedVideoId.isNullOrBlank()) {
                val radioRes = getUpNextRadio(seedVideoId)
                radioRes.getOrNull()?.let { upNext ->
                    if (upNext.tracks.isNotEmpty()) {
                        // Anti-duplicacao: Remove a faixa que ja esta tocando ou em destaque para diversificar o grid
                        val filteredTracks = upNext.tracks.filterNot { it.id == seedVideoId }
                        val finalTracks = if (filteredTracks.size >= 8) filteredTracks else upNext.tracks
                        val quickPicks = HomeSection(
                            title = "Escolha a dedo",
                            items = finalTracks.take(16),
                            style = HomeSectionStyle.QUICK_PICKS_GRID
                        )
                        sections.removeAll { it.style == HomeSectionStyle.QUICK_PICKS_GRID }
                        sections.add(0, quickPicks)
                    }
                }
            }

            // 2. Personalização baseada nos artistas mais ouvidos pelo ouvinte
            if (topArtists.isNotEmpty()) {
                val favoriteArtist = topArtists.first()
                val artistSearchRes = search(favoriteArtist, SearchFilter.SONGS).getOrNull()

                if (artistSearchRes != null && artistSearchRes.songs.isNotEmpty()) {
                    val recommendedSection = HomeSection(
                        title = "Músicas em alta para você",
                        strapline = "COM BASE NO QUE VOCÊ OUVE ($favoriteArtist)",
                        items = artistSearchRes.songs.take(10),
                        style = HomeSectionStyle.TWO_ROW_CAROUSEL
                    )
                    val insertIdx = if (sections.isNotEmpty() && sections[0].style == HomeSectionStyle.QUICK_PICKS_GRID) 1 else 0
                    sections.add(insertIdx, recommendedSection)
                }

                if (topArtists.size > 1) {
                    val secondArtist = topArtists[1]
                    val secondSearchRes = search(secondArtist, SearchFilter.SONGS).getOrNull()
                    if (secondSearchRes != null && secondSearchRes.songs.isNotEmpty()) {
                        val secondSection = HomeSection(
                            title = "Mais de $secondArtist",
                            strapline = "OUTRO ARTISTA QUE VOCÊ CURTE",
                            items = secondSearchRes.songs.take(10),
                            style = HomeSectionStyle.TWO_ROW_CAROUSEL
                        )
                        val insertIdx = if (sections.size > 2) 2 else sections.size
                        sections.add(insertIdx, secondSection)
                    }
                }
            }

            Result.success(HomeFeedData(chips = chips, sections = sections))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getHomeFeed(): Result<List<HomeSection>> =
        getHomeFeedData(params = null).map { it.sections }

    /**
     * Obtém os dados completos da Home (Chips de Humor/Atividade + Seções de Carrosséis e Quick Picks),
     * mesclando `FEmusic_home` e `FEmusic_explore` quando nenhum filtro de chip está selecionado.
     */
    suspend fun getHomeFeedData(params: String? = null): Result<HomeFeedData> = withContext(Dispatchers.IO) {
        try {
            if (!params.isNullOrBlank()) {
                val body = executeBrowseRequest("FEmusic_home", params).getOrThrow()
                val chips = parseHomeChips(body)
                val sections = parseHomeFeedResponse(body)
                return@withContext Result.success(HomeFeedData(chips = chips, sections = sections))
            }

            coroutineScope {
                val homeDeferred = async { executeBrowseRequest("FEmusic_home", null) }
                val exploreDeferred = async { executeBrowseRequest("FEmusic_explore", null) }

                val homeRes = homeDeferred.await()
                val exploreRes = exploreDeferred.await()

                val homeBody = homeRes.getOrElse { return@coroutineScope Result.failure(it) }
                val chips = parseHomeChips(homeBody)
                val mergedSections = parseHomeFeedResponse(homeBody).toMutableList()

                exploreRes.getOrNull()?.let { exploreBody ->
                    val exploreSections = parseHomeFeedResponse(exploreBody)
                    for (sec in exploreSections) {
                        if (mergedSections.none { it.title.equals(sec.title, ignoreCase = true) }) {
                            mergedSections.add(sec)
                        }
                    }
                }

                val firstQuickPicksIdx = mergedSections.indexOfFirst { it.style == HomeSectionStyle.QUICK_PICKS_GRID }
                if (firstQuickPicksIdx > 0) {
                    val quickPicksSec = mergedSections.removeAt(firstQuickPicksIdx)
                    mergedSections.add(0, quickPicksSec)
                }

                Result.success(HomeFeedData(chips = chips, sections = mergedSections))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun executeBrowseRequest(browseId: String, params: String? = null): Result<String> {
        return try {
            val payload = buildBrowsePayload(browseId, params)
            val request = Request.Builder()
                .url(BROWSE_URL)
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
            Result.success(body)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    internal fun buildBrowsePayload(browseId: String, params: String? = null): JSONObject {
        val root = JSONObject()
        val context = JSONObject()
        val client = JSONObject()

        client.put("clientName", "WEB_REMIX")
        client.put("clientVersion", "1.20231204.01.00")
        client.put("hl", "pt-BR")
        client.put("gl", "BR")

        context.put("client", client)
        root.put("context", context)
        root.put("browseId", browseId)
        if (!params.isNullOrBlank()) {
            root.put("params", params)
        }
        return root
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

        if (videoId.startsWith("playlist_")) {
            val rawPlaylistId = videoId.removePrefix("playlist_")
            root.put("playlistId", rawPlaylistId)
        } else {
            root.put("videoId", videoId)
            val resolvedPlaylistId = if (!params.isNullOrBlank()) {
                chipPlaylistIdMap[params] ?: "RDAMVM$videoId"
            } else {
                "RDAMVM$videoId"
            }
            root.put("playlistId", resolvedPlaylistId)
        }

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

            var relatedBrowseId: String? = null
            var lyricsBrowseId: String? = null
            if (tabs != null) {
                for (t in 0 until tabs.length()) {
                    val tabItem = tabs.optJSONObject(t)?.optJSONObject("tabRenderer") ?: continue
                    val titleObj = tabItem.optJSONObject("title")
                    val tabTitle = if (titleObj != null) {
                        extractSegmentsFromRuns(titleObj.optJSONArray("runs")).joinToString(" ").lowercase(Locale.ROOT)
                    } else {
                        tabItem.optString("title").lowercase(Locale.ROOT)
                    }
                    val bId = tabItem.optJSONObject("endpoint")?.optJSONObject("browseEndpoint")?.optString("browseId")
                    if (!bId.isNullOrBlank()) {
                        if (tabTitle.contains("relacionad") || tabTitle.contains("related") ||
                            bId.startsWith("MPTR") || bId.startsWith("FEmusic_related")) {
                            relatedBrowseId = bId
                        } else if (tabTitle.contains("letra") || tabTitle.contains("lyric") ||
                            bId.startsWith("MPLY") || bId.startsWith("FEmusic_lyrics")) {
                            lyricsBrowseId = bId
                        }
                    }
                }
            }

            UpNextResult(
                tracks = tracks,
                chips = chips,
                queueSourceTitle = queueSourceTitle,
                relatedBrowseId = relatedBrowseId,
                lyricsBrowseId = lyricsBrowseId
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

        val lengthObj = renderer.optJSONObject("lengthText")
        val rawLength = lengthObj?.optString("simpleText")?.takeIf { it.isNotBlank() }
            ?: extractSegmentsFromRuns(lengthObj?.optJSONArray("runs")).firstOrNull()
            ?: lengthObj?.optJSONObject("accessibility")?.optJSONObject("accessibilityData")?.optString("label")
            ?: ""
        var durationText = extractDurationFromSegment(rawLength)
        if (durationText == null) {
            durationText = bylineSegments.firstNotNullOfOrNull { extractDurationFromSegment(it) }
        }
        val finalDuration = durationText ?: "--:--"
        val durationSeconds = parseDurationToSeconds(finalDuration)

        val thumbnailUrl = getBestThumbnailUrl(
            renderer.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
        )

        var artistBrowseId: String? = null
        for (r in 0 until bylineRuns.length()) {
            val runObj = bylineRuns.optJSONObject(r) ?: continue
            val bId = runObj.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")?.optString("browseId")
            if (!bId.isNullOrBlank() && bId.startsWith("UC")) {
                artistBrowseId = bId
                break
            }
        }

        return Track(
            id = videoId,
            title = title,
            artist = artistName,
            album = albumName,
            thumbnailUrl = thumbnailUrl,
            durationText = finalDuration,
            durationSeconds = durationSeconds,
            sourceIdentifier = "INNERTUBE_NEXT_RADIO",
            artistBrowseId = artistBrowseId
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

        val finalSongs = mergedSongs.sortedBy { if (it.durationText != "--:--") 0 else 1 }

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
            val browseId = card.optJSONObject("onTap")
                ?.optJSONObject("browseEndpoint")
                ?.optString("browseId")
                ?: card.optJSONObject("title")
                    ?.optJSONArray("runs")
                    ?.optJSONObject(0)
                    ?.optJSONObject("navigationEndpoint")
                    ?.optJSONObject("browseEndpoint")
                    ?.optString("browseId")
                ?: card.optJSONObject("header")
                    ?.optJSONObject("musicCardShelfHeaderBasicRenderer")
                    ?.optJSONObject("title")
                    ?.optJSONArray("runs")
                    ?.optJSONObject(0)
                    ?.optJSONObject("navigationEndpoint")
                    ?.optJSONObject("browseEndpoint")
                    ?.optString("browseId")
                ?: "card_${title.hashCode()}"

            val artist = Artist(
                id = browseId,
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
                ?.optString("browseId")
                ?: col0Runs.optJSONObject(0)
                    ?.optJSONObject("navigationEndpoint")
                    ?.optJSONObject("browseEndpoint")
                    ?.optString("browseId")
                ?: item.optJSONObject("onTap")
                    ?.optJSONObject("browseEndpoint")
                    ?.optString("browseId")
                ?: "artist_${title.hashCode()}"

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

    fun extractDurationFromItem(item: JSONObject, flexCols: JSONArray?): String {
        if (flexCols != null) {
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

        if (flexCols != null) {
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
        }

        return "--:--"
    }

    private fun extractDurationFromSegment(segment: String): String? {
        val cleaned = segment.replace('\u00A0', ' ').trim()
        if (DURATION_REGEX.matcher(cleaned).matches()) {
            return normalizeDurationText(cleaned)
        }
        val inlineMatcher = DURATION_INLINE_REGEX.matcher(cleaned)
        if (inlineMatcher.find()) {
            return normalizeDurationText(inlineMatcher.group(0) ?: cleaned)
        }
        val accessMatcher = ACCESSIBILITY_DURATION_REGEX.matcher(cleaned)
        if (accessMatcher.find()) {
            val min = accessMatcher.group(1)?.toIntOrNull() ?: 0
            val sec = accessMatcher.group(2)?.toIntOrNull() ?: 0
            return String.format(Locale.US, "%02d:%02d", min, sec)
        }
        val secMatcher = ACCESSIBILITY_SECONDS_ONLY_REGEX.matcher(cleaned)
        if (secMatcher.find()) {
            val sec = secMatcher.group(1)?.toIntOrNull() ?: 0
            return String.format(Locale.US, "00:%02d", sec)
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

    /**
     * Extrai os chips de filtro de humor/atividade do cabeçalho da Home (`chipCloudRenderer`).
     */
    /**
     * Faz o parse da resposta de `/browse` de um álbum ou playlist, extraindo
     * metadados de cabeçalho, capa, avatar do artista e a lista numerada de faixas.
     */
    fun parseAlbumResponse(rawJson: String, browseId: String): AlbumDetail {
        val root = JSONObject(rawJson)
        val twoCol = root.optJSONObject("contents")?.optJSONObject("twoColumnBrowseResultsRenderer")

        var headerObj: JSONObject? = null
        val tabs = twoCol?.optJSONArray("tabs")
        if (tabs != null && tabs.length() > 0) {
            val secContents = tabs.optJSONObject(0)
                ?.optJSONObject("tabRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("sectionListRenderer")
                ?.optJSONArray("contents")
            if (secContents != null) {
                for (i in 0 until secContents.length()) {
                    val s = secContents.optJSONObject(i)
                    if (s?.has("musicResponsiveHeaderRenderer") == true) {
                        headerObj = s.optJSONObject("musicResponsiveHeaderRenderer")
                        break
                    }
                }
            }
        }
        if (headerObj == null) {
            val rootHeader = root.optJSONObject("header")
            headerObj = rootHeader?.optJSONObject("musicResponsiveHeaderRenderer")
                ?: rootHeader?.optJSONObject("musicDetailHeaderRenderer")
        }

        val title = headerObj?.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
            ?: "Álbum"

        val subtitleRuns = headerObj?.optJSONObject("subtitle")?.optJSONArray("runs")
        var albumType = "Álbum"
        var year: String? = null
        if (subtitleRuns != null) {
            for (i in 0 until subtitleRuns.length()) {
                val t = subtitleRuns.optJSONObject(i)?.optString("text")?.trim().orEmpty()
                if (t.matches(Regex("\\d{4}"))) {
                    year = t
                } else if (t.equals("Single", ignoreCase = true) || t.equals("EP", ignoreCase = true) || t.equals("Álbum", ignoreCase = true) || t.equals("Album", ignoreCase = true) || t.equals("Playlist", ignoreCase = true)) {
                    albumType = t
                }
            }
        }

        val straplineRuns = headerObj?.optJSONObject("straplineTextOne")?.optJSONArray("runs")
        val straplineBuilder = StringBuilder()
        if (straplineRuns != null) {
            for (i in 0 until straplineRuns.length()) {
                straplineBuilder.append(straplineRuns.optJSONObject(i)?.optString("text").orEmpty())
            }
        }
        val artistName = straplineBuilder.toString().trim().ifEmpty {
            subtitleRuns?.optJSONObject(0)?.optString("text")?.trim() ?: "Artista"
        }

        val albumThumbnails = headerObj?.optJSONObject("thumbnail")
            ?.optJSONObject("musicThumbnailRenderer")
            ?.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails")
            ?: headerObj?.optJSONObject("thumbnail")
                ?.optJSONObject("croppedSquareThumbnailRenderer")
                ?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails")
        val albumThumbUrl = getBestThumbnailUrl(albumThumbnails)

        val artistThumbnails = headerObj?.optJSONObject("straplineThumbnail")
            ?.optJSONObject("musicThumbnailRenderer")
            ?.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails")
        val artistThumbUrl = getBestThumbnailUrl(artistThumbnails)

        val tracks = mutableListOf<Track>()
        val secListContents = twoCol?.optJSONObject("secondaryContents")
            ?.optJSONObject("sectionListRenderer")
            ?.optJSONArray("contents")
            ?: tabs?.optJSONObject(0)
                ?.optJSONObject("tabRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("sectionListRenderer")
                ?.optJSONArray("contents")

        if (secListContents != null) {
            for (i in 0 until secListContents.length()) {
                val sec = secListContents.optJSONObject(i)
                val shelf = sec?.optJSONObject("musicShelfRenderer")
                    ?: sec?.optJSONObject("musicPlaylistShelfRenderer")
                val shelfItems = shelf?.optJSONArray("contents") ?: continue

                for (j in 0 until shelfItems.length()) {
                    val item = shelfItems.optJSONObject(j)?.optJSONObject("musicResponsiveListItemRenderer") ?: continue

                    val itemTitle = item.optJSONArray("flexColumns")
                        ?.optJSONObject(0)
                        ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                        ?.optJSONObject("text")
                        ?.optJSONArray("runs")
                        ?.optJSONObject(0)
                        ?.optString("text") ?: continue

                    val videoId = item.optJSONObject("playlistItemData")?.optString("videoId")
                        ?.takeIf { it.isNotEmpty() }
                        ?: item.optJSONObject("navigationEndpoint")?.optJSONObject("watchEndpoint")?.optString("videoId")
                        ?: item.optJSONObject("overlay")
                            ?.optJSONObject("musicItemThumbnailOverlayRenderer")
                            ?.optJSONObject("content")
                            ?.optJSONObject("musicPlayButtonRenderer")
                            ?.optJSONObject("playNavigationEndpoint")
                            ?.optJSONObject("watchEndpoint")
                            ?.optString("videoId")
                        ?: continue

                    val fc1Runs = item.optJSONArray("flexColumns")
                        ?.optJSONObject(1)
                        ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                        ?.optJSONObject("text")
                        ?.optJSONArray("runs")
                    val fc1Builder = StringBuilder()
                    if (fc1Runs != null) {
                        for (r in 0 until fc1Runs.length()) {
                            fc1Builder.append(fc1Runs.optJSONObject(r)?.optString("text").orEmpty())
                        }
                    }
                    val trackArtist = fc1Builder.toString().trim().ifEmpty { artistName }

                    val durationText = item.optJSONArray("fixedColumns")
                        ?.optJSONObject(0)
                        ?.optJSONObject("musicResponsiveListItemFixedColumnRenderer")
                        ?.optJSONObject("text")
                        ?.optJSONArray("runs")
                        ?.optJSONObject(0)
                        ?.optString("text") ?: "--:--"

                    val itemThumbnails = item.optJSONObject("thumbnail")
                        ?.optJSONObject("musicThumbnailRenderer")
                        ?.optJSONObject("thumbnail")
                        ?.optJSONArray("thumbnails")
                    val itemThumbUrl = getBestThumbnailUrl(itemThumbnails) ?: albumThumbUrl

                    tracks.add(
                        Track(
                            id = videoId,
                            title = itemTitle,
                            artist = trackArtist,
                            album = title,
                            thumbnailUrl = itemThumbUrl,
                            durationText = durationText,
                            durationSeconds = parseDurationToSeconds(durationText),
                            sourceIdentifier = "INNERTUBE_WEB_REMIX",
                            browseId = browseId
                        )
                    )
                }
            }
        }

        return AlbumDetail(
            browseId = browseId,
            title = title,
            artistName = artistName,
            artistThumbnailUrl = artistThumbUrl,
            albumType = albumType,
            year = year,
            thumbnailUrl = albumThumbUrl,
            tracks = tracks
        )
    }

    /**
     * Faz o parse da resposta de `/browse` de um perfil de artista, extraindo
     * foto/banner, biografia, contagem de inscritos, top songs, álbuns, singles, vídeos e artistas similares.
     */
    fun parseArtistResponse(rawJson: String, browseId: String): ArtistDetail {
        val root = JSONObject(rawJson)
        val headerObj = root.optJSONObject("header")?.optJSONObject("musicImmersiveHeaderRenderer")
            ?: root.optJSONObject("header")?.optJSONObject("musicVisualHeaderRenderer")

        val name = headerObj?.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
            ?: "Artista"

        val monthlyListeners = headerObj?.optJSONObject("monthlyListenerCount")
            ?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")

        val subscriberCount = monthlyListeners
            ?: headerObj?.optJSONObject("subscriptionButton")
                ?.optJSONObject("subscribeButtonRenderer")
                ?.optJSONObject("subscriberCountText")
                ?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")

        val descRuns = headerObj?.optJSONObject("description")?.optJSONArray("runs")
        var description = if (descRuns != null && descRuns.length() > 0) {
            buildString {
                for (r in 0 until descRuns.length()) {
                    append(descRuns.optJSONObject(r)?.optString("text").orEmpty())
                }
            }
        } else null

        val bannerThumbnails = headerObj?.optJSONObject("thumbnail")
            ?.optJSONObject("musicThumbnailRenderer")
            ?.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails")
            ?: headerObj?.optJSONObject("foregroundThumbnail")
                ?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails")
        val bannerUrl = getBestThumbnailUrl(bannerThumbnails)

        val radioVideoId = headerObj?.optJSONObject("startRadioButton")
            ?.optJSONObject("buttonRenderer")
            ?.optJSONObject("navigationEndpoint")
            ?.optJSONObject("watchEndpoint")
            ?.optString("videoId")
            ?: headerObj?.optJSONObject("playButton")
                ?.optJSONObject("buttonRenderer")
                ?.optJSONObject("navigationEndpoint")
                ?.optJSONObject("watchEndpoint")
                ?.optString("videoId")

        val topSongs = mutableListOf<Track>()
        val albums = mutableListOf<Track>()
        val singles = mutableListOf<Track>()
        val videos = mutableListOf<Track>()
        val fans = mutableListOf<Artist>()

        val tabs = root.optJSONObject("contents")
            ?.optJSONObject("singleColumnBrowseResultsRenderer")
            ?.optJSONArray("tabs")
        val secContents = tabs?.optJSONObject(0)
            ?.optJSONObject("tabRenderer")
            ?.optJSONObject("content")
            ?.optJSONObject("sectionListRenderer")
            ?.optJSONArray("contents")

        if (secContents != null) {
            if (description.isNullOrBlank()) {
                for (i in 0 until secContents.length()) {
                    val sec = secContents.optJSONObject(i) ?: continue
                    val descShelf = sec.optJSONObject("musicDescriptionShelfRenderer") ?: continue
                    val runs = descShelf.optJSONObject("description")?.optJSONArray("runs")
                        ?: descShelf.optJSONArray("runs")
                    if (runs != null && runs.length() > 0) {
                        val text = buildString {
                            for (r in 0 until runs.length()) {
                                append(runs.optJSONObject(r)?.optString("text").orEmpty())
                            }
                        }
                        if (text.isNotBlank()) {
                            description = text
                            break
                        }
                    }
                }
            }

            for (i in 0 until secContents.length()) {
                val sec = secContents.optJSONObject(i)
                val shelf = sec?.optJSONObject("musicShelfRenderer")
                    ?: sec?.optJSONObject("musicCarouselShelfRenderer") ?: continue

                val shelfTitle = shelf.optJSONObject("header")
                    ?.optJSONObject("musicCarouselShelfBasicHeaderRenderer")
                    ?.optJSONObject("title")
                    ?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                    ?: shelf.optJSONObject("title")
                        ?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                    ?: ""

                val shelfItems = shelf.optJSONArray("contents") ?: continue
                val lowerTitle = shelfTitle.lowercase(Locale.ROOT)

                val isShelfRenderer = sec.has("musicShelfRenderer")
                val isTopSongs = isShelfRenderer ||
                    lowerTitle.contains("top") ||
                    lowerTitle.contains("mais tocadas") ||
                    lowerTitle.contains("songs") ||
                    (lowerTitle.contains("músicas") && !lowerTitle.contains("álbum") && !lowerTitle.contains("single") && !lowerTitle.contains("playlist"))

                when {
                    isTopSongs -> {
                        for (j in 0 until shelfItems.length()) {
                            val item = shelfItems.optJSONObject(j)?.optJSONObject("musicResponsiveListItemRenderer") ?: continue
                            val itemTitle = item.optJSONArray("flexColumns")
                                ?.optJSONObject(0)
                                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                                ?.optJSONObject("text")
                                ?.optJSONArray("runs")
                                ?.optJSONObject(0)
                                ?.optString("text") ?: continue

                            val videoId = item.optJSONObject("playlistItemData")?.optString("videoId")
                                ?.takeIf { it.isNotEmpty() }
                                ?: item.optJSONObject("navigationEndpoint")?.optJSONObject("watchEndpoint")?.optString("videoId")
                                ?: item.optJSONObject("overlay")
                                    ?.optJSONObject("musicItemThumbnailOverlayRenderer")
                                    ?.optJSONObject("content")
                                    ?.optJSONObject("musicPlayButtonRenderer")
                                    ?.optJSONObject("playNavigationEndpoint")
                                    ?.optJSONObject("watchEndpoint")
                                    ?.optString("videoId")
                                ?: item.optJSONArray("flexColumns")
                                    ?.optJSONObject(0)
                                    ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                                    ?.optJSONObject("text")
                                    ?.optJSONArray("runs")
                                    ?.optJSONObject(0)
                                    ?.optJSONObject("navigationEndpoint")
                                    ?.optJSONObject("watchEndpoint")
                                    ?.optString("videoId")
                                ?: continue

                            val durationText = item.optJSONArray("fixedColumns")
                                ?.optJSONObject(0)
                                ?.optJSONObject("musicResponsiveListItemFixedColumnRenderer")
                                ?.optJSONObject("text")
                                ?.optJSONArray("runs")
                                ?.optJSONObject(0)
                                ?.optString("text") ?: "--:--"

                            val itemThumbnails = item.optJSONObject("thumbnail")
                                ?.optJSONObject("musicThumbnailRenderer")
                                ?.optJSONObject("thumbnail")
                                ?.optJSONArray("thumbnails")
                            val itemThumbUrl = getBestThumbnailUrl(itemThumbnails) ?: bannerUrl

                            topSongs.add(
                                Track(
                                    id = videoId,
                                    title = itemTitle,
                                    artist = name,
                                    album = null,
                                    thumbnailUrl = itemThumbUrl,
                                    durationText = durationText,
                                    durationSeconds = parseDurationToSeconds(durationText),
                                    sourceIdentifier = "INNERTUBE_WEB_REMIX",
                                    browseId = browseId
                                )
                            )
                        }
                    }

                    lowerTitle.contains("album") || lowerTitle.contains("álbum") -> {
                        for (j in 0 until shelfItems.length()) {
                            val item = shelfItems.optJSONObject(j)?.optJSONObject("musicTwoRowItemRenderer") ?: continue
                            val itemTitle = item.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text") ?: continue
                            val subRuns = item.optJSONObject("subtitle")?.optJSONArray("runs")
                            val itemSub = if (subRuns != null) {
                                (0 until subRuns.length()).mapNotNull { subRuns.optJSONObject(it)?.optString("text") }.joinToString("")
                            } else ""
                            val albumBrowseId = item.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")?.optString("browseId") ?: continue
                            val itemThumbnails = item.optJSONObject("thumbnailRenderer")?.optJSONObject("musicThumbnailRenderer")?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                                ?: item.optJSONObject("thumbnail")?.optJSONObject("musicThumbnailRenderer")?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                            val itemThumb = getBestThumbnailUrl(itemThumbnails)

                            albums.add(
                                Track(
                                    id = albumBrowseId,
                                    title = itemTitle,
                                    artist = name,
                                    album = itemTitle,
                                    thumbnailUrl = itemThumb,
                                    durationText = if (itemSub.isNotBlank()) "Álbum • $itemSub" else "Álbum",
                                    sourceIdentifier = "INNERTUBE_WEB_REMIX",
                                    browseId = albumBrowseId
                                )
                            )
                        }
                    }

                    lowerTitle.contains("single") || lowerTitle.contains("ep") -> {
                        for (j in 0 until shelfItems.length()) {
                            val item = shelfItems.optJSONObject(j)?.optJSONObject("musicTwoRowItemRenderer") ?: continue
                            val itemTitle = item.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text") ?: continue
                            val subRuns = item.optJSONObject("subtitle")?.optJSONArray("runs")
                            val itemSub = if (subRuns != null) {
                                (0 until subRuns.length()).mapNotNull { subRuns.optJSONObject(it)?.optString("text") }.joinToString("")
                            } else ""
                            val singleBrowseId = item.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")?.optString("browseId") ?: continue
                            val itemThumbnails = item.optJSONObject("thumbnailRenderer")?.optJSONObject("musicThumbnailRenderer")?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                                ?: item.optJSONObject("thumbnail")?.optJSONObject("musicThumbnailRenderer")?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                            val itemThumb = getBestThumbnailUrl(itemThumbnails)

                            singles.add(
                                Track(
                                    id = singleBrowseId,
                                    title = itemTitle,
                                    artist = name,
                                    album = itemTitle,
                                    thumbnailUrl = itemThumb,
                                    durationText = if (itemSub.isNotBlank()) itemSub else "Single",
                                    sourceIdentifier = "INNERTUBE_WEB_REMIX",
                                    browseId = singleBrowseId
                                )
                            )
                        }
                    }

                    lowerTitle.contains("video") || lowerTitle.contains("vídeo") || lowerTitle.contains("ao vivo") -> {
                        for (j in 0 until shelfItems.length()) {
                            val item = shelfItems.optJSONObject(j)?.optJSONObject("musicTwoRowItemRenderer") ?: continue
                            val itemTitle = item.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text") ?: continue
                            val subRuns = item.optJSONObject("subtitle")?.optJSONArray("runs")
                            val itemSub = if (subRuns != null) {
                                (0 until subRuns.length()).mapNotNull { subRuns.optJSONObject(it)?.optString("text") }.joinToString("")
                            } else ""
                            val videoId = item.optJSONObject("navigationEndpoint")?.optJSONObject("watchEndpoint")?.optString("videoId")
                                ?: item.optJSONObject("onTap")?.optJSONObject("watchEndpoint")?.optString("videoId")
                                ?: item.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optJSONObject("navigationEndpoint")?.optJSONObject("watchEndpoint")?.optString("videoId")
                                ?: continue
                            val itemThumbnails = item.optJSONObject("thumbnailRenderer")?.optJSONObject("musicThumbnailRenderer")?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                                ?: item.optJSONObject("thumbnail")?.optJSONObject("musicThumbnailRenderer")?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                            val itemThumb = getBestThumbnailUrl(itemThumbnails)

                            videos.add(
                                Track(
                                    id = videoId,
                                    title = itemTitle,
                                    artist = name,
                                    album = null,
                                    thumbnailUrl = itemThumb,
                                    durationText = if (itemSub.isNotBlank()) itemSub else "Vídeo",
                                    sourceIdentifier = "INNERTUBE_WEB_REMIX"
                                )
                            )
                        }
                    }

                    lowerTitle.contains("fans") || lowerTitle.contains("fãs") || lowerTitle.contains("outros") || lowerTitle.contains("parecidos") || lowerTitle.contains("gostar") -> {
                        for (j in 0 until shelfItems.length()) {
                            val item = shelfItems.optJSONObject(j)?.optJSONObject("musicTwoRowItemRenderer") ?: continue
                            val itemTitle = item.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text") ?: continue
                            val subRuns = item.optJSONObject("subtitle")?.optJSONArray("runs")
                            val itemSub = if (subRuns != null) {
                                (0 until subRuns.length()).mapNotNull { subRuns.optJSONObject(it)?.optString("text") }.joinToString("")
                            } else null
                            val relatedBrowseId = item.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")?.optString("browseId")
                                ?: item.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")?.optString("browseId")
                                ?: continue
                            val itemThumbnails = item.optJSONObject("thumbnailRenderer")?.optJSONObject("musicThumbnailRenderer")?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                                ?: item.optJSONObject("thumbnail")?.optJSONObject("musicThumbnailRenderer")?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                            val itemThumb = getBestThumbnailUrl(itemThumbnails)

                            fans.add(
                                Artist(
                                    id = relatedBrowseId,
                                    name = itemTitle,
                                    subtitle = itemSub,
                                    thumbnailUrl = itemThumb
                                )
                            )
                        }
                    }
                }
            }
        }

        return ArtistDetail(
            browseId = browseId,
            name = name,
            bannerUrl = bannerUrl,
            subscriberCount = subscriberCount,
            description = description,
            radioVideoId = radioVideoId,
            topSongs = topSongs,
            albums = albums,
            singles = singles,
            videos = videos,
            fansMightAlsoLike = fans
        )
    }

    private fun parseDurationToSeconds(duration: String): Long {
        val parts = duration.trim().split(":")
        return when (parts.size) {
            2 -> {
                val mins = parts[0].toLongOrNull() ?: 0L
                val secs = parts[1].toLongOrNull() ?: 0L
                mins * 60 + secs
            }
            3 -> {
                val hrs = parts[0].toLongOrNull() ?: 0L
                val mins = parts[1].toLongOrNull() ?: 0L
                val secs = parts[2].toLongOrNull() ?: 0L
                hrs * 3600 + mins * 60 + secs
            }
            else -> 0L
        }
    }

    fun parseHomeChips(jsonString: String): List<HomeChip> {
        val rawChips = mutableListOf<HomeChip>()
        try {
            val root = JSONObject(jsonString)
            val chipsArray = root.optJSONObject("contents")
                ?.optJSONObject("singleColumnBrowseResultsRenderer")
                ?.optJSONArray("tabs")
                ?.optJSONObject(0)
                ?.optJSONObject("tabRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("sectionListRenderer")
                ?.optJSONObject("header")
                ?.optJSONObject("chipCloudRenderer")
                ?.optJSONArray("chips") ?: return rawChips

            for (i in 0 until chipsArray.length()) {
                val chipRenderer = chipsArray.optJSONObject(i)
                    ?.optJSONObject("chipCloudChipRenderer") ?: continue
                val title = chipRenderer.optJSONObject("text")
                    ?.optJSONArray("runs")
                    ?.optJSONObject(0)
                    ?.optString("text")
                    ?.trim() ?: continue
                val params = chipRenderer.optJSONObject("navigationEndpoint")
                    ?.optJSONObject("browseEndpoint")
                    ?.optString("params")
                    ?.trim() ?: continue

                if (title.isNotEmpty() && params.isNotEmpty() && !title.equals("Podcasts", ignoreCase = true)) {
                    rawChips.add(HomeChip(title = title, params = params))
                }
            }
        } catch (e: Exception) {
            // Retorna chips extraídos
        }

        val preferredOrder = listOf(
            "Energia", "Relax", "Para treinar", "Foco", "Festa",
            "Romance", "Positividade", "Para ouvir no trânsito", "Para dormir", "Triste"
        )
        return rawChips.sortedBy { chip ->
            val idx = preferredOrder.indexOfFirst { it.equals(chip.title, ignoreCase = true) }
            if (idx == -1) 999 else idx
        }
    }

    /**
     * Faz o parse da resposta de /youtubei/v1/browse para o feed Home,
     * extraindo as seções com carrosséis horizontais e grades de 4 faixas (`musicCarouselShelfRenderer`).
     */
    fun parseHomeFeedResponse(jsonString: String): List<HomeSection> {
        val sections = mutableListOf<HomeSection>()
        try {
            val root = JSONObject(jsonString)
            val sectionContents = root.optJSONObject("contents")
                ?.optJSONObject("singleColumnBrowseResultsRenderer")
                ?.optJSONArray("tabs")
                ?.optJSONObject(0)
                ?.optJSONObject("tabRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("sectionListRenderer")
                ?.optJSONArray("contents") ?: return sections

            for (i in 0 until sectionContents.length()) {
                val sectionObj = sectionContents.optJSONObject(i) ?: continue
                val carousel = sectionObj.optJSONObject("musicCarouselShelfRenderer") ?: continue

                val header = carousel.optJSONObject("header")
                    ?.optJSONObject("musicCarouselShelfBasicHeaderRenderer")
                val title = header?.optJSONObject("title")
                    ?.optJSONArray("runs")
                    ?.optJSONObject(0)
                    ?.optString("text")
                    ?.trim()
                    ?: continue

                val strapline = header.optJSONObject("strapline")
                    ?.optJSONArray("runs")
                    ?.optJSONObject(0)
                    ?.optString("text")
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }

                val numItemsPerColumn = carousel.optString("numItemsPerColumn", "1").toIntOrNull() ?: 1

                val itemsArray = carousel.optJSONArray("contents") ?: continue
                val sectionSongs = mutableListOf<Track>()
                val dummyArtists = mutableListOf<Artist>()
                var hasResponsiveItems = false

                for (j in 0 until itemsArray.length()) {
                    val itemWrapper = itemsArray.optJSONObject(j) ?: continue
                    val responsiveItem = itemWrapper.optJSONObject("musicResponsiveListItemRenderer")
                    if (responsiveItem != null) {
                        hasResponsiveItems = true
                        parseResponsiveListItem(responsiveItem, sectionSongs, dummyArtists)
                    } else {
                        val twoRowItem = itemWrapper.optJSONObject("musicTwoRowItemRenderer")
                        if (twoRowItem != null) {
                            val track = parseTwoRowItem(twoRowItem)
                            if (track != null && sectionSongs.none { it.id == track.id }) {
                                sectionSongs.add(track)
                            }
                        }
                    }
                }

                if (sectionSongs.isNotEmpty()) {
                    val style = if (numItemsPerColumn >= 4 || hasResponsiveItems) {
                        HomeSectionStyle.QUICK_PICKS_GRID
                    } else {
                        HomeSectionStyle.TWO_ROW_CAROUSEL
                    }
                    sections.add(
                        HomeSection(
                            title = title,
                            items = sectionSongs,
                            strapline = strapline,
                            style = style
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Retorna seções extraídas até o momento
        }
        return sections
    }

    private fun parseTwoRowItem(item: JSONObject): Track? {
        val titleObj = item.optJSONObject("title")
        val title = titleObj?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")?.trim() ?: return null
        if (title.isEmpty()) return null

        val overlayPlayNav = item.optJSONObject("thumbnailOverlay")
            ?.optJSONObject("musicItemThumbnailOverlayRenderer")
            ?.optJSONObject("content")
            ?.optJSONObject("musicPlayButtonRenderer")
            ?.optJSONObject("playNavigationEndpoint")
        val itemNav = item.optJSONObject("navigationEndpoint")

        val videoId = overlayPlayNav?.optJSONObject("watchEndpoint")?.optString("videoId")
            ?.takeIf { it.isNotEmpty() }
            ?: itemNav?.optJSONObject("watchEndpoint")?.optString("videoId").orEmpty()

        val playlistId = overlayPlayNav?.optJSONObject("watchPlaylistEndpoint")?.optString("playlistId")
            ?.takeIf { it.isNotEmpty() }
            ?: overlayPlayNav?.optJSONObject("watchEndpoint")?.optString("playlistId")
                ?.takeIf { it.isNotEmpty() }
            ?: itemNav?.optJSONObject("watchPlaylistEndpoint")?.optString("playlistId")
                ?.takeIf { it.isNotEmpty() }
            ?: itemNav?.optJSONObject("browseEndpoint")?.optString("browseId")
                ?.removePrefix("VL")
                ?.takeIf { it.startsWith("RD") || it.startsWith("PL") || it.startsWith("OL") }
                .orEmpty()

        if (videoId.isEmpty() && playlistId.isEmpty()) return null

        val subtitleRuns = item.optJSONObject("subtitle")?.optJSONArray("runs")
        val subtitleBuilder = StringBuilder()
        var extractedDuration: String? = null
        if (subtitleRuns != null) {
            for (r in 0 until subtitleRuns.length()) {
                val runObj = subtitleRuns.optJSONObject(r) ?: continue
                val text = runObj.optString("text").orEmpty()
                subtitleBuilder.append(text)
                if (extractedDuration == null) {
                    extractedDuration = extractDurationFromSegment(text)
                }
            }
        }
        val fullSubtitle = subtitleBuilder.toString().trim()
        if (extractedDuration == null && fullSubtitle.isNotBlank()) {
            extractedDuration = extractDurationFromSegment(fullSubtitle)
        }
        val artist = fullSubtitle.ifEmpty { "YouTube Music" }
        val finalDuration = extractedDuration ?: "--:--"
        val durationSeconds = parseDurationToSeconds(finalDuration)

        val thumbnails = item.optJSONObject("thumbnailRenderer")
            ?.optJSONObject("musicThumbnailRenderer")
            ?.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails")
        val thumbnailUrl = getBestThumbnailUrl(thumbnails)

        val browseId = itemNav?.optJSONObject("browseEndpoint")?.optString("browseId")
            ?.takeIf { it.isNotEmpty() }
            ?: overlayPlayNav?.optJSONObject("browseEndpoint")?.optString("browseId")

        val resolvedId = videoId.ifEmpty { browseId ?: "playlist_$playlistId" }
        return Track(
            id = resolvedId,
            title = title,
            artist = artist,
            album = null,
            thumbnailUrl = thumbnailUrl,
            durationText = finalDuration,
            durationSeconds = durationSeconds,
            sourceIdentifier = "INNERTUBE_WEB_REMIX",
            playlistId = playlistId.ifEmpty { null },
            browseId = browseId
        )
    }
}
