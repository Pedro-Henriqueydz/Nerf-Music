package com.nerf.music.player

import android.content.Context
import android.util.Log
import com.nerf.music.data.repository.AudioQuality
import com.nerf.music.util.NetworkUtils
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
        .build(),
    private val context: Context? = null
) : StreamResolver {

    companion object {
        private const val TAG = "InnerTubeStreamResolver"
        private const val YOUTUBE_PLAYER_URL = "https://www.youtube.com/youtubei/v1/player"
        private const val YOUTUBE_MUSIC_PLAYER_URL = "https://music.youtube.com/youtubei/v1/player"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    override suspend fun resolveStreamUrl(videoId: String): Result<String> =
        resolveStreamUrl(videoId, targetQuality = null)

    override suspend fun resolveStreamUrl(videoId: String, targetQuality: AudioQuality?): Result<String> = withContext(Dispatchers.IO) {
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
                    val audioUrl = extractAudioUrlFromPlayerResponse(body, config.clientName, targetQuality)
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
     * Extrai a URL direta de áudio disponível no JSON retornado pelo endpoint player com suporte
     * à seleção de qualidade alvo (AudioQuality) e priorização de Opus (audio/webm, itag 251).
     */
    fun extractAudioUrlFromPlayerResponse(
        jsonString: String,
        clientName: String = "CLIENT",
        targetQuality: AudioQuality? = null
    ): String? {
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

            val candidates = mutableListOf<CandidateFormat>()

            // 1. Procura em adaptiveFormats (DASH / formatos isolados de áudio)
            val adaptiveFormats = streamingData.optJSONArray("adaptiveFormats")
            if (adaptiveFormats != null) {
                for (i in 0 until adaptiveFormats.length()) {
                    val format = adaptiveFormats.optJSONObject(i) ?: continue
                    val mimeType = format.optString("mimeType")
                    val isAudio = mimeType.startsWith("audio/")
                    val url = format.optString("url")
                    val hasCipher = format.has("signatureCipher") || format.has("cipher")
                    val itag = format.optInt("itag", -1)
                    val bitrate = format.optLong("bitrate", 0L)

                    if (isAudio && url.isNullOrBlank() && hasCipher) {
                        Log.w(TAG, "[$clientName] Áudio bloqueado por Cipher para itag=$itag (mime=$mimeType)")
                    }

                    if (isAudio && !url.isNullOrBlank() && !hasCipher) {
                        candidates.add(CandidateFormat(url = url, mimeType = mimeType, itag = itag, bitrate = bitrate))
                    }
                }
            }

            // 2. Se não encontrou URL direta em adaptiveFormats, verifica formats progressivos
            if (candidates.isEmpty()) {
                val formats = streamingData.optJSONArray("formats")
                if (formats != null) {
                    for (i in 0 until formats.length()) {
                        val format = formats.optJSONObject(i) ?: continue
                        val url = format.optString("url")
                        val hasCipher = format.has("signatureCipher") || format.has("cipher")
                        val mimeType = format.optString("mimeType")
                        val itag = format.optInt("itag", -1)
                        val bitrate = format.optLong("bitrate", 0L)

                        if (url.isNullOrBlank() && hasCipher) {
                            Log.w(TAG, "[$clientName] Áudio progressivo bloqueado por Cipher para itag=$itag")
                        }

                        if (!url.isNullOrBlank() && !hasCipher &&
                            (mimeType.contains("audio") || mimeType.contains("mp4") || mimeType.contains("webm"))
                        ) {
                            candidates.add(CandidateFormat(url = url, mimeType = mimeType, itag = itag, bitrate = bitrate))
                        }
                    }
                }
            }

            if (candidates.isEmpty()) {
                Log.w(TAG, "[$clientName] Nenhum formato de áudio reproduzível direto encontrado")
                return null
            }

            // Seleciona o formato de acordo com a qualidade alvo informada
            val selected = when (targetQuality) {
                AudioQuality.LOW -> {
                    // Menor bitrate viável (~48-64 kbps)
                    candidates.minByOrNull { it.bitrate }
                }
                AudioQuality.NORMAL -> {
                    // Bitrate intermediário próximo a 128 kbps (128_000 bps)
                    candidates.minByOrNull { kotlin.math.abs(it.bitrate - 128_000) }
                }
                AudioQuality.HIGH, AudioQuality.VERY_HIGH -> {
                    // Prioriza Opus (audio/webm, itag 251) sobre AAC com maior fidelidade acústica
                    val itag251 = candidates.firstOrNull { it.itag == 251 }
                    if (itag251 != null) {
                        itag251
                    } else {
                        val opusCandidates = candidates.filter {
                            it.mimeType.contains("webm", ignoreCase = true) || it.mimeType.contains("opus", ignoreCase = true)
                        }
                        opusCandidates.maxByOrNull { it.bitrate } ?: candidates.maxByOrNull { it.bitrate }
                    }
                }
                AudioQuality.AUTOMATIC -> {
                    val isWifi = context?.let { NetworkUtils.isConnectedToWifi(it) } ?: true
                    if (isWifi) {
                        val itag251 = candidates.firstOrNull { it.itag == 251 }
                        if (itag251 != null) {
                            itag251
                        } else {
                            val opusCandidates = candidates.filter {
                                it.mimeType.contains("webm", ignoreCase = true) || it.mimeType.contains("opus", ignoreCase = true)
                            }
                            opusCandidates.maxByOrNull { it.bitrate } ?: candidates.maxByOrNull { it.bitrate }
                        }
                    } else {
                        candidates.minByOrNull { kotlin.math.abs(it.bitrate - 128_000) }
                    }
                }
                null -> {
                    // Padrão: prioriza itag 251 se disponível e com bitrate alto, ou máximo bitrate absoluto
                    val itag251 = candidates.firstOrNull { it.itag == 251 }
                    val maxBitrate = candidates.maxOfOrNull { it.bitrate } ?: 0L
                    if (itag251 != null && itag251.bitrate >= maxBitrate) {
                        itag251
                    } else {
                        candidates.maxByOrNull { it.bitrate }
                    }
                }
            } ?: candidates.first()

            Log.i(TAG, "[$clientName] Faixa selecionada com sucesso (itag: ${selected.itag}, bitrate: ${selected.bitrate}, mime: ${selected.mimeType})")
            selected.url
        } catch (e: Exception) {
            Log.e(TAG, "[$clientName] Erro ao analisar resposta do player: ${e.message}")
            null
        }
    }

    private data class CandidateFormat(
        val url: String,
        val mimeType: String,
        val itag: Int,
        val bitrate: Long
    )
}
