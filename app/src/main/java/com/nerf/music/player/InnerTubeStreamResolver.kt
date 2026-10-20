package com.nerf.music.player

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Resolvedor de stream real para o catálogo do YouTube Music via InnerTube.
 *
 * Utiliza o endpoint `/youtubei/v1/player` com estratégias de fallback de múltiplos clientes
 * (ANDROID_VR, TVHTML5, ANDROID_MUSIC) para contornar bloqueios de autenticação e extrair
 * o fluxo de áudio direto (adaptiveFormats audio/webm ou audio/mp4).
 */
class InnerTubeStreamResolver(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) : StreamResolver {

    companion object {
        private const val YOUTUBE_PLAYER_URL = "https://www.youtube.com/youtubei/v1/player"
        private const val YOUTUBE_MUSIC_PLAYER_URL = "https://music.youtube.com/youtubei/v1/player"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private const val USER_AGENT_MOBILE =
            "Mozilla/5.0 (Linux; Android 12; Quest 3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }

    override suspend fun resolveStreamUrl(videoId: String): Result<String> = withContext(Dispatchers.IO) {
        if (videoId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Identificador de vídeo vazio"))
        }

        // Tenta sequencialmente os clientes configurados até obter uma URL de áudio reproduzível
        val clientConfigs = listOf(
            ClientConfig(
                clientName = "ANDROID_VR",
                clientVersion = "1.60.19",
                endpointUrl = YOUTUBE_PLAYER_URL,
                deviceMake = "Oculus",
                deviceModel = "Quest 3",
                osName = "Android",
                osVersion = "12L",
                platform = "MOBILE"
            ),
            ClientConfig(
                clientName = "TVHTML5",
                clientVersion = "7.20230405.08.01",
                endpointUrl = YOUTUBE_PLAYER_URL
            ),
            ClientConfig(
                clientName = "ANDROID_MUSIC",
                clientVersion = "6.40.52",
                endpointUrl = YOUTUBE_MUSIC_PLAYER_URL
            )
        )

        for (config in clientConfigs) {
            try {
                val payload = buildPlayerPayload(videoId, config)
                val request = Request.Builder()
                    .url(config.endpointUrl)
                    .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .header("User-Agent", USER_AGENT_MOBILE)
                    .header("Origin", "https://music.youtube.com")
                    .header("Referer", "https://music.youtube.com/")
                    .build()

                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val audioUrl = extractAudioUrlFromPlayerResponse(body)
                    if (!audioUrl.isNullOrBlank()) {
                        return@withContext Result.success(audioUrl)
                    }
                }
            } catch (_: Exception) {
                // Continua para o próximo cliente em caso de erro na requisição
            }
        }

        Result.failure(IOException("Falha ao carregar a faixa. O servidor recusou a conexão."))
    }

    private data class ClientConfig(
        val clientName: String,
        val clientVersion: String,
        val endpointUrl: String,
        val deviceMake: String? = null,
        val deviceModel: String? = null,
        val osName: String? = null,
        val osVersion: String? = null,
        val platform: String? = null
    )

    private fun buildPlayerPayload(videoId: String, config: ClientConfig): JSONObject {
        val root = JSONObject()
        root.put("videoId", videoId)
        root.put("contentCheckOk", true)
        root.put("racyCheckOk", true)

        val context = JSONObject()
        val client = JSONObject()
        client.put("clientName", config.clientName)
        client.put("clientVersion", config.clientVersion)
        client.put("hl", "pt-BR")
        client.put("gl", "BR")

        config.deviceMake?.let { client.put("deviceMake", it) }
        config.deviceModel?.let { client.put("deviceModel", it) }
        config.osName?.let { client.put("osName", it) }
        config.osVersion?.let { client.put("osVersion", it) }
        config.platform?.let { client.put("platform", it) }

        context.put("client", client)
        root.put("context", context)
        return root
    }

    /**
     * Extrai a melhor URL direta de áudio disponível no JSON retornado pelo endpoint player.
     * Prioriza faixas de áudio adaptativas (audio/webm, audio/mp4) com o maior bitrate.
     */
    fun extractAudioUrlFromPlayerResponse(jsonString: String): String? {
        if (jsonString.isBlank()) return null

        return try {
            val root = JSONObject(jsonString)
            val playabilityStatus = root.optJSONObject("playabilityStatus")
            val status = playabilityStatus?.optString("status")
            if (status != "OK") {
                return null
            }

            val streamingData = root.optJSONObject("streamingData") ?: return null

            var bestAudioUrl: String? = null
            var maxBitrate = -1L

            // 1. Procura em adaptiveFormats (DASH / formatos isolados de áudio)
            val adaptiveFormats = streamingData.optJSONArray("adaptiveFormats")
            if (adaptiveFormats != null) {
                for (i in 0 until adaptiveFormats.length()) {
                    val format = adaptiveFormats.optJSONObject(i) ?: continue
                    val mimeType = format.optString("mimeType")
                    if (!mimeType.startsWith("audio/")) continue

                    val url = format.optString("url")
                    if (url.isNullOrBlank()) continue

                    val bitrate = format.optLong("bitrate", 0L)
                    if (bitrate > maxBitrate) {
                        maxBitrate = bitrate
                        bestAudioUrl = url
                    }
                }
            }

            // 2. Se não encontrou URL direta em adaptiveFormats, verifica formats progressivos
            if (bestAudioUrl == null) {
                val formats = streamingData.optJSONArray("formats")
                if (formats != null) {
                    for (i in 0 until formats.length()) {
                        val format = formats.optJSONObject(i) ?: continue
                        val url = format.optString("url")
                        if (url.isNullOrBlank()) continue

                        val mimeType = format.optString("mimeType")
                        if (mimeType.contains("audio") || mimeType.contains("mp4") || mimeType.contains("webm")) {
                            val bitrate = format.optLong("bitrate", 0L)
                            if (bitrate > maxBitrate) {
                                maxBitrate = bitrate
                                bestAudioUrl = url
                            }
                        }
                    }
                }
            }

            bestAudioUrl
        } catch (_: Exception) {
            null
        }
    }
}
