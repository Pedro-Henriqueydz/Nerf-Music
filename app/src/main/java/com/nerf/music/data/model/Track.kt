package com.nerf.music.data.model

import kotlinx.serialization.Serializable

/**
 * Representa os metadados de uma faixa musical ou card reproduzível (álbum/mix/playlist)
 * retornado pelo catálogo InnerTube.
 */
@Serializable
data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val thumbnailUrl: String? = null,
    val durationText: String = "--:--",
    val durationSeconds: Long = 0,
    val sourceIdentifier: String = "INNERTUBE_WEB_REMIX",
    val playlistId: String? = null,
    val browseId: String? = null
) {
    /**
     * Retorna se este item é um Álbum ou Playlist navegável.
     */
    val isAlbumOrPlaylist: Boolean
        get() = (browseId != null && (browseId.startsWith("MPREb_") || browseId.startsWith("VLPL") || browseId.startsWith("RDAM"))) ||
                id.startsWith("MPREb_") || id.startsWith("VLPL") || id.startsWith("RDAM")

    /**
     * Retorna a URL da capa em alta resolução (544x544) quando proveniente do Google/YouTube Music,
     * ideal para exibição em destaque nos cards e na tela completa do Player.
     */
    val highResThumbnailUrl: String?
        get() = thumbnailUrl?.replace(Regex("=w\\d+-h\\d+"), "=w544-h544")
}
