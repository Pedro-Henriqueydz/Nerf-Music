package com.nerf.music

import com.nerf.music.data.repository.SemanticVersion
import com.nerf.music.data.repository.UpdateRepositoryImpl
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
        assertFalse(
            "Versões equivalentes (1.0.1 vs v1.0.1) não devem disparar atualização",
            AppUpdateManager.isCurrentVersionOlder("1.0.1", "v1.0.1")
        )
        assertFalse(
            "Versões equivalentes com 'V' maiúsculo (1.0.1 vs V1.0.1) não devem disparar atualização",
            AppUpdateManager.isCurrentVersionOlder("1.0.1", "V1.0.1")
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
    fun testSemanticVersionNumericOrdering() {
        // Ordenação alfabética falharia aqui ("1.0.2" > "1.0.10" alfabeticamente),
        // mas numericamente 1.0.10 é estritamente maior.
        assertTrue(
            "1.0.10 deve ser maior que 1.0.2",
            SemanticVersion.isRemoteVersionStrictlyNewer("1.0.2", "v1.0.10")
        )
        assertFalse(
            "1.0.2 não deve ser maior que 1.0.10",
            SemanticVersion.isRemoteVersionStrictlyNewer("1.0.10", "v1.0.2")
        )
    }

    @Test
    fun testSanitizeVersion() {
        assertEquals("1.0.1", SemanticVersion.sanitizeVersion("v1.0.1"))
        assertEquals("1.0.1", SemanticVersion.sanitizeVersion("V1.0.1"))
        assertEquals("1.0.1", SemanticVersion.sanitizeVersion("  v1.0.1  "))
        assertEquals("1.0.1", SemanticVersion.sanitizeVersion("v1.0.1-beta1"))
        assertEquals("1.0.1", SemanticVersion.sanitizeVersion("v1.0.1+build.42"))
    }

    @Test
    fun testUpdateRepositoryIntegration() {
        val repo = UpdateRepositoryImpl()
        assertFalse(
            "UpdateRepository não deve apontar atualização se versão for idêntica",
            repo.isRemoteVersionNewer("1.0.1", "v1.0.1")
        )
        assertTrue(
            "UpdateRepository deve apontar atualização se versão remota for estritamente superior",
            repo.isRemoteVersionNewer("1.0.1", "v1.0.2")
        )
        assertFalse(
            "UpdateRepository não deve apontar atualização se versão local for superior à remota",
            repo.isRemoteVersionNewer("1.1.0", "v1.0.9")
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
