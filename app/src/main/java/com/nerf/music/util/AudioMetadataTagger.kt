package com.nerf.music.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.util.Log
import com.nerf.music.data.model.Track
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Utilitário para escrita e leitura de metadados de áudio (ID3v2 para MP3 e átomos 'ilst' para MP4/M4A),
 * garantindo que a arte da capa (artwork) e os metadados (Título, Artista, Álbum) sejam embutidos
 * no arquivo físico e lidos confiavelmente em varreduras de sincronização offline.
 */
object AudioMetadataTagger {

    private const val TAG = "AudioMetadataTagger"

    data class ExtractedMetadata(
        val title: String?,
        val artist: String?,
        val album: String?,
        val durationMs: Long,
        val artworkBytes: ByteArray?
    )

    /**
     * Extrai metadados completos (incluindo artwork embutida) de um arquivo de áudio
     * utilizando [MediaMetadataRetriever] de forma segura contra exceções de ambiente.
     */
    fun extractMetadata(file: File): ExtractedMetadata? {
        if (!file.exists() || file.length() < 1024L) return null
        var retriever: MediaMetadataRetriever? = null
        return try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)

            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.trim()?.takeIf { it.isNotBlank() }
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.trim()?.takeIf { it.isNotBlank() }
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.trim()?.takeIf { it.isNotBlank() }
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val artwork = retriever.embeddedPicture

            ExtractedMetadata(
                title = title,
                artist = artist,
                album = album,
                durationMs = durationMs,
                artworkBytes = artwork
            )
        } catch (t: Throwable) {
            Log.d(TAG, "extractMetadata não pôde ler ${file.name}: ${t.message}")
            null
        } finally {
            try {
                retriever?.release()
            } catch (_: Throwable) {}
        }
    }

    /**
     * Notifica o MediaScanner do Android para que o MediaStore indexe o arquivo e seus metadados.
     */
    fun scanFileWithMediaScanner(context: Context?, file: File) {
        if (context == null || !file.exists()) return
        try {
            MediaScannerConnection.scanFile(
                context.applicationContext,
                arrayOf(file.absolutePath),
                null
            ) { path, uri ->
                Log.d(TAG, "MediaScanner indexou arquivo: $path -> $uri")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao invocar MediaScannerConnection: ${e.message}")
        }
    }

    /**
     * Embuti metadados (Título, Artista, Álbum e Arte da Capa) diretamente no arquivo de mídia física.
     */
    fun embedMetadata(audioFile: File, track: Track, artworkFile: File?) {
        if (!audioFile.exists() || audioFile.length() < 1024L) return
        val ext = audioFile.extension.lowercase()

        val artBytes = if (artworkFile != null && artworkFile.exists() && artworkFile.length() > 0) {
            try {
                if (artworkFile.length() <= 3_000_000L) artworkFile.readBytes() else null
            } catch (_: Exception) { null }
        } else null

        try {
            when (ext) {
                "m4a", "mp4" -> embedM4aMetadata(audioFile, track, artBytes)
                "mp3" -> embedMp3Id3v2Metadata(audioFile, track, artBytes)
                else -> {
                    // Para formatos de container fechado como WebM/Opus, salvamos o arquivo companheiro
                    // e garantimos a integridade do arquivo.
                    Log.d(TAG, "Formato $ext: metadados garantidos via companion artwork e Room DB.")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Erro ao embutir tags em ${audioFile.name}: ${e.message}")
        }
    }

    // =========================================================================
    // M4A / MP4 ITUNES METADATA WRITER
    // =========================================================================

    private fun embedM4aMetadata(file: File, track: Track, artworkBytes: ByteArray?) {
        val tempFile = File(file.parentFile, "${file.name}.meta_tmp")
        try {
            val raf = RandomAccessFile(file, "r")
            val fileLength = raf.length()

            var moovOffset = -1L
            var moovSize = -1L
            var mdatOffset = -1L

            // 1. Escaneia átomos de nível superior
            var pos = 0L
            while (pos < fileLength - 8) {
                raf.seek(pos)
                val sizeInt = raf.readInt()
                val typeBytes = ByteArray(4)
                raf.readFully(typeBytes)
                val type = String(typeBytes, Charsets.US_ASCII)

                val boxSize = when {
                    sizeInt == 1 -> {
                        raf.readLong()
                    }
                    sizeInt == 0 -> {
                        fileLength - pos
                    }
                    else -> sizeInt.toLong() and 0xFFFFFFFFL
                }

                if (boxSize < 8) break

                if (type == "moov") {
                    moovOffset = pos
                    moovSize = boxSize
                } else if (type == "mdat") {
                    mdatOffset = pos
                }

                pos += boxSize
            }

            if (moovOffset < 0 || moovSize <= 0) {
                raf.close()
                return
            }

            // Lê o átomo moov original
            raf.seek(moovOffset)
            val moovData = ByteArray(moovSize.toInt())
            raf.readFully(moovData)

            // Constrói o novo átomo udta com metadados ilst
            val udtaBox = buildUdtaBox(track, artworkBytes)

            // Substitui ou injeta udta dentro de moov
            val (updatedMoovData, moovSizeDelta) = injectUdtaIntoMoov(moovData, udtaBox)

            // Se moov fica antes de mdat, é imperativo ajustar os chunk offsets (stco / co64)
            val finalMoovData = if (moovOffset < mdatOffset && moovSizeDelta != 0L) {
                adjustChunkOffsets(updatedMoovData, moovSizeDelta)
            } else {
                updatedMoovData
            }

            // Escreve o arquivo reconstruído
            val out = FileOutputStream(tempFile)
            raf.seek(0)

            if (moovOffset < mdatOffset) {
                // [Pré-moov (ex: ftyp)] + [Novo moov] + [Pós-moov (ex: mdat)]
                copyBytes(raf, 0, moovOffset, out)
                out.write(finalMoovData)
                val postMoovStart = moovOffset + moovSize
                copyBytes(raf, postMoovStart, fileLength - postMoovStart, out)
            } else {
                // [Pré-moov (ex: ftyp, mdat)] + [Novo moov]
                copyBytes(raf, 0, moovOffset, out)
                out.write(finalMoovData)
                val postMoovStart = moovOffset + moovSize
                if (postMoovStart < fileLength) {
                    copyBytes(raf, postMoovStart, fileLength - postMoovStart, out)
                }
            }

            out.flush()
            out.close()
            raf.close()

            if (tempFile.exists() && tempFile.length() > 0) {
                if (file.delete()) {
                    tempFile.renameTo(file)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao atualizar atomos M4A: ${e.message}")
            if (tempFile.exists()) tempFile.delete()
        }
    }

    private fun buildUdtaBox(track: Track, artworkBytes: ByteArray?): ByteArray {
        val ilstContent = ByteArrayOutputStream()

        // 1. Título (©nam)
        if (track.title.isNotBlank()) {
            ilstContent.write(buildTextItem("©nam", track.title))
        }

        // 2. Artista (©ART)
        if (track.artist.isNotBlank() && !track.artist.equals("Desconhecido", ignoreCase = true)) {
            ilstContent.write(buildTextItem("©ART", track.artist))
        }

        // 3. Álbum (©alb)
        if (!track.album.isNullOrBlank()) {
            ilstContent.write(buildTextItem("©alb", track.album))
        }

        // 4. Arte de Capa (covr)
        if (artworkBytes != null && artworkBytes.isNotEmpty()) {
            val isPng = artworkBytes.size > 8 &&
                    artworkBytes[0] == 0x89.toByte() && artworkBytes[1] == 0x50.toByte()
            val typeFlag = if (isPng) 14 else 13 // 13 = JPEG, 14 = PNG
            ilstContent.write(buildDataItem("covr", typeFlag, artworkBytes))
        }

        val ilstBytes = buildBox("ilst", ilstContent.toByteArray())

        // Handler box dentro de meta
        val hdlrPayload = ByteBuffer.allocate(25).apply {
            order(ByteOrder.BIG_ENDIAN)
            putInt(0) // version & flags
            putInt(0) // predefined
            put("mdir".toByteArray(Charsets.US_ASCII)) // handler type
            put("appl".toByteArray(Charsets.US_ASCII)) // manufacturer
            putInt(0)
            putInt(0)
            put(0.toByte())
        }.array()
        val hdlrBox = buildBox("hdlr", hdlrPayload)

        // Meta box (possui 4 bytes de version/flags antes de seus filhos)
        val metaContent = ByteBuffer.allocate(4 + hdlrBox.size + ilstBytes.size).apply {
            order(ByteOrder.BIG_ENDIAN)
            putInt(0) // version & flags
            put(hdlrBox)
            put(ilstBytes)
        }.array()
        val metaBox = buildBox("meta", metaContent)

        return buildBox("udta", metaBox)
    }

    private fun buildTextItem(boxName: String, text: String): ByteArray {
        val utf8 = text.toByteArray(Charsets.UTF_8)
        return buildDataItem(boxName, 1, utf8)
    }

    private fun buildDataItem(boxName: String, typeFlag: Int, payload: ByteArray): ByteArray {
        val dataPayload = ByteBuffer.allocate(8 + payload.size).apply {
            order(ByteOrder.BIG_ENDIAN)
            putInt(typeFlag) // Type indicator
            putInt(0)        // Locale / country code
            put(payload)
        }.array()
        val dataBox = buildBox("data", dataPayload)
        return buildBox(boxName, dataBox)
    }

    private fun buildBox(type: String, payload: ByteArray): ByteArray {
        val size = 8 + payload.size
        return ByteBuffer.allocate(size).apply {
            order(ByteOrder.BIG_ENDIAN)
            putInt(size)
            put(type.toByteArray(Charsets.ISO_8859_1))
            put(payload)
        }.array()
    }

    private fun injectUdtaIntoMoov(moovData: ByteArray, udtaBox: ByteArray): Pair<ByteArray, Long> {
        val buffer = ByteBuffer.wrap(moovData).order(ByteOrder.BIG_ENDIAN)
        val originalSize = buffer.int.toLong() and 0xFFFFFFFFL
        var offset = 8

        var udtaStart = -1
        var udtaSize = 0

        while (offset < moovData.size - 8) {
            val bSize = buffer.getInt(offset).toLong() and 0xFFFFFFFFL
            if (bSize < 8 || offset + bSize > moovData.size) break

            val typeBytes = ByteArray(4)
            System.arraycopy(moovData, offset + 4, typeBytes, 0, 4)
            val type = String(typeBytes, Charsets.US_ASCII)

            if (type == "udta") {
                udtaStart = offset
                udtaSize = bSize.toInt()
                break
            }
            offset += bSize.toInt()
        }

        val out = ByteArrayOutputStream()
        if (udtaStart >= 0) {
            // Substitui udta existente
            out.write(moovData, 8, udtaStart - 8)
            out.write(udtaBox)
            val remainingStart = udtaStart + udtaSize
            if (remainingStart < moovData.size) {
                out.write(moovData, remainingStart, moovData.size - remainingStart)
            }
        } else {
            // Anexa udta no final do moov
            out.write(moovData, 8, moovData.size - 8)
            out.write(udtaBox)
        }

        val newPayload = out.toByteArray()
        val newMoovSize = 8 + newPayload.size
        val finalMoov = ByteBuffer.allocate(newMoovSize).apply {
            order(ByteOrder.BIG_ENDIAN)
            putInt(newMoovSize)
            put("moov".toByteArray(Charsets.US_ASCII))
            put(newPayload)
        }.array()

        val delta = newMoovSize.toLong() - originalSize
        return Pair(finalMoov, delta)
    }

    private fun adjustChunkOffsets(moovData: ByteArray, delta: Long): ByteArray {
        val result = moovData.clone()
        val buf = ByteBuffer.wrap(result).order(ByteOrder.BIG_ENDIAN)

        // Varre recursivamente por caixas stco e co64
        var i = 8
        while (i < result.size - 8) {
            val size = buf.getInt(i).toLong() and 0xFFFFFFFFL
            if (size < 8 || i + size > result.size) {
                i += 1
                continue
            }
            val typeBytes = ByteArray(4)
            System.arraycopy(result, i + 4, typeBytes, 0, 4)
            val type = String(typeBytes, Charsets.US_ASCII)

            if (type == "stco") {
                // 4 size + 4 type + 4 ver/flags + 4 count + count*4 offsets
                if (i + 16 <= result.size) {
                    val count = buf.getInt(i + 12)
                    var cur = i + 16
                    for (c in 0 until count) {
                        if (cur + 4 > result.size) break
                        val oldOff = buf.getInt(cur).toLong() and 0xFFFFFFFFL
                        buf.putInt(cur, (oldOff + delta).toInt())
                        cur += 4
                    }
                }
            } else if (type == "co64") {
                if (i + 16 <= result.size) {
                    val count = buf.getInt(i + 12)
                    var cur = i + 16
                    for (c in 0 until count) {
                        if (cur + 8 > result.size) break
                        val oldOff = buf.getLong(cur)
                        buf.putLong(cur, oldOff + delta)
                        cur += 8
                    }
                }
            }
            i += 1
        }
        return result
    }

    private fun copyBytes(raf: RandomAccessFile, start: Long, length: Long, out: FileOutputStream) {
        raf.seek(start)
        val buf = ByteArray(64 * 1024)
        var remaining = length
        while (remaining > 0) {
            val toRead = minOf(remaining, buf.size.toLong()).toInt()
            val r = raf.read(buf, 0, toRead)
            if (r == -1) break
            out.write(buf, 0, r)
            remaining -= r
        }
    }

    // =========================================================================
    // MP3 ID3v2.3 METADATA WRITER
    // =========================================================================

    private fun embedMp3Id3v2Metadata(file: File, track: Track, artworkBytes: ByteArray?) {
        val tempFile = File(file.parentFile, "${file.name}.id3_tmp")
        try {
            val fis = FileInputStream(file)
            val header = ByteArray(10)
            val bytesRead = fis.read(header)
            var audioDataStart = 0L

            if (bytesRead == 10 && header[0] == 'I'.code.toByte() && header[1] == 'D'.code.toByte() && header[2] == '3'.code.toByte()) {
                val tagSize = (header[6].toInt() and 0x7F shl 21) or
                        (header[7].toInt() and 0x7F shl 14) or
                        (header[8].toInt() and 0x7F shl 7) or
                        (header[9].toInt() and 0x7F)
                audioDataStart = (10 + tagSize).toLong()
            }
            fis.close()

            val tagFrames = ByteArrayOutputStream()

            // 1. TIT2 (Title)
            if (track.title.isNotBlank()) {
                tagFrames.write(buildId3v2TextFrame("TIT2", track.title))
            }
            // 2. TPE1 (Artist)
            if (track.artist.isNotBlank() && !track.artist.equals("Desconhecido", ignoreCase = true)) {
                tagFrames.write(buildId3v2TextFrame("TPE1", track.artist))
            }
            // 3. TALB (Album)
            if (!track.album.isNullOrBlank()) {
                tagFrames.write(buildId3v2TextFrame("TALB", track.album))
            }
            // 4. APIC (Artwork Cover)
            if (artworkBytes != null && artworkBytes.isNotEmpty()) {
                val apicStream = ByteArrayOutputStream().apply {
                    write(0x00) // Encoding: ISO-8859-1 para MIME
                    write("image/jpeg\u0000".toByteArray(Charsets.ISO_8859_1))
                    write(0x03) // Picture type: Front Cover
                    write(0x00) // Description: empty
                    write(artworkBytes)
                }
                val apicData = apicStream.toByteArray()
                val frameHeader = ByteBuffer.allocate(10).apply {
                    order(ByteOrder.BIG_ENDIAN)
                    put("APIC".toByteArray(Charsets.US_ASCII))
                    putInt(apicData.size)
                    putShort(0)
                }.array()
                tagFrames.write(frameHeader)
                tagFrames.write(apicData)
            }

            val framesData = tagFrames.toByteArray()
            val totalTagPayloadSize = framesData.size

            // Constrói cabeçalho ID3v2.3 com synchsafe integer
            val id3Header = ByteBuffer.allocate(10).apply {
                order(ByteOrder.BIG_ENDIAN)
                put("ID3".toByteArray(Charsets.US_ASCII))
                put(3.toByte()) // Version 2.3
                put(0.toByte()) // Revision
                put(0.toByte()) // Flags
                put(((totalTagPayloadSize shr 21) and 0x7F).toByte())
                put(((totalTagPayloadSize shr 14) and 0x7F).toByte())
                put(((totalTagPayloadSize shr 7) and 0x7F).toByte())
                put((totalTagPayloadSize and 0x7F).toByte())
            }.array()

            val fos = FileOutputStream(tempFile)
            fos.write(id3Header)
            fos.write(framesData)

            val raf = RandomAccessFile(file, "r")
            raf.seek(audioDataStart)
            val buf = ByteArray(64 * 1024)
            var r: Int
            while (raf.read(buf).also { r = it } != -1) {
                fos.write(buf, 0, r)
            }
            raf.close()
            fos.flush()
            fos.close()

            if (tempFile.exists() && tempFile.length() > 0) {
                if (file.delete()) {
                    tempFile.renameTo(file)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao gravar tags ID3 no MP3: ${e.message}")
            if (tempFile.exists()) tempFile.delete()
        }
    }

    private fun buildId3v2TextFrame(frameId: String, text: String): ByteArray {
        val utf8Bytes = text.toByteArray(Charsets.UTF_8)
        val payload = ByteBuffer.allocate(1 + utf8Bytes.size).apply {
            put(0x03.toByte()) // UTF-8 encoding flag
            put(utf8Bytes)
        }.array()

        val header = ByteBuffer.allocate(10).apply {
            order(ByteOrder.BIG_ENDIAN)
            put(frameId.toByteArray(Charsets.US_ASCII))
            putInt(payload.size)
            putShort(0)
        }.array()

        return ByteBuffer.allocate(header.size + payload.size).apply {
            put(header)
            put(payload)
        }.array()
    }
}
