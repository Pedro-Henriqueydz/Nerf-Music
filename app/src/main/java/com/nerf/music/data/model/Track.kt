package com.nerf.music.data.model

import kotlinx.serialization.Serializable

/**
 * Representa os metadados de uma faixa musical retornada pelo catálogo.
 *
 * IMPORTANTE: Ter os metadados no catálogo NÃO garante que a faixa é reproduzível
 * diretamente sem autorização ou resolução de stream específica.
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
    val sourceIdentifier: String = "INNERTUBE_WEB_REMIX"
) {
    /**
     * Retorna a URL da capa em alta resolução (544x544) quando proveniente do Google/YouTube Music,
     * ideal para exibição em destaque na tela completa do Player.
     */
    val highResThumbnailUrl: String?
        get() = thumbnailUrl?.replace(Regex("=w\\d+-h\\d+"), "=w544-h544")
}
