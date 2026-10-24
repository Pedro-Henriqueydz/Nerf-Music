package com.nerf.music.player

import android.content.Context
import android.util.Log
import com.nerf.music.data.repository.AudioQuality
import com.nerf.music.data.repository.DataSaverMode
import com.nerf.music.util.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.io.IOException

/**
 * Resolvedor de streams de mídia (áudio e vídeo para Canvas) utilizando a biblioteca NewPipeExtractor,
 * com suporte à seleção dinâmica de bitrate baseado nas configurações de Qualidade de Mídia
 * e Modo de Economia de Dados (idêntico ao Spotify 9.1.94).
 */
class NewPipeStreamResolver(
    private val context: Context? = null
) : StreamResolver {

    companion object {
        private const val TAG = "NewPipeStreamResolver"
        private const val YOUTUBE_WATCH_BASE = "https://www.youtube.com/watch?v="
    }

    override suspend fun resolveStreamUrl(videoId: String): Result<String> = withContext(Dispatchers.IO) {
        if (videoId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Identificador de vídeo vazio"))
        }

        try {
            if (NewPipe.getDownloader() == null) {
                NewPipe.init(OkHttpDownloader())
            }

            val youtubeService = NewPipe.getService(ServiceList.YouTube.serviceId)
            val watchUrl = "$YOUTUBE_WATCH_BASE$videoId"

            val streamInfo = StreamInfo.getInfo(youtubeService, watchUrl)
            val audioStreams = streamInfo.audioStreams

            if (audioStreams.isNullOrEmpty()) {
                Log.w(TAG, "Nenhum fluxo de áudio retornado pelo NewPipeExtractor para videoId=$videoId")
                return@withContext Result.failure(IOException("Falha ao carregar a faixa. O servidor recusou a conexão."))
            }

            val validStreams = audioStreams.filter { !it.content.isNullOrBlank() }
            if (validStreams.isEmpty()) {
                Log.w(TAG, "Todos os fluxos de áudio retornaram URLs vazias para videoId=$videoId")
                return@withContext Result.failure(IOException("Falha ao carregar a faixa. O servidor recusou a conexão."))
            }

            // Seleciona o stream conforme o perfil de qualidade e economia de dados ativo
            val targetQuality = determineTargetQuality()
            val selectedStream = pickStreamForQuality(validStreams, targetQuality)

            val audioUrl = selectedStream.content
            if (audioUrl.isNullOrBlank()) {
                return@withContext Result.failure(IOException("Falha ao obter URL válida da faixa selecionada."))
            }

            Log.i(
                TAG,
                "Áudio decifrado com sucesso via NewPipe (Qualidade=$targetQuality): itag=${selectedStream.itag}, bitrate=${selectedStream.averageBitrate}, codec=${selectedStream.codec}"
            )
            Result.success(audioUrl)
        } catch (e: ExtractionException) {
            Log.e(TAG, "Exceção de extração/cipher do NewPipe para videoId=$videoId: ${e.message}", e)
            Result.failure(IOException("Falha ao carregar a faixa. O servidor recusou a conexão.", e))
        } catch (e: Exception) {
            Log.e(TAG, "Falha na resolução de áudio via NewPipe para videoId=$videoId: ${e.message}", e)
            Result.failure(IOException("Falha ao carregar a faixa. O servidor recusou a conexão.", e))
        }
    }

    private fun determineTargetQuality(): AudioQuality {
        val ctx = context ?: return AudioQuality.HIGH
        val prefs = ctx.getSharedPreferences("nerf_user_settings_prefs", Context.MODE_PRIVATE)
        val dataSaverName = prefs.getString("key_data_saver_mode", DataSaverMode.ALWAYS_OFF.name) ?: DataSaverMode.ALWAYS_OFF.name
        val dataSaver = try { DataSaverMode.valueOf(dataSaverName) } catch (_: Exception) { DataSaverMode.ALWAYS_OFF }

        val isWifi = NetworkUtils.isConnectedToWifi(ctx)

        // Se o modo Economia de dados estiver sempre ativado ou em automático numa rede móvel, força Baixa qualidade
        if (dataSaver == DataSaverMode.ALWAYS_ON || (dataSaver == DataSaverMode.AUTOMATIC && !isWifi)) {
            return AudioQuality.LOW
        }

        return if (isWifi) {
            val qName = prefs.getString("key_wifi_audio_quality", AudioQuality.HIGH.name) ?: AudioQuality.HIGH.name
            try { AudioQuality.valueOf(qName) } catch (_: Exception) { AudioQuality.HIGH }
        } else {
            val qName = prefs.getString("key_cellular_audio_quality", AudioQuality.AUTOMATIC.name) ?: AudioQuality.AUTOMATIC.name
            try { AudioQuality.valueOf(qName) } catch (_: Exception) { AudioQuality.AUTOMATIC }
        }
    }

    private fun pickStreamForQuality(streams: List<AudioStream>, quality: AudioQuality): AudioStream {
        return when (quality) {
            AudioQuality.LOW -> {
                // Menor bitrate viável (~48k-70k opus/m4a) para economia drástica de banda
                streams.minByOrNull { it.averageBitrate } ?: streams.first()
            }
            AudioQuality.NORMAL -> {
                // Bitrate intermediário próximo a 128 kbps
                streams.minByOrNull { kotlin.math.abs(it.averageBitrate - 128_000) } ?: streams.first()
            }
            AudioQuality.HIGH, AudioQuality.VERY_HIGH -> {
                // Maior bitrate disponível (ex: Opus 160k ou 256k)
                streams.maxByOrNull { it.averageBitrate } ?: streams.first()
            }
            AudioQuality.AUTOMATIC -> {
                val isWifi = context?.let { NetworkUtils.isConnectedToWifi(it) } ?: true
                if (isWifi) {
                    streams.maxByOrNull { it.averageBitrate } ?: streams.first()
                } else {
                    streams.minByOrNull { kotlin.math.abs(it.averageBitrate - 128_000) } ?: streams.first()
                }
            }
        }
    }

    override suspend fun resolveVideoStreamUrl(videoId: String): Result<String?> = withContext(Dispatchers.IO) {
        if (videoId.isBlank()) {
            return@withContext Result.success(null)
        }

        try {
            if (NewPipe.getDownloader() == null) {
                NewPipe.init(OkHttpDownloader())
            }

            val youtubeService = NewPipe.getService(ServiceList.YouTube.serviceId)
            val watchUrl = "$YOUTUBE_WATCH_BASE$videoId"

            val streamInfo = StreamInfo.getInfo(youtubeService, watchUrl)
            val videoStreams = (streamInfo.videoOnlyStreams.orEmpty() + streamInfo.videoStreams.orEmpty())

            if (videoStreams.isEmpty()) {
                return@withContext Result.success(null)
            }

            // Para economia de dados e renderização suave de Canvas em background,
            // escolhemos um stream leve (360p ou 480p)
            val validStreams = videoStreams.filter { !it.content.isNullOrBlank() }
            Log.i(TAG, "VideoStream count para videoId=$videoId: total=${videoStreams.size}, validos=${validStreams.size}")
            val selectedStream = validStreams
                .firstOrNull { it.resolution.contains("360p") || it.resolution.contains("480p") }
                ?: validStreams.firstOrNull()

            val videoUrl = selectedStream?.content
            Log.i(TAG, "Canvas video selecionado: itag=${selectedStream?.itag}, resolution=${selectedStream?.resolution}, url_valida=${!videoUrl.isNullOrBlank()}")
            Result.success(videoUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Falha na resolução de vídeo para Canvas via NewPipe para videoId=$videoId: ${e.message}", e)
            Result.success(null)
        }
    }
}
