package com.nerf.music.player

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.io.IOException

/**
 * Resolvedor de stream baseado na biblioteca NewPipeExtractor.
 *
 * Utiliza o motor JavaScript (Rhino) e parsers nativos do NewPipeExtractor para:
 * - Decifrar assinaturas (`signatureCipher`).
 * - Resolver parâmetros de estrangulamento (`n-parameter`).
 * - Contornar barreiras de anti-bot e extrair URLs de áudio diretas (Opus/AAC).
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
            // Garante inicialização com OkHttpDownloader caso ainda não tenha sido inicializado
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

            // Seleciona o melhor stream com URL válida, priorizando maior bitrate (Opus/AAC)
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
}
