package com.nerf.music.player

import android.util.Log
import com.nerf.music.data.repository.AudioQuality

/**
 * Resolvedor composto que combina múltiplos resolvedores de stream (InnerTube e NewPipeExtractor)
 * para garantir máxima velocidade de inicialização (<200ms) e tolerância zero a falhas.
 *
 * Estratégia:
 * 1. Tenta primeiro o NewPipeExtractor, que respeita as preferências de qualidade e economia.
 * 2. Em caso de falha, tenta o resolvedor InnerTube como alternativa.
 */
class CompositeStreamResolver(
    internal val primary: StreamResolver,
    internal val secondary: StreamResolver
) : StreamResolver {

    companion object {
        private const val TAG = "CompositeStreamResolver"
    }

    override suspend fun resolveStreamUrl(videoId: String): Result<String> {
        return resolveStreamUrl(videoId, targetQuality = null)
    }

    override suspend fun resolveStreamUrl(videoId: String, targetQuality: AudioQuality?): Result<String> {
        if (videoId.isBlank()) {
            return Result.failure(IllegalArgumentException("videoId não pode ser vazio"))
        }

        val primaryResult = primary.resolveStreamUrl(videoId, targetQuality)
        if (primaryResult.isSuccess && !primaryResult.getOrNull().isNullOrBlank()) {
            return primaryResult
        }

        Log.w(
            TAG,
            "Resolvedor primário falhou para videoId=$videoId (${primaryResult.exceptionOrNull()?.message}). Acionando resolvedor secundário..."
        )

        val secondaryResult = secondary.resolveStreamUrl(videoId, targetQuality)
        if (secondaryResult.isSuccess && !secondaryResult.getOrNull().isNullOrBlank()) {
            Log.i(TAG, "Fallback secundário resolveu stream com sucesso para videoId=$videoId")
            return secondaryResult
        }

        Log.e(
            TAG,
            "Ambos os resolvedores de stream falharam para videoId=$videoId. Secundário: ${secondaryResult.exceptionOrNull()?.message}"
        )
        return secondaryResult
    }

    override suspend fun resolveVideoStreamUrl(videoId: String): Result<String?> {
        val primaryVideo = primary.resolveVideoStreamUrl(videoId)
        if (primaryVideo.isSuccess && primaryVideo.getOrNull() != null) {
            return primaryVideo
        }
        return secondary.resolveVideoStreamUrl(videoId)
    }
}

/** Uses NewPipe first because it applies the user's active quality and data-saver profile. */
internal fun createDefaultStreamResolver(context: android.content.Context?): StreamResolver =
    CompositeStreamResolver(
        primary = NewPipeStreamResolver(context),
        secondary = InnerTubeStreamResolver()
    )
