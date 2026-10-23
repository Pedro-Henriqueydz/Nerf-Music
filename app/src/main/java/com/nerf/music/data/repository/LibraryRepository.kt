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

            // 2. Faz stream e gravação para arquivo local
            val destFile = File(downloadsDir, "${track.id}.opus")
            val request = Request.Builder()
                .url(streamUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(java.io.IOException("Falha HTTP ao baixar áudio: ${response.code}"))
            }

            val body = response.body ?: return@withContext Result.failure(java.io.IOException("Corpo de resposta nulo"))
            val totalBytes = body.contentLength()
            var downloadedBytes = 0L

            body.byteStream().use { input ->
                FileOutputStream(destFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        if (totalBytes > 0) {
                            val progress = downloadedBytes.toFloat() / totalBytes.toFloat()
                            onProgress(progress)
                        }
                    }
                    output.flush()
                }
            }

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
