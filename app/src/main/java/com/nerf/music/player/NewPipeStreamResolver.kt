package com.nerf.music.player

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.io.IOException

/**
 * Resolvedor de streams de mídia (áudio e vídeo para Canvas) utilizando a biblioteca NewPipeExtractor.
 */
class NewPipeStreamResolver : StreamResolver {

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

            // Seleciona o melhor stream de áudio com URL válida
            val bestStream = audioStreams
                .filter { !it.content.isNullOrBlank() }
                .maxByOrNull { it.averageBitrate }

            val audioUrl = bestStream?.content
            if (audioUrl.isNullOrBlank()) {
                Log.w(TAG, "Todos os fluxos de áudio retornaram URLs vazias para videoId=$videoId")
                return@withContext Result.failure(IOException("Falha ao carregar a faixa. O servidor recusou a conexão."))
            }

            Log.i(
                TAG,
                "Áudio decifrado com sucesso via NewPipeExtractor: itag=${bestStream.itag}, bitrate=${bestStream.averageBitrate}, codec=${bestStream.codec}"
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
            Log.w(TAG, "Não foi possível extrair stream de vídeo para Canvas videoId=$videoId: ${e.message}")
            Result.success(null)
        }
    }
}
