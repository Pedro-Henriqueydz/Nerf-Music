package com.nerf.music.jam

import com.nerf.music.data.model.Track
import kotlinx.serialization.Serializable

enum class JamConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    ERROR
}

@Serializable
data class JamParticipant(
    val peerId: String,
    val displayName: String,
    val isHost: Boolean = false,
    val rttMs: Long = 0L
)

@Serializable
enum class JamMessageType {
    JOIN,
    WELCOME,
    STATE_SNAPSHOT,
    COMMAND,
    PING,
    PONG,
    LEAVE
}

@Serializable
enum class JamCommandType {
    PLAY,
    PAUSE,
    SEEK,
    SKIP_NEXT,
    SKIP_PREV,
    PLAY_TRACK,
    SELECT_QUEUE_INDEX,
    ADD_TO_QUEUE,
    PLAY_NEXT_IN_QUEUE,
    REMOVE_QUEUE_ITEM,
    MOVE_QUEUE_ITEM
}

@Serializable
data class JamWireMessage(
    val type: JamMessageType,
    val sessionId: String,
    val senderId: String,
    val senderName: String = "",
    val timestampMs: Long = System.currentTimeMillis(),
    // Campos para STATE_SNAPSHOT / WELCOME
    val hostId: String? = null,
    val hostName: String? = null,
    val participants: List<JamParticipant> = emptyList(),
    val currentTrack: Track? = null,
    val queue: List<Track> = emptyList(),
    val currentQueueIndex: Int = 0,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    // Campos para COMMAND
    val command: JamCommandType? = null,
    val targetPositionMs: Long? = null,
    val targetQueueIndex: Int? = null,
    val fromQueueIndex: Int? = null,
    val toQueueIndex: Int? = null,
    val targetTrack: Track? = null,
    val targetTracks: List<Track> = emptyList(),
    // Campos para PING / PONG
    val pingTimestampMs: Long? = null
)

data class JamSessionState(
    val isInJam: Boolean = false,
    val isHost: Boolean = false,
    val sessionId: String = "",
    val localPeerId: String = "",
    val localDisplayName: String = "",
    val hostName: String = "",
    val wsEndpointUrl: String = "",
    val inviteDeepLink: String = "",
    val participants: List<JamParticipant> = emptyList(),
    val connectionStatus: JamConnectionStatus = JamConnectionStatus.DISCONNECTED,
    val rttMs: Long = 0L,
    val errorMessage: String? = null
)
