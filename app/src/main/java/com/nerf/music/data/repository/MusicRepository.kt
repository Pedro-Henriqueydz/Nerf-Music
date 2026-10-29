package com.nerf.music.data.repository

import com.nerf.music.data.model.AlbumDetail
import com.nerf.music.data.model.ArtistDetail
import com.nerf.music.data.model.HomeFeedData
import com.nerf.music.data.model.HomeSection
import com.nerf.music.data.model.SearchFilter
import com.nerf.music.data.model.SearchResult
import com.nerf.music.data.model.UpNextResult
import com.nerf.music.data.model.TrackRelatedData

/**
 * Interface do repositório para acesso ao catálogo musical e à rádio dinâmica.
 */
interface MusicRepository {
    /**
     * Realiza uma busca no catálogo sem exigir conta de usuário.
     * Retorna [SearchResult] contendo músicas, artistas e destaques mapeados de forma independente.
     */
    suspend fun search(
        query: String,
        filter: SearchFilter = SearchFilter.ALL
    ): Result<SearchResult>

    /**
     * Obtém o feed principal da Home com carrosséis organizados.
     */
    suspend fun getHomeFeed(): Result<List<HomeSection>>

    suspend fun getExploreFeed(): Result<List<HomeSection>> = getHomeFeed()
    suspend fun getNewReleases(): Result<List<com.nerf.music.data.model.Track>> = Result.success(emptyList())
    suspend fun discoverPersonalizedRecentReleases(
        preferredArtists: List<String>,
        seedVideoIds: List<String>,
        penalizedArtists: Set<String> = emptySet(),
        seedRadioTracks: List<com.nerf.music.data.model.Track> = emptyList()
    ): List<com.nerf.music.data.model.Track> = emptyList()
    suspend fun verifyReleaseDates(candidates: List<com.nerf.music.data.model.Track>, preferredArtists: List<String>): List<com.nerf.music.data.model.Track> = emptyList()
    suspend fun getChartsFeed(): Result<List<HomeSection>> = Result.success(emptyList())
    suspend fun getMoodCategories(): Result<List<Pair<String, String>>> = Result.success(emptyList())
    suspend fun getMoodFeed(browseId: String): Result<List<HomeSection>> = Result.success(emptyList())

    /**
     * Obtém os dados completos da Home do YouTube Music (chips de humor + carrosséis),
     * permitindo filtragem opcional via [params] de chip.
     */
    suspend fun getHomeFeedData(params: String? = null): Result<HomeFeedData> {
        return getHomeFeed().map { HomeFeedData(chips = emptyList(), sections = it) }
    }

    /**
     * Obtém o feed personalizado com base no histórico do usuário.
     */
    suspend fun getPersonalizedHomeFeedData(
        seedVideoId: String? = null,
        topArtists: List<String> = emptyList(),
        params: String? = null
    ): Result<HomeFeedData>

    /**
     * Obtém os detalhes completos de um álbum ou playlist via [browseId].
     */
    suspend fun getAlbumOrPlaylist(browseId: String): Result<AlbumDetail>

    /**
     * Obtém os detalhes completos do perfil de um artista via [browseId] (channelId UC...).
     */
    suspend fun getArtist(browseId: String): Result<ArtistDetail>

    /**
     * Obtém a fila de próximas músicas (Rádio Dinâmica / Mix) e os chips de filtro
     * via endpoint `/youtubei/v1/next`.
     */
    suspend fun getTrackRelated(relatedBrowseId: String): Result<TrackRelatedData>

    suspend fun getUpNextRadio(
        videoId: String,
        params: String? = null
    ): Result<UpNextResult>
}
