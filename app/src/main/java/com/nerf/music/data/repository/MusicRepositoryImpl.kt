package com.nerf.music.data.repository

import com.nerf.music.data.model.Artist
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

        val boostedSongs = result.songs.sortedBy { track ->
            calculateTrackRelevanceTier(track, cleanedQuery)
        }

        val boostedArtists = result.artists.sortedBy { artist ->
            calculateArtistRelevanceTier(artist, cleanedQuery)
        }

        return result.copy(
            songs = boostedSongs,
            artists = boostedArtists
        )
    }

    internal fun calculateTrackRelevanceTier(track: Track, query: String): Int {
        val q = query.trim()
        if (q.isEmpty()) return 3

        if (track.artist.equals(q, ignoreCase = true) || track.title.equals(q, ignoreCase = true)) {
            return 0
        }

        val collaboratorNames = track.artist.split(COLLAB_SPLIT_REGEX)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (collaboratorNames.any { it.equals(q, ignoreCase = true) } ||
            containsExactWord(track.artist, q) ||
            containsExactWord(track.title, q)
        ) {
            return 1
        }

        if (track.artist.contains(q, ignoreCase = true) || track.title.contains(q, ignoreCase = true)) {
            return 2
        }

        return 3
    }

    private fun calculateArtistRelevanceTier(artist: Artist, query: String): Int {
        val q = query.trim()
        return when {
            artist.name.equals(q, ignoreCase = true) -> 0
            containsExactWord(artist.name, q) -> 1
            artist.name.contains(q, ignoreCase = true) -> 2
            else -> 3
        }
    }

    private fun containsExactWord(text: String, word: String): Boolean {
        if (text.isBlank() || word.isBlank()) return false
        val pattern = Pattern.compile(
            "(?i)(^|[^\\p{L}\\p{N}])${Pattern.quote(word)}($|[^\\p{L}\\p{N}])"
        )
        return pattern.matcher(text).find()
    }

    companion object {
        private val COLLAB_SPLIT_REGEX = Regex("(?i)\\s+(?:e|&|feat\\.?|ft\\.?|x)\\s+|,\\s*")
    }
}
