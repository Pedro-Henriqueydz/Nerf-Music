package com.nerf.music.data.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Motor universal de seleção, verificação de recência e ranqueamento de lançamentos
 * compatíveis com o perfil acústico de qualquer usuário.
 */
object ReleaseRecommendations {

    private val YEAR_REGEX = Regex("\\b(19\\d{2}|20\\d{2})\\b")
    private val RELEASE_TYPE_PREFIX = Regex("^(?:Álbum|Album|Single|EP)\\s*•\\s*", RegexOption.IGNORE_CASE)
    private val GENERIC_ARTIST_LABELS = setOf(
        "playlist", "playlist automática", "auto playlist", "youtube music",
        "nerf music", "sua playlist", "artista desconhecido", "desconhecido", "--"
    )

    /**
     * Limpa uma lista de nomes de artistas vinda do histórico/afinidade, removendo rótulos
     * genéricos de playlists e artistas com afinidade negativa.
     */
    fun cleanPreferredArtists(
        rawArtists: List<String>,
        penalizedArtists: Set<String> = emptySet()
    ): List<String> {
        val penalizedNormalized = penalizedArtists.map { it.trim().lowercase(Locale.ROOT) }.toSet()
        val result = mutableListOf<String>()
        for (raw in rawArtists) {
            val clean = raw.trim()
            if (clean.isBlank()) continue
            val lower = clean.lowercase(Locale.ROOT)
            if (lower in GENERIC_ARTIST_LABELS || lower.startsWith("playlist")) continue
            if (lower in penalizedNormalized) continue
            // Verifica se algum colaborador individual ou combinação exata foi penalizada
            val parts = splitContributors(clean)
            if (parts.isNotEmpty() && parts.all { it.lowercase(Locale.ROOT) in penalizedNormalized }) continue
            if (result.none { it.equals(clean, ignoreCase = true) }) {
                result.add(clean)
            }
        }
        return result
    }

    /**
     * Extrai os nomes dos artistas/colaboradores de um campo `artist` ou subtítulo do InnerTube,
     * removendo marcadores de tipo ("Single • ", "Álbum • ", "EP • ") e anos (" • 2026").
     */
    fun extractContributors(rawArtistField: String): List<String> {
        if (rawArtistField.isBlank()) return emptyList()
        val segments = rawArtistField.split("•")
            .map { it.replace('\u00A0', ' ').trim() }
            .filter { segment ->
                val lower = segment.lowercase(Locale.ROOT)
                segment.isNotEmpty() &&
                    lower != "single" &&
                    lower != "ep" &&
                    lower != "álbum" &&
                    lower != "album" &&
                    !YEAR_REGEX.matches(segment)
            }
        val artistPart = segments.firstOrNull() ?: return emptyList()
        return splitContributors(artistPart)
    }

    private fun splitContributors(artistText: String): List<String> {
        return artistText
            .replace(RELEASE_TYPE_PREFIX, "")
            .split(" • ", " & ", ", ", " e ", " feat. ", " ft. ", " part. ")
            .map { it.trim() }
            .filter {
                val lower = it.lowercase(Locale.ROOT)
                it.isNotBlank() && !YEAR_REGEX.matches(it) && lower !in GENERIC_ARTIST_LABELS
            }
    }

    /**
     * Extrai o ano de lançamento (ex: 2025, 2026) se estiver presente nos metadados do card.
     */
    fun extractReleaseYear(track: Track): Int? {
        val sources = listOfNotNull(track.durationText, track.artist, track.album)
        for (source in sources) {
            val match = YEAR_REGEX.findAll(source).lastOrNull()
            val year = match?.groupValues?.getOrNull(1)?.toIntOrNull()
            if (year != null && year in 1950..2100) {
                return year
            }
        }
        return null
    }

    /** Full dates independently matched to a catalog item are required. */
    fun isRecentRelease(track: Track, today: LocalDate = LocalDate.now()): Boolean {
        val id = track.browseId ?: track.id
        if (!id.startsWith("MPREb_")) return false
        val date = try { LocalDate.parse(track.verifiedReleaseDate) } catch (_: Exception) { return false }
        return !date.isAfter(today) && ChronoUnit.DAYS.between(date, today) <= 180
    }

    fun matchesProfileGenre(candidateGenre: String?, profileGenres: Set<String>): Boolean {
        val genre = candidateGenre?.trim()?.lowercase(Locale.ROOT).orEmpty()
        return genre.isNotEmpty() && profileGenres.any { it.trim().equals(genre, ignoreCase = true) }
    }

    fun rank(
        releases: List<Track>,
        preferredArtists: List<String>,
        relatedArtists: List<String>,
        penalizedArtists: Set<String> = emptySet(),
        maxPerArtist: Int = 3,
        maxResults: Int = 24,
        today: LocalDate = LocalDate.now()
    ): List<Track> {
        val cleanPreferred = cleanPreferredArtists(preferredArtists, penalizedArtists)
        val cleanRelated = cleanPreferredArtists(relatedArtists, penalizedArtists)
        val penalizedNormalized = penalizedArtists.map { it.trim().lowercase(Locale.ROOT) }.toSet()

        fun isPenalized(track: Track): Boolean {
            if (penalizedNormalized.isEmpty()) return false
            val rawArtistClean = track.artist.replace(RELEASE_TYPE_PREFIX, "")
                .replace(Regex("\\s*•\\s*(19|20)\\d{2}$"), "")
                .trim()
                .lowercase(Locale.ROOT)
            if (rawArtistClean in penalizedNormalized) return true
            val contributors = extractContributors(track.artist)
            return contributors.any { it.lowercase(Locale.ROOT) in penalizedNormalized }
        }

        fun matches(track: Track, name: String): Boolean {
            val target = name.trim()
            if (target.isBlank()) return false
            val targetContributors = splitContributors(target)
            val trackContributors = extractContributors(track.artist)
            if (trackContributors.any { tc -> tc.equals(target, ignoreCase = true) }) return true
            if (targetContributors.size > 1 && targetContributors.any { tc ->
                    trackContributors.any { it.equals(tc, ignoreCase = true) }
                }) return true
            return false
        }

        fun score(track: Track): Int {
            if (isPenalized(track)) return -100
            val artistScore = when {
                cleanPreferred.any { matches(track, it) } -> 10
                cleanRelated.any { matches(track, it) } -> 5
                track.sourceIdentifier == "ITUNES_GENRE_MATCH" -> 4
                else -> 0
            }
            return if (artistScore > 0) artistScore else 0
        }

        val uniqueRecent = releases
            .filter { isRecentRelease(it, today = today) }
            .filterNot { isPenalized(it) }
            .distinctBy { it.browseId ?: it.id }

        if (cleanPreferred.isEmpty() && cleanRelated.isEmpty()) {
            return uniqueRecent.groupBy { extractContributors(it.artist).firstOrNull()?.lowercase(Locale.ROOT) ?: it.artist.lowercase(Locale.ROOT) }
                .values.flatMap { it.take(maxPerArtist) }.take(maxResults)
        }

        val relevant = uniqueRecent
            .map { it to score(it) }
            .filter { it.second > 0 }
            .sortedWith(compareByDescending<Pair<Track, Int>> { it.second }.thenByDescending { it.first.verifiedReleaseDate })
            .map { it.first }

        // Aplica guarda de diversidade por artista principal para misturar favoritos e descobertas
        val result = mutableListOf<Track>()
        val artistCounts = mutableMapOf<String, Int>()

        for (item in relevant) {
            val primaryKey = extractContributors(item.artist).firstOrNull()?.lowercase(Locale.ROOT)
                ?: item.artist.lowercase(Locale.ROOT)
            val count = artistCounts[primaryKey] ?: 0
            if (count < maxPerArtist) {
                result.add(item)
                artistCounts[primaryKey] = count + 1
            }
            if (result.size >= maxResults) break
        }

        return result.take(maxResults)
    }
}
