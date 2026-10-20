package com.nerf.music.data.model

import kotlinx.serialization.Serializable

/**
 * Representa um Artista retornado pelo catálogo musical.
 * Mantido como tipo distinto de [Track] para evitar que artistas
 * sejam tratados erroneamente como músicas reproduzíveis.
 */
@Serializable
data class Artist(
    val id: String,
    val name: String,
    val subtitle: String? = null,
    val thumbnailUrl: String? = null
)
