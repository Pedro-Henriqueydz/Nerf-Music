package com.nerf.music.data.repository

import java.security.MessageDigest

/** Stable local IDs for playlists synced from an external service. */
internal object ImportedPlaylistIdentity {
    fun idFor(sourceKey: String): String {
        require(sourceKey.isNotBlank()) { "sourceKey não pode ser vazio" }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(sourceKey.toByteArray(Charsets.UTF_8))
            .take(12)
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
        return "import_pl_$digest"
    }
}
