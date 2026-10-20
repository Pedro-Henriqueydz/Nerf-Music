package com.nerf.music.player

import android.util.Log
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
 * Utiliza o endpoint `/youtubei/v1/player` com estratégias de fallback de múltiplos clientes,
 * priorizando clientes da Apple (IOS_MUSIC, IOS) e Web Remix com sincronização estrita de headers
 * (User-Agent, Origin, Referer) para contornar restrições de DRM e autenticação em faixas musicais.
 */
class InnerTubeStreamResolver(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) : StreamResolver {

    companion object {
        private const val TAG = "InnerTubeStreamResolver"
        private const val YOUTUBE_PLAYER_URL = "https://www.youtube.com/youtubei/v1/player"
        private const val YOUTUBE_MUSIC_PLAYER_URL = "https://music.youtube.com/youtubei/v1/player"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    override suspend fun resolveStreamUrl(videoId: String): Result<String> = withContext(Dispatchers.IO) {
        if (videoId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Identificador de vídeo vazio"))
        }

        // Lista de clientes ordenados por eficácia no catálogo musical
        val clientConfigs = listOf(
            // 1. IOS_MUSIC (cliente oficial do app YouTube Music no iOS)
            ClientConfig(
                clientName = "IOS_MUSIC",
                clientVersion = "6.41.2",
                endpointUrl = YOUTUBE_MUSIC_PLAYER_URL,
                userAgent = "com.google.ios.youtubemusic/6.41.2 (iPhone14,3; U; CPU iOS 15_6 like Mac OS X)",
                origin = "https://music.youtube.com",
                deviceMake = "Apple",
                deviceModel = "iPhone14,3",
                osName = "iOS",
                osVersion = "15.6.0.19G71"
            ),
            // 2. IOS legado (frequentemente recebe URLs diretas não ofuscadas)
            ClientConfig(
                clientName = "IOS",
                clientVersion = "19.29.1",
                endpointUrl = YOUTUBE_PLAYER_URL,
                userAgent = "com.google.ios.youtube/19.29.1 (iPhone14,3; U; CPU iOS 17_5_1 like Mac OS X)",
                origin = "https://www.youtube.com",
                deviceMake = "Apple",
                deviceModel = "iPhone14,3",
                osName = "iPhone",
                osVersion = "17.5.1"
            ),
            // 3. IOS moderno (versão 21.x para compatibilidade caso 19.x seja rejeitado)
            ClientConfig(
                clientName = "IOS",
                clientVersion = "21.26.4",
                endpointUrl = YOUTUBE_PLAYER_URL,
                userAgent = "com.google.ios.youtube/21.26.4 (iPhone16,2; U; CPU iOS 18_3_2 like Mac OS X;)",
                origin = "https://www.youtube.com",
                deviceMake = "Apple",
                deviceModel = "iPhone16,2",
                osName = "iPhone",
                osVersion = "18.3.2"
            ),
            // 4. WEB_REMIX (cliente web do YouTube Music)
            ClientConfig(
                clientName = "WEB_REMIX",
                clientVersion = "1.20231214.00.00",
                endpointUrl = YOUTUBE_MUSIC_PLAYER_URL,
                userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                origin = "https://music.youtube.com"
            ),
            // 5. ANDROID_VR (perfil Quest 3 para extração progressiva direta)
            ClientConfig(
                clientName = "ANDROID_VR",
                clientVersion = "1.60.19",
                endpointUrl = YOUTUBE_PLAYER_URL,
                userAgent = "Mozilla/5.0 (Linux; Android 12; Quest 3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
                origin = "https://www.youtube.com",
                deviceMake = "Oculus",
                deviceModel = "Quest 3",
                osName = "Android",
                osVersion = "12L",
                platform = "MOBILE"
            ),
            // 6. TVHTML5 (porta de Smart TV)
            ClientConfig(
                clientName = "TVHTML5",
                clientVersion = "7.20230405.08.01",
                endpointUrl = YOUTUBE_PLAYER_URL,
                userAgent = "Mozilla/5.0 (SMART-TV; LINUX; Tizen 6.0) AppleWebKit/537.36",
                origin = "https://www.youtube.com"
            )
        )

        for (config in clientConfigs) {
            try {
                val payload = buildPlayerPayload(videoId, config)
                val request = Request.Builder()
                    .url(config.endpointUrl)
                    .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .header("User-Agent", config.userAgent)
                    .header("Origin", config.origin)
                    .header("Referer", "${config.origin}/")
                    .header("Accept-Language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7")
                    .build()

                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val audioUrl = extractAudioUrlFromPlayerResponse(body, config.clientName)
                    if (!audioUrl.isNullOrBlank()) {
                        Log.i(TAG, "[${config.clientName}] URL de áudio resolvida com sucesso para videoId=$videoId")
                        return@withContext Result.success(audioUrl)
                    }
                } else {
                    Log.w(TAG, "[${config.clientName}] HTTP ${response.code} ao resolver videoId=$videoId")
                }
            } catch (e: Exception) {
                Log.w(TAG, "[${config.clientName}] Falha na requisição para videoId=$videoId: ${e.message}")
            }
        }

        Log.e(TAG, "Nenhum cliente conseguiu resolver uma URL de áudio direta para videoId=$videoId")
        Result.failure(IOException("Falha ao carregar a faixa. O servidor recusou a conexão."))
    }

    private data class ClientConfig(
        val clientName: String,
        val clientVersion: String,
        val endpointUrl: String,
        val userAgent: String,
        val origin: String,
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
     * Registra avisos de diagnóstico para playabilityStatus e bloqueios por cipher.
     */
    fun extractAudioUrlFromPlayerResponse(jsonString: String, clientName: String = "CLIENT"): String? {
        if (jsonString.isBlank()) return null

        return try {
            val root = JSONObject(jsonString)
            val playabilityStatus = root.optJSONObject("playabilityStatus")
            val status = playabilityStatus?.optString("status") ?: "STATUS_AUSENTE"
            val reason = playabilityStatus?.optString("reason") ?: ""

            if (status != "OK") {
                Log.w(TAG, "[$clientName] playabilityStatus não OK: status=$status, reason=$reason")
                return null
            }

            val streamingData = root.optJSONObject("streamingData")
            if (streamingData == null) {
                Log.w(TAG, "[$clientName] streamingData ausente na resposta")
                return null
            }

            var bestAudioUrl: String? = null
            var maxBitrate = -1L

            // 1. Procura em adaptiveFormats (DASH / formatos isolados de áudio)
            val adaptiveFormats = streamingData.optJSONArray("adaptiveFormats")
            if (adaptiveFormats != null) {
                for (i in 0 until adaptiveFormats.length()) {
                    val format = adaptiveFormats.optJSONObject(i) ?: continue
                    val mimeType = format.optString("mimeType")
                    val isAudio = mimeType.startsWith("audio/")

                    val url = format.optString("url")
                    val hasCipher = format.has("signatureCipher") || format.has("cipher")

                    if (isAudio && url.isNullOrBlank() && hasCipher) {
                        val itag = format.optInt("itag", -1)
                        Log.w(TAG, "[$clientName] Áudio bloqueado por Cipher para itag=$itag (mime=$mimeType)")
                    }

                    if (!isAudio) continue
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
                        val hasCipher = format.has("signatureCipher") || format.has("cipher")
                        val mimeType = format.optString("mimeType")

                        if (url.isNullOrBlank() && hasCipher) {
                            val itag = format.optInt("itag", -1)
                            Log.w(TAG, "[$clientName] Áudio progressivo bloqueado por Cipher para itag=$itag")
                        }

                        if (url.isNullOrBlank()) continue

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

            if (bestAudioUrl != null) {
                Log.i(TAG, "[$clientName] Faixa selecionada com sucesso (bitrate: $maxBitrate)")
            }

            bestAudioUrl
        } catch (e: Exception) {
            Log.e(TAG, "[$clientName] Erro ao analisar resposta do player: ${e.message}")
            null
        }
    }
}
