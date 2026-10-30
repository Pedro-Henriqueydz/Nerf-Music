package com.nerf.music.updater

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.nerf.music.BuildConfig
import com.nerf.music.data.repository.SemanticVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Informações da versão mais recente disponível no GitHub Releases.
 */
data class AppUpdateInfo(
    val tagName: String,
    val downloadUrl: String,
    val releaseNotes: String? = null
)

/**
 * Gerenciador de Autoreparo (In-App Updater) do NERF Music:
 * - Consulta `https://api.github.com/repos/Pedro-Henriqueydz/Nerf-Music/releases/latest`
 * - Compara `tag_name` com `BuildConfig.VERSION_NAME`
 * - Faz download do APK via `DownloadManager` e dispara a instalação via `FileProvider` e `Intent.ACTION_VIEW`
 */
class AppUpdateManager(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()
) {

    companion object {
        private const val TAG = "AppUpdateManager"
        const val LATEST_RELEASE_API_URL =
            "https://api.github.com/repos/Pedro-Henriqueydz/Nerf-Music/releases/latest"

        /**
         * Compara a versão atual do app (`BuildConfig.VERSION_NAME`, ex: "1.0.0") com a `tag_name`
         * da release no GitHub (ex: "v1.0.1" ou "1.1.0").
         * Retorna `true` se a versão atual do app for menor que a versão remota.
         */
        fun isCurrentVersionOlder(currentVersion: String, remoteTagName: String): Boolean {
            return SemanticVersion.isRemoteVersionStrictlyNewer(currentVersion, remoteTagName)
        }

        internal fun parseVersionParts(rawVersion: String): List<Int> {
            val semVer = SemanticVersion.parse(rawVersion)
            return listOf(semVer.major, semVer.minor, semVer.patch)
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Consulta a última release no GitHub e retorna [AppUpdateInfo] caso exista uma versão mais nova
     * que `BuildConfig.VERSION_NAME`.
     */
    suspend fun fetchLatestRelease(): AppUpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(LATEST_RELEASE_API_URL)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "NERF-Music-Android/${BuildConfig.VERSION_NAME}")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.d(TAG, "Verificação de release retornou HTTP ${response.code}")
                    return@withContext null
                }

                val bodyString = response.body?.string().orEmpty()
                parseLatestReleaseJson(bodyString)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Não foi possível consultar última release: ${e.message}")
            null
        }
    }

    suspend fun checkForUpdate(
        currentVersionName: String = BuildConfig.VERSION_NAME
    ): AppUpdateInfo? = withContext(Dispatchers.IO) {
        val releaseInfo = fetchLatestRelease() ?: return@withContext null
        if (isCurrentVersionOlder(currentVersionName, releaseInfo.tagName)) {
            releaseInfo
        } else {
            null
        }
    }

    /**
     * Extrai `tag_name` e `browser_download_url` do primeiro asset no JSON da API do GitHub.
     */
    internal fun parseLatestReleaseJson(rawJson: String): AppUpdateInfo? {
        if (rawJson.isBlank()) return null
        return try {
            val root = json.parseToJsonElement(rawJson) as? JsonObject ?: return null
            val tagName = (root["tag_name"] as? JsonPrimitive)?.contentOrNull?.trim()
                ?.takeIf { it.isNotEmpty() } ?: return null

            val assets = root["assets"] as? JsonArray ?: return null
            // Valida e filtra estritamente por assets que sejam arquivos .apk com URL segura HTTPS
            val apkAsset = assets.filterIsInstance<JsonObject>().firstOrNull { asset ->
                val name = (asset["name"] as? JsonPrimitive)?.contentOrNull.orEmpty()
                val url = (asset["browser_download_url"] as? JsonPrimitive)?.contentOrNull.orEmpty()
                name.endsWith(".apk", ignoreCase = true) && url.startsWith("https://", ignoreCase = true)
            } ?: return null

            val browserDownloadUrl = (apkAsset["browser_download_url"] as? JsonPrimitive)
                ?.contentOrNull?.trim()
                ?.takeIf { it.isNotEmpty() } ?: return null

            val releaseNotes = (root["body"] as? JsonPrimitive)?.contentOrNull?.trim()
                ?.let(::extractInAppReleaseNotes)
            AppUpdateInfo(
                tagName = tagName,
                downloadUrl = browserDownloadUrl,
                releaseNotes = releaseNotes
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao processar JSON de release do GitHub", e)
            null
        }
    }

    private fun extractInAppReleaseNotes(body: String): String {
        val startMarker = "<!-- IN_APP_START -->"
        val endMarker = "<!-- IN_APP_END -->"
        val start = body.indexOf(startMarker)
        val end = body.indexOf(endMarker)
        return if (start >= 0 && end > start) {
            body.substring(start + startMarker.length, end).trim()
        } else {
            body
        }
    }

    /**
     * Inicia o download do APK usando `DownloadManager` e dispara a instalação automaticamente
     * via `FileProvider` e `Intent.ACTION_VIEW` ao concluir.
     */
    fun downloadAndInstallApk(
        context: Context,
        updateInfo: AppUpdateInfo,
        onStatusMessage: (String) -> Unit = {}
    ) {
        val appContext = context.applicationContext
        val downloadManager = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
        if (downloadManager == null) {
            onStatusMessage("Gerenciador de downloads indisponível no dispositivo.")
            return
        }

        val sanitizedTag = updateInfo.tagName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val fileName = "nerf-music-$sanitizedTag.apk"
        val downloadsDir = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val targetFile = File(downloadsDir, fileName)

        if (targetFile.exists()) {
            targetFile.delete()
        }

        val request = DownloadManager.Request(Uri.parse(updateInfo.downloadUrl))
            .setTitle("NERF Music ${updateInfo.tagName}")
            .setDescription("Baixando nova versão do aplicativo...")
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
            .setDestinationInExternalFilesDir(appContext, Environment.DIRECTORY_DOWNLOADS, fileName)

        val downloadId = try {
            downloadManager.enqueue(request)
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao enfileirar download do APK", e)
            onStatusMessage("Erro ao iniciar o download da atualização.")
            return
        }

        onStatusMessage("Baixando atualização ${updateInfo.tagName}...")

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val completedId = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: -1L
                if (completedId != downloadId) return

                try {
                    appContext.unregisterReceiver(this)
                } catch (_: Exception) {
                }

                if (targetFile.exists() && targetFile.length() > 0L) {
                    installApk(appContext, targetFile, onStatusMessage)
                } else {
                    onStatusMessage("Falha ao concluir o download do APK.")
                }
            }
        }

        ContextCompat.registerReceiver(
            appContext,
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    /**
     * Inicia a instalação do arquivo APK usando `FileProvider` e `Intent.ACTION_VIEW`.
     */
    fun installApk(
        context: Context,
        apkFile: File,
        onStatusMessage: (String) -> Unit = {}
    ) {
        try {
            if (!apkFile.exists() || apkFile.length() <= 0L) {
                onStatusMessage("Arquivo de atualização não encontrado.")
                return
            }

            // Validação de integridade do diretório (prevenção contra Path Traversal)
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            if (downloadsDir == null || !apkFile.canonicalPath.startsWith(downloadsDir.canonicalPath)) {
                Log.e(TAG, "Tentativa de instalação fora do diretório autorizado de downloads.")
                onStatusMessage("Arquivo de atualização inválido ou não seguro.")
                return
            }

            // Validação estrutural do APK e confirmação de packageName compatível antes da instalação
            val archiveInfo = context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0)
            if (archiveInfo == null || archiveInfo.packageName != context.packageName) {
                Log.e(TAG, "APK baixado não corresponde ao pacote do aplicativo (${archiveInfo?.packageName} != ${context.packageName})")
                onStatusMessage("Arquivo de atualização corrompido ou inválido.")
                return
            }

            val authority = "${context.packageName}.fileprovider"
            val apkUri = FileProvider.getUriForFile(context, authority, apkFile)

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao abrir instalador de pacotes para ${apkFile.absolutePath}", e)
            onStatusMessage("Erro ao abrir o instalador do APK: ${e.localizedMessage}")
        }
    }
}
