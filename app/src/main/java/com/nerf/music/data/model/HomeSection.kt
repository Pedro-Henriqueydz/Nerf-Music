package com.nerf.music.data.model

import kotlinx.serialization.Serializable

/**
 * Representa uma seção do feed da Home (ex: "Escolha a dedo", "Em alta", etc.)
 * contendo uma lista de faixas musicais para exibição em carrossel horizontal.
 */
@Serializable
data class HomeSection(
    val title: String,
    val items: List<Track>
)
