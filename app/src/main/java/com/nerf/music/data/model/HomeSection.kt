package com.nerf.music.data.model

import kotlinx.serialization.Serializable

/**
 * Estilo de renderização de cada seção da Home do YouTube Music:
 * - [QUICK_PICKS_GRID]: Grade horizontal de colunas empilhadas com 4 faixas por coluna (ex: "Escolha a dedo", "Em alta").
 * - [TWO_ROW_CAROUSEL]: Carrossel de cards quadrados grandes de 155dp (ex: "Clássicos da madrugada", "Novos álbuns e singles").
 */
@Serializable
enum class HomeSectionStyle {
    QUICK_PICKS_GRID,
    TWO_ROW_CAROUSEL
}

/**
 * Chip de filtro de humor/atividade do topo da Home do YouTube Music (ex: "Energia", "Relax", "Para treinar", "Foco").
 */
@Serializable
data class HomeChip(
    val title: String,
    val params: String
)

/**
 * Dados completos da aba Home (chips de humor + seções de carrossel).
 */
@Serializable
data class HomeFeedData(
    val chips: List<HomeChip> = emptyList(),
    val sections: List<HomeSection> = emptyList()
)

/**
 * Representa uma seção do feed da Home do YouTube Music.
 */
@Serializable
data class HomeSection(
    val title: String,
    val items: List<Track>,
    val strapline: String? = null,
    val style: HomeSectionStyle = HomeSectionStyle.TWO_ROW_CAROUSEL
)
