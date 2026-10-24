package com.nerf.music.data.model

import kotlinx.serialization.Serializable

/**
 * Estilo de renderização de cada seção da Home do YouTube Music:
 * - [SPEED_DIAL]: Grade de 3 colunas x 3 linhas paginada com dots indicadores e progresso.
 * - [QUICK_PICKS_GRID]: Grade horizontal de colunas empilhadas com 4 faixas por coluna (ex: "Escolha a dedo", "Covers e remixes").
 * - [TWO_ROW_CAROUSEL]: Carrossel de cards quadrados de 155dp (ex: "Novos álbuns e singles", "Mixes").
 * - [DAILY_DISCOVER]: Hero card imersivo de descoberta diária com a assinatura "Sounds like X by Y".
 * - [YOUR_DOWNLOADS]: Seção de atalhos rápidos de downloads com gradientes oficiais (Liked Music, Offline Mix).
 * - [COMMUNITY_PLAYLIST]: Card com mosaico 2x2 e faixas em destaque da comunidade.
 */
@Serializable
enum class HomeSectionStyle {
    SPEED_DIAL,
    QUICK_PICKS_GRID,
    TWO_ROW_CAROUSEL,
    DAILY_DISCOVER,
    YOUR_DOWNLOADS,
    COMMUNITY_PLAYLIST
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
 * Representa uma seção do feed dinâmico da Home do YouTube Music.
 */
@Serializable
data class HomeSection(
    val title: String,
    val items: List<Track>,
    val strapline: String? = null,
    val style: HomeSectionStyle = HomeSectionStyle.TWO_ROW_CAROUSEL,
    val heroSubtitle: String? = null,
    val heroCategory: String? = null,
    val communityAuthor: String? = null,
    val communityViewsText: String? = null
)
