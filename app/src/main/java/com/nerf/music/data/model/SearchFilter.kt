package com.nerf.music.data.model

/**
 * Filtros de busca disponíveis no InnerTube (YouTube Music Web).
 */
enum class SearchFilter(val displayName: String, val paramValue: String?) {
    ALL("Geral", null),
    SONGS("Músicas", "EgWKAQIIAWoSEAUQCRAKEAMQBBAQEBUQERAO")
}
