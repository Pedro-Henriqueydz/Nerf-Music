package com.nerf.music.data.repository

import android.content.Context
import com.nerf.music.BuildConfig
import com.nerf.music.updater.AppUpdateInfo
import com.nerf.music.updater.AppUpdateManager

/**
 * Representa os três blocos numéricos da Versão Semântica (SemVer): Major, Minor e Patch.
 */
data class SemanticVersion(
    val major: Int = 0,
    val minor: Int = 0,
    val patch: Int = 0
) : Comparable<SemanticVersion> {

    override fun compareTo(other: SemanticVersion): Int {
        if (this.major != other.major) return this.major.compareTo(other.major)
        if (this.minor != other.minor) return this.minor.compareTo(other.minor)
        return this.patch.compareTo(other.patch)
    }

    companion object {
        /**
         * Converte uma string de versão (ex: "v1.0.1", "1.0.0", "V2.3.4-beta")
         * em [SemanticVersion] com blocos numéricos inteiros.
         */
        fun parse(rawVersion: String): SemanticVersion {
            val sanitized = sanitizeVersion(rawVersion)
            if (sanitized.isBlank()) return SemanticVersion(0, 0, 0)

            val parts = sanitized.split(".")
            val major = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: 0
            val minor = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
            val patch = parts.getOrNull(2)?.trim()?.toIntOrNull() ?: 0

            return SemanticVersion(major, minor, patch)
        }

        /**
         * Higieniza a string de versão:
         * - Remove espaços em branco
         * - Remove qualquer caractere 'v' ou 'V' do início
         * - Remove sufixos de pré-lançamento (-beta, -rc) e metadados de build (+build)
         */
        fun sanitizeVersion(rawVersion: String): String {
            return rawVersion
                .trim()
                .replace(Regex("^[vV]+"), "")
                .substringBefore("-")
                .substringBefore("+")
                .trim()
        }

        /**
         * Regra de negócio rigorosa:
         * Retorna `true` SE E SOMENTE SE a versão remota for ESTRITAMENTE MAIOR que a versão local.
         * Versões equivalentes (ex: local "1.0.1" e remota "v1.0.1") ou inferiores retornam `false`.
         */
        fun isRemoteVersionStrictlyNewer(localVersion: String, remoteVersion: String): Boolean {
            val local = parse(localVersion)
            val remote = parse(remoteVersion)
            return remote > local
        }
    }
}

/**
 * Repositório responsável pela verificação e download de atualizações do app.
 */
interface UpdateRepository {
    suspend fun checkForUpdate(
        currentVersionName: String = BuildConfig.VERSION_NAME
    ): AppUpdateInfo?

    fun downloadAndInstallApk(
        context: Context,
        updateInfo: AppUpdateInfo,
        onStatusMessage: (String) -> Unit = {}
    )

    fun sanitizeVersion(rawVersion: String): String
    fun isRemoteVersionNewer(localVersion: String, remoteVersion: String): Boolean
}

class UpdateRepositoryImpl(
    private val appUpdateManager: AppUpdateManager = AppUpdateManager()
) : UpdateRepository {

    override suspend fun checkForUpdate(currentVersionName: String): AppUpdateInfo? {
        val latestRelease = appUpdateManager.fetchLatestRelease() ?: return null
        return if (isRemoteVersionNewer(currentVersionName, latestRelease.tagName)) {
            latestRelease
        } else {
            null
        }
    }

    override fun downloadAndInstallApk(
        context: Context,
        updateInfo: AppUpdateInfo,
        onStatusMessage: (String) -> Unit
    ) {
        appUpdateManager.downloadAndInstallApk(context, updateInfo, onStatusMessage)
    }

    override fun sanitizeVersion(rawVersion: String): String {
        return SemanticVersion.sanitizeVersion(rawVersion)
    }

    override fun isRemoteVersionNewer(localVersion: String, remoteVersion: String): Boolean {
        return SemanticVersion.isRemoteVersionStrictlyNewer(localVersion, remoteVersion)
    }
}
