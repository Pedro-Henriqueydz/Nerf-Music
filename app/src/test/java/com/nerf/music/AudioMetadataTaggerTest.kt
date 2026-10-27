package com.nerf.music

import com.nerf.music.data.model.Track
import com.nerf.music.util.AudioMetadataTagger
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioMetadataTaggerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testMp3Id3v2EmbeddingWithArtworkAndMetadata() {
        val root = tempFolder.newFolder("mp3_test")
        val mp3File = File(root, "song.mp3")

        // Cria arquivo MP3 mínimo com frame de áudio dummy (suficiente para > 1024 bytes)
        val dummyAudio = ByteArray(2048) { 0xFF.toByte() }
        mp3File.writeBytes(dummyAudio)

        val artFile = File(root, "song.jpg")
        val dummyArtwork = ByteArray(1024) { (it % 256).toByte() }
        artFile.writeBytes(dummyArtwork)

        val track = Track(
            id = "test_mp3_track",
            title = "Test Song Title",
            artist = "Test Artist Name",
            album = "Test Album Name",
            durationSeconds = 180L
        )

        AudioMetadataTagger.embedMetadata(mp3File, track, artFile)

        assertTrue(mp3File.exists())
        val taggedBytes = mp3File.readBytes()

        // 1. Valida cabeçalho ID3v2.3
        assertEquals('I'.code.toByte(), taggedBytes[0])
        assertEquals('D'.code.toByte(), taggedBytes[1])
        assertEquals('3'.code.toByte(), taggedBytes[2])
        assertEquals(3.toByte(), taggedBytes[3]) // Version 2.3

        val taggedString = String(taggedBytes, Charsets.ISO_8859_1)

        // 2. Valida frames TIT2, TPE1, TALB e APIC
        assertTrue("Deve conter frame de título TIT2", taggedString.contains("TIT2"))
        assertTrue("Deve conter texto do título", taggedString.contains("Test Song Title"))
        assertTrue("Deve conter frame de artista TPE1", taggedString.contains("TPE1"))
        assertTrue("Deve conter nome do artista", taggedString.contains("Test Artist Name"))
        assertTrue("Deve conter frame de álbum TALB", taggedString.contains("TALB"))
        assertTrue("Deve conter nome do álbum", taggedString.contains("Test Album Name"))
        assertTrue("Deve conter frame de imagem APIC", taggedString.contains("APIC"))
        assertTrue("Deve conter MIME type de imagem", taggedString.contains("image/jpeg"))
    }

    @Test
    fun testM4aMetadataEmbeddingWithArtworkAndMoovBox() {
        val root = tempFolder.newFolder("m4a_test")
        val m4aFile = File(root, "song.m4a")

        // Constrói container MP4 mínimo: ftyp (24 bytes) + moov (32 bytes) + mdat (1024 bytes)
        val ftyp = ByteBuffer.allocate(24).apply {
            order(ByteOrder.BIG_ENDIAN)
            putInt(24)
            put("ftyp".toByteArray(Charsets.US_ASCII))
            put("M4A ".toByteArray(Charsets.US_ASCII))
            putInt(0)
            put("M4A ".toByteArray(Charsets.US_ASCII))
        }.array()

        val moovPayload = ByteBuffer.allocate(24).apply {
            order(ByteOrder.BIG_ENDIAN)
            putInt(24)
            put("mvhd".toByteArray(Charsets.US_ASCII))
            put(ByteArray(16))
        }.array()
        val moov = ByteBuffer.allocate(8 + moovPayload.size).apply {
            order(ByteOrder.BIG_ENDIAN)
            putInt(8 + moovPayload.size)
            put("moov".toByteArray(Charsets.US_ASCII))
            put(moovPayload)
        }.array()

        val mdatPayload = ByteArray(1024) { 0x55.toByte() }
        val mdat = ByteBuffer.allocate(8 + mdatPayload.size).apply {
            order(ByteOrder.BIG_ENDIAN)
            putInt(8 + mdatPayload.size)
            put("mdat".toByteArray(Charsets.US_ASCII))
            put(mdatPayload)
        }.array()

        m4aFile.writeBytes(ftyp + moov + mdat)

        val artFile = File(root, "song.jpg")
        val dummyArtwork = ByteArray(512) { (it % 256).toByte() }
        artFile.writeBytes(dummyArtwork)

        val track = Track(
            id = "test_m4a_track",
            title = "M4A Title Test",
            artist = "M4A Artist Test",
            album = "M4A Album Test",
            durationSeconds = 240L
        )

        AudioMetadataTagger.embedMetadata(m4aFile, track, artFile)

        assertTrue(m4aFile.exists())
        val resultBytes = m4aFile.readBytes()
        val resultString = String(resultBytes, Charsets.ISO_8859_1)

        // Valida que o container M4A recebeu udta, meta, ilst, ©nam, ©ART, ©alb e covr
        assertTrue("Deve conter átomo udta", resultString.contains("udta"))
        assertTrue("Deve conter átomo meta", resultString.contains("meta"))
        assertTrue("Deve conter átomo ilst", resultString.contains("ilst"))
        assertTrue("Deve conter átomo ©nam", resultString.contains("©nam"))
        assertTrue("Deve conter título", resultString.contains("M4A Title Test"))
        assertTrue("Deve conter átomo ©ART", resultString.contains("©ART"))
        assertTrue("Deve conter artista", resultString.contains("M4A Artist Test"))
        assertTrue("Deve conter átomo ©alb", resultString.contains("©alb"))
        assertTrue("Deve conter álbum", resultString.contains("M4A Album Test"))
        assertTrue("Deve conter átomo covr", resultString.contains("covr"))
    }
}
