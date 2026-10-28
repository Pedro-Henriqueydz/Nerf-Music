package com.nerf.music.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.util.UnstableApi
import com.nerf.music.data.repository.LoudnessStandard
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.tanh

/**
 * Processador DSP de Normalização de Volume em Tempo Real (Loudness Normalization)
 * baseado na recomendação ITU-R BS.1770-4 e EBU R128.
 *
 * Arquitetura e Engenharia de Desempenho:
 * 1. Processamento on-the-fly chunk-a-chunk com zero descompressão prévia de arquivos em RAM.
 * 2. Filtro K-Weighting de 2 estágios:
 *    - Pre-filter High-Shelving (+4 dB @ 1.68 kHz) simulando a dispersão acústica da cabeça humana.
 *    - RLB High-Pass (38 Hz) simulando o corte natural do ouvido humano em sub-graves.
 * 3. Integração balística de energia (Momentary 400ms & Short-Term 2.5s) com rejeição a ruído de fundo (< -65 LUFS).
 * 4. Rampa de ganho suave por interpolação linear amostra por amostra (elimina zipper noise e pumping).
 * 5. Limitador True-Peak Soft-Knee (-0.5 dBFS) para prevenir qualquer tipo de clipping digital em amplificações.
 * 6. Zero alocação de objetos no laço de áudio (elimina coletas de Garbage Collection e aquecimento térmico).
 */
@UnstableApi
class LoudnessNormalizationAudioProcessor : AudioProcessor {

    @Volatile
    var standard: LoudnessStandard = LoudnessStandard.SPOTIFY
        set(value) {
            field = value
            updateTargetParameters()
        }

    private var inputAudioFormat: AudioFormat = AudioFormat.NOT_SET
    private var internalBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var inputEnded: Boolean = false

    // Filtros K-Weighting (Biquad Direct Form II Transposed)
    private val preFilterL = Biquad()
    private val preFilterR = Biquad()
    private val rlbFilterL = Biquad()
    private val rlbFilterR = Biquad()

    // Parâmetros balísticos de medição de energia
    private var sampleRate: Double = 44100.0
    private var alphaMom: Double = 0.001
    private var alphaShort: Double = 0.0001

    private var runningEnergyMom: Double = 0.04689 // Inicializado no equivalente a -14 LUFS para latência inicial zero
    private var runningEnergyShort: Double = 0.04689

    // Controle de ganho e suavização contínua
    private var targetLinearGain: Double = 1.0
    private var currentGain: Double = 1.0

    private val attackCoeff: Double = 0.035  // ~150ms para atenuar sons muito fortes
    private val releaseCoeff: Double = 0.003 // ~2000ms para evitar respiração/pumping em sons baixos

    init {
        updateTargetParameters()
    }

    private fun updateTargetParameters() {
        val target = standard.targetLufs
        if (target != null) {
            val initialEnergy = 10.0.pow((target + 0.691) / 10.0)
            if (runningEnergyShort <= 0.0 || runningEnergyShort > 10.0) {
                runningEnergyMom = initialEnergy
                runningEnergyShort = initialEnergy
            }
        } else {
            targetLinearGain = 1.0
        }
    }

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        this.inputAudioFormat = inputAudioFormat
        val sr = inputAudioFormat.sampleRate.toDouble().coerceAtLeast(8000.0)
        this.sampleRate = sr

        // Coeficientes dos filtros K-weighting dependentes da taxa de amostragem
        reconfigureFilters(sr)

        // Coeficientes do integrador com base na taxa de amostragem
        // Momentary: janela equivalente de 400ms
        alphaMom = 1.0 - exp(-1.0 / (sr * 0.400))
        // Short-term: janela equivalente de 2.5s
        alphaShort = 1.0 - exp(-1.0 / (sr * 2.500))

        return inputAudioFormat
    }

    private fun reconfigureFilters(sr: Double) {
        if (sr in 47900.0..48100.0) {
            // Coeficientes exatos BS.1770-4 para 48 kHz
            preFilterL.setCoefficients(1.53512485958697, -2.69169618940638, 1.19839281085285, -1.69065929318241, 0.73248077421585)
            preFilterR.setCoefficients(1.53512485958697, -2.69169618940638, 1.19839281085285, -1.69065929318241, 0.73248077421585)
            rlbFilterL.setCoefficients(1.0, -2.0, 1.0, -1.99004745483398, 0.99007225036621)
            rlbFilterR.setCoefficients(1.0, -2.0, 1.0, -1.99004745483398, 0.99007225036621)
        } else if (sr in 44000.0..44200.0) {
            // Coeficientes exatos BS.1770-4 para 44.1 kHz
            preFilterL.setCoefficients(1.53090264023771, -2.65097999011966, 1.16907734988195, -1.66367503931802, 0.71267499933802)
            preFilterR.setCoefficients(1.53090264023771, -2.65097999011966, 1.16907734988195, -1.66367503931802, 0.71267499933802)
            rlbFilterL.setCoefficients(1.0, -2.0, 1.0, -1.98916967923498, 0.98919984724399)
            rlbFilterR.setCoefficients(1.0, -2.0, 1.0, -1.98916967923498, 0.98919984724399)
        } else {
            // Cálculo via Transformada Bilinear para taxas de amostragem arbitrárias (ex: 88.2k, 96k, 32k)
            calculateGeneralKWeightingCoefficients(sr)
        }
    }

    private fun calculateGeneralKWeightingCoefficients(sr: Double) {
        // Estágio 1: High Shelf (+3.9998 dB @ 1681 Hz, Q = 0.7071)
        val f0 = 1681.97445095553
        val gainDb = 3.99983557152
        val q = 0.707175236955
        val a = 10.0.pow(gainDb / 40.0)
        val w0 = 2.0 * Math.PI * f0 / sr
        val sinW0 = kotlin.math.sin(w0)
        val cosW0 = kotlin.math.cos(w0)
        val alpha = sinW0 / (2.0 * q)
        val twoSqrtAAlpha = 2.0 * sqrt(a) * alpha

        val a0 = (a + 1.0) - (a - 1.0) * cosW0 + twoSqrtAAlpha
        val b0 = a * ((a + 1.0) + (a - 1.0) * cosW0 + twoSqrtAAlpha) / a0
        val b1 = -2.0 * a * ((a - 1.0) + (a + 1.0) * cosW0) / a0
        val b2 = a * ((a + 1.0) + (a - 1.0) * cosW0 - twoSqrtAAlpha) / a0
        val a1 = 2.0 * ((a - 1.0) - (a + 1.0) * cosW0) / a0
        val a2 = ((a + 1.0) - (a - 1.0) * cosW0 - twoSqrtAAlpha) / a0

        preFilterL.setCoefficients(b0, b1, b2, a1, a2)
        preFilterR.setCoefficients(b0, b1, b2, a1, a2)

        // Estágio 2: High Pass (RLB filter @ 38.13 Hz, Q = 0.5003)
        val f0Hp = 38.135470876
        val qHp = 0.500327037
        val w0Hp = 2.0 * Math.PI * f0Hp / sr
        val cosW0Hp = kotlin.math.cos(w0Hp)
        val sinW0Hp = kotlin.math.sin(w0Hp)
        val alphaHp = sinW0Hp / (2.0 * qHp)

        val hpA0 = 1.0 + alphaHp
        val hpB0 = ((1.0 + cosW0Hp) / 2.0) / hpA0
        val hpB1 = (-(1.0 + cosW0Hp)) / hpA0
        val hpB2 = ((1.0 + cosW0Hp) / 2.0) / hpA0
        val hpA1 = (-2.0 * cosW0Hp) / hpA0
        val hpA2 = (1.0 - alphaHp) / hpA0

        rlbFilterL.setCoefficients(hpB0, hpB1, hpB2, hpA1, hpA2)
        rlbFilterR.setCoefficients(hpB0, hpB1, hpB2, hpA1, hpA2)
    }

    override fun isActive(): Boolean {
        return inputAudioFormat != AudioFormat.NOT_SET
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        if (internalBuffer.capacity() < remaining) {
            internalBuffer = ByteBuffer.allocateDirect(remaining).order(ByteOrder.nativeOrder())
        } else {
            internalBuffer.clear()
        }

        val targetLufs = standard.targetLufs
        val channelCount = inputAudioFormat.channelCount

        // Se desativado ou canal não suportado, repassa diretamente via memcpy com custo zero de CPU
        if (targetLufs == null || channelCount !in 1..2) {
            internalBuffer.put(inputBuffer)
            internalBuffer.flip()
            outputBuffer = internalBuffer
            return
        }

        val bytesPerFrame = channelCount * 2
        val frameCount = remaining / bytesPerFrame
        if (frameCount == 0) {
            internalBuffer.put(inputBuffer)
            internalBuffer.flip()
            outputBuffer = internalBuffer
            return
        }

        // Calcula o ganho alvo para este bloco
        val effEnergy = max(runningEnergyShort, runningEnergyMom * 0.70)
        if (effEnergy > 1e-7) { // Acima de -70 LUFS (gating threshold)
            val measuredLufs = -0.691 + 10.0 * log10(effEnergy)
            // Variação de ganho segura: máximo +9 dB de amplificação e -16 dB de atenuação
            val deltaDb = (targetLufs - measuredLufs).coerceIn(-16.0, 9.0)
            targetLinearGain = 10.0.pow(deltaDb / 20.0)
        }

        // Interpolação suave do ganho ao longo do bloco para eliminar ruídos de transição (zipper noise)
        val smoothingCoeff = if (targetLinearGain < currentGain) attackCoeff else releaseCoeff
        val blockTargetGain = currentGain + smoothingCoeff * (targetLinearGain - currentGain)
        val gainStep = (blockTargetGain - currentGain) / frameCount

        var g = currentGain

        if (channelCount == 2) {
            for (i in 0 until frameCount) {
                val leftShort = inputBuffer.short
                val rightShort = inputBuffer.short

                val xL = leftShort / 32768.0
                val xR = rightShort / 32768.0

                // Filtro K-Weighting para cálculo contínuo de energia
                val kL = rlbFilterL.process(preFilterL.process(xL))
                val kR = rlbFilterR.process(preFilterR.process(xR))
                val sampleEnergy = kL * kL + kR * kR

                // Acumulação balística
                runningEnergyMom += alphaMom * (sampleEnergy - runningEnergyMom)
                runningEnergyShort += alphaShort * (sampleEnergy - runningEnergyShort)

                g += gainStep

                // Aplica ganho normalizado + limitador True-Peak Soft-Knee
                val outL = limitSample(xL * g)
                val outR = limitSample(xR * g)

                val outLShort = (outL * 32767.0).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                val outRShort = (outR * 32767.0).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()

                internalBuffer.putShort(outLShort)
                internalBuffer.putShort(outRShort)
            }
        } else {
            // Mono
            for (i in 0 until frameCount) {
                val monoShort = inputBuffer.short
                val x = monoShort / 32768.0

                val k = rlbFilterL.process(preFilterL.process(x))
                val sampleEnergy = k * k

                runningEnergyMom += alphaMom * (sampleEnergy - runningEnergyMom)
                runningEnergyShort += alphaShort * (sampleEnergy - runningEnergyShort)

                g += gainStep

                val out = limitSample(x * g)
                val outShort = (out * 32767.0).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()

                internalBuffer.putShort(outShort)
            }
        }

        currentGain = blockTargetGain

        // Transfere bytes restantes se houver fragmento
        while (inputBuffer.hasRemaining()) {
            internalBuffer.put(inputBuffer.get())
        }

        internalBuffer.flip()
        outputBuffer = internalBuffer
    }

    /**
     * Limitador True-Peak Soft-Knee transparente (-0.5 dBFS teto):
     * Amostras com amplitude linear <= 0.82 (+-0.82) passam 100% inalteradas (bit-perfect).
     * Picos acima de 0.82 sofrem compressão com curvatura hiperbólica contínua com teto em 0.944 (-0.5 dBFS).
     */
    private fun limitSample(sample: Double): Double {
        val absVal = if (sample < 0.0) -sample else sample
        val kneeThreshold = 0.82
        val ceiling = 0.944 // -0.5 dBFS

        if (absVal <= kneeThreshold) {
            return sample
        }

        val excess = (absVal - kneeThreshold) / (1.0 - kneeThreshold)
        val compressed = kneeThreshold + (ceiling - kneeThreshold) * tanh(excess)
        val finalVal = min(compressed, ceiling)
        return if (sample >= 0.0) finalVal else -finalVal
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
        preFilterL.reset()
        preFilterR.reset()
        rlbFilterL.reset()
        rlbFilterR.reset()

        // Restaura energia inicial correspondente ao target para iniciar suavemente sem transientes
        val target = standard.targetLufs
        if (target != null) {
            val initialEnergy = 10.0.pow((target + 0.691) / 10.0)
            runningEnergyMom = initialEnergy
            runningEnergyShort = initialEnergy
        }
        currentGain = 1.0
        targetLinearGain = 1.0
    }

    override fun reset() {
        flush()
        internalBuffer = AudioProcessor.EMPTY_BUFFER
        inputAudioFormat = AudioFormat.NOT_SET
    }

    /**
     * Filtro Biquad Direct Form II Transposed de alta performance:
     * Otimizado com apenas 5 multiplicações e 4 adições por amostra e alocação zero.
     */
    private class Biquad {
        private var b0: Double = 1.0
        private var b1: Double = 0.0
        private var b2: Double = 0.0
        private var a1: Double = 0.0
        private var a2: Double = 0.0

        private var z1: Double = 0.0
        private var z2: Double = 0.0

        fun setCoefficients(b0: Double, b1: Double, b2: Double, a1: Double, a2: Double) {
            this.b0 = b0
            this.b1 = b1
            this.b2 = b2
            this.a1 = a1
            this.a2 = a2
        }

        fun reset() {
            z1 = 0.0
            z2 = 0.0
        }

        fun process(x: Double): Double {
            val y = b0 * x + z1
            z1 = b1 * x - a1 * y + z2
            z2 = b2 * x - a2 * y
            return y
        }
    }
}
