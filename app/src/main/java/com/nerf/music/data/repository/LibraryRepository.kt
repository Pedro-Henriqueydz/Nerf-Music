package com.nerf.music.data.repository

import android.content.Context
import android.util.Log
import com.nerf.music.data.model.DownloadedTrack
import com.nerf.music.data.model.Track
import com.nerf.music.data.model.UserPlaylist
import com.nerf.music.player.NewPipeStreamResolver
import com.nerf.music.player.StreamResolver
import kotlinx.coroutines.Dispatchers
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
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.UUID

interface LibraryRepository {
    val likedTracksFlow: StateFlow<List<Track>>
    val playlistsFlow: StateFlow<List<UserPlaylist>>
    val downloadedTracksFlow: StateFlow<List<DownloadedTrack>>

    fun isFavorite(trackId: String): Boolean
    fun toggleFavorite(track: Track): Boolean

    fun isDownloaded(trackId: String): Boolean
    fun getDownloadedFilePath(trackId: String): String?
    suspend fun downloadTrack(track: Track, onProgress: (Float) -> Unit = {}): Result<DownloadedTrack>
    fun removeDownload(trackId: String): Boolean

    fun createPlaylist(title: String, description: String = ""): UserPlaylist
    fun deletePlaylist(playlistId: String): Boolean
    fun updatePlaylistTitle(playlistId: String, newTitle: String): Boolean
    fun addTrackToPlaylist(playlistId: String, track: Track): Boolean
    fun removeTrackFromPlaylist(playlistId: String, trackId: String): Boolean
    fun isTrackInPlaylist(playlistId: String, trackId: String): Boolean
}

class LibraryRepositoryImpl(
    private val context: Context,
    private val streamResolver: StreamResolver = NewPipeStreamResolver(),
    private val httpClient: OkHttpClient = OkHttpClient()
) : LibraryRepository {

    companion object {
        private const val TAG = "LibraryRepository"
        private const val PREFS_NAME = "nerf_library_prefs"
        private const val KEY_LIKED_TRACKS = "liked_tracks_json"
        private const val KEY_PLAYLISTS = "user_playlists_json"
        private const val KEY_DOWNLOADS = "downloaded_tracks_json"
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

    private val _likedTracksFlow = MutableStateFlow<List<Track>>(emptyList())
    override val likedTracksFlow: StateFlow<List<Track>> = _likedTracksFlow.asStateFlow()

    private val _playlistsFlow = MutableStateFlow<List<UserPlaylist>>(emptyList())
    override val playlistsFlow: StateFlow<List<UserPlaylist>> = _playlistsFlow.asStateFlow()

    private val _downloadedTracksFlow = MutableStateFlow<List<DownloadedTrack>>(emptyList())
    override val downloadedTracksFlow: StateFlow<List<DownloadedTrack>> = _downloadedTracksFlow.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        try {
            val rawLiked = prefs.getString(KEY_LIKED_TRACKS, null)
            if (!rawLiked.isNullOrBlank()) {
                _likedTracksFlow.value = json.decodeFromString(rawLiked)
            }

            val rawPlaylists = prefs.getString(KEY_PLAYLISTS, null)
            if (!rawPlaylists.isNullOrBlank()) {
                _playlistsFlow.value = json.decodeFromString(rawPlaylists)
            }

            val rawDownloads = prefs.getString(KEY_DOWNLOADS, null)
            if (!rawDownloads.isNullOrBlank()) {
                val list = json.decodeFromString<List<DownloadedTrack>>(rawDownloads)
                // Valida se os arquivos locais ainda existem no disco
                val existing = list.filter { File(it.localFilePath).exists() }
                _downloadedTracksFlow.value = existing
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao carregar dados da biblioteca: ${e.message}", e)
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
        return added
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
        val dt = _downloadedTracksFlow.value.firstOrNull { it.track.id == trackId } ?: return false
        return File(dt.localFilePath).exists()
    }

    override fun getDownloadedFilePath(trackId: String): String? {
        val dt = _downloadedTracksFlow.value.firstOrNull { it.track.id == trackId } ?: return null
        val f = File(dt.localFilePath)
        return if (f.exists()) dt.localFilePath else null
    }

    override suspend fun downloadTrack(
        track: Track,
        onProgress: (Float) -> Unit
    ): Result<DownloadedTrack> = withContext(Dispatchers.IO) {
        if (track.id.isBlank()) return@withContext Result.failure(IllegalArgumentException("TrackId vazio"))

        try {
            // 1. Resolve URL de áudio JIT via NewPipe
            val streamUrl = streamResolver.resolveStreamUrl(track.id).getOrThrow()

            // 2. Determina o tamanho total do áudio (Content-Length / Content-Range)
            val streamUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
            var totalBytes = -1L

            try {
                val headReq = Request.Builder()
                    .url(streamUrl)
                    .head()
                    .header("User-Agent", streamUserAgent)
                    .build()
                httpClient.newCall(headReq).execute().use { resp ->
                    totalBytes = resp.header("Content-Length")?.toLongOrNull() ?: -1L
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
                    }
                } catch (_: Exception) {}
            }

            val destFile = File(downloadsDir, "${track.id}.opus")
            val tempFile = File(downloadsDir, "${track.id}.tmp")
            if (tempFile.exists()) tempFile.delete()

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
                                    if (!chunkResp.isSuccessful && chunkResp.code != 206) {
                                        throw java.io.IOException("Falha HTTP ${chunkResp.code} no chunk $chunkStart-$chunkEnd")
                                    }
                                    val body = chunkResp.body ?: throw java.io.IOException("Corpo de resposta nulo")
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
                } finally {
                    try { raf.close() } catch (_: Exception) {}
                }
            } else {
                // Fallback caso Content-Length/Range não esteja disponível
                val request = Request.Builder()
                    .url(streamUrl)
                    .header("User-Agent", streamUserAgent)
                    .build()
                val response = httpClient.newCall(request).execute()
                val body = response.body ?: throw java.io.IOException("Corpo nulo")
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

            onProgress(1f)
            if (destFile.exists()) destFile.delete()
            tempFile.renameTo(destFile)

            val downloadedTrack = DownloadedTrack(
                track = track,
                localFilePath = destFile.absolutePath,
                downloadedAtEpochMs = System.currentTimeMillis(),
                fileSizeBytes = destFile.length()
            )

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
        if (index >= 0) {
            val removed = current.removeAt(index)
            val f = File(removed.localFilePath)
            if (f.exists()) f.delete()
            _downloadedTracksFlow.value = current
            saveDownloads(current)
            return true
        }
        return false
    }

    private fun saveDownloads(downloads: List<DownloadedTrack>) {
        try {
            val raw = json.encodeToString(downloads)
            prefs.edit().putString(KEY_DOWNLOADS, raw).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao salvar downloads: ${e.message}", e)
        }
    }

    override fun createPlaylist(title: String, description: String): UserPlaylist {
        val newPlaylist = UserPlaylist(
            id = "user_pl_${UUID.randomUUID().toString().take(8)}",
            title = title.trim().ifBlank { "Minha Playlist" },
            description = description.trim(),
            createdAtEpochMs = System.currentTimeMillis(),
            tracks = emptyList()
        )
        val current = _playlistsFlow.value.toMutableList()
        current.add(0, newPlaylist)
        _playlistsFlow.value = current
        savePlaylists(current)
        return newPlaylist
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
            return true
        }
        return false
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
}
