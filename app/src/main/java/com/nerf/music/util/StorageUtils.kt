package com.nerf.music.util

import android.content.Context
import java.io.File
import java.text.DecimalFormat

data class StorageBreakdown(
    val cacheSizeBytes: Long,
    val downloadsSizeBytes: Long,
    val totalAppSizeBytes: Long,
    val freeSpaceBytes: Long,
    val totalDeviceSizeBytes: Long
)

object StorageUtils {
    fun getStorageBreakdown(context: Context): StorageBreakdown {
        val cacheSize = getFolderSize(context.cacheDir) + getFolderSize(context.codeCacheDir)
        val downloadsDir = File(context.filesDir, "offline_audio")
        val downloadsSize = getFolderSize(downloadsDir)
        val filesDirSize = getFolderSize(context.filesDir)
        val totalAppSize = cacheSize + filesDirSize

        val freeSpace = context.filesDir.freeSpace
        val totalSpace = context.filesDir.totalSpace

        return StorageBreakdown(
            cacheSizeBytes = cacheSize,
            downloadsSizeBytes = downloadsSize,
            totalAppSizeBytes = totalAppSize,
            freeSpaceBytes = freeSpace,
            totalDeviceSizeBytes = totalSpace
        )
    }

    fun clearCache(context: Context): Boolean {
        return try {
            context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
            context.codeCacheDir.listFiles()?.forEach { it.deleteRecursively() }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val clampedGroup = digitGroups.coerceIn(0, units.size - 1)
        val df = DecimalFormat("#,##0.#")
        return "${df.format(bytes / Math.pow(1024.0, clampedGroup.toDouble()))} ${units[clampedGroup]}"
    }

    private fun getFolderSize(file: File?): Long {
        if (file == null || !file.exists()) return 0L
        return try {
            file.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
        } catch (_: Exception) {
            0L
        }
    }
}
