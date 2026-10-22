package com.nerf.music.data.model

import kotlinx.serialization.Serializable

/**
 * Seção de conteúdo relacionado (Related / "Relacionados") exibido ao clicar no título da música no Player.
 * Ex: "Talvez você também goste de", "Playlists recomendadas", "Outras apresentações", "Artistas parecidos".
 */
@Serializable
data class RelatedSection(
    val title: String,
    val items: List<Track>,
    val style: HomeSectionStyle = HomeSectionStyle.QUICK_PICKS_GRID
)

/**
 * Dados completos da aba "Relacionados" (Related) de uma música / vídeo.
 */
@Serializable
data class TrackRelatedData(
    val sections: List<RelatedSection> = emptyList(),
    val aboutArtistHeader: String? = null,
    val aboutArtistDescription: String? = null
)
