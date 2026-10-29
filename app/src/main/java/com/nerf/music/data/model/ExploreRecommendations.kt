package com.nerf.music.data.model

import java.util.Locale

/** Seleciona descobertas reais a partir de rádios, sem reciclar atalhos da Home. */
object ExploreRecommendations {
    fun selectDiscoveryTracks(
        radioGroups: List<List<Track>>,
        excludedTrackIds: Set<String>,
        penalizedArtists: Set<String>,
        isTrackPenalized: (String) -> Boolean = { false },
        limit: Int = 16
    ): List<Track> {
        if (limit <= 0) return emptyList()

        val blockedArtists = penalizedArtists
            .map { normalize(it) }
            .filter { it.isNotBlank() }
            .toSet()
        val candidates = interleave(radioGroups)
            .filter { track ->
                track.id.isNotBlank() &&
                    !track.isAlbumOrPlaylist &&
                    track.id !in excludedTrackIds &&
                    !isTrackPenalized(track.id) &&
                    ReleaseRecommendations.extractContributors(track.artist)
                        .none { normalize(it) in blockedArtists }
            }
            .distinctBy { it.id }

        // Evita que uma única rádio/artista domine toda a seção, mantendo a ordem de relevância.
        val artistCounts = mutableMapOf<String, Int>()
        return candidates.filter { track ->
            val artistKey = ReleaseRecommendations.extractContributors(track.artist)
                .firstOrNull()
                ?.let(::normalize)
                .orEmpty()
            val count = artistCounts[artistKey] ?: 0
            if (artistKey.isNotBlank() && count >= MAX_TRACKS_PER_ARTIST) {
                false
            } else {
                if (artistKey.isNotBlank()) artistCounts[artistKey] = count + 1
                true
            }
        }.take(limit)
    }

    private fun interleave(groups: List<List<Track>>): List<Track> = buildList {
        val largestGroup = groups.maxOfOrNull { it.size } ?: 0
        repeat(largestGroup) { index ->
            groups.forEach { group -> group.getOrNull(index)?.let(::add) }
        }
    }

    private fun normalize(value: String): String = value.trim().lowercase(Locale.ROOT)

    private const val MAX_TRACKS_PER_ARTIST = 2
}
