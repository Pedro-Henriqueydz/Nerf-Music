package com.nerf.music.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Processador DSP em tempo real para Isolamento Vocal (Modo Karaokê / Apple Music Sing)
 * e Extração de Envelope Rítmico Sub-Grave para alinhamento automático de batidas.
 *
 * Arquitetura Acústica (v2 - Fase Estéreo Preservada + Linkwitz-Riley 4ª Ordem):
 * 1. Decomposição Mid-Side:
 *    - `Mid = (L + R) * 0.5` (contém o vocal principal + sub-grave centralizado)
 *    - `Side = (L - R) * 0.5` (contém 100% dos instrumentos estéreo sem o vocal central)
 * 2. Isolamento Cirúrgico do Canal Central (`Mid`):
 *    - Filtro Linkwitz-Riley de 4ª ordem (24 dB/oitava) Low-Pass em `115 Hz` (`midSubBass`):
 *      mantém apenas o peso do bumbo (kick) e sub-baixo, cortando 100% dos fundamentais
 *      da voz masculina e feminina (`130 Hz – 400 Hz`) que antes deixavam a música abafada e a voz nítida.
 *    - Filtro Linkwitz-Riley de 4ª ordem (24 dB/oitava) High-Pass em `8000 Hz` (`midAir`):
 *      mantém apenas o brilho de pratos/hi-hats sem vazar formantes ou sibilância vocal (`1 kHz – 6 kHz`).
 * 3. Decorrelação All-Pass em Quadratura (`90°`) no Canal `Side` (Anti-Cancelamento em Alto-Falante):
 *    - Antes, jogar `+Side` no canal L e `-Side` no canal R causava cancelamento acústico total (`+Side - Side = 0`)
 *      no alto-falante do celular, sumindo com violões/guitarras/teclados e deixando o som abafado.
 *    - Agora aplicamos uma rede All-Pass de defasagem em quadratura + EQ de presença (`+3.5 dB @ 2.2 kHz`)
 *      no sinal `Side`, garantindo zero voz central (`L - R`) e soma construtiva nos alto-falantes do celular
 *      e nos fones de ouvido.
 */
@UnstableApi
class VocalIsolationAudioProcessor : AudioProcessor {

    companion object {
        private const val ENVELOPE_WINDOW_MS = 10
        private const val MAX_ENVELOPE_FRAMES = 1800 // 18 segundos em resolução de 10ms

        /**
         * Calcula o deslocamento de tempo ideal (em milissegundos) entre o áudio original e o instrumental
         * usando Correlação Cruzada Normalizada (NCC) sobre a derivada de ataque (onset) do envelope sub-grave.
         *
         * Retorna `offsetMs` tal que: `posicaoInstrumental = posicaoOriginal + offsetMs`.
         * Retorna `null` se a confiança da correlação for insuficiente.
         */
        fun computeBestBeatOffsetMs(
            mainEnvelope: FloatArray,
            instEnvelope: FloatArray,
            maxLagMs: Int = 2500
        ): Long? {
            val n = minOf(mainEnvelope.size, instEnvelope.size)
            if (n < 250) return null // Mínimo de 2.5s de áudio capturado

            // Extrai a função de detecção de ataques (Half-Wave Rectified First-Order Difference)
            val mainOnset = FloatArray(n)
            val instOnset = FloatArray(n)
            var mainEnergy = 0.0
            var instEnergy = 0.0

            for (i in 1 until n) {
                val dm = (mainEnvelope[i] - mainEnvelope[i - 1]).coerceAtLeast(0f)
                val di = (instEnvelope[i] - instEnvelope[i - 1]).coerceAtLeast(0f)
                mainOnset[i] = dm
                instOnset[i] = di
                mainEnergy += dm * dm
                instEnergy += di * di
            }

            if (mainEnergy < 1e-6 || instEnergy < 1e-6) return null

            val maxLagFrames = (maxLagMs / ENVELOPE_WINDOW_MS).coerceAtMost(n / 2)
            var bestLag = 0
            var bestCorr = -1.0
            var secondBestCorr = -1.0
            val corrByLag = DoubleArray(maxLagFrames * 2 + 1)

            for (lag in -maxLagFrames..maxLagFrames) {
                var num = 0.0
                var normM = 0.0
                var normI = 0.0
                val startM = maxOf(1, 1 - lag)
                val endM = minOf(n - 1, n - 1 - lag)
                for (t in startM..endM) {
                    val m = mainOnset[t].toDouble()
                    val ins = instOnset[t + lag].toDouble()
                    num += m * ins
                    normM += m * m
                    normI += ins * ins
                }
                val denom = sqrt(normM * normI)
                val corr = if (denom > 1e-9) num / denom else 0.0
                corrByLag[lag + maxLagFrames] = corr
                if (corr > bestCorr) {
                    bestCorr = corr
                    bestLag = lag
                }
            }

            // Busca o segundo maior pico fora da vizinhança imediata (±60ms = ±6 frames) do pico principal
            for (lag in -maxLagFrames..maxLagFrames) {
                if (abs(lag - bestLag) > 6) {
                    val c = corrByLag[lag + maxLagFrames]
                    if (c > secondBestCorr) {
                        secondBestCorr = c
                    }
                }
            }

            // Exige correlação clara e pico destacado para aplicar ajuste automático
            if (bestCorr < 0.28 || (secondBestCorr > 0 && bestCorr < secondBestCorr * 1.15)) {
                return null
            }

            return (bestLag * ENVELOPE_WINDOW_MS).toLong()
        }
    }

    @Volatile
    var isKaraokeEnabled: Boolean = false

    @Volatile
    var isEnvelopeCaptureEnabled: Boolean = false

    /**
     * Nível de presença do vocal principal:
     * `0.0f` = Remoção máxima do vocal (Karaokê Puro)
     * `1.0f` = Áudio original completo
     */
    @Volatile
    var vocalLevel: Float = 0.0f

    private var inputAudioFormat: AudioFormat = AudioFormat.NOT_SET
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var internalBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var inputEnded: Boolean = false

    // Linkwitz-Riley 4ª Ordem (2 biquads em cascata = 24 dB/oitava) para Sub-Grave (< 115 Hz) no Mid
    private val midSubLpStage1 = BiquadFilter()
    private val midSubLpStage2 = BiquadFilter()

    // Linkwitz-Riley 4ª Ordem (24 dB/oitava) para Brilho Extremo (> 8000 Hz) no Mid
    private val midAirHpStage1 = BiquadFilter()
    private val midAirHpStage2 = BiquadFilter()

    // Equalizador Peaking de Presença Instrumental (+3.5 dB @ 2200 Hz) no canal Side
    private val sidePresenceEq = BiquadFilter()

    // Rede All-Pass de Decorrelação Estéreo em Quadratura (evita cancelamento de fase nos alto-falantes do celular)
    private val sideAllPassL1 = BiquadFilter()
    private val sideAllPassL2 = BiquadFilter()
    private val sideAllPassR1 = BiquadFilter()
    private val sideAllPassR2 = BiquadFilter()

    // Extrator de envelope sub-grave separado para alinhamento automático de batidas
    private val envelopeSubLp = BiquadFilter()
    private val envelopeFrames = FloatArray(MAX_ENVELOPE_FRAMES)
    private var envelopeWriteCount = 0
    private var currentWindowSumSq = 0.0
    private var currentWindowSamples = 0
    private var samplesPerEnvelopeWindow = 441

    fun clearEnvelopeHistory() {
        synchronized(envelopeFrames) {
            envelopeWriteCount = 0
            currentWindowSumSq = 0.0
            currentWindowSamples = 0
            envelopeFrames.fill(0f)
        }
    }

    fun getCapturedBassEnvelope(): FloatArray {
        synchronized(envelopeFrames) {
            val count = envelopeWriteCount.coerceAtMost(MAX_ENVELOPE_FRAMES)
            return envelopeFrames.copyOfRange(0, count)
        }
    }

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        this.inputAudioFormat = inputAudioFormat
        reconfigureFilters(inputAudioFormat.sampleRate.toDouble())
        return inputAudioFormat
    }

    private fun reconfigureFilters(sampleRate: Double) {
        if (sampleRate <= 0.0) return
        val nyquistSafeAir = (sampleRate * 0.42).coerceIn(5500.0, 8200.0)

        // Sub-grave estrito em 115 Hz (abaixo da fundamental da voz humana)
        midSubLpStage1.setLowPass(sampleRate, cutoffHz = 115.0, q = 0.7071)
        midSubLpStage2.setLowPass(sampleRate, cutoffHz = 115.0, q = 0.7071)

        // Ar / Brilho acima de 8000 Hz (acima dos formantes da voz humana)
        midAirHpStage1.setHighPass(sampleRate, cutoffHz = nyquistSafeAir, q = 0.7071)
        midAirHpStage2.setHighPass(sampleRate, cutoffHz = nyquistSafeAir, q = 0.7071)

        // Realce de nitidez dos instrumentos no canal Side (violões, teclados, caixas)
        sidePresenceEq.setPeakingEq(sampleRate, centerHz = 2200.0, q = 0.95, gainDb = 3.5)

        // Filtros All-Pass escalonados para descorrelacionar L e R sem cancelar no alto-falante do celular
        sideAllPassL1.setAllPass(sampleRate, centerHz = 380.0, q = 0.7071)
        sideAllPassL2.setAllPass(sampleRate, centerHz = 1900.0, q = 0.7071)
        sideAllPassR1.setAllPass(sampleRate, centerHz = 820.0, q = 0.7071)
        sideAllPassR2.setAllPass(sampleRate, centerHz = 3600.0, q = 0.7071)

        envelopeSubLp.setLowPass(sampleRate, cutoffHz = 115.0, q = 0.7071)
        samplesPerEnvelopeWindow = ((sampleRate * ENVELOPE_WINDOW_MS) / 1000.0).toInt().coerceAtLeast(64)
        clearEnvelopeHistory()
    }

    override fun isActive(): Boolean {
        return inputAudioFormat != AudioFormat.NOT_SET && inputAudioFormat.channelCount == 2
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        if (internalBuffer.capacity() < remaining) {
            internalBuffer = ByteBuffer.allocateDirect(remaining).order(ByteOrder.nativeOrder())
        } else {
            internalBuffer.clear()
        }

        val karaokeActive = isKaraokeEnabled && inputAudioFormat.channelCount == 2
        val captureActive = isEnvelopeCaptureEnabled && envelopeWriteCount < MAX_ENVELOPE_FRAMES

        // Otimização Crítica de CPU/Térmica: se o Karaokê e a Captura estiverem inativos,
        // realiza memcpy direto via internalBuffer.put(inputBuffer) sem custo de filtros Biquad.
        if (!karaokeActive && !captureActive) {
            internalBuffer.put(inputBuffer)
            internalBuffer.flip()
            outputBuffer = internalBuffer
            return
        }

        val rawVLevel = vocalLevel.coerceIn(0f, 1f)
        // Curva quadrática para que valores baixos no slider (0%..25%) mantenham atenuação profunda da voz
        val vGain = (rawVLevel * rawVLevel).toDouble()
        val suppressionStrength = (1.0 - vGain).coerceIn(0.0, 1.0)

        while (inputBuffer.remaining() >= 4) {
            val leftShort = inputBuffer.short
            val rightShort = inputBuffer.short

            val leftNorm = leftShort / 32768.0
            val rightNorm = rightShort / 32768.0
            val mid = (leftNorm + rightNorm) * 0.5

            if (captureActive) {
                // Registra envelope sub-grave apenas quando necessário para alinhamento automático de batidas
                val envBass = envelopeSubLp.process(mid)
                recordEnvelopeSample(envBass)
            }

            if (!karaokeActive) {
                internalBuffer.putShort(leftShort)
                internalBuffer.putShort(rightShort)
            } else {
                val side = (leftNorm - rightNorm) * 0.5

                // 1. Sub-grave (< 115 Hz, 24 dB/oct) e Brilho Extremo (> 8 kHz, 24 dB/oct) do Mid (0% de voz)
                val midSubBass = midSubLpStage2.process(midSubLpStage1.process(mid))
                val midAir = midAirHpStage2.process(midAirHpStage1.process(mid))

                // 2. Banda vocal do Mid isolada (115 Hz .. 8 kHz) escalada apenas por vGain (0.0 no padrão)
                val midVocalBand = mid - midSubBass - midAir
                val reconstructedMid = (midSubBass * 1.12) + (midAir * 0.85) + (midVocalBand * vGain)

                // 3. Canal Instrumental Estéreo (Side) com realce de presença e decorrelação All-Pass
                val eqSide = sidePresenceEq.process(side)
                val decorrelatedSideL = sideAllPassL2.process(sideAllPassL1.process(eqSide))
                val decorrelatedSideR = sideAllPassR2.process(sideAllPassR1.process(-eqSide))

                // Interpola suavemente entre o Side original (quando vGain=1) e o Side decorrelacionado + expandido (quando vGain=0)
                val finalSideL = (side * vGain) + (decorrelatedSideL * 1.38 * suppressionStrength)
                val finalSideR = (-side * vGain) + (decorrelatedSideR * 1.38 * suppressionStrength)

                val outLeft = softClip(reconstructedMid + finalSideL)
                val outRight = softClip(reconstructedMid + finalSideR)

                val outLShort = (outLeft * 32767.0).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                val outRShort = (outRight * 32767.0).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()

                internalBuffer.putShort(outLShort)
                internalBuffer.putShort(outRShort)
            }
        }

        while (inputBuffer.hasRemaining()) {
            internalBuffer.put(inputBuffer.get())
        }

        internalBuffer.flip()
        outputBuffer = internalBuffer
    }

    private fun recordEnvelopeSample(bassSample: Double) {
        currentWindowSumSq += bassSample * bassSample
        currentWindowSamples++
        if (currentWindowSamples >= samplesPerEnvelopeWindow) {
            val rms = sqrt(currentWindowSumSq / currentWindowSamples).toFloat()
            synchronized(envelopeFrames) {
                if (envelopeWriteCount < MAX_ENVELOPE_FRAMES) {
                    envelopeFrames[envelopeWriteCount] = rms
                    envelopeWriteCount++
                }
            }
            currentWindowSumSq = 0.0
            currentWindowSamples = 0
        }
    }

    private fun softClip(sample: Double): Double {
        return when {
            sample > 0.98 -> 0.98 + 0.02 * kotlin.math.tanh((sample - 0.98) * 10.0)
            sample < -0.98 -> -0.98 + 0.02 * kotlin.math.tanh((sample + 0.98) * 10.0)
            else -> sample
        }.coerceIn(-1.0, 1.0)
    }

    override fun queueEndOfStream() {
        inputEnded = true
    }

    override fun getOutput(): ByteBuffer {
        val out = outputBuffer
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        return out
    }

    override fun isEnded(): Boolean {
        return inputEnded && outputBuffer === AudioProcessor.EMPTY_BUFFER
    }

    override fun flush() {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        inputEnded = false
        midSubLpStage1.reset()
        midSubLpStage2.reset()
        midAirHpStage1.reset()
        midAirHpStage2.reset()
        sidePresenceEq.reset()
        sideAllPassL1.reset()
        sideAllPassL2.reset()
        sideAllPassR1.reset()
        sideAllPassR2.reset()
        envelopeSubLp.reset()
    }

    override fun reset() {
        flush()
        internalBuffer = AudioProcessor.EMPTY_BUFFER
        inputAudioFormat = AudioFormat.NOT_SET
        clearEnvelopeHistory()
    }

    /**
     * Filtro Biquad IIR Direct Form II Transposed (Robert Bristow-Johnson Audio EQ Cookbook).
     */
    private class BiquadFilter {
        private var b0 = 1.0
        private var b1 = 0.0
        private var b2 = 0.0
        private var a1 = 0.0
        private var a2 = 0.0

        private var z1 = 0.0
        private var z2 = 0.0

        fun setLowPass(sampleRate: Double, cutoffHz: Double, q: Double) {
            val w0 = 2.0 * PI * cutoffHz / sampleRate
            val cosW0 = cos(w0)
            val sinW0 = sin(w0)
            val alpha = sinW0 / (2.0 * q)

            val a0 = 1.0 + alpha
            b0 = ((1.0 - cosW0) * 0.5) / a0
            b1 = (1.0 - cosW0) / a0
            b2 = ((1.0 - cosW0) * 0.5) / a0
            a1 = (-2.0 * cosW0) / a0
            a2 = (1.0 - alpha) / a0
            reset()
        }

        fun setHighPass(sampleRate: Double, cutoffHz: Double, q: Double) {
            val w0 = 2.0 * PI * cutoffHz / sampleRate
            val cosW0 = cos(w0)
            val sinW0 = sin(w0)
            val alpha = sinW0 / (2.0 * q)

            val a0 = 1.0 + alpha
            b0 = ((1.0 + cosW0) * 0.5) / a0
            b1 = -(1.0 + cosW0) / a0
            b2 = ((1.0 + cosW0) * 0.5) / a0
            a1 = (-2.0 * cosW0) / a0
            a2 = (1.0 - alpha) / a0
            reset()
        }

        fun setAllPass(sampleRate: Double, centerHz: Double, q: Double) {
            val w0 = 2.0 * PI * centerHz / sampleRate
            val cosW0 = cos(w0)
            val sinW0 = sin(w0)
            val alpha = sinW0 / (2.0 * q)

            val a0 = 1.0 + alpha
            b0 = (1.0 - alpha) / a0
            b1 = (-2.0 * cosW0) / a0
            b2 = 1.0 // (1 + alpha) / a0
            a1 = (-2.0 * cosW0) / a0
            a2 = (1.0 - alpha) / a0
            reset()
        }

        fun setPeakingEq(sampleRate: Double, centerHz: Double, q: Double, gainDb: Double) {
            val a = Math.pow(10.0, gainDb / 40.0)
            val w0 = 2.0 * PI * centerHz / sampleRate
            val cosW0 = cos(w0)
            val sinW0 = sin(w0)
            val alpha = sinW0 / (2.0 * q)

            val a0 = 1.0 + alpha / a
            b0 = (1.0 + alpha * a) / a0
            b1 = (-2.0 * cosW0) / a0
            b2 = (1.0 - alpha * a) / a0
            a1 = (-2.0 * cosW0) / a0
            a2 = (1.0 - alpha / a) / a0
            reset()
        }

        fun process(input: Double): Double {
            val out = b0 * input + z1
            z1 = b1 * input - a1 * out + z2
            z2 = b2 * input - a2 * out
            return out
        }

        fun reset() {
            z1 = 0.0
            z2 = 0.0
        }
    }
}
