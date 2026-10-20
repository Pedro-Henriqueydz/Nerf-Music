package com.nerf.music

import com.nerf.music.updater.AppUpdateManager
import org.junit.Assert.*
import org.junit.Test

class AppUpdateManagerTest {

    @Test
    fun testVersionComparisonLogic() {
        assertFalse(
            "Mesma versão (1.0.0 vs v1.0.0) não deve disparar atualização",
            AppUpdateManager.isCurrentVersionOlder("1.0.0", "v1.0.0")
        )
        assertTrue(
            "Versão patch mais nova (1.0.0 vs v1.0.1) deve disparar atualização",
            AppUpdateManager.isCurrentVersionOlder("1.0.0", "v1.0.1")
        )
        assertTrue(
            "Versão minor mais nova (1.0.0 vs v1.1.0) deve disparar atualização",
            AppUpdateManager.isCurrentVersionOlder("1.0.0", "v1.1.0")
        )
        assertTrue(
            "Versão major mais nova (1.0.0 vs v2.0.0) deve disparar atualização",
            AppUpdateManager.isCurrentVersionOlder("1.0.0", "v2.0.0")
        )
        assertFalse(
            "Versão local maior que remota (1.2.0 vs v1.0.5) não deve disparar atualização",
            AppUpdateManager.isCurrentVersionOlder("1.2.0", "v1.0.5")
        )
    }

    @Test
    fun testParseLatestReleaseJsonExtractsTagAndFirstAssetUrl() {
        val manager = AppUpdateManager()
        val sampleJson = """
        {
          "tag_name": "v1.0.1",
          "name": "v1.0.1",
          "body": "Melhorias de estabilidade e rádio dinâmica",
          "assets": [
            {
              "name": "nerf-music-v1.0.1.apk",
              "browser_download_url": "https://github.com/Pedro-Henriqueydz/Nerf-Music/releases/download/v1.0.1/nerf-music-v1.0.1.apk"
            }
          ]
        }
        """.trimIndent()

        val parsed = manager.parseLatestReleaseJson(sampleJson)
        assertNotNull("Deve extrair AppUpdateInfo válido", parsed)
        assertEquals("v1.0.1", parsed?.tagName)
        assertEquals(
            "https://github.com/Pedro-Henriqueydz/Nerf-Music/releases/download/v1.0.1/nerf-music-v1.0.1.apk",
            parsed?.downloadUrl
        )
        assertEquals("Melhorias de estabilidade e rádio dinâmica", parsed?.releaseNotes)
    }
}
