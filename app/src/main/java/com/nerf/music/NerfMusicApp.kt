package com.nerf.music

import android.app.Application
import com.nerf.music.player.OkHttpDownloader
import org.schabi.newpipe.extractor.NewPipe

/**
 * Classe de aplicação do NERF Music.
 *
 * Responsável por inicializar componentes globais como o NewPipeExtractor
 * para decifração de streams e resolução de mídia do YouTube.
 */
class NerfMusicApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Inicializa o NewPipeExtractor com o downloader baseado em OkHttp
        NewPipe.init(OkHttpDownloader())
    }
}
