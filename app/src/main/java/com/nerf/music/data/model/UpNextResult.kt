package com.nerf.music.data.model

/**
 * Chip de filtro da fila dinâmica (ex: "Tudo", "Familiar", "Descobrir", "Popular", "Em alta").
 */
data class QueueFilterChip(
    val title: String,
    val params: String,
    val playlistId: String? = null,
    val isSelected: Boolean = false
)

/**
 * Resultado da Rádio Dinâmica (`/youtubei/v1/next`), contendo a lista de próximas faixas,
 * os chips de filtro da fila e o título de origem da fila.
 */
data class UpNextResult(
    val tracks: List<Track> = emptyList(),
    val chips: List<QueueFilterChip> = emptyList(),
    val queueSourceTitle: String? = null
)
