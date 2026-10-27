package com.nerf.music.util

import android.content.Context
import com.nerf.music.data.repository.DataSaverMode
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Interceptor de rede OkHttp que impõe as regras de Economia de Dados (Data Saver).
 * Quando a economia de dados estiver ativa (ALWAYS_ON ou AUTOMATIC em dados móveis):
 * 1. Bloqueia requisições a vídeos pesados de Canvas (.mp4).
 * 2. Redireciona URLs de capas de alta resolução (maxresdefault.jpg / hq720.jpg) para versões leves (hqdefault.jpg / mqdefault.jpg).
 */
class DataSaverInterceptor(
    private val context: Context
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()

        val isDataSaverActive = isDataSaverActive()

        if (isDataSaverActive) {
            // 1. Bloqueia download de Canvas (.mp4)
            if (url.contains(".mp4") || url.contains("/canvas/")) {
                throw IOException("Download de Canvas suprimido pela Economia de Dados.")
            }

            // 2. Transforma capas HD pesadas do YouTube em capas otimizadas de baixa resolução
            if (url.contains("maxresdefault.jpg") || url.contains("hq720.jpg")) {
                val optimizedUrl = url
                    .replace("maxresdefault.jpg", "hqdefault.jpg")
                    .replace("hq720.jpg", "mqdefault.jpg")
                val optimizedRequest = request.newBuilder()
                    .url(optimizedUrl)
                    .build()
                return chain.proceed(optimizedRequest)
            }
        }

        return chain.proceed(request)
    }

    private fun isDataSaverActive(): Boolean {
        val prefs = context.applicationContext.getSharedPreferences("nerf_user_settings_prefs", Context.MODE_PRIVATE)
        val modeStr = prefs.getString("key_data_saver_mode", DataSaverMode.ALWAYS_OFF.name) ?: DataSaverMode.ALWAYS_OFF.name
        val mode = try {
            DataSaverMode.valueOf(modeStr)
        } catch (_: Exception) {
            DataSaverMode.ALWAYS_OFF
        }

        val isWifi = NetworkUtils.isConnectedToWifi(context)
        return when (mode) {
            DataSaverMode.ALWAYS_ON -> true
            DataSaverMode.AUTOMATIC -> !isWifi
            DataSaverMode.ALWAYS_OFF -> false
        }
    }
}
