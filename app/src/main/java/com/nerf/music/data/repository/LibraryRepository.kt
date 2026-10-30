package com.nerf.music.data.repository

import android.content.Context
import android.util.Log
import com.nerf.music.data.model.DownloadedTrack
import com.nerf.music.data.model.Track
import com.nerf.music.data.model.UserPlaylist
import com.nerf.music.player.NewPipeStreamResolver
import com.nerf.music.player.StreamResolver
import com.nerf.music.data.db.AppDatabase
import com.nerf.music.data.db.DownloadedTrackEntity
import com.nerf.music.data.db.LibraryTrackEntity
import com.nerf.music.util.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.UUID
import java.util.Locale
import android.os.Environment
import com.nerf.music.util.AudioMetadataTagger
import com.nerf.music.util.DataSaverInterceptor

interface LibraryRepository {
    val likedTracksFlow: StateFlow<List<Track>>
    val playlistsFlow: StateFlow<List<UserPlaylist>>
    val downloadedTracksFlow: StateFlow<List<DownloadedTrack>>

    fun isFavorite(trackId: String): Boolean
    fun toggleFavorite(track: Track): Boolean
    fun addFavorites(tracks: List<Track>): Int

    fun isDownloaded(trackId: String): Boolean
    fun getDownloadedFilePath(trackId: String): String?
    suspend fun downloadTrack(track: Track, isSmartDownload: Boolean = false, onProgress: (Float) -> Unit = {}): Result<DownloadedTrack>
    fun removeDownload(trackId: String): Boolean
    suspend fun syncDownloadsWithDisk(): Int

    fun createPlaylist(title: String, description: String = "", initialTracks: List<Track> = emptyList()): UserPlaylist
    /** Cria ou atualiza a playlist importada identificada pela chave estável de origem. */
    fun upsertImportedPlaylist(sourceKey: String, title: String, description: String, tracks: List<Track>): UserPlaylist
    fun deletePlaylist(playlistId: String): Boolean
    fun updatePlaylistTitle(playlistId: String, newTitle: String): Boolean
    fun addTrackToPlaylist(playlistId: String, track: Track): Boolean
    fun addTracksToPlaylist(playlistId: String, tracks: List<Track>): Int
    fun removeTrackFromPlaylist(playlistId: String, trackId: String): Boolean
    fun isTrackInPlaylist(playlistId: String, trackId: String): Boolean
}

class LibraryRepositoryImpl(
    private val context: Context,
    private val streamResolver: StreamResolver = NewPipeStreamResolver(context),
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(DataSaverInterceptor(context))
        .build(),
    private val spotifyCanvasRepository: SpotifyCanvasRepository = SpotifyCanvasRepositoryImpl(httpClient),
    private val lyricsRepository: LyricsRepository = LyricsRepositoryImpl()
) : LibraryRepository {

    companion object {
        private const val TAG = "LibraryRepository"
        private const val PREFS_NAME = "nerf_library_prefs"
        private const val KEY_LIKED_TRACKS = "liked_tracks_json"
        private const val KEY_PLAYLISTS = "user_playlists_json"
        private const val KEY_DOWNLOADS = "downloaded_tracks_json"

        private val _likedTracksFlow = MutableStateFlow<List<Track>>(emptyList())
        val sharedLikedTracksFlow: StateFlow<List<Track>> = _likedTracksFlow.asStateFlow()

        private val _playlistsFlow = MutableStateFlow<List<UserPlaylist>>(emptyList())
        val sharedPlaylistsFlow: StateFlow<List<UserPlaylist>> = _playlistsFlow.asStateFlow()

        private val _downloadedTracksFlow = MutableStateFlow<List<DownloadedTrack>>(emptyList())
        val sharedDownloadedTracksFlow: StateFlow<List<DownloadedTrack>> = _downloadedTracksFlow.asStateFlow()
    }

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val downloadsDir = File(context.applicationContext.filesDir, "offline_audio").apply {
        if (!exists()) mkdirs()
    }

    private val database = AppDatabase.getInstance(context)
    private val downloadedTrackDao = database.downloadedTrackDao()
    private val libraryTrackDao = database.libraryTrackDao()

    override val likedTracksFlow: StateFlow<List<Track>> = sharedLikedTracksFlow
    override val playlistsFlow: StateFlow<List<UserPlaylist>> = sharedPlaylistsFlow
    override val downloadedTracksFlow: StateFlow<List<DownloadedTrack>> = sharedDownloadedTracksFlow

    private val prefChangeListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            KEY_DOWNLOADS -> reloadDownloads()
            KEY_LIKED_TRACKS -> reloadLiked()
            KEY_PLAYLISTS -> reloadPlaylists()
        }
    }

    init {
        loadData()
        prefs.registerOnSharedPreferenceChangeListener(prefChangeListener)
        @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val migrationNow = System.currentTimeMillis()
                _likedTracksFlow.value.forEachIndexed { index, track ->
                    libraryTrackDao.markAdded(
                        LibraryTrackEntity.fromTrack(track),
                        migrationNow - index * 1_000L,
                        liked = true
                    )
                }
                _playlistsFlow.value.forEach { playlist ->
                    playlist.tracks.forEachIndexed { index, track ->
                        libraryTrackDao.markAdded(
                            LibraryTrackEntity.fromTrack(track),
                            playlist.createdAtEpochMs + index,
                            liked = _likedTracksFlow.value.any { it.id == track.id }
                        )
                    }
                }
                val dbTracks = downloadedTrackDao.getAllTracks()
                if (dbTracks.isNotEmpty()) {
                    val tracks = dbTracks.map { it.toDownloadedTrack() }
                    _downloadedTracksFlow.value = tracks
                    saveDownloads(tracks)
                } else {
                    val rawDownloads = prefs.getString(KEY_DOWNLOADS, null)
                    if (!rawDownloads.isNullOrBlank()) {
                        val list = json.decodeFromString<List<DownloadedTrack>>(rawDownloads)
                        val entities = list.map { DownloadedTrackEntity.fromDownloadedTrack(it) }
                        downloadedTrackDao.insertAll(entities)
                    }
                }
                syncDownloadsWithDisk()
            } catch (e: Exception) {
                Log.w(TAG, "Falha na inicialização do Room DB: ${e.message}")
            }
        }
    }

    private fun loadData() {
        reloadLiked()
        reloadPlaylists()
        reloadDownloads()
    }

    private fun reloadLiked() {
        try {
            val rawLiked = prefs.getString(KEY_LIKED_TRACKS, null)
            _likedTracksFlow.value = if (!rawLiked.isNullOrBlank()) {
                json.decodeFromString(rawLiked)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao recarregar músicas curtidas: ${e.message}", e)
        }
    }

    private fun reloadPlaylists() {
        try {
            val rawPlaylists = prefs.getString(KEY_PLAYLISTS, null)
            _playlistsFlow.value = if (!rawPlaylists.isNullOrBlank()) {
                json.decodeFromString(rawPlaylists)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao recarregar playlists: ${e.message}", e)
        }
    }

    private fun reloadDownloads() {
        try {
            val rawDownloads = prefs.getString(KEY_DOWNLOADS, null)
            if (!rawDownloads.isNullOrBlank()) {
                val list = json.decodeFromString<List<DownloadedTrack>>(rawDownloads)
                val existing = list.mapNotNull { dt ->
                    val file = File(dt.localFilePath)
                    if (file.exists()) {
                        dt
                    } else {
                        val fallback = listOf("m4a", "opus", "webm")
                            .map { ext -> File(downloadsDir, "${dt.track.id}.$ext") }
                            .firstOrNull { it.exists() }
                        if (fallback != null) {
                            dt.copy(localFilePath = fallback.absolutePath, fileSizeBytes = fallback.length())
                        } else null
                    }
                }
                _downloadedTracksFlow.value = existing
            } else {
                _downloadedTracksFlow.value = emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao recarregar downloads: ${e.message}", e)
        }
    }

    override fun isFavorite(trackId: String): Boolean {
        return _likedTracksFlow.value.any { it.id == trackId }
    }

    override fun toggleFavorite(track: Track): Boolean {
        if (track.id.isBlank()) return false
        val current = _likedTracksFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == track.id }
        val added: Boolean
        if (index >= 0) {
            current.removeAt(index)
            added = false
        } else {
            current.add(0, track)
            added = true
        }
        _likedTracksFlow.value = current
        saveLikedTracks(current)
        @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
        GlobalScope.launch(Dispatchers.IO) {
            if (added) {
                libraryTrackDao.markAdded(LibraryTrackEntity.fromTrack(track), System.currentTimeMillis(), liked = true)
            } else {
                libraryTrackDao.clearLiked(track.id)
            }
        }
        return added
    }

    override fun addFavorites(tracks: List<Track>): Int {
        if (tracks.isEmpty()) return 0
        val current = _likedTracksFlow.value.toMutableList()
        val existingIds = current.map { it.id }.toSet()
        val toAdd = tracks.filter { it.id.isNotBlank() && it.id !in existingIds }
        if (toAdd.isEmpty()) return 0
        current.addAll(0, toAdd)
        _likedTracksFlow.value = current
        saveLikedTracks(current)
        val addedAt = System.currentTimeMillis()
        @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
        GlobalScope.launch(Dispatchers.IO) {
            toAdd.forEachIndexed { index, track ->
                libraryTrackDao.markAdded(LibraryTrackEntity.fromTrack(track), addedAt + index, liked = true)
            }
        }
        return toAdd.size
    }

    private fun saveLikedTracks(tracks: List<Track>) {
        try {
            val raw = json.encodeToString(tracks)
            prefs.edit().putString(KEY_LIKED_TRACKS, raw).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao salvar músicas curtidas: ${e.message}", e)
        }
    }

    override fun isDownloaded(trackId: String): Boolean {
        val dt = _downloadedTracksFlow.value.firstOrNull { it.track.id == trackId }
        if (dt != null) {
            val f = File(dt.localFilePath)
            if (f.exists() && f.length() > 0L) return true
        }
        val possibleExts = listOf("opus", "m4a", "webm", "mp4")
        return possibleExts.any { ext ->
            val f = File(downloadsDir, "$trackId.$ext")
            f.exists() && f.length() > 0L
        }
    }

    override fun getDownloadedFilePath(trackId: String): String? {
        val dt = _downloadedTracksFlow.value.firstOrNull { it.track.id == trackId }
        if (dt != null) {
            val f = File(dt.localFilePath)
            if (f.exists() && f.length() > 0L) return dt.localFilePath
        }
        val possibleExts = listOf("opus", "m4a", "webm", "mp4")
        val fallback = possibleExts.map { File(downloadsDir, "$trackId.$it") }.firstOrNull { it.exists() && it.length() > 0L }
        return fallback?.absolutePath
    }

    override suspend fun downloadTrack(
        track: Track,
        isSmartDownload: Boolean,
        onProgress: (Float) -> Unit
    ): Result<DownloadedTrack> = withContext(Dispatchers.IO) {
        if (track.id.isBlank()) return@withContext Result.failure(IllegalArgumentException("TrackId vazio"))

        val isWifi = NetworkUtils.isConnectedToWifi(context)
        val isNetworkAvailable = NetworkUtils.isNetworkAvailable(context)
        val isCellular = isNetworkAvailable && !isWifi
        val settingsPrefs = context.getSharedPreferences("nerf_user_settings_prefs", Context.MODE_PRIVATE)
        val allowCellular = settingsPrefs.getBoolean("key_download_over_cellular", false)
        val dataSaverModeStr = settingsPrefs.getString("key_data_saver_mode", "ALWAYS_OFF") ?: "ALWAYS_OFF"
        val dataSaverMode = try {
            DataSaverMode.valueOf(dataSaverModeStr)
        } catch (_: Exception) {
            DataSaverMode.ALWAYS_OFF
        }

        if (dataSaverMode == DataSaverMode.ALWAYS_ON) {
            return@withContext Result.failure(
                IllegalStateException("Download bloqueado: Economia de Dados (Sempre ativado) está ativo.")
            )
        }

        if (!isWifi && (isCellular || !isNetworkAvailable)) {
            if (!allowCellular) {
                return@withContext Result.failure(
                    IllegalStateException("Download pausado: 'Downloads via dados móveis' está desativado nas configurações.")
                )
            }
            if (dataSaverMode == DataSaverMode.AUTOMATIC) {
                return@withContext Result.failure(
                    IllegalStateException("Download bloqueado em rede móvel pela Economia de Dados.")
                )
            }
        }

        val isDataSaverActive = dataSaverMode == DataSaverMode.ALWAYS_ON || (dataSaverMode == DataSaverMode.AUTOMATIC && !isWifi)
        val isDownloadCanvasEnabled = settingsPrefs.getBoolean("key_download_canvas_enabled", false)
        val isDownloadLyricsEnabled = settingsPrefs.getBoolean("key_download_lyrics_enabled", true)

        try {
            val downloadQualityStr = settingsPrefs.getString("key_download_audio_quality", "LOSSLESS") ?: "LOSSLESS"
            val targetAudioQuality = when (downloadQualityStr) {
                "LOSSLESS", "VERY_HIGH" -> AudioQuality.VERY_HIGH
                "HIGH" -> AudioQuality.HIGH
                "NORMAL" -> AudioQuality.NORMAL
                else -> AudioQuality.VERY_HIGH
            }
            // 1. Resolve URL de áudio JIT via NewPipe com a qualidade alvo (Padrão: Lossless / Máxima Fidelidade)
            val streamUrl = streamResolver.resolveStreamUrl(track.id, targetAudioQuality).getOrThrow()

            // 2. Determina o tamanho total do áudio (Content-Length / Content-Range) e MIME Type
            val streamUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
            var totalBytes = -1L
            var detectedContentType: String? = null

            try {
                val headReq = Request.Builder()
                    .url(streamUrl)
                    .head()
                    .header("User-Agent", streamUserAgent)
                    .build()
                httpClient.newCall(headReq).execute().use { resp ->
                    totalBytes = resp.header("Content-Length")?.toLongOrNull() ?: -1L
                    detectedContentType = resp.header("Content-Type")
                }
            } catch (_: Exception) {}

            if (totalBytes <= 0) {
                try {
                    val probeReq = Request.Builder()
                        .url(streamUrl)
                        .header("User-Agent", streamUserAgent)
                        .header("Range", "bytes=0-0")
                        .build()
                    httpClient.newCall(probeReq).execute().use { resp ->
                        val cr = resp.header("Content-Range")
                        if (cr != null && cr.contains("/")) {
                            totalBytes = cr.substringAfter("/").trim().toLongOrNull() ?: -1L
                        }
                        if (detectedContentType.isNullOrBlank()) {
                            detectedContentType = resp.header("Content-Type")
                        }
                    }
                } catch (_: Exception) {}
            }

            val tempFile = File(downloadsDir, "${track.id}.tmp")
            if (tempFile.exists()) tempFile.delete()

            var fallbackToSequential = totalBytes <= 0

            if (totalBytes > 0) {
                val raf = RandomAccessFile(tempFile, "rw")
                raf.setLength(totalBytes)

                val chunkSize = 1024 * 1024L // Chunks de 1MB para bypass total do throttling do YouTube
                val chunks = mutableListOf<Pair<Long, Long>>()
                var startPos = 0L
                while (startPos < totalBytes) {
                    val endPos = minOf(startPos + chunkSize - 1, totalBytes - 1)
                    chunks.add(Pair(startPos, endPos))
                    startPos = endPos + 1
                }

                val downloadedBytes = AtomicLong(0L)
                var lastProgressTime = 0L

                try {
                    coroutineScope {
                        val dispatcher = Dispatchers.IO.limitedParallelism(3)
                        val jobs = chunks.map { (chunkStart, chunkEnd) ->
                            async(dispatcher) {
                                val chunkReq = Request.Builder()
                                    .url(streamUrl)
                                    .header("User-Agent", streamUserAgent)
                                    .header("Range", "bytes=$chunkStart-$chunkEnd")
                                    .build()

                                httpClient.newCall(chunkReq).execute().use { chunkResp ->
                                    if (chunkResp.code == 200) {
                                        // O servidor não honrou Range e enviou o stream completo; aborta chunks para evitar corrupção
                                        throw Http200RangeUnsupportedException()
                                    }
                                    if (!chunkResp.isSuccessful || chunkResp.code != 206) {
                                        throw IOException("Falha HTTP ${chunkResp.code} no chunk $chunkStart-$chunkEnd")
                                    }
                                    if (detectedContentType.isNullOrBlank()) {
                                        detectedContentType = chunkResp.header("Content-Type")
                                    }
                                    val body = chunkResp.body ?: throw IOException("Corpo de resposta nulo")
                                    val input = body.byteStream()
                                    val buf = ByteArray(64 * 1024)
                                    var currentPos = chunkStart
                                    var bytesRead: Int
                                    while (input.read(buf).also { bytesRead = it } != -1) {
                                        synchronized(raf) {
                                            raf.seek(currentPos)
                                            raf.write(buf, 0, bytesRead)
                                        }
                                        currentPos += bytesRead
                                        val total = downloadedBytes.addAndGet(bytesRead.toLong())
                                        val now = System.currentTimeMillis()
                                        if (now - lastProgressTime >= 80) {
                                            lastProgressTime = now
                                            onProgress((total.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f))
                                        }
                                    }
                                }
                            }
                        }
                        jobs.awaitAll()
                    }
                } catch (e: Http200RangeUnsupportedException) {
                    Log.w(TAG, "Servidor retornou HTTP 200 para Range request. Abortando chunks paralelos e iniciando download sequencial...")
                    fallbackToSequential = true
                } finally {
                    try { raf.close() } catch (_: Exception) {}
                }
            }

            if (fallbackToSequential) {
                if (tempFile.exists()) tempFile.delete()
                val request = Request.Builder()
                    .url(streamUrl)
                    .header("User-Agent", streamUserAgent)
                    .build()
                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw IOException("Falha no download sequencial: HTTP ${response.code}")
                }
                val body = response.body ?: throw IOException("Corpo nulo no download sequencial")
                if (detectedContentType.isNullOrBlank()) {
                    detectedContentType = response.header("Content-Type")
                }
                val streamLength = body.contentLength()
                var readBytes = 0L
                body.byteStream().use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            readBytes += bytesRead
                            if (streamLength > 0) {
                                onProgress((readBytes.toFloat() / streamLength.toFloat()).coerceIn(0f, 1f))
                            }
                        }
                        output.flush()
                    }
                }
            }

            // Detecta a extensão correta (.m4a ou .opus/.webm) para o container de mídia
            val extension = determineExtension(detectedContentType, streamUrl)
            val destFile = File(downloadsDir, "${track.id}.$extension")

            // Remove arquivos residuais de formatos anteriores
            listOf("opus", "m4a", "webm").forEach { ext ->
                val f = File(downloadsDir, "${track.id}.$ext")
                if (f.exists()) f.delete()
            }

            onProgress(1f)
            val renamed = tempFile.renameTo(destFile)
            if (!renamed) {
                Log.w(TAG, "renameTo falhou de ${tempFile.name} para ${destFile.name}. Executando cópia via stream...")
                tempFile.inputStream().use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                tempFile.delete()
            }

            // Baixa o companion artwork para persistência offline
            // Se Economia de Dados estiver ativa, suprime o download da capa HD pesada
            var artworkLocalPath: String? = null
            val artworkFile = File(downloadsDir, "${track.id}.jpg")
            val candidateArtUrl = if (isDataSaverActive) {
                track.thumbnailUrl
            } else {
                track.highResThumbnailUrl?.takeIf { it.isNotBlank() } ?: track.thumbnailUrl
            }

            if (!candidateArtUrl.isNullOrBlank() && !candidateArtUrl.startsWith("file://")) {
                try {
                    val artReq = Request.Builder()
                        .url(candidateArtUrl)
                        .get()
                        .build()
                    httpClient.newCall(artReq).execute().use { artResp ->
                        if (artResp.isSuccessful) {
                            artResp.body?.byteStream()?.use { artIn ->
                                FileOutputStream(artworkFile).use { artOut ->
                                    artIn.copyTo(artOut)
                                }
                            }
                            if (artworkFile.exists() && artworkFile.length() > 0) {
                                artworkLocalPath = artworkFile.absolutePath
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Falha ao baixar artwork de ${track.id}: ${e.message}")
                }
            } else if (artworkFile.exists() && artworkFile.length() > 0) {
                artworkLocalPath = artworkFile.absolutePath
            }

            // Baixa o Canvas de vídeo (.mp4) se habilitado e não suprimido por Economia de Dados
            if (isDownloadCanvasEnabled && !isDataSaverActive) {
                val canvasFile = File(downloadsDir, "${track.id}.mp4")
                try {
                    val canvasRes = spotifyCanvasRepository.getCanvasUrl(track.title, track.artist)
                    val canvasUrl = canvasRes.getOrNull()
                    if (!canvasUrl.isNullOrBlank()) {
                        val canvasReq = Request.Builder()
                            .url(canvasUrl)
                            .get()
                            .build()
                        httpClient.newCall(canvasReq).execute().use { cResp ->
                            if (cResp.isSuccessful) {
                                cResp.body?.byteStream()?.use { cIn ->
                                    FileOutputStream(canvasFile).use { cOut ->
                                        cIn.copyTo(cOut)
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Falha ao baixar companion Canvas para ${track.id}: ${e.message}")
                }
            }

            // Baixa o companion LRC de letras se habilitado
            if (isDownloadLyricsEnabled) {
                val lrcFile = File(downloadsDir, "${track.id}.lrc")
                try {
                    val lyricsRes = lyricsRepository.getLyrics(
                        trackId = track.id,
                        title = track.title,
                        artist = track.artist,
                        durationSeconds = track.durationSeconds.toInt()
                    )
                    val lyricsData = lyricsRes.getOrNull()
                    if (lyricsData != null) {
                        val lrcContent = if (lyricsData.syncedLines.isNotEmpty()) {
                            lyricsData.syncedLines.joinToString("\n") { line ->
                                val min = (line.timeMs / 60000).toString().padStart(2, '0')
                                val sec = ((line.timeMs % 60000) / 1000).toString().padStart(2, '0')
                                val csec = ((line.timeMs % 1000) / 10).toString().padStart(2, '0')
                                "[$min:$sec.$csec]${line.text}"
                            }
                        } else if (!lyricsData.plainLyrics.isNullOrBlank() && !lyricsData.plainLyrics.trim().equals("null", ignoreCase = true)) {
                            lyricsData.plainLyrics.trim()
                        } else null

                        if (!lrcContent.isNullOrBlank()) {
                            lrcFile.writeText(lrcContent)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Falha ao baixar companion Letras para ${track.id}: ${e.message}")
                }
            }

            // Embuti metadados (Título, Artista, Álbum e Arte da Capa) diretamente no arquivo físico de áudio
            try {
                AudioMetadataTagger.embedMetadata(
                    audioFile = destFile,
                    track = track,
                    artworkFile = if (artworkFile.exists() && artworkFile.length() > 0) artworkFile else null
                )
                AudioMetadataTagger.scanFileWithMediaScanner(context, destFile)
            } catch (e: Exception) {
                Log.w(TAG, "Falha ao embutir metadados/artwork em ${destFile.name}: ${e.message}")
            }

            val effectiveThumbnail = artworkLocalPath?.let { "file://$it" } ?: track.thumbnailUrl
            val downloadedTrack = DownloadedTrack(
                track = track.copy(thumbnailUrl = effectiveThumbnail),
                localFilePath = destFile.absolutePath,
                downloadedAtEpochMs = System.currentTimeMillis(),
                fileSizeBytes = destFile.length(),
                isSmartDownload = isSmartDownload,
                localArtworkPath = artworkLocalPath
            )

            // Salva no banco de dados Room
            try {
                downloadedTrackDao.insert(DownloadedTrackEntity.fromDownloadedTrack(downloadedTrack))
            } catch (e: Exception) {
                Log.w(TAG, "Erro ao persistir download no Room DB: ${e.message}")
            }

            val current = _downloadedTracksFlow.value.filterNot { it.track.id == track.id }.toMutableList()
            current.add(0, downloadedTrack)
            _downloadedTracksFlow.value = current
            saveDownloads(current)

            Result.success(downloadedTrack)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao baixar faixa ${track.id}: ${e.message}", e)
            Result.failure(e)
        }
    }

    override fun removeDownload(trackId: String): Boolean {
        val current = _downloadedTracksFlow.value.toMutableList()
        val index = current.indexOfFirst { it.track.id == trackId }
        var deletedAny = false
        if (index >= 0) {
            val removed = current.removeAt(index)
            val f = File(removed.localFilePath)
            if (f.exists()) {
                f.delete()
                deletedAny = true
            }
            val artFile = File(downloadsDir, "$trackId.jpg")
            if (artFile.exists()) {
                artFile.delete()
            }
            _downloadedTracksFlow.value = current
            saveDownloads(current)
        }
        listOf("opus", "m4a", "webm", "mp4").forEach { ext ->
            val f = File(downloadsDir, "$trackId.$ext")
            if (f.exists()) {
                f.delete()
                deletedAny = true
            }
        }
        val artFile = File(downloadsDir, "$trackId.jpg")
        if (artFile.exists()) {
            artFile.delete()
        }
        val lrcFile = File(downloadsDir, "$trackId.lrc")
        if (lrcFile.exists()) {
            lrcFile.delete()
            deletedAny = true
        }

        @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
        GlobalScope.launch(Dispatchers.IO) {
            try {
                downloadedTrackDao.deleteById(trackId)
            } catch (e: Exception) {
                Log.w(TAG, "Erro ao deletar do Room DB: ${e.message}")
            }
        }

        return deletedAny || index >= 0
    }

    override suspend fun syncDownloadsWithDisk(): Int = withContext(Dispatchers.IO) {
        var reconciledCount = 0
        try {
            // 1. Escaneia offline_audio e pastas de armazenamento local por arquivos de áudio válidos
            val audioExtensions = setOf("opus", "m4a", "webm", "mp3", "ogg", "flac", "aac")
            val scanDirs = mutableListOf(downloadsDir)
            try {
                val appFiles = File(context.applicationContext.filesDir, "offline_audio")
                if (appFiles.exists() && appFiles.isDirectory && !scanDirs.contains(appFiles)) {
                    scanDirs.add(appFiles)
                }
                val extFiles = context.applicationContext.getExternalFilesDir(null)
                if (extFiles != null) {
                    val extAudioDir = File(extFiles, "offline_audio")
                    if (extAudioDir.exists() && extAudioDir.isDirectory && !scanDirs.contains(extAudioDir)) {
                        scanDirs.add(extAudioDir)
                    }
                }
                val publicMusic = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                if (publicMusic != null && publicMusic.exists() && publicMusic.isDirectory) {
                    if (!scanDirs.contains(publicMusic)) {
                        scanDirs.add(publicMusic)
                    }
                    val nerfMusicDir = File(publicMusic, "NerfMusic")
                    if (nerfMusicDir.exists() && nerfMusicDir.isDirectory && !scanDirs.contains(nerfMusicDir)) {
                        scanDirs.add(nerfMusicDir)
                    }
                    publicMusic.listFiles { f -> f.isDirectory && !f.name.startsWith(".") }?.forEach { sub ->
                        if (!scanDirs.contains(sub)) scanDirs.add(sub)
                    }
                }
                val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (publicDownloads != null && publicDownloads.exists() && publicDownloads.isDirectory) {
                    val nerfDownDir = File(publicDownloads, "NerfMusic")
                    if (nerfDownDir.exists() && nerfDownDir.isDirectory && !scanDirs.contains(nerfDownDir)) {
                        scanDirs.add(nerfDownDir)
                    }
                }
            } catch (_: Exception) {}

            val diskFilesByTrackId = mutableMapOf<String, File>()
            for (dir in scanDirs) {
                val files = dir.listFiles { file ->
                    file.isFile && file.extension.lowercase() in audioExtensions && file.length() >= 512L
                }?.toList().orEmpty()
                for (file in files) {
                    val trackId = file.nameWithoutExtension
                    if (trackId.isNotBlank() && !diskFilesByTrackId.containsKey(trackId)) {
                        diskFilesByTrackId[trackId] = file
                    }
                }
            }

            // Consulta MediaStore do sistema Android para indexar faixas de áudio públicas presentes no armazenamento
            try {
                val projection = arrayOf(
                    android.provider.MediaStore.Audio.Media._ID,
                    android.provider.MediaStore.Audio.Media.DATA
                )
                context.contentResolver.query(
                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    null
                )?.use { cursor ->
                    val dataCol = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media.DATA)
                    while (cursor.moveToNext()) {
                        val path = if (dataCol != -1) cursor.getString(dataCol) else null
                        if (!path.isNullOrBlank()) {
                            val f = File(path)
                            if (f.exists() && f.isFile && f.extension.lowercase() in audioExtensions && f.length() >= 512L) {
                                val trackId = f.nameWithoutExtension
                                if (trackId.isNotBlank() && !diskFilesByTrackId.containsKey(trackId)) {
                                    diskFilesByTrackId[trackId] = f
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Falha ao consultar MediaStore para sync de downloads: ${e.message}")
            }

            // 2. Busca registros atuais no Room DB
            val currentDbEntities = downloadedTrackDao.getAllTracks().toMutableList()
            val dbTrackIds = currentDbEntities.map { it.id }.toSet()

            // 3. Purga registros do banco cujo arquivo físico foi removido
            for (entity in currentDbEntities) {
                val physicalFile = File(entity.localFilePath)
                val exists = physicalFile.exists() || diskFilesByTrackId.containsKey(entity.id)
                if (!exists) {
                    downloadedTrackDao.deleteById(entity.id)
                    reconciledCount++
                } else if (!physicalFile.exists() && diskFilesByTrackId.containsKey(entity.id)) {
                    val newFile = diskFilesByTrackId[entity.id]!!
                    val updated = entity.copy(
                        localFilePath = newFile.absolutePath,
                        fileSizeBytes = newFile.length()
                    )
                    downloadedTrackDao.update(updated)
                    reconciledCount++
                }
            }

            // 4. Recupera arquivos órfãos no disco que não constam no banco e extrai metadados ID3/MP4/Artwork
            val historyRepo = UserHistoryRepositoryImpl(context.applicationContext)
            val recentTracks = try { historyRepo.getRecentlyPlayed() } catch (_: Exception) { emptyList() }
            val recentMap = recentTracks.associateBy { it.id }

            for ((trackId, file) in diskFilesByTrackId) {
                val artFile = File(file.parentFile ?: downloadsDir, "$trackId.jpg")
                val fallbackDownloadsArt = File(downloadsDir, "$trackId.jpg")
                var localArtPath = when {
                    artFile.exists() && artFile.length() > 0 -> artFile.absolutePath
                    fallbackDownloadsArt.exists() && fallbackDownloadsArt.length() > 0 -> fallbackDownloadsArt.absolutePath
                    else -> null
                }

                // Extração dinâmica de tags ID3/MP4 e arte embutida caso não haja dados completos
                val extracted = try {
                    AudioMetadataTagger.extractMetadata(file)
                } catch (_: Throwable) {
                    null
                }

                // Se houver arte embutida no áudio e o companion .jpg não existir fisicamente, salva no disco
                if (localArtPath == null && extracted?.artworkBytes != null && extracted.artworkBytes.isNotEmpty()) {
                    try {
                        val companionArt = File(downloadsDir, "$trackId.jpg")
                        FileOutputStream(companionArt).use { it.write(extracted.artworkBytes) }
                        if (companionArt.exists() && companionArt.length() > 0) {
                            localArtPath = companionArt.absolutePath
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "Falha ao persistir imagem embutida de $trackId: ${e.message}")
                    }
                }

                if (!dbTrackIds.contains(trackId)) {
                    val knownTrack = recentMap[trackId] ?: _likedTracksFlow.value.firstOrNull { it.id == trackId }

                    val finalTitle = knownTrack?.title
                        ?: extracted?.title
                        ?: "Faixa offline $trackId"

                    val finalArtist = knownTrack?.artist
                        ?: extracted?.artist
                        ?: "Áudio Local"

                    val finalAlbum = knownTrack?.album ?: extracted?.album

                    val finalDurationSec = when {
                        knownTrack != null && knownTrack.durationSeconds > 0L -> knownTrack.durationSeconds
                        extracted != null && extracted.durationMs > 0L -> extracted.durationMs / 1000L
                        else -> 0L
                    }

                    val finalDurationText = when {
                        knownTrack != null && knownTrack.durationText.isNotBlank() && knownTrack.durationText != "--:--" -> knownTrack.durationText
                        finalDurationSec > 0L -> String.format(Locale.getDefault(), "%02d:%02d", finalDurationSec / 60, finalDurationSec % 60)
                        else -> "--:--"
                    }

                    val newEntity = DownloadedTrackEntity(
                        id = trackId,
                        title = finalTitle,
                        artist = finalArtist,
                        album = finalAlbum,
                        thumbnailUrl = localArtPath?.let { "file://$it" } ?: knownTrack?.thumbnailUrl,
                        durationText = finalDurationText,
                        durationSeconds = finalDurationSec,
                        sourceIdentifier = knownTrack?.sourceIdentifier ?: "OFFLINE_DISK",
                        playlistId = knownTrack?.playlistId,
                        browseId = knownTrack?.browseId,
                        artistBrowseId = knownTrack?.artistBrowseId,
                        localFilePath = file.absolutePath,
                        downloadedAtEpochMs = file.lastModified(),
                        fileSizeBytes = file.length(),
                        isSmartDownload = false,
                        localArtworkPath = localArtPath
                    )
                    downloadedTrackDao.insert(newEntity)
                    reconciledCount++
                } else {
                    val existingEntity = currentDbEntities.firstOrNull { it.id == trackId }
                    if (existingEntity != null) {
                        var needsUpdate = false
                        var updatedEntity = existingEntity

                        if (existingEntity.localArtworkPath.isNullOrBlank() && localArtPath != null) {
                            updatedEntity = updatedEntity.copy(
                                localArtworkPath = localArtPath,
                                thumbnailUrl = "file://$localArtPath"
                            )
                            needsUpdate = true
                        }

                        // Se a entidade tinha título ou artista genérico de fallback, atualiza com as tags lidas
                        if (existingEntity.title.startsWith("Faixa offline") && !extracted?.title.isNullOrBlank()) {
                            updatedEntity = updatedEntity.copy(
                                title = extracted!!.title!!,
                                artist = extracted.artist ?: updatedEntity.artist,
                                album = extracted.album ?: updatedEntity.album
                            )
                            needsUpdate = true
                        }

                        if (needsUpdate) {
                            downloadedTrackDao.update(updatedEntity)
                            reconciledCount++
                        }
                    }
                }
            }

            // 5. Atualiza fluxo em memória e sincroniza SharedPreferences
            val updatedEntities = downloadedTrackDao.getAllTracks()
            val updatedList = updatedEntities.map { it.toDownloadedTrack() }
            _downloadedTracksFlow.value = updatedList
            saveDownloads(updatedList)

            Log.i(TAG, "syncDownloadsWithDisk concluído. $reconciledCount itens reconciliados, ${updatedList.size} faixas offline.")
        } catch (e: Exception) {
            Log.e(TAG, "Erro durante syncDownloadsWithDisk: ${e.message}", e)
        }
        reconciledCount
    }

    private fun saveDownloads(downloads: List<DownloadedTrack>) {
        try {
            val raw = json.encodeToString(downloads)
            prefs.edit().putString(KEY_DOWNLOADS, raw).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao salvar downloads: ${e.message}", e)
        }
    }

    override fun createPlaylist(title: String, description: String, initialTracks: List<Track>): UserPlaylist {
        val newPlaylist = UserPlaylist(
            id = "user_pl_${UUID.randomUUID().toString().take(8)}",
            title = title.trim().ifBlank { "Minha Playlist" },
            description = description.trim(),
            createdAtEpochMs = System.currentTimeMillis(),
            tracks = initialTracks
        )
        val current = _playlistsFlow.value.toMutableList()
        current.add(0, newPlaylist)
        _playlistsFlow.value = current
        savePlaylists(current)
        return newPlaylist
    }

    override fun upsertImportedPlaylist(
        sourceKey: String,
        title: String,
        description: String,
        tracks: List<Track>
    ): UserPlaylist {
        val importedId = ImportedPlaylistIdentity.idFor(sourceKey)
        val current = _playlistsFlow.value.toMutableList()
        val normalizedTitle = title.trim()
        val sourceMarker = synchronizedImportMarker(sourceKey)
        val stableIndex = current.indexOfFirst { it.id == importedId }
        val legacyCandidates = if (stableIndex >= 0 || sourceMarker == null) {
            emptyList()
        } else {
            current.indices.filter { index ->
                val candidate = current[index]
                candidate.title.trim().equals(normalizedTitle, ignoreCase = true) &&
                    candidate.description.substringBefore('•').trim().equals(sourceMarker, ignoreCase = true)
            }
        }
        val incomingTrackIds = tracks.map { it.id }.toSet()
        val legacyIndex = when {
            legacyCandidates.isEmpty() -> -1
            else -> {
                val ranked = legacyCandidates.map { index ->
                    index to current[index].tracks.count { it.id in incomingTrackIds }
                }
                val highestOverlap = ranked.maxOf { it.second }
                val bestMatches = ranked.filter { it.second == highestOverlap }
                bestMatches.singleOrNull()?.takeIf { highestOverlap > 0 }?.first ?: -1
            }
        }
        val index = if (stableIndex >= 0) stableIndex else legacyIndex
        val playlist = if (index >= 0) {
            val existing = current[index]
            existing.copy(
                id = importedId,
                title = title.trim().ifBlank { existing.title },
                description = description.trim(),
                tracks = tracks,
                customThumbnailUrl = existing.customThumbnailUrl
            ).also { current[index] = it }
        } else {
            UserPlaylist(
                id = importedId,
                title = title.trim().ifBlank { "Playlist importada" },
                description = description.trim(),
                createdAtEpochMs = System.currentTimeMillis(),
                tracks = tracks
            ).also { current.add(0, it) }
        }

        // Old versions synced these same playlists with random local IDs. Once their stable
        // counterpart exists, remove only other same-source, same-title rows with identical
        // track IDs so a repeated sync does not leave a second copy in the library.
        val incomingTrackIdList = tracks.map { it.id }.sorted()
        val duplicateIndexes = current.indices.filter { duplicateIndex ->
            if (current[duplicateIndex].id == importedId || sourceMarker == null) return@filter false
            val candidate = current[duplicateIndex]
            candidate.title.trim().equals(playlist.title.trim(), ignoreCase = true) &&
                candidate.description.substringBefore('•').trim().equals(sourceMarker, ignoreCase = true) &&
                candidate.tracks.map { it.id }.sorted() == incomingTrackIdList
        }
        duplicateIndexes.sortedDescending().forEach(current::removeAt)
        if (legacyIndex >= 0 && legacyIndex != stableIndex) {
            Log.i(TAG, "Playlist importada existente adotada para sync estável: ${playlist.title}")
        }
        if (duplicateIndexes.isNotEmpty()) {
            Log.i(TAG, "${duplicateIndexes.size} cópia(s) antiga(s) idêntica(s) removida(s) no sync de ${playlist.title}")
        }

        _playlistsFlow.value = current
        savePlaylists(current)
        return playlist
    }

    private fun synchronizedImportMarker(sourceKey: String): String? = when {
        sourceKey.startsWith("youtube_music:") -> "Sincronizada do YouTube Music"
        sourceKey.startsWith("spotify:") -> "Sincronizada do Spotify"
        else -> null
    }

    override fun updatePlaylistTitle(playlistId: String, newTitle: String): Boolean {
        val current = _playlistsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == playlistId }
        if (index >= 0) {
            val pl = current[index]
            current[index] = pl.copy(title = newTitle.trim().ifBlank { pl.title })
            _playlistsFlow.value = current
            savePlaylists(current)
            return true
        }
        return false
    }

    override fun deletePlaylist(playlistId: String): Boolean {
        val current = _playlistsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == playlistId }
        if (index >= 0) {
            current.removeAt(index)
            _playlistsFlow.value = current
            savePlaylists(current)
            return true
        }
        return false
    }

    override fun addTrackToPlaylist(playlistId: String, track: Track): Boolean {
        val current = _playlistsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == playlistId }
        if (index >= 0) {
            val playlist = current[index]
            if (playlist.tracks.any { it.id == track.id }) return false
            val updatedTracks = playlist.tracks.toMutableList().apply { add(track) }
            current[index] = playlist.copy(tracks = updatedTracks)
            _playlistsFlow.value = current
            savePlaylists(current)
            @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
            GlobalScope.launch(Dispatchers.IO) {
                libraryTrackDao.markAdded(
                    LibraryTrackEntity.fromTrack(track),
                    System.currentTimeMillis(),
                    liked = _likedTracksFlow.value.any { it.id == track.id }
                )
            }
            return true
        }
        return false
    }

    override fun addTracksToPlaylist(playlistId: String, tracks: List<Track>): Int {
        val current = _playlistsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == playlistId }
        if (index >= 0) {
            val playlist = current[index]
            val existingIds = playlist.tracks.map { it.id }.toSet()
            val toAdd = tracks.filter { it.id !in existingIds }
            if (toAdd.isEmpty()) return 0
            val updatedTracks = playlist.tracks + toAdd
            current[index] = playlist.copy(tracks = updatedTracks)
            _playlistsFlow.value = current
            savePlaylists(current)
            val addedAt = System.currentTimeMillis()
            @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
            GlobalScope.launch(Dispatchers.IO) {
                toAdd.forEachIndexed { offset, track ->
                    libraryTrackDao.markAdded(
                        LibraryTrackEntity.fromTrack(track),
                        addedAt + offset,
                        liked = _likedTracksFlow.value.any { it.id == track.id }
                    )
                }
            }
            return toAdd.size
        }
        return 0
    }

    override fun removeTrackFromPlaylist(playlistId: String, trackId: String): Boolean {
        val current = _playlistsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == playlistId }
        if (index >= 0) {
            val playlist = current[index]
            val updatedTracks = playlist.tracks.filterNot { it.id == trackId }
            current[index] = playlist.copy(tracks = updatedTracks)
            _playlistsFlow.value = current
            savePlaylists(current)
            return true
        }
        return false
    }

    override fun isTrackInPlaylist(playlistId: String, trackId: String): Boolean {
        val playlist = _playlistsFlow.value.firstOrNull { it.id == playlistId } ?: return false
        return playlist.tracks.any { it.id == trackId }
    }

    private fun savePlaylists(playlists: List<UserPlaylist>) {
        try {
            val raw = json.encodeToString(playlists)
            prefs.edit().putString(KEY_PLAYLISTS, raw).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao salvar playlists: ${e.message}", e)
        }
    }

    private fun determineExtension(contentType: String?, url: String): String {
        val ct = contentType?.lowercase() ?: ""
        val u = url.lowercase()
        return when {
            ct.contains("audio/mp4") || ct.contains("audio/x-m4a") || ct.contains("audio/aac") ||
                    u.contains("mime=audio%2fmp4") || u.contains("mime=audio/mp4") || u.contains("itag=140") || u.contains("itag=139") -> "m4a"
            ct.contains("audio/webm") || ct.contains("audio/opus") || ct.contains("audio/ogg") ||
                    u.contains("mime=audio%2fwebm") || u.contains("mime=audio/webm") || u.contains("itag=251") || u.contains("itag=250") || u.contains("itag=249") -> "opus"
            else -> "opus"
        }
    }
}

private class Http200RangeUnsupportedException : java.io.IOException("Servidor retornou HTTP 200 em vez de 206 (Range não suportado)")

