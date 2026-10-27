package com.nerf.music.player

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Processador DSP em tempo real para Isolamento/Remoção Vocal (Modo Karaokê / Sing)
 * rodando 100% localmente no pipeline PCM do ExoPlayer (Media3).
 *
 * Algoritmo: Decomposição Estéreo Mid/Side com Filtragem Biquad Band-Isolated:
 * - Decompõe o sinal estéreo em Canal Central (Mid = (L + R)/2) e Lateral (Side = (L - R)/2).
 * - Preserva os graves centrais (< 160 Hz, bumbo e contrabaixo) via filtro Butterworth Low-Pass de 2ª ordem.
 * - Preserva o brilho/agudos centrais (> 6200 Hz, pratos e ambiência) via filtro Butterworth High-Pass de 2ª ordem.
 * - Atenua seletivamente a banda vocal central (160 Hz – 6200 Hz) de acordo com [vocalLevel] (0.0 = remoção máxima,
 *   0.15 = voz-guia suave estilo Apple Music Sing, 1.0 = voz original 100%).
 */
@OptIn(UnstableApi::class)
class VocalIsolationAudioProcessor : BaseAudioProcessor() {

    @Volatile
    var isKaraokeEnabled: Boolean = false

    /**
     * Nível de presença da voz original quando [isKaraokeEnabled] = true:
     * - `0.0f`: Remoção total da banda vocal central (100% instrumental)
     * - `0.15f`: Voz guia suave (padrão ideal para cantar junto)
     * - `1.0f`: Voz original sem atenuação
     */
    @Volatile
    var vocalLevel: Float = 0.12f

    // Coeficientes Biquad Low-Pass (< 160 Hz) para preservar bumbo/baixo no centro
    private var lpB0 = 1.0
    private var lpB1 = 0.0
    private var lpB2 = 0.0
    private var lpA1 = 0.0
    private var lpA2 = 0.0
    private var lpZ1 = 0.0
    private var lpZ2 = 0.0

    // Coeficientes Biquad High-Pass (> 6200 Hz) para preservar hi-hats/agudos no centro
    private var hpB0 = 1.0
    private var hpB1 = 0.0
    private var hpB2 = 0.0
    private var hpA1 = 0.0
    private var hpA2 = 0.0
    private var hpZ1 = 0.0
    private var hpZ2 = 0.0

    private var currentSampleRate = 44100

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        // Suporta fluxos estéreo PCM 16-bit ou PCM Float (padrão decodificado pelo Media3 para Opus/AAC/MP3)
        if (inputAudioFormat.channelCount != 2) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT &&
            inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT
        ) {
            return AudioProcessor.AudioFormat.NOT_SET
        }

        currentSampleRate = inputAudioFormat.sampleRate.coerceAtLeast(8000)
        recalculateBiquadCoefficients(currentSampleRate.toDouble())
        resetFilterState()
        return inputAudioFormat
    }

    private fun recalculateBiquadCoefficients(sampleRate: Double) {
        val q = 1.0 / sqrt(2.0) // Butterworth Q = 0.7071

        // 1. Low-Pass Filter em 280 Hz (preserva bumbo, contrabaixo e corpo da caixa no centro)
        val fcLow = 280.0.coerceAtMost(sampleRate * 0.45)
        val w0Low = 2.0 * PI * fcLow / sampleRate
        val cosW0Low = cos(w0Low)
        val sinW0Low = sin(w0Low)
        val alphaLow = sinW0Low / (2.0 * q)
        val a0Low = 1.0 + alphaLow
        lpB0 = ((1.0 - cosW0Low) * 0.5) / a0Low
        lpB1 = (1.0 - cosW0Low) / a0Low
        lpB2 = ((1.0 - cosW0Low) * 0.5) / a0Low
        lpA1 = (-2.0 * cosW0Low) / a0Low
        lpA2 = (1.0 - alphaLow) / a0Low

        // 2. High-Pass Filter em 3500 Hz (preserva definição, pratos, violões e brilho para não abafar)
        val fcHigh = 3500.0.coerceAtMost(sampleRate * 0.45)
        val w0High = 2.0 * PI * fcHigh / sampleRate
        val cosW0High = cos(w0High)
        val sinW0High = sin(w0High)
        val alphaHigh = sinW0High / (2.0 * q)
        val a0High = 1.0 + alphaHigh
        hpB0 = ((1.0 + cosW0High) * 0.5) / a0High
        hpB1 = -(1.0 + cosW0High) / a0High
        hpB2 = ((1.0 + cosW0High) * 0.5) / a0High
        hpA1 = (-2.0 * cosW0High) / a0High
        hpA2 = (1.0 - alphaHigh) / a0High
    }

    private fun resetFilterState() {
        lpZ1 = 0.0
        lpZ2 = 0.0
        hpZ1 = 0.0
        hpZ2 = 0.0
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val outputBuffer = replaceOutputBuffer(remaining)
        outputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)

        val enabled = isKaraokeEnabled
        val vLevel = vocalLevel.coerceIn(0f, 1f).toDouble()

        // Bypass ultra-rápido quando o modo Karaokê está desativado
        if (!enabled || vLevel >= 0.995) {
            outputBuffer.put(inputBuffer)
            outputBuffer.flip()
            return
        }

        val sideBoost = 1.0 + (1.0 - vLevel) * 0.16

        if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT) {
            // Cada frame estéreo 16-bit = 4 bytes (2 bytes Left + 2 bytes Right)
            val frameCount = remaining / 4
            for (i in 0 until frameCount) {
                val leftShort = inputBuffer.short
                val rightShort = inputBuffer.short

                val left = leftShort / 32768.0
                val right = rightShort / 32768.0

                val (outL, outR) = processStereoFrame(left, right, vLevel, sideBoost)

                val clampedL = (outL * 32767.0).coerceIn(-32768.0, 32767.0).toInt().toShort()
                val clampedR = (outR * 32767.0).coerceIn(-32768.0, 32767.0).toInt().toShort()

                outputBuffer.putShort(clampedL)
                outputBuffer.putShort(clampedR)
            }
            // Copia bytes residuais caso haja desalinhamento improvável
            while (inputBuffer.hasRemaining()) {
                outputBuffer.put(inputBuffer.get())
            }
        } else if (inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT) {
            // Cada frame estéreo Float = 8 bytes (4 bytes Left + 4 bytes Right)
            val frameCount = remaining / 8
            for (i in 0 until frameCount) {
                val left = inputBuffer.float.toDouble()
                val right = inputBuffer.float.toDouble()

                val (outL, outR) = processStereoFrame(left, right, vLevel, sideBoost)

                outputBuffer.putFloat(outL.toFloat().coerceIn(-1f, 1f))
                outputBuffer.putFloat(outR.toFloat().coerceIn(-1f, 1f))
            }
            while (inputBuffer.hasRemaining()) {
                outputBuffer.put(inputBuffer.get())
            }
        } else {
            outputBuffer.put(inputBuffer)
        }

        outputBuffer.flip()
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun processStereoFrame(
        left: Double,
        right: Double,
        vLevel: Double,
        sideBoost: Double
    ): Pair<Double, Double> {
        val mid = 0.5 * (left + right)
        val side = 0.5 * (left - right) * sideBoost

        // Biquad Direct Form II Transposed para Low-Pass (Grave < 280 Hz)
        val midBass = lpB0 * mid + lpZ1
        lpZ1 = lpB1 * mid - lpA1 * midBass + lpZ2
        lpZ2 = lpB2 * mid - lpA2 * midBass

        // Biquad Direct Form II Transposed para High-Pass (Brilho/Presença > 3500 Hz)
        val midTreble = hpB0 * mid + hpZ1
        hpZ1 = hpB1 * mid - hpA1 * midTreble + hpZ2
        hpZ2 = hpB2 * mid - hpA2 * midTreble

        // Isola estritamente a banda vocal central (280 Hz - 3500 Hz) e compensa presença de graves/agudos
        val midVocal = mid - midBass - midTreble
        val clarityGain = 1.0 + (1.0 - vLevel) * 0.18
        val midFiltered = (midBass * 1.08) + (midTreble * clarityGain) + (midVocal * vLevel)

        val rawOutL = midFiltered + side
        val rawOutR = midFiltered - side

        return softClip(rawOutL) to softClip(rawOutR)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun softClip(sample: Double): Double {
        return when {
            sample > 0.95 -> 0.95 + 0.05 * ((sample - 0.95) / (1.0 + (sample - 0.95) * 20.0))
            sample < -0.95 -> -0.95 + 0.05 * ((sample + 0.95) / (1.0 - (sample + 0.95) * 20.0))
            else -> sample
        }
    }

    override fun onFlush() {
        resetFilterState()
    }

    override fun onReset() {
        resetFilterState()
    }
}
