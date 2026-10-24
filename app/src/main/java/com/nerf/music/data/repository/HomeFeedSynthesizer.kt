package com.nerf.music.data.repository

import android.content.Context
import android.util.Log
import com.nerf.music.data.model.*
import com.nerf.music.data.network.InnerTubeCatalogClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Orquestrador do feed dinâmico da Home do NERF Music.
 *
 * Constrói a tela inicial viva nos moldes da versão oficial do YouTube Music 9.41:
 * 1. Speed Dial 3x3 paginado (9 itens por página com dots, playlists do usuário e curtidas).
 * 2. Seção contextual de dia e horário ("Churrasco & Fim de semana", "Energia", "Foco", "Relax").
 * 3. Your Downloads com os cartões gradientes oficiais (Liked Music, Offline Mix).
 * 4. Covers e remixes reais (versões slowed, sped-up, remixes e VIP edits).
 * 5. Hero Card "Your daily discover" com o selo "Sounds like X by Y".
 * 6. Carrosséis temáticos (Mixed for you, Novos álbuns e lançamentos).
 */
class HomeFeedSynthesizer(
    private val catalogClient: InnerTubeCatalogClient = InnerTubeCatalogClient()
) {
    companion object {
        private const val TAG = "HomeFeedSynthesizer"
    }

    suspend fun synthesizeHomeFeed(
        baseFeedData: HomeFeedData,
        historyRepo: UserHistoryRepository?,
        libraryRepo: LibraryRepository?,
        affinityRepo: UserAffinityRepository?,
        selectedChip: HomeChip? = null
    ): HomeFeedData = withContext(Dispatchers.IO) {
        val syntheticSections = mutableListOf<HomeSection>()

        val userRecent = historyRepo?.getRecentlyPlayed().orEmpty()
        val userLiked = libraryRepo?.likedTracksFlow?.value.orEmpty()
        val userPlaylists = libraryRepo?.playlistsFlow?.value.orEmpty()
        val downloadedTracks = libraryRepo?.downloadedTracksFlow?.value.orEmpty()
        val topArtists = historyRepo?.getTopArtists(5).orEmpty()
        val favoriteSeedId = historyRepo?.getFavoriteSeedVideoId()

        // -----------------------------------------------------------------------------------------
        // 1. SPEED DIAL (Grade 3x3 Paginada - 9 itens por página, até 27 itens)
        // -----------------------------------------------------------------------------------------
        if (selectedChip == null) {
            val speedDialItems = mutableListOf<Track>()
            val seenDialIds = mutableSetOf<String>()

            // A. Atalho especial: Músicas Curtidas (se houver faixas curtidas)
            if (userLiked.isNotEmpty()) {
                val likedShortcut = Track(
                    id = "shortcut_liked_music",
                    title = "Músicas Curtidas",
                    artist = "Playlist automática",
                    album = "${userLiked.size} músicas",
                    thumbnailUrl = userLiked.firstOrNull()?.thumbnailUrl,
                    durationText = "${userLiked.size} músicas",
                    sourceIdentifier = "SHORTCUT_LIKED"
                )
                speedDialItems.add(likedShortcut)
                seenDialIds.add(likedShortcut.id)
            }

            // B. Playlists criadas pelo usuário
            userPlaylists.forEach { playlist ->
                if (speedDialItems.size < 27 && seenDialIds.add(playlist.id)) {
                    val pTrack = Track(
                        id = playlist.id,
                        title = playlist.title,
                        artist = "Sua playlist",
                        album = "${playlist.tracks.size} músicas",
                        thumbnailUrl = playlist.thumbnailUrl,
                        durationText = "${playlist.tracks.size} músicas",
                        sourceIdentifier = "USER_PLAYLIST",
                        playlistId = playlist.id
                    )
                    speedDialItems.add(pTrack)
                }
            }

            // C. Músicas recentes do histórico com alta afinidade
            userRecent.forEach { track ->
                if (speedDialItems.size < 27 && track.id.isNotBlank() && seenDialIds.add(track.id)) {
                    if (affinityRepo?.isTrackPenalized(track.id) != true) {
                        speedDialItems.add(track)
                    }
                }
            }

            // D. Fallback: Se o usuário for novo e tiver poucos itens, preenche até 9 com o catálogo
            if (speedDialItems.size < 9) {
                baseFeedData.sections.firstOrNull { it.items.isNotEmpty() }?.items?.forEach { track ->
                    if (speedDialItems.size < 9 && seenDialIds.add(track.id)) {
                        speedDialItems.add(track)
                    }
                }
            }

            if (speedDialItems.isNotEmpty()) {
                syntheticSections.add(
                    HomeSection(
                        title = "Speed dial",
                        items = speedDialItems,
                        strapline = null,
                        style = HomeSectionStyle.SPEED_DIAL
                    )
                )
            }
        }

        // -----------------------------------------------------------------------------------------
        // 2. SEÇÃO CONTEXTUAL DE HORÁRIO E FIM DE SEMANA ("Para Churrasco", "Energia", "Relax")
        // -----------------------------------------------------------------------------------------
        if (selectedChip == null) {
            val cal = Calendar.getInstance()
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            val hourOfDay = cal.get(Calendar.HOUR_OF_DAY)

            val isWeekend = dayOfWeek == Calendar.FRIDAY && hourOfDay >= 17 ||
                    dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY

            val (timeTitle, timeStrapline, timeQuery) = when {
                isWeekend -> Triple(
                    "Para o fim de semana",
                    "CHURRASCO, FESTA E DESCONTRAÇÃO",
                    "churrasco sertanejo pagode festa funk"
                )
                hourOfDay in 5..11 -> Triple(
                    "Energia para o seu dia",
                    "COMECE A MANHÃ NO RITMO CERTO",
                    "energia treino motivacao pop rock"
                )
                hourOfDay in 12..17 -> Triple(
                    "Foco e ritmo",
                    "MÚSICA PARA ACOMPANHAR SUA TARDE",
                    "foco trabalho ritmo pop indie"
                )
                hourOfDay in 18..23 -> Triple(
                    "Para desacelerar",
                    "VIBE NOTURNA E RELAX",
                    "chill relax r&b phonk wave lofi"
                )
                else -> Triple(
                    "Madrugada calma",
                    "LO-FI, AMBIENTE E TRANQUILIDADE",
                    "lofi sleep ambient calm instrumental"
                )
            }

            val contextSearchResult = catalogClient.search(timeQuery, SearchFilter.SONGS).getOrNull()
            if (contextSearchResult != null && contextSearchResult.songs.isNotEmpty()) {
                syntheticSections.add(
                    HomeSection(
                        title = timeTitle,
                        strapline = timeStrapline,
                        items = contextSearchResult.songs.take(16),
                        style = HomeSectionStyle.QUICK_PICKS_GRID
                    )
                )
            }
        }

        // -----------------------------------------------------------------------------------------
        // 3. YOUR DOWNLOADS (Dinâmico: aparece se houver downloads ou curtidas)
        // -----------------------------------------------------------------------------------------
        if (selectedChip == null && (downloadedTracks.isNotEmpty() || userLiked.isNotEmpty())) {
            val downloadCards = mutableListOf<Track>()

            // A. Liked Music
            downloadCards.add(
                Track(
                    id = "downloads_liked_music",
                    title = "Músicas Curtidas",
                    artist = "Auto playlist",
                    album = "${userLiked.size} músicas",
                    thumbnailUrl = userLiked.firstOrNull()?.thumbnailUrl,
                    durationText = "${userLiked.size} músicas",
                    sourceIdentifier = "DOWNLOADS_LIKED"
                )
            )

            // B. Offline Mix / Smart Downloads
            val offlineCount = if (downloadedTracks.isNotEmpty()) downloadedTracks.size else (userLiked.size.coerceAtLeast(25))
            downloadCards.add(
                Track(
                    id = "downloads_offline_mix",
                    title = "Mix Offline",
                    artist = "NERF Music",
                    album = "$offlineCount faixas baixadas",
                    thumbnailUrl = downloadedTracks.firstOrNull()?.track?.thumbnailUrl ?: userRecent.firstOrNull()?.thumbnailUrl,
                    durationText = "$offlineCount músicas",
                    sourceIdentifier = "DOWNLOADS_OFFLINE_MIX"
                )
            )

            syntheticSections.add(
                HomeSection(
                    title = "Seus downloads",
                    strapline = "DISPONÍVEIS OFFLINE",
                    items = downloadCards,
                    style = HomeSectionStyle.YOUR_DOWNLOADS
                )
            )
        }

        // -----------------------------------------------------------------------------------------
        // 4. COVERS AND REMIXES (Foco estrito em versões Slowed, Sped Up, Remixes e VIP Edits)
        // -----------------------------------------------------------------------------------------
        val topArtistForRemix = topArtists.firstOrNull() ?: userRecent.firstOrNull()?.artist
        val remixQuery = if (!topArtistForRemix.isNullOrBlank()) {
            "$topArtistForRemix slowed remix vip cover"
        } else {
            "phonk slowed reverb remix"
        }

        val coversSearchResult = catalogClient.search(remixQuery, SearchFilter.SONGS).getOrNull()
        if (coversSearchResult != null && coversSearchResult.songs.isNotEmpty()) {
            // Filtra para garantir que sejam faixas alternativas/remixes
            val filteredCovers = coversSearchResult.songs.filter { track ->
                val t = track.title.lowercase()
                t.contains("remix") || t.contains("slowed") || t.contains("sped") ||
                        t.contains("cover") || t.contains("reverb") || t.contains("vip") ||
                        t.contains("edit") || t.contains("version")
            }.ifEmpty { coversSearchResult.songs }

            syntheticSections.add(
                HomeSection(
                    title = "Covers e remixes",
                    strapline = "VERSÕES ALTERNATIVAS E RELEITURAS",
                    items = filteredCovers.take(16),
                    style = HomeSectionStyle.QUICK_PICKS_GRID
                )
            )
        }

        // -----------------------------------------------------------------------------------------
        // 5. YOUR DAILY DISCOVER (Hero Card nobre com a assinatura "Sounds like X by Y")
        // -----------------------------------------------------------------------------------------
        val favoriteTrack = userRecent.firstOrNull() ?: userLiked.firstOrNull()
        if (favoriteTrack != null && favoriteSeedId != null) {
            val radioRes = catalogClient.getUpNextRadio(favoriteSeedId).getOrNull()
            val candidate = radioRes?.tracks?.firstOrNull { candidate ->
                candidate.id != favoriteTrack.id &&
                        userRecent.none { it.id == candidate.id } &&
                        userLiked.none { it.id == candidate.id }
            }

            if (candidate != null) {
                syntheticSections.add(
                    HomeSection(
                        title = "Sua descoberta diária",
                        strapline = "ESCOLHA ESPECIAL DE HOJE",
                        items = listOf(candidate),
                        style = HomeSectionStyle.DAILY_DISCOVER,
                        heroCategory = "NOVA DESCOBERTA",
                        heroSubtitle = "Parecido com ${favoriteTrack.title} de ${favoriteTrack.artist}"
                    )
                )
            }
        }

        // -----------------------------------------------------------------------------------------
        // 6. SEÇÕES DE CATÁLOGO DO YOUTUBE MUSIC (Novos Álbuns, Vídeos, Mixes, Comunidade)
        // -----------------------------------------------------------------------------------------
        baseFeedData.sections.forEach { section ->
            // Evita duplicar seções que já sintetizamos no topo
            val titleLower = section.title.lowercase()
            val isDuplicate = titleLower.contains("escolha a dedo") ||
                    titleLower.contains("ouvir novamente") ||
                    titleLower.contains("speed dial")

            if (!isDuplicate && section.items.isNotEmpty()) {
                syntheticSections.add(section)
            }
        }

        HomeFeedData(
            chips = baseFeedData.chips,
            sections = syntheticSections
        )
    }
}
