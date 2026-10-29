package com.nerf.music.data.repository

import android.content.Context
import android.util.Log
import com.nerf.music.data.model.*
import com.nerf.music.data.network.InnerTubeCatalogClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Orquestrador e Sintetizador do feed dinâmico da Home do NERF Music.
 *
 * Constrói uma experiência viva, altamente personalizada e reativa ao DNA acústico do usuário:
 * 1. Speed Dial 3x3 Paginado (Favoritos imediatos, playlists do usuário e faixas recentes).
 * 2. "Seu Supermix" (Equilíbrio áureo: 40% favoritos conhecidos + 60% novidades contextuais).
 * 3. Prateleiras Algorítmicas Dedicadas ("Porque você ouve [Artista]").
 * 4. Seção Contextual de Horário ("Vibe Noturna", "Energia", "Para o Fim de Semana") adaptada ao gosto do usuário.
 * 5. Your Downloads (Cartões com gradientes elegantes para reprodução offline imediata).
 * 6. Covers e Remixes exclusivos (Slowed, Sped-up, VIP edits e versões alternativas).
 * 7. Hero Card "Sua Descoberta Diária" ("Parecido com X de Y").
 * 8. Suporte completo a Mood Chips (Energia, Relax, Foco, Treino) com filtragem de afinidade.
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
        val topArtists = (affinityRepo?.getTopAffinitiveArtists(5).orEmpty() + historyRepo?.getTopArtists(5).orEmpty()).distinct().filter { affinityRepo?.isArtistPenalized(it) != true }.take(5)
        val favoriteSeedId = historyRepo?.getFavoriteSeedVideoId()
            ?: userLiked.firstOrNull()?.id
            ?: userRecent.firstOrNull()?.id

        val primaryArtist = topArtists.firstOrNull() ?: userRecent.firstOrNull()?.artist

        // -----------------------------------------------------------------------------------------
        // 1. SPEED DIAL (Grade 3x3 Paginada - 9 itens por página, até 27 itens)
        // -----------------------------------------------------------------------------------------
        val hasUserListeningActivity = userRecent.isNotEmpty() || userLiked.isNotEmpty() || userPlaylists.isNotEmpty()
        if (selectedChip == null && hasUserListeningActivity) {
            val speedDialItems = mutableListOf<Track>()
            val seenDialIds = mutableSetOf<String>()

            // A. Atalho especial: Músicas Curtidas
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

            // C. Músicas recentes do histórico com alta afinidade (sem penalidade)
            userRecent.forEach { track ->
                if (speedDialItems.size < 27 && track.id.isNotBlank() && seenDialIds.add(track.id)) {
                    if (affinityRepo?.isTrackPenalized(track.id) != true) {
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
        // 2. SEU SUPERMIX DIÁRIO (40% Favoritos Conhecidos + 60% Novidades Contextuais)
        // -----------------------------------------------------------------------------------------
        if (selectedChip == null && !favoriteSeedId.isNullOrBlank()) {
            val supermixItems = mutableListOf<Track>()
            val supermixSeenIds = mutableSetOf<String>()

            // 40% Favoritas e Recentes
            (userLiked + userRecent).filter { it.id.isNotBlank() }.distinctBy { it.id }.take(6).forEach {
                if (supermixSeenIds.add(it.id)) {
                    supermixItems.add(it)
                }
            }

            // 60% Novidades da Rádio de Recomendações
            try {
                val radioResult = catalogClient.getUpNextRadio(favoriteSeedId).getOrNull()
                radioResult?.tracks?.forEach { rec ->
                    if (supermixItems.size < 16 && supermixSeenIds.add(rec.id)) {
                        if (affinityRepo?.isTrackPenalized(rec.id) != true && affinityRepo?.isArtistPenalized(rec.artist) != true) {
                            supermixItems.add(rec)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Falha ao gerar Supermix: ${e.message}")
            }

            if (supermixItems.size >= 6) {
                syntheticSections.add(
                    HomeSection(
                        title = "Seu Supermix",
                        strapline = "FAVORITOS E NOVAS DESCOBERTAS",
                        items = supermixItems.take(16),
                        style = HomeSectionStyle.QUICK_PICKS_GRID
                    )
                )
            }
        }

        // -----------------------------------------------------------------------------------------
        // 3. PRATELEIRAS DEDICADAS POR ARTISTA ("Porque você ouve [Artista]")
        // -----------------------------------------------------------------------------------------
        if (selectedChip == null && topArtists.isNotEmpty()) {
            for (artist in topArtists.take(3)) {
                try {
                    val artistTracksResult = catalogClient.search(artist, SearchFilter.SONGS).getOrNull()
                    val artistSongs = artistTracksResult?.songs?.filter { track ->
                        track.id.isNotBlank() && affinityRepo?.isTrackPenalized(track.id) != true
                    }.orEmpty()

                    if (artistSongs.isNotEmpty()) {
                        syntheticSections.add(
                            HomeSection(
                                title = "Porque você ouve $artist",
                                strapline = "COM BASE NAS SUAS PREFERÊNCIAS",
                                items = artistSongs.take(12),
                                style = HomeSectionStyle.TWO_ROW_CAROUSEL
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Falha ao buscar músicas do artista $artist: ${e.message}")
                }
            }
        }

        // -----------------------------------------------------------------------------------------
        // 4. SEÇÃO CONTEXTUAL DE HORÁRIO ADAPTADA AO GOSTO REAL DO USUÁRIO
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
                    if (!primaryArtist.isNullOrBlank()) "$primaryArtist festa remix live" else "churrasco sertanejo pagode festa funk"
                )
                hourOfDay in 5..11 -> Triple(
                    "Energia para o seu dia",
                    "COMECE A MANHÃ NO RITMO CERTO",
                    if (!primaryArtist.isNullOrBlank()) "$primaryArtist up tempo energia" else "energia treino motivacao pop rock"
                )
                hourOfDay in 12..17 -> Triple(
                    "Foco e ritmo",
                    "MÚSICA PARA ACOMPANHAR SUA TARDE",
                    if (!primaryArtist.isNullOrBlank()) "$primaryArtist beat ritmo" else "foco trabalho ritmo pop indie"
                )
                hourOfDay in 18..23 -> Triple(
                    "Para desacelerar",
                    "VIBE NOTURNA E RELAX",
                    if (!primaryArtist.isNullOrBlank()) "$primaryArtist chill wave slowed" else "chill relax instrumental"
                )
                else -> Triple(
                    "Madrugada calma",
                    "LO-FI, AMBIENTE E TRANQUILIDADE",
                    if (!primaryArtist.isNullOrBlank()) "$primaryArtist ambient lofi" else "lofi sleep ambient calm instrumental"
                )
            }

            val contextSearchResult = catalogClient.search(timeQuery, SearchFilter.SONGS).getOrNull()
            if (contextSearchResult != null && contextSearchResult.songs.isNotEmpty()) {
                val filteredContext = contextSearchResult.songs.filter {
                    affinityRepo?.isTrackPenalized(it.id) != true
                }
                if (filteredContext.isNotEmpty()) {
                    syntheticSections.add(
                        HomeSection(
                            title = timeTitle,
                            strapline = timeStrapline,
                            items = filteredContext.take(16),
                            style = HomeSectionStyle.QUICK_PICKS_GRID
                        )
                    )
                }
            }
        }

        // -----------------------------------------------------------------------------------------
        // 5. YOUR DOWNLOADS (Dinâmico: aparece se houver downloads ou curtidas)
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
        // 6. COVERS AND REMIXES (Foco estrito em versões Slowed, Sped Up, Remixes e VIP Edits)
        // -----------------------------------------------------------------------------------------
        if (selectedChip == null) {
            val topArtistForRemix = topArtists.firstOrNull() ?: userRecent.firstOrNull()?.artist
            val remixQuery = if (!topArtistForRemix.isNullOrBlank()) {
                "$topArtistForRemix slowed remix vip cover"
            } else {
                "popular remix cover"
            }

            val coversSearchResult = catalogClient.search(remixQuery, SearchFilter.SONGS).getOrNull()
            if (coversSearchResult != null && coversSearchResult.songs.isNotEmpty()) {
                val filteredCovers = coversSearchResult.songs.filter { track ->
                    val t = track.title.lowercase()
                    (t.contains("remix") || t.contains("slowed") || t.contains("sped") ||
                            t.contains("cover") || t.contains("reverb") || t.contains("vip") ||
                            t.contains("edit") || t.contains("version")) &&
                            affinityRepo?.isTrackPenalized(track.id) != true
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
        }

        // -----------------------------------------------------------------------------------------
        // 7. YOUR DAILY DISCOVER (Hero Card com a assinatura "Parecido com X de Y")
        // -----------------------------------------------------------------------------------------
        if (selectedChip == null) {
            val favoriteTrack = userRecent.firstOrNull() ?: userLiked.firstOrNull()
            if (favoriteTrack != null && favoriteSeedId != null) {
                val radioRes = catalogClient.getUpNextRadio(favoriteSeedId).getOrNull()
                val candidate = radioRes?.tracks?.firstOrNull { candidate ->
                    candidate.id != favoriteTrack.id &&
                            userRecent.none { it.id == candidate.id } &&
                            userLiked.none { it.id == candidate.id } &&
                            affinityRepo?.isTrackPenalized(candidate.id) != true
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
        }

        // -----------------------------------------------------------------------------------------
        // 8. MOOD CHIPS ATIVOS (Energia, Relax, Treino, Foco, etc.)
        // -----------------------------------------------------------------------------------------
        if (selectedChip != null) {
            val moodQuery = if (!primaryArtist.isNullOrBlank()) {
                "$primaryArtist ${selectedChip.title}"
            } else {
                "${selectedChip.title} music"
            }

            try {
                val moodSearch = catalogClient.search(moodQuery, SearchFilter.SONGS).getOrNull()
                val moodSongs = moodSearch?.songs?.filter {
                    affinityRepo?.isTrackPenalized(it.id) != true
                }.orEmpty()

                if (moodSongs.isNotEmpty()) {
                    syntheticSections.add(
                        HomeSection(
                            title = "Mix de ${selectedChip.title}",
                            strapline = "PERSONALIZADO PARA O SEU MOMENTO",
                            items = moodSongs.take(16),
                            style = HomeSectionStyle.QUICK_PICKS_GRID
                        )
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Falha ao sintetizar seção do chip ${selectedChip.title}: ${e.message}")
            }
        }

        // -----------------------------------------------------------------------------------------
        // 9. SEÇÕES DO CATÁLOGO ORIGINAL (Lançamentos, Álbuns, Comunidade)
        // -----------------------------------------------------------------------------------------
        baseFeedData.sections.forEach { section ->
            val titleLower = section.title.lowercase()
            val isDuplicate = titleLower.contains("escolha a dedo") ||
                    titleLower.contains("ouvir novamente") ||
                    titleLower.contains("speed dial") ||
                    titleLower.contains("supermix")

            if (!isDuplicate && section.items.isNotEmpty()) {
                val nonPenalized = section.items.filter { affinityRepo?.isTrackPenalized(it.id) != true }
                if (nonPenalized.isNotEmpty()) {
                    syntheticSections.add(section.copy(items = nonPenalized))
                }
            }
        }

        HomeFeedData(
            chips = baseFeedData.chips,
            sections = syntheticSections
        )
    }
}
