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

/**
 * Configurações da sala JAM controladas pelo Host e espelhadas em todos os participantes.
 */
@Serializable
data class JamRoomSettings(
    val allowGuestsToQueue: Boolean = true,
    val allowGuestsToControlPlayback: Boolean = true,
    val repeatModeName: String = "OFF", // "OFF", "ALL", "ONE"
    val hostCrossfadeSeconds: Int = 0, // 0..12s (aplicado a todos os participantes da Jam)
    val hostAutomixEnabled: Boolean = true,
    val hostGaplessEnabled: Boolean = true,
    val syncKaraokeWithRoom: Boolean = false,
    val isKaraokeActiveInRoom: Boolean = false,
    val karaokeVocalLevelInRoom: Float = 0.0f
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
    MOVE_QUEUE_ITEM,
    SET_REPEAT_MODE,
    KICK_PARTICIPANT,
    REQUEST_SYNC
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
    val roomSettings: JamRoomSettings = JamRoomSettings(),
    val actionAnnouncement: String? = null,
    // Campos para COMMAND
    val command: JamCommandType? = null,
    val targetPositionMs: Long? = null,
    val targetQueueIndex: Int? = null,
    val fromQueueIndex: Int? = null,
    val toQueueIndex: Int? = null,
    val targetTrack: Track? = null,
    val targetTracks: List<Track> = emptyList(),
    val targetRepeatModeName: String? = null,
    val targetPeerId: String? = null,
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
    val roomSettings: JamRoomSettings = JamRoomSettings(),
    val connectionStatus: JamConnectionStatus = JamConnectionStatus.DISCONNECTED,
    val rttMs: Long = 0L,
    val errorMessage: String? = null
)
