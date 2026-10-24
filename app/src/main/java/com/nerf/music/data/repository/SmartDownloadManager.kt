package com.nerf.music.data.repository

import android.content.Context
import android.util.Log
import com.nerf.music.data.model.DownloadedTrack
import com.nerf.music.data.model.Track
import com.nerf.music.util.NetworkUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class SmartDownloadSyncState(
    val isSyncing: Boolean = false,
    val targetQuota: Int = 100,
    val currentDownloadedCount: Int = 0,
    val currentSmartDownloadedCount: Int = 0,
    val currentTrackTitle: String? = null,
    val currentTrackProgress: Float = 0f,
    val lastSyncTimestamp: Long = 0L,
    val lastErrorMessage: String? = null
)

/**
 * Gerenciador dos Downloads Inteligentes (Smart Downloads estilo YouTube Music).
 *
 * Algoritmo e Regras:
 * 1. Prioridade Hierárquica:
 *    - 40% Faixas Mais Ouvidas Recentemente + Músicas Curtidas
 *    - 30% Músicas de Playlists do Usuário
 *    - 30% Recomendações e Descobertas
 * 2. Cota Configurável: 50, 100, 250, 500 músicas.
 * 3. Rotação Automática: Descarte inteligente apenas de smart downloads antigos quando a cota
 *    for reduzida ou novos gostos do usuário surgirem (Downloads manuais NUNCA são apagados).
 * 4. Padrão Lossless: Baixa as faixas na máxima fidelidade definida nas configurações.
 */
class SmartDownloadManager(
    private val context: Context,
    private val libraryRepository: LibraryRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val userHistoryRepository: UserHistoryRepository,
    private val userAffinityRepository: UserAffinityRepository = UserAffinityRepositoryImpl(context),
    private val catalogClient: com.nerf.music.data.network.InnerTubeCatalogClient = com.nerf.music.data.network.InnerTubeCatalogClient()
) {

    // Mutex de sincronizacao em nivel de processo
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _syncState = MutableStateFlow(
        SmartDownloadSyncState(
            targetQuota = userSettingsRepository.smartDownloadsLimit.value,
            currentDownloadedCount = libraryRepository.downloadedTracksFlow.value.size,
            currentSmartDownloadedCount = libraryRepository.downloadedTracksFlow.value.count { it.isSmartDownload }
        )
    )
    val syncState: StateFlow<SmartDownloadSyncState> = _syncState.asStateFlow()

    init {
        // Observa alterações na biblioteca para atualizar estatísticas em tempo real
        scope.launch {
            libraryRepository.downloadedTracksFlow.collect { downloads ->
                _syncState.value = _syncState.value.copy(
                    currentDownloadedCount = downloads.size,
                    currentSmartDownloadedCount = downloads.count { it.isSmartDownload }
                )
            }
        }
    }

    /**
     * Executa a sincronização dos Downloads Inteligentes.
     * Retorna o número de novas músicas baixadas.
     */
    suspend fun syncNow(force: Boolean = false): Result<Int> = withContext(Dispatchers.IO) {
        if (!globalSyncMutex.tryLock()) {
            Log.d(TAG, "Sincronização de Smart Downloads já em andamento. Ignorando chamada concorrente.")
            return@withContext Result.success(0)
        }

        try {
            if (!userSettingsRepository.isSmartDownloadsEnabled.value && !force) {
                Log.d(TAG, "Smart Downloads desativado pelo usuário.")
                return@withContext Result.success(0)
            }

            val isWifi = NetworkUtils.isConnectedToWifi(context)
            val wifiOnly = userSettingsRepository.isSmartDownloadsWifiOnly.value
            val allowCellular = userSettingsRepository.isDownloadOverCellular.value

            if (wifiOnly && !isWifi) {
                val msg = "Smart Downloads pausado: aguardando conexão Wi-Fi."
                Log.i(TAG, msg)
                _syncState.value = _syncState.value.copy(lastErrorMessage = msg)
                return@withContext Result.failure(IllegalStateException(msg))
            } else if (!isWifi && !allowCellular) {
                val msg = "Smart Downloads pausado: downloads via dados móveis desativados."
                Log.i(TAG, msg)
                _syncState.value = _syncState.value.copy(lastErrorMessage = msg)
                return@withContext Result.failure(IllegalStateException(msg))
            }

            val targetQuota = userSettingsRepository.smartDownloadsLimit.value.coerceIn(10, 1000)

            _syncState.value = _syncState.value.copy(
                isSyncing = true,
                targetQuota = targetQuota,
                lastErrorMessage = null
            )

            // 1. Sintetiza a lista ponderada de candidatos
            val candidates = synthesizeCandidates(targetQuota)
            if (candidates.isEmpty()) {
                Log.d(TAG, "Nenhum candidato a Smart Download encontrado.")
                _syncState.value = _syncState.value.copy(
                    isSyncing = false,
                    lastSyncTimestamp = System.currentTimeMillis()
                )
                return@withContext Result.success(0)
            }

            val candidateIds = candidates.map { it.id }.toSet()
            val existingDownloads = libraryRepository.downloadedTracksFlow.value
            val existingIds = existingDownloads.map { it.track.id }.toSet()

            // 2. Rotação: Remove smart downloads obsoletos se exceder a cota
            val currentSmartDownloads = existingDownloads.filter { it.isSmartDownload }
            val smartDownloadsToKeep = currentSmartDownloads.filter { candidateIds.contains(it.track.id) }
            val smartDownloadsToEvict = currentSmartDownloads.filterNot { candidateIds.contains(it.track.id) }

            // Se o total mantido + futuros ultrapassar a cota, libera os mais antigos
            val availableQuota = (targetQuota - smartDownloadsToKeep.size).coerceAtLeast(0)
            if (currentSmartDownloads.size > targetQuota) {
                val excess = currentSmartDownloads.size - targetQuota
                val evictList = smartDownloadsToEvict.sortedBy { it.downloadedAtEpochMs }.take(excess)
                for (evicted in evictList) {
                    Log.i(TAG, "Rotacionando smart download obsoleto: ${evicted.track.title} (${evicted.track.id})")
                    libraryRepository.removeDownload(evicted.track.id)
                }
            }

            // 3. Seleciona faixas que ainda não foram baixadas
            val toDownload = candidates.filter { !existingIds.contains(it.id) }.take(availableQuota)
            var downloadedCount = 0

            Log.i(TAG, "Iniciando download de ${toDownload.size} faixas para Smart Downloads (Cota: $targetQuota)...")

            for ((index, track) in toDownload.withIndex()) {
                if (!userSettingsRepository.isSmartDownloadsEnabled.value) {
                    Log.i(TAG, "Sincronização interrompida: usuário desativou Smart Downloads.")
                    break
                }

                _syncState.value = _syncState.value.copy(
                    currentTrackTitle = track.title,
                    currentTrackProgress = 0f
                )

                val result = libraryRepository.downloadTrack(
                    track = track,
                    isSmartDownload = true,
                    onProgress = { progress ->
                        _syncState.value = _syncState.value.copy(
                            currentTrackProgress = progress
                        )
                    }
                )

                if (result.isSuccess) {
                    downloadedCount++
                    Log.d(TAG, "Smart Download concluído (${index + 1}/${toDownload.size}): ${track.title}")
                } else {
                    Log.w(TAG, "Falha ao baixar faixa inteligente ${track.title}: ${result.exceptionOrNull()?.message}")
                }
            }

            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                currentTrackTitle = null,
                currentTrackProgress = 0f,
                lastSyncTimestamp = System.currentTimeMillis()
            )

            Result.success(downloadedCount)
        } catch (e: CancellationException) {
            _syncState.value = _syncState.value.copy(isSyncing = false)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Erro durante Smart Downloads sync: ${e.message}", e)
            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                lastErrorMessage = e.message
            )
            Result.failure(e)
        } finally {
            globalSyncMutex.unlock()
        }
    }

    /**
     * Sintetiza a lista de candidatos a Smart Download respeitando a ponderação premium:
     * - 40% Favoritos e Mais Ouvidos
     * - 30% Playlists do Usuário
     * - 30% Descoberta e Afinidade
     */
    private suspend fun synthesizeCandidates(limit: Int): List<Track> {
        val liked = libraryRepository.likedTracksFlow.value
        val recent = userHistoryRepository.getRecentlyPlayed()
        val playlistsTracks = libraryRepository.playlistsFlow.value.flatMap { it.tracks }

        val candidates = mutableListOf<Track>()
        val seenIds = mutableSetOf<String>()

        fun addTrack(track: Track) {
            if (track.id.isNotBlank() && !seenIds.contains(track.id) && !userAffinityRepository.isTrackPenalized(track.id)) {
                seenIds.add(track.id)
                candidates.add(track)
            }
        }

        // 1. Favoritas e Mais Ouvidas (Recent) - ~40%
        val topLikedLimit = (limit * 0.40).toInt().coerceAtLeast(10)
        liked.take(topLikedLimit).forEach { addTrack(it) }
        recent.take(topLikedLimit).forEach { addTrack(it) }

        // 2. Playlists do Usuário - ~30%
        val playlistLimit = (limit * 0.30).toInt().coerceAtLeast(10)
        playlistsTracks.take(playlistLimit).forEach { addTrack(it) }

        // 3. Restante de favoritos, histórico e playlists locais
        liked.drop(topLikedLimit).forEach { addTrack(it) }
        recent.drop(topLikedLimit).forEach { addTrack(it) }
        playlistsTracks.drop(playlistLimit).forEach { addTrack(it) }

        // 4. Expansão Online estilo YouTube Music: Se o acervo local do usuário for menor que a cota,
        // consulta a API de rádio e recomendações (Up Next) usando as faixas favoritas como sementes
        if (candidates.size < limit) {
            var seeds = (liked + recent).filter { it.id.isNotBlank() }.distinctBy { it.id }
            if (seeds.isEmpty()) {
                seeds = listOf(
                    Track(id = "kJQP7kiw5Fk", title = "Despacito", artist = "Luis Fonsi"),
                    Track(id = "JGwWNGJdvx8", title = "Shape of You", artist = "Ed Sheeran")
                )
            }

            Log.i(TAG, "Candidatos locais (${candidates.size}) abaixo da cota ($limit). Expandindo via catálogo com ${seeds.size} sementes...")

            for (seed in seeds) {
                if (candidates.size >= limit) break
                try {
                    val radioResult = catalogClient.getUpNextRadio(seed.id).getOrNull()
                    val recommendations = radioResult?.tracks.orEmpty()
                    for (rec in recommendations) {
                        if (candidates.size >= limit) break
                        addTrack(rec)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Falha ao consultar rádio de recomendações para semente ${seed.title}: ${e.message}")
                }
            }
        }

        return candidates.take(limit)
    }

    companion object {
        private const val TAG = "SmartDownloadManager"
        private val globalSyncMutex = Mutex()

        /**
         * Estimativa de consumo em disco para N músicas em qualidade Lossless/Máxima Fidelidade (~5.2MB por faixa).
         */
        fun estimateStorageBytes(trackCount: Int): Long {
            return trackCount * 5_450_000L // ~5.2 MB por música Opus/AAC estéreo
        }

        fun formatStorageEstimate(trackCount: Int): String {
            val bytes = estimateStorageBytes(trackCount)
            val mb = bytes / (1024 * 1024)
            return if (mb >= 1000) {
                String.format(java.util.Locale.US, "%.1f GB", mb / 1024.0)
            } else {
                "$mb MB"
            }
        }
    }
}
