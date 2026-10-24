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
 * Elimina a rigidez e repetição de algoritmos básicos, aplicando:
 * 1. Filtro de Skip Penalty (elimina faixas rejeitadas pelo ouvinte).
 * 2. Guarda de Diversidade (evita múltiplos lançamentos consecutivos do mesmo artista).
 * 3. Proporção Áurea de Mistura: Intercala novas descobertas do cluster acústico
 *    com faixas queridinhas do histórico e biblioteca do usuário.
 * 4. Sintonizador de Modo: Familiar vs. Descoberta vs. Equilibrado.
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
        maxQueueSize: Int = 30
    ): List<Track> {
        if (rawCandidates.isEmpty()) return listOf(seedTrack)

        val resultQueue = mutableListOf<Track>()
        val seenTrackIds = mutableSetOf<String>()

        // 1. A primeira faixa é sempre a semente atual
        resultQueue.add(seedTrack)
        seenTrackIds.add(seedTrack.id)

        // 2. Filtra candidatos brutos descartando músicas com Skip Penalty negativo
        val filteredCandidates = rawCandidates.filter { candidate ->
            !seenTrackIds.contains(candidate.id) && !affinityRepo.isTrackPenalized(candidate.id)
        }.toMutableList()

        // 3. Seleciona candidatos familiares da biblioteca do usuário compatíveis com o artista/gênero da semente
        val seedArtist = seedTrack.artist.trim().lowercase()
        val familiarPool = (userLikedTracks + userRecentTracks)
            .filter { it.id.isNotBlank() && !seenTrackIds.contains(it.id) && !affinityRepo.isTrackPenalized(it.id) }
            .distinctBy { it.id }
            .sortedByDescending { affinityRepo.getTrackAffinity(it.id) + affinityRepo.getArtistAffinity(it.artist) }
            .toMutableList()

        // 4. Determina proporção de familiares vs. novas descobertas conforme o modo escolhido
        val (familiarRatio, newRatio) = when (tuningMode) {
            RadioTuningMode.FAMILIAR -> Pair(0.65f, 0.35f)
            RadioTuningMode.DISCOVERY -> Pair(0.10f, 0.90f)
            RadioTuningMode.DEEP_CUTS -> Pair(0.20f, 0.80f)
            RadioTuningMode.BALANCED -> Pair(0.30f, 0.70f)
        }

        // 5. Montagem intercalada inteligente da fila (Proporção Áurea)
        var lastArtist = seedArtist

        while (resultQueue.size < maxQueueSize && (filteredCandidates.isNotEmpty() || familiarPool.isNotEmpty())) {
            val shouldPickFamiliar = familiarPool.isNotEmpty() &&
                    (Math.random() < familiarRatio || filteredCandidates.isEmpty())

            val nextTrack: Track? = if (shouldPickFamiliar) {
                // Tenta pegar familiar com artista diferente do anterior para garantir diversidade
                val diverseFamiliar = familiarPool.firstOrNull { it.artist.trim().lowercase() != lastArtist }
                    ?: familiarPool.firstOrNull()
                if (diverseFamiliar != null) {
                    familiarPool.remove(diverseFamiliar)
                }
                diverseFamiliar
            } else {
                // Pega do cluster da rádio priorizando artistas não repetidos consecutivamente
                val diverseCandidate = filteredCandidates.firstOrNull { it.artist.trim().lowercase() != lastArtist }
                    ?: filteredCandidates.firstOrNull()
                if (diverseCandidate != null) {
                    filteredCandidates.remove(diverseCandidate)
                }
                diverseCandidate
            }

            if (nextTrack != null && seenTrackIds.add(nextTrack.id)) {
                resultQueue.add(nextTrack)
                lastArtist = nextTrack.artist.trim().lowercase()
            } else if (filteredCandidates.isEmpty() && familiarPool.isEmpty()) {
                break
            }
        }

        Log.i(TAG, "SmartQueue gerada com sucesso: ${resultQueue.size} faixas para '${seedTrack.title}' (Modo: $tuningMode)")
        return resultQueue
    }
}
