package com.nerf.music.data.repository

import com.nerf.music.data.model.AlbumDetail
import com.nerf.music.data.model.Artist
import com.nerf.music.data.model.HomeFeedData
import com.nerf.music.data.model.HomeSection
import com.nerf.music.data.model.SearchFilter
import com.nerf.music.data.model.SearchResult
import com.nerf.music.data.model.Track
import com.nerf.music.data.model.UpNextResult
import com.nerf.music.data.network.InnerTubeCatalogClient
import java.util.regex.Pattern

/**
 * Implementação padrão do repositório musical usando o cliente InnerTube anônimo.
 *
 * Aplica priorização de relevância ("Exact Match Boost") para garantir que faixas e artistas
 * com correspondência exata (case-insensitive) à busca fiquem no topo da lista antes de
 * resultados aproximados (ex: priorizar "Altare" acima de "Altares" ou "Altar").
 */
class MusicRepositoryImpl(
    private val catalogClient: InnerTubeCatalogClient = InnerTubeCatalogClient()
) : MusicRepository {

    override suspend fun search(
        query: String,
        filter: SearchFilter
    ): Result<SearchResult> {
        return catalogClient.search(query, filter).map { rawResult ->
            prioritizeSearchResults(rawResult, query)
        }
    }

    override suspend fun getHomeFeed(): Result<List<HomeSection>> {
        return catalogClient.getHomeFeed()
    }

    override suspend fun getHomeFeedData(params: String?): Result<HomeFeedData> {
        return catalogClient.getHomeFeedData(params)
    }

    override suspend fun getPersonalizedHomeFeedData(
        seedVideoId: String?,
        topArtists: List<String>,
        params: String?
    ): Result<HomeFeedData> {
        return catalogClient.getPersonalizedHomeFeedData(seedVideoId, topArtists, params)
    }

    override suspend fun getAlbumOrPlaylist(browseId: String): Result<AlbumDetail> {
        return catalogClient.getAlbumOrPlaylist(browseId)
    }

    override suspend fun getUpNextRadio(
        videoId: String,
        params: String?
    ): Result<UpNextResult> {
        return catalogClient.getUpNextRadio(videoId, params)
    }

    /**
     * Ordena de forma estável (preservando a ordem do Top Result `musicCardShelfRenderer` dentro
     * de cada faixa de relevância) priorizando correspondências exatas de título ou artista.
     */
    fun prioritizeSearchResults(result: SearchResult, query: String): SearchResult {
        val cleanedQuery = query.trim()
        if (cleanedQuery.isEmpty()) return result

        val queryTokens = cleanedQuery.lowercase().split(Pattern.compile("\\s+")).filter { it.isNotEmpty() }

        val sortedSongs = result.songs.sortedWith(
            compareBy<Track> { track ->
                computeTrackScore(track, cleanedQuery, queryTokens)
            }
        )

        val sortedArtists = result.artists.sortedWith(
            compareBy<Artist> { artist ->
                computeArtistScore(artist, cleanedQuery)
            }
        )

        return result.copy(
            songs = sortedSongs,
            artists = sortedArtists
        )
    }

    private fun computeTrackScore(track: Track, exactQuery: String, tokens: List<String>): Int {
        val title = track.title.trim()
        val artist = track.artist.trim()

        val exactTitleMatch = title.equals(exactQuery, ignoreCase = true)
        val exactArtistMatch = artist.equals(exactQuery, ignoreCase = true)

        if (exactTitleMatch && exactArtistMatch) return 0
        if (exactTitleMatch) return 1
        if (exactArtistMatch) return 2

        val titleWords = title.lowercase().split(Pattern.compile("[^\\p{L}\\p{N}]+")).filter { it.isNotEmpty() }
        val artistWords = artist.lowercase().split(Pattern.compile("[^\\p{L}\\p{N}]+")).filter { it.isNotEmpty() }

        val allTokensMatchExactWord = tokens.all { token ->
            titleWords.contains(token) || artistWords.contains(token)
        }
        if (allTokensMatchExactWord) return 3

        if (title.startsWith(exactQuery, ignoreCase = true) || artist.startsWith(exactQuery, ignoreCase = true)) {
            return 4
        }

        if (title.contains(exactQuery, ignoreCase = true) || artist.contains(exactQuery, ignoreCase = true)) {
            return 5
        }

        return 6
    }

    private fun computeArtistScore(artist: Artist, exactQuery: String): Int {
        val name = artist.name.trim()
        if (name.equals(exactQuery, ignoreCase = true)) return 0
        if (name.startsWith(exactQuery, ignoreCase = true)) return 1
        if (name.contains(exactQuery, ignoreCase = true)) return 2
        return 3
    }
}
