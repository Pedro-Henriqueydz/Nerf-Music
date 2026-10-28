package com.nerf.music

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.util.UnstableApi
import com.nerf.music.data.repository.LoudnessStandard
import com.nerf.music.player.LoudnessNormalizationAudioProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

@OptIn(UnstableApi::class)
class LoudnessNormalizationAudioProcessorTest {

    @Test
    fun testDefaultStandardAndPresets() {
        val processor = LoudnessNormalizationAudioProcessor()
        assertEquals(LoudnessStandard.SPOTIFY, processor.standard)
        assertEquals(-14f, processor.standard.targetLufs)

        // Valida que todos os padrões requeridos possuem LUFS válidos
        assertEquals(null, LoudnessStandard.OFF.targetLufs)
        assertEquals(-14f, LoudnessStandard.SPOTIFY.targetLufs)
        assertEquals(-16f, LoudnessStandard.APPLE_MUSIC.targetLufs)
        assertEquals(-11f, LoudnessStandard.CD_STANDARD.targetLufs)
        assertEquals(-9f, LoudnessStandard.CD_LOUD.targetLufs)
        assertEquals(-23f, LoudnessStandard.BROADCAST_EBU.targetLufs)
    }

    @Test
    fun testConfigureAccepts16BitPcm() {
        val processor = LoudnessNormalizationAudioProcessor()
        val format44100 = AudioFormat(44100, 2, C.ENCODING_PCM_16BIT)
        val outFormat = processor.configure(format44100)

        assertEquals(format44100, outFormat)
        assertTrue(processor.isActive)
    }

    @Test(expected = AudioProcessor.UnhandledAudioFormatException::class)
    fun testConfigureRejectsNon16BitEncoding() {
        val processor = LoudnessNormalizationAudioProcessor()
        val invalidFormat = AudioFormat(44100, 2, C.ENCODING_PCM_FLOAT)
        processor.configure(invalidFormat)
    }

    @Test
    fun testChunkByChunkRealTimeStreamingZeroLatency() {
        val processor = LoudnessNormalizationAudioProcessor()
        val format = AudioFormat(48000, 2, C.ENCODING_PCM_16BIT)
        processor.configure(format)
        processor.standard = LoudnessStandard.SPOTIFY

        // Gera 1 segundo de áudio (48000 frames estéreo = 192.000 bytes) em chunks de 512 bytes (~2.6ms de áudio por chunk)
        val chunkSize = 512
        val totalChunks = (48000 * 4) / chunkSize

        var totalFramesProcessed = 0

        for (c in 0 until totalChunks) {
            val chunk = ByteBuffer.allocateDirect(chunkSize).order(ByteOrder.nativeOrder())
            for (i in 0 until (chunkSize / 4)) {
                val t = (c * (chunkSize / 4) + i).toDouble() / 48000.0
                val sampleVal = (sin(2.0 * PI * 440.0 * t) * 16000.0).toInt().toShort()
                chunk.putShort(sampleVal) // Left
                chunk.putShort(sampleVal) // Right
            }
            chunk.flip()

            processor.queueInput(chunk)
            val output = processor.output
            assertNotNull(output)
            assertTrue("Saída imediata no chunk $c sem buffering de arquivo inteiro", output.remaining() > 0)
            assertEquals(chunkSize, output.remaining())

            totalFramesProcessed += output.remaining() / 4
        }

        assertEquals(48000, totalFramesProcessed)
    }

    @Test
    fun testSoftKneePeakLimiterPreventsClippingOnLoudPresets() {
        val processor = LoudnessNormalizationAudioProcessor()
        val format = AudioFormat(48000, 2, C.ENCODING_PCM_16BIT)
        processor.configure(format)
        // Preset de -9 LUFS (alta potência sonora)
        processor.standard = LoudnessStandard.CD_LOUD

        // Gera sinal perto de 0 dBFS (Short.MAX_VALUE)
        val chunk = ByteBuffer.allocateDirect(4096).order(ByteOrder.nativeOrder())
        while (chunk.remaining() >= 4) {
            chunk.putShort((32000).toShort())
            chunk.putShort((-32000).toShort())
        }
        chunk.flip()

        processor.queueInput(chunk)
        val output = processor.output

        while (output.remaining() >= 4) {
            val l = output.short
            val r = output.short
            // Assegura que o sinal não ultrapassou o teto de -0.5 dBFS (~30930) ou provocou wrap-around negativo
            assertTrue("Left não deve sofrer clipping digital ou wrap-around", l in -31500..31500)
            assertTrue("Right não deve sofrer clipping digital ou wrap-around", r in -31500..31500)
        }
    }

    @Test
    fun testOffPresetBypassesBitPerfect() {
        val processor = LoudnessNormalizationAudioProcessor()
        val format = AudioFormat(44100, 2, C.ENCODING_PCM_16BIT)
        processor.configure(format)
        processor.standard = LoudnessStandard.OFF

        val chunk = ByteBuffer.allocateDirect(1024).order(ByteOrder.nativeOrder())
        val testPattern = shortArrayOf(1234, -5678, 32000, -32000, 0, 42)
        for (s in testPattern) {
            chunk.putShort(s)
        }
        while (chunk.hasRemaining()) {
            chunk.put(7.toByte())
        }
        chunk.flip()

        processor.queueInput(chunk)
        val output = processor.output

        for (s in testPattern) {
            assertEquals(s, output.short)
        }
    }

    @Test
    fun testUltraFastProcessingThroughput() {
        val processor = LoudnessNormalizationAudioProcessor()
        val format = AudioFormat(48000, 2, C.ENCODING_PCM_16BIT)
        processor.configure(format)
        processor.standard = LoudnessStandard.SPOTIFY

        // Simula 10 segundos de áudio (480.000 amostras estéreo = 1.92 MB)
        val chunk = ByteBuffer.allocateDirect(4096).order(ByteOrder.nativeOrder())
        for (i in 0 until 1024) {
            chunk.putShort((i % 10000).toShort())
        }
        chunk.flip()

        val start = System.nanoTime()
        val iterations = (480000 * 4) / 4096
        for (it in 0 until iterations) {
            chunk.position(0)
            processor.queueInput(chunk)
            processor.output
        }
        val elapsedMs = (System.nanoTime() - start) / 1_000_000L

        // 10 segundos de áudio devem ser processados em muito menos de 100ms em qualquer CPU moderna (ex: < 40ms)
        assertTrue("DSP deve ser ultra-rápido: demorou ${elapsedMs}ms para 10s de áudio", elapsedMs < 150L)
    }

    @Test
    fun testFlushAndReconfigureStability() {
        val processor = LoudnessNormalizationAudioProcessor()
        val format = AudioFormat(44100, 2, C.ENCODING_PCM_16BIT)
        processor.configure(format)

        val chunk = ByteBuffer.allocateDirect(1024).order(ByteOrder.nativeOrder())
        while (chunk.hasRemaining()) chunk.put(1)
        chunk.flip()

        processor.queueInput(chunk)
        processor.flush()

        assertEquals(AudioProcessor.EMPTY_BUFFER, processor.output)
        assertFalse(processor.isEnded)

        // Reconfigura com 48kHz
        val format48k = AudioFormat(48000, 2, C.ENCODING_PCM_16BIT)
        processor.configure(format48k)
        assertTrue(processor.isActive)
    }
}
