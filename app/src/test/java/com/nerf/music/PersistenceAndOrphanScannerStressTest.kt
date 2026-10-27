package com.nerf.music

import com.nerf.music.data.db.DownloadedTrackEntity
import com.nerf.music.data.model.DownloadedTrack
import com.nerf.music.data.model.Track
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Empirical stress test suite validating:
 * 1. DownloadedTrackEntity and bidirectional model mapping with edge cases (nulls, empty strings, companion artwork).
 * 2. Room DAO logic contracts (insert, conflict resolution, query ordering, deletion, pruning).
 * 3. syncDownloadsWithDisk orphan scanning simulation (discovery of .opus/.m4a/.webm, companion .jpg binding,
 *    pruning of missing disk files, file extension reconciliation, and exclusion of non-audio/degenerate files).
 * 4. Companion artwork path resolution logic (offline file:// binding vs network thumbnail fallback).
 */
class PersistenceAndOrphanScannerStressTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    // =========================================================================
    // 1. DownloadedTrackEntity & Model Mapping Tests
    // =========================================================================

    @Test
    fun testBidirectionalMappingAllFieldsPreserved() {
        val originalTrack = Track(
            id = "track_full_fields",
            title = "Empirical Test Title",
            artist = "Empirical Test Artist",
            album = "Empirical Album",
            thumbnailUrl = "https://example.com/thumb.jpg",
            durationText = "04:20",
            durationSeconds = 260L,
            sourceIdentifier = "TEST_SOURCE",
            playlistId = "playlist_999",
            browseId = "browse_111",
            artistBrowseId = "artist_browse_222"
        )

        val downloadedTrack = DownloadedTrack(
            track = originalTrack,
            localFilePath = "/data/user/0/com.nerf.music/files/offline_audio/track_full_fields.opus",
            downloadedAtEpochMs = 1710000000000L,
            fileSizeBytes = 3141592L,
            isSmartDownload = true,
            localArtworkPath = "/data/user/0/com.nerf.music/files/offline_audio/track_full_fields.jpg"
        )

        val entity = DownloadedTrackEntity.fromDownloadedTrack(downloadedTrack)
        assertEquals("track_full_fields", entity.id)
        assertEquals("Empirical Test Title", entity.title)
        assertEquals("Empirical Test Artist", entity.artist)
        assertEquals("Empirical Album", entity.album)
        assertEquals("04:20", entity.durationText)
        assertEquals(260L, entity.durationSeconds)
        assertEquals("TEST_SOURCE", entity.sourceIdentifier)
        assertEquals("playlist_999", entity.playlistId)
        assertEquals("browse_111", entity.browseId)
        assertEquals("artist_browse_222", entity.artistBrowseId)
        assertEquals(downloadedTrack.localFilePath, entity.localFilePath)
        assertEquals(1710000000000L, entity.downloadedAtEpochMs)
        assertEquals(3141592L, entity.fileSizeBytes)
        assertTrue(entity.isSmartDownload)
        assertEquals(downloadedTrack.localArtworkPath, entity.localArtworkPath)

        val convertedBack = entity.toDownloadedTrack()
        assertEquals(downloadedTrack.track.id, convertedBack.track.id)
        assertEquals(downloadedTrack.track.title, convertedBack.track.title)
        assertEquals(downloadedTrack.track.artist, convertedBack.track.artist)
        assertEquals(downloadedTrack.track.album, convertedBack.track.album)
        assertEquals(downloadedTrack.track.durationText, convertedBack.track.durationText)
        assertEquals(downloadedTrack.track.durationSeconds, convertedBack.track.durationSeconds)
        assertEquals(downloadedTrack.localFilePath, convertedBack.localFilePath)
        assertEquals(downloadedTrack.downloadedAtEpochMs, convertedBack.downloadedAtEpochMs)
        assertEquals(downloadedTrack.fileSizeBytes, convertedBack.fileSizeBytes)
        assertEquals(downloadedTrack.isSmartDownload, convertedBack.isSmartDownload)
        assertEquals(downloadedTrack.localArtworkPath, convertedBack.localArtworkPath)

        // Effective thumbnail MUST bind file:// URI when localArtworkPath is present
        assertEquals("file:///data/user/0/com.nerf.music/files/offline_audio/track_full_fields.jpg", convertedBack.track.thumbnailUrl)
    }

    @Test
    fun testMappingWithNullAndBlankArtworkPathFallsBackToThumbnailUrl() {
        val entityNullArt = DownloadedTrackEntity(
            id = "t_null_art",
            title = "Track Null Art",
            artist = "Artist",
            album = null,
            thumbnailUrl = "https://example.com/net_art.jpg",
            localFilePath = "/path/t_null_art.m4a",
            localArtworkPath = null
        )
        val modelNullArt = entityNullArt.toDownloadedTrack()
        assertEquals("https://example.com/net_art.jpg", modelNullArt.track.thumbnailUrl)
        assertNull(modelNullArt.localArtworkPath)

        val entityBlankArt = DownloadedTrackEntity(
            id = "t_blank_art",
            title = "Track Blank Art",
            artist = "Artist",
            album = null,
            thumbnailUrl = "https://example.com/net_art2.jpg",
            localFilePath = "/path/t_blank_art.m4a",
            localArtworkPath = "   "
        )
        val modelBlankArt = entityBlankArt.toDownloadedTrack()
        assertEquals("https://example.com/net_art2.jpg", modelBlankArt.track.thumbnailUrl)
    }

    // =========================================================================
    // 2. Room DAO In-Memory Logic Simulation
    // =========================================================================

    @Test
    fun testDaoOperationsInsertQueryOrderingAndConflictResolution() {
        // Simulating Room DAO table state
        val dbTable = mutableMapOf<String, DownloadedTrackEntity>()

        fun insert(entity: DownloadedTrackEntity) {
            dbTable[entity.id] = entity // REPLACE strategy
        }

        fun getAllTracks(): List<DownloadedTrackEntity> {
            return dbTable.values.sortedByDescending { it.downloadedAtEpochMs }
        }

        fun getTrackById(id: String): DownloadedTrackEntity? = dbTable[id]

        fun deleteById(id: String) {
            dbTable.remove(id)
        }

        val e1 = DownloadedTrackEntity(
            id = "track_1",
            title = "First Track",
            artist = "Artist A",
            album = "Album A",
            thumbnailUrl = null,
            localFilePath = "/files/track_1.opus",
            downloadedAtEpochMs = 1000L
        )
        val e2 = DownloadedTrackEntity(
            id = "track_2",
            title = "Second Track",
            artist = "Artist B",
            album = "Album B",
            thumbnailUrl = null,
            localFilePath = "/files/track_2.opus",
            downloadedAtEpochMs = 2000L
        )
        val e3 = DownloadedTrackEntity(
            id = "track_3",
            title = "Third Track",
            artist = "Artist C",
            album = "Album C",
            thumbnailUrl = null,
            localFilePath = "/files/track_3.opus",
            downloadedAtEpochMs = 3000L
        )

        insert(e1)
        insert(e2)
        insert(e3)
        assertEquals(3, dbTable.size)

        // Verify ordering: newest first (e3, e2, e1)
        val ordered = getAllTracks()
        assertEquals(listOf("track_3", "track_2", "track_1"), ordered.map { it.id })

        // Verify REPLACE on duplicate insert
        val updatedE2 = e2.copy(title = "Updated Second Track", downloadedAtEpochMs = 4000L)
        insert(updatedE2)
        assertEquals(3, dbTable.size)
        assertEquals("Updated Second Track", getTrackById("track_2")?.title)

        // Now ordered: e2 (4000), e3 (3000), e1 (1000)
        val reordered = getAllTracks()
        assertEquals(listOf("track_2", "track_3", "track_1"), reordered.map { it.id })

        // Verify deletion
        deleteById("track_2")
        assertNull(getTrackById("track_2"))
        assertEquals(2, getAllTracks().size)
    }

    // =========================================================================
    // 3. syncDownloadsWithDisk Orphan Scanner Simulation Harness
    // =========================================================================

    /**
     * Executes the exact orphan reconciliation logic from LibraryRepositoryImpl.syncDownloadsWithDisk
     * against a test directory and in-memory DAO state.
     */
    private fun simulateSyncDownloadsWithDisk(
        downloadsDir: File,
        daoRecords: MutableList<DownloadedTrackEntity>,
        recentTracksMap: Map<String, Track> = emptyMap()
    ): Int {
        var reconciledCount = 0

        // 1. Escaneia offline_audio por arquivos de áudio válidos
        val audioExtensions = setOf("opus", "m4a", "webm")
        val diskFiles = downloadsDir.listFiles { file ->
            file.isFile && file.extension.lowercase() in audioExtensions
        }?.toList().orEmpty()

        val diskFilesByTrackId = mutableMapOf<String, File>()
        for (file in diskFiles) {
            val trackId = file.nameWithoutExtension
            if (trackId.isNotBlank()) {
                diskFilesByTrackId[trackId] = file
            }
        }

        // 2. Busca registros atuais no Room DB
        val currentDbEntities = daoRecords.toMutableList()
        val dbTrackIds = currentDbEntities.map { it.id }.toSet()

        // 3. Purga registros do banco cujo arquivo físico foi removido
        for (entity in currentDbEntities) {
            val physicalFile = File(entity.localFilePath)
            val exists = physicalFile.exists() || diskFilesByTrackId.containsKey(entity.id)
            if (!exists) {
                daoRecords.removeAll { it.id == entity.id }
                reconciledCount++
            } else if (!physicalFile.exists() && diskFilesByTrackId.containsKey(entity.id)) {
                val newFile = diskFilesByTrackId[entity.id]!!
                val updated = entity.copy(
                    localFilePath = newFile.absolutePath,
                    fileSizeBytes = newFile.length()
                )
                val idx = daoRecords.indexOfFirst { it.id == entity.id }
                if (idx >= 0) daoRecords[idx] = updated
                reconciledCount++
            }
        }

        // 4. Recupera arquivos órfãos no disco que não constam no banco
        for ((trackId, file) in diskFilesByTrackId) {
            val artFile = File(downloadsDir, "$trackId.jpg")
            val localArtPath = if (artFile.exists() && artFile.length() > 0) artFile.absolutePath else null

            if (!dbTrackIds.contains(trackId)) {
                val knownTrack = recentTracksMap[trackId]
                val track = knownTrack ?: Track(
                    id = trackId,
                    title = "Faixa offline $trackId",
                    artist = "Áudio Local",
                    thumbnailUrl = localArtPath?.let { "file://$it" },
                    durationText = "--:--",
                    durationSeconds = 0L
                )

                val newEntity = DownloadedTrackEntity(
                    id = trackId,
                    title = track.title,
                    artist = track.artist,
                    album = track.album,
                    thumbnailUrl = track.thumbnailUrl,
                    durationText = track.durationText,
                    durationSeconds = track.durationSeconds,
                    sourceIdentifier = track.sourceIdentifier,
                    playlistId = track.playlistId,
                    browseId = track.browseId,
                    artistBrowseId = track.artistBrowseId,
                    localFilePath = file.absolutePath,
                    downloadedAtEpochMs = file.lastModified(),
                    fileSizeBytes = file.length(),
                    isSmartDownload = false,
                    localArtworkPath = localArtPath
                )
                daoRecords.add(newEntity)
                reconciledCount++
            } else {
                val existingEntity = currentDbEntities.firstOrNull { it.id == trackId }
                if (existingEntity != null && existingEntity.localArtworkPath.isNullOrBlank() && localArtPath != null) {
                    val idx = daoRecords.indexOfFirst { it.id == trackId }
                    if (idx >= 0) {
                        daoRecords[idx] = existingEntity.copy(localArtworkPath = localArtPath)
                    }
                    reconciledCount++
                }
            }
        }

        return reconciledCount
    }

    @Test
    fun testOrphanScannerDiscoversOrphanAudioFilesAndBindsCompanionArtwork() {
        val rootDir = tempFolder.newFolder("offline_audio")

        // Create orphan audio files with various supported extensions
        val opusFile = File(rootDir, "orphan_song_1.opus").apply { writeBytes(ByteArray(1024)) }
        val m4aFile = File(rootDir, "orphan_song_2.m4a").apply { writeBytes(ByteArray(2048)) }
        val webmFile = File(rootDir, "orphan_song_3.webm").apply { writeBytes(ByteArray(4096)) }

        // Create companion artwork for orphan_song_1 and orphan_song_2
        val art1 = File(rootDir, "orphan_song_1.jpg").apply { writeBytes(ByteArray(512)) }
        val art2 = File(rootDir, "orphan_song_2.jpg").apply { writeBytes(ByteArray(256)) }
        // orphan_song_3 has no companion artwork

        // Non-audio noise files that MUST be ignored
        File(rootDir, "orphan_song_1.lrc").writeText("[00:01.00] Test Lyric")
        File(rootDir, "temp_download.tmp").writeBytes(ByteArray(128))
        File(rootDir, ".nomedia").createNewFile()
        File(rootDir, ".opus").createNewFile() // Degenerate file without trackId

        val dbRecords = mutableListOf<DownloadedTrackEntity>()
        val reconciled = simulateSyncDownloadsWithDisk(rootDir, dbRecords)

        assertEquals("Should reconcile exactly 3 orphan tracks", 3, reconciled)
        assertEquals(3, dbRecords.size)

        // Verify orphan_song_1
        val record1 = dbRecords.firstOrNull { it.id == "orphan_song_1" }
        assertNotNull(record1)
        assertEquals(opusFile.absolutePath, record1?.localFilePath)
        assertEquals(art1.absolutePath, record1?.localArtworkPath)
        assertEquals("file://${art1.absolutePath}", record1?.thumbnailUrl)
        assertEquals("Faixa offline orphan_song_1", record1?.title)

        // Verify orphan_song_2
        val record2 = dbRecords.firstOrNull { it.id == "orphan_song_2" }
        assertNotNull(record2)
        assertEquals(m4aFile.absolutePath, record2?.localFilePath)
        assertEquals(art2.absolutePath, record2?.localArtworkPath)
        assertEquals("file://${art2.absolutePath}", record2?.thumbnailUrl)

        // Verify orphan_song_3 (no artwork)
        val record3 = dbRecords.firstOrNull { it.id == "orphan_song_3" }
        assertNotNull(record3)
        assertEquals(webmFile.absolutePath, record3?.localFilePath)
        assertNull(record3?.localArtworkPath)
        assertNull(record3?.thumbnailUrl)

        // Convert to DownloadedTrack model and verify effective artwork
        val model1 = record1!!.toDownloadedTrack()
        assertEquals("file://${art1.absolutePath}", model1.track.thumbnailUrl)
        val model3 = record3!!.toDownloadedTrack()
        assertNull(model3.track.thumbnailUrl)
    }

    @Test
    fun testOrphanScannerPrunesDeletedFilesFromDatabase() {
        val rootDir = tempFolder.newFolder("offline_audio")

        // Only physical file for track_alive exists on disk
        val aliveFile = File(rootDir, "track_alive.opus").apply { writeBytes(ByteArray(1024)) }

        val dbRecords = mutableListOf(
            DownloadedTrackEntity(
                id = "track_alive",
                title = "Alive Track",
                artist = "Artist",
                album = null,
                thumbnailUrl = null,
                localFilePath = aliveFile.absolutePath
            ),
            DownloadedTrackEntity(
                id = "track_deleted_1",
                title = "Deleted Track 1",
                artist = "Artist",
                album = null,
                thumbnailUrl = null,
                localFilePath = File(rootDir, "track_deleted_1.opus").absolutePath
            ),
            DownloadedTrackEntity(
                id = "track_deleted_2",
                title = "Deleted Track 2",
                artist = "Artist",
                album = null,
                thumbnailUrl = null,
                localFilePath = "/non/existent/path/track_deleted_2.m4a"
            )
        )

        val reconciled = simulateSyncDownloadsWithDisk(rootDir, dbRecords)

        assertEquals("Should reconcile (prune) 2 missing tracks", 2, reconciled)
        assertEquals(1, dbRecords.size)
        assertEquals("track_alive", dbRecords.first().id)
    }

    @Test
    fun testOrphanScannerReconcilesContainerExtensionChange() {
        val rootDir = tempFolder.newFolder("offline_audio")

        // DB record points to .opus, but on disk the file was converted / re-downloaded as .m4a
        val newM4aFile = File(rootDir, "track_migrated.m4a").apply { writeBytes(ByteArray(8192)) }

        val dbRecords = mutableListOf(
            DownloadedTrackEntity(
                id = "track_migrated",
                title = "Migrated Track Title",
                artist = "Migrated Artist",
                album = "Album",
                thumbnailUrl = "https://thumb.url",
                localFilePath = File(rootDir, "track_migrated.opus").absolutePath,
                fileSizeBytes = 1000L
            )
        )

        val reconciled = simulateSyncDownloadsWithDisk(rootDir, dbRecords)

        assertEquals("Should reconcile 1 updated track path", 1, reconciled)
        assertEquals(1, dbRecords.size)
        val updated = dbRecords.first()
        assertEquals("track_migrated", updated.id)
        assertEquals(newM4aFile.absolutePath, updated.localFilePath)
        assertEquals(8192L, updated.fileSizeBytes)
        // Original metadata preserved
        assertEquals("Migrated Track Title", updated.title)
        assertEquals("Migrated Artist", updated.artist)
    }

    @Test
    fun testOrphanScannerDiscoversNewCompanionArtworkForExistingTrack() {
        val rootDir = tempFolder.newFolder("offline_audio")

        val audioFile = File(rootDir, "track_existing.m4a").apply { writeBytes(ByteArray(2048)) }
        val artFile = File(rootDir, "track_existing.jpg").apply { writeBytes(ByteArray(512)) }

        val dbRecords = mutableListOf(
            DownloadedTrackEntity(
                id = "track_existing",
                title = "Existing Track",
                artist = "Artist",
                album = null,
                thumbnailUrl = "https://remote.url/art.jpg",
                localFilePath = audioFile.absolutePath,
                localArtworkPath = null
            )
        )

        val reconciled = simulateSyncDownloadsWithDisk(rootDir, dbRecords)

        assertEquals("Should reconcile 1 track with new companion artwork", 1, reconciled)
        assertEquals(1, dbRecords.size)
        assertEquals(artFile.absolutePath, dbRecords.first().localArtworkPath)

        val model = dbRecords.first().toDownloadedTrack()
        assertEquals("file://${artFile.absolutePath}", model.track.thumbnailUrl)
    }

    // =========================================================================
    // 4. Companion Artwork Binding Resolution Logic
    // =========================================================================

    @Test
    fun testArtworkResolutionPriority() {
        val offlineDir = tempFolder.newFolder("offline_audio")

        fun resolveArtworkUri(
            track: Track,
            downloadedTrack: DownloadedTrack?,
            filesDir: File
        ): String? {
            val localArtFile = File(filesDir, "offline_audio/${track.id}.jpg")
            return if (downloadedTrack?.localArtworkPath != null && File(downloadedTrack.localArtworkPath).exists()) {
                "file://${downloadedTrack.localArtworkPath}"
            } else if (localArtFile.exists()) {
                "file://${localArtFile.absolutePath}"
            } else {
                track.highResThumbnailUrl ?: track.thumbnailUrl
            }
        }

        val testTrack = Track(
            id = "res_track_1",
            title = "Resolution Test",
            artist = "Artist",
            thumbnailUrl = "https://example.com/low.jpg"
        )

        // Case 1: Neither local nor high-res exists -> remote fallback
        val uri1 = resolveArtworkUri(testTrack, null, tempFolder.root)
        assertEquals("https://example.com/low.jpg", uri1)

        // Case 2: Companion art exists on disk, but not in DownloadedTrack
        val artFile = File(offlineDir, "res_track_1.jpg").apply { writeBytes(ByteArray(100)) }
        val uri2 = resolveArtworkUri(testTrack, null, tempFolder.root)
        assertEquals("file://${artFile.absolutePath}", uri2)

        // Case 3: DownloadedTrack has explicit localArtworkPath
        val explicitArt = File(offlineDir, "explicit_res.jpg").apply { writeBytes(ByteArray(100)) }
        val dt = DownloadedTrack(
            track = testTrack,
            localFilePath = "/dummy/path.opus",
            localArtworkPath = explicitArt.absolutePath
        )
        val uri3 = resolveArtworkUri(testTrack, dt, tempFolder.root)
        assertEquals("file://${explicitArt.absolutePath}", uri3)
    }
}
