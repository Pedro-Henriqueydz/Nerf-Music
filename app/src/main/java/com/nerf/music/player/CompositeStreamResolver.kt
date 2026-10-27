package com.nerf.music.player

import android.content.Context
import android.util.Log
import com.nerf.music.data.repository.AudioQuality
import com.nerf.music.data.repository.DataSaverMode
import com.nerf.music.util.NetworkUtils

/**
 * Resolvedor composto que combina múltiplos resolvedores de stream (InnerTube e NewPipeExtractor)
 * para garantir máxima velocidade de inicialização (<200ms) e tolerância zero a falhas.
 *
 * Estratégia:
 * 1. Tenta primeiro o NewPipeExtractor, que respeita as preferências de qualidade e economia.
 * 2. Em caso de falha, tenta o resolvedor InnerTube com a mesma qualidade alvo e respeito a economia de dados.
 */
class CompositeStreamResolver(
    internal val primary: StreamResolver,
    internal val secondary: StreamResolver,
    private val context: Context? = null
) : StreamResolver {

    companion object {
        private const val TAG = "CompositeStreamResolver"
        private const val PREFS_NAME = "nerf_user_settings_prefs"
    }

    override suspend fun resolveStreamUrl(videoId: String): Result<String> {
        return resolveStreamUrl(videoId, targetQuality = null)
    }

    override suspend fun resolveStreamUrl(videoId: String, targetQuality: AudioQuality?): Result<String> {
        if (videoId.isBlank()) {
            return Result.failure(IllegalArgumentException("videoId não pode ser vazio"))
        }

        val effectiveQuality = determineEffectiveQuality(targetQuality)

        val primaryResult = primary.resolveStreamUrl(videoId, effectiveQuality)
        if (primaryResult.isSuccess && !primaryResult.getOrNull().isNullOrBlank()) {
            return primaryResult
        }

        Log.w(
            TAG,
            "Resolvedor primário falhou para videoId=$videoId (${primaryResult.exceptionOrNull()?.message}). Acionando resolvedor secundário..."
        )

        val secondaryResult = secondary.resolveStreamUrl(videoId, effectiveQuality)
        if (secondaryResult.isSuccess && !secondaryResult.getOrNull().isNullOrBlank()) {
            Log.i(TAG, "Fallback secundário resolveu stream com sucesso para videoId=$videoId (qualidade=$effectiveQuality)")
            return secondaryResult
        }

        Log.e(
            TAG,
            "Ambos os resolvedores de stream falharam para videoId=$videoId. Secundário: ${secondaryResult.exceptionOrNull()?.message}"
        )
        return secondaryResult
    }

    private fun determineEffectiveQuality(targetQuality: AudioQuality?): AudioQuality? {
        // Se a qualidade foi explicitamente especificada (ex: download em Lossless/Alta), preserva a qualidade alvo
        if (targetQuality != null) {
            return targetQuality
        }

        val ctx = context ?: return null
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val dataSaverName = prefs.getString("key_data_saver_mode", DataSaverMode.ALWAYS_OFF.name)
        val dataSaver = try {
            DataSaverMode.valueOf(dataSaverName ?: DataSaverMode.ALWAYS_OFF.name)
        } catch (_: Exception) {
            DataSaverMode.ALWAYS_OFF
        }

        val isWifi = NetworkUtils.isConnectedToWifi(ctx)

        // Se a Economia de Dados estiver ativa, força AudioQuality.LOW para streaming
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
        secondary = InnerTubeStreamResolver(context = context),
        context = context
    )
