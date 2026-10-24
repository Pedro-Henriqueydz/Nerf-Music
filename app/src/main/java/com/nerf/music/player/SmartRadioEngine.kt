package com.nerf.music.player

import android.util.Log
import com.nerf.music.data.model.Track
import com.nerf.music.data.repository.UserAffinityRepository

/**
 * Modos de sintonização da Rádio Dinâmica (estilo YouTube Music & Spotify)
 */
enum class RadioTuningMode(val title: String) {
    BALANCED("Tudo"),
    FAMILIAR("Familiar"),
    DISCOVERY("Descobrir"),
    DEEP_CUTS("Lado B")
}

/**
 * Motor de recomendação e reordenamento inteligente de rádios musicais.
 *
 * Princípios do Algoritmo Premium:
 * 1. Respeito Absoluto ao Cluster Acústico: Nunca injeta faixas de outros gêneros
 *    (ex: Rock em eletrônica) só porque estão no histórico geral.
 * 2. Afinidade Contextual: Apenas impulsiona faixas familiares se o artista ou faixa
 *    fizerem parte do cluster acústico da rádio atual.
 * 3. Filtro de Skip Penalty: Elimina automaticamente faixas rejeitadas pelo ouvinte.
 * 4. Guarda de Diversidade (Anti-Clustering): Impede 2 músicas consecutivas do mesmo artista.
 * 5. Sintonização Dinâmica: Modos "Familiar", "Descobrir", "Lado B" e "Tudo".
 */
object SmartRadioEngine {

    private const val TAG = "SmartRadioEngine"

    fun buildSmartQueue(
        seedTrack: Track,
        rawCandidates: List<Track>,
        userLikedTracks: List<Track>,
        userRecentTracks: List<Track>,
        affinityRepo: UserAffinityRepository,
        tuningMode: RadioTuningMode = RadioTuningMode.BALANCED,
        maxQueueSize: Int = 35
    ): List<Track> {
        if (rawCandidates.isEmpty()) return listOf(seedTrack)

        val resultQueue = mutableListOf<Track>()
        val seenTrackIds = mutableSetOf<String>()

        // 1. A primeira faixa da rádio é sempre a semente atual
        resultQueue.add(seedTrack)
        seenTrackIds.add(seedTrack.id)

        // 2. Extrai os artistas válidos do cluster acústico retornado pelo YouTube Music
        val clusterArtists = rawCandidates.map { it.artist.trim().lowercase() }
            .filter { it.isNotBlank() && it != "--" }
            .toSet()

        val seedArtist = seedTrack.artist.trim().lowercase()

        // 3. Filtra candidatos brutos da rádio, descartando Skip Penalties e duplicatas
        val validClusterCandidates = rawCandidates.filter { candidate ->
            !seenTrackIds.contains(candidate.id) && !affinityRepo.isTrackPenalized(candidate.id)
        }

        // 4. Seleciona faixas familiares ESTRITAMENTE do mesmo universo acústico (mesmo artista ou artistas do cluster)
        val compatibleFamiliarTracks = (userLikedTracks + userRecentTracks)
            .filter { candidate ->
                candidate.id.isNotBlank() &&
                !seenTrackIds.contains(candidate.id) &&
                !affinityRepo.isTrackPenalized(candidate.id) &&
                (candidate.artist.trim().lowercase() == seedArtist || clusterArtists.contains(candidate.artist.trim().lowercase()))
            }
            .distinctBy { it.id }

        // 5. Calcula pontuação de afinidade contextual para cada faixa do cluster
        data class ScoredTrack(val track: Track, val score: Float, val isFamiliar: Boolean)

        val scoredTracks = mutableListOf<ScoredTrack>()

        // Adiciona as faixas compatíveis da biblioteca do usuário com bônus de familiaridade
        compatibleFamiliarTracks.forEach { track ->
            val trackAffinity = affinityRepo.getTrackAffinity(track.id)
            val artistAffinity = affinityRepo.getArtistAffinity(track.artist)
            val isSameArtist = track.artist.trim().lowercase() == seedArtist
            val baseBonus = if (isSameArtist) 15f else 10f
            val totalScore = baseBonus + trackAffinity + (artistAffinity * 0.5f)
            scoredTracks.add(ScoredTrack(track, totalScore, isFamiliar = true))
        }

        // Adiciona os candidatos do cluster do YouTube Music
        validClusterCandidates.forEachIndexed { index, track ->
            if (scoredTracks.none { it.track.id == track.id }) {
                // Pontuação baseada no rank original do YouTube (quanto mais cedo, mais relevante ao cluster)
                val rankScore = (validClusterCandidates.size - index).toFloat()
                val trackAffinity = affinityRepo.getTrackAffinity(track.id)
                val artistAffinity = affinityRepo.getArtistAffinity(track.artist)
                val totalScore = rankScore + (trackAffinity * 2f) + (artistAffinity * 0.8f)
                scoredTracks.add(ScoredTrack(track, totalScore, isFamiliar = false))
            }
        }

        // 6. Ordenação conforme o Modo de Sintonização
        val sortedCandidates = when (tuningMode) {
            RadioTuningMode.FAMILIAR -> {
                // Prioriza faixas conhecidas e artistas favoritos
                scoredTracks.sortedByDescending { if (it.isFamiliar) it.score + 20f else it.score }
            }
            RadioTuningMode.DISCOVERY -> {
                // Prioriza novas descobertas que o usuário ainda não tocou
                scoredTracks.sortedByDescending { if (!it.isFamiliar) it.score + 15f else it.score - 10f }
            }
            RadioTuningMode.DEEP_CUTS -> {
                // Prioriza remixes, faixas menos óbvias do cluster
                scoredTracks.sortedByDescending { it.score * (if (it.track.title.contains("remix", ignoreCase = true) || it.track.title.contains("vip", ignoreCase = true)) 2f else 1f) }
            }
            RadioTuningMode.BALANCED -> {
                // Equilíbrio perfeito
                scoredTracks.sortedByDescending { it.score }
            }
        }.map { it.track }.toMutableList()

        // 7. Montagem com Guarda de Diversidade (Anti-Clustering):
        // Impede que 2 faixas consecutivas pertençam ao mesmo artista
        var lastArtist = seedArtist

        while (resultQueue.size < maxQueueSize && sortedCandidates.isNotEmpty()) {
            val nextTrack = sortedCandidates.firstOrNull { it.artist.trim().lowercase() != lastArtist }
                ?: sortedCandidates.firstOrNull()

            if (nextTrack != null) {
                sortedCandidates.remove(nextTrack)
                if (seenTrackIds.add(nextTrack.id)) {
                    resultQueue.add(nextTrack)
                    lastArtist = nextTrack.artist.trim().lowercase()
                }
            } else {
                break
            }
        }

        Log.i(TAG, "SmartQueue gerada com perfeição: ${resultQueue.size} faixas para '${seedTrack.title}' (Modo: $tuningMode)")
        return resultQueue
    }
}
