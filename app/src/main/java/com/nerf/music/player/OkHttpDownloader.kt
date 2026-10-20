package com.nerf.music.player

import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Implementação de [Downloader] para o NewPipeExtractor utilizando OkHttpClient.
 * Gerencia requisições de rede, cookies e extração de páginas do YouTube de forma eficiente.
 */
class OkHttpDownloader(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) : Downloader() {

    @Throws(IOException::class)
    override fun execute(request: org.schabi.newpipe.extractor.downloader.Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val okHeaders = mutableMapOf<String, String>()
        for ((key, values) in headers) {
            if (values.isNotEmpty()) {
                okHeaders[key] = values[0]
            }
        }

        // Garante cabeçalhos padrão úteis
        if (!okHeaders.containsKey("User-Agent")) {
            okHeaders["User-Agent"] =
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        }

        val requestBody = if (dataToSend != null && (httpMethod == "POST" || httpMethod == "PUT")) {
            val contentType = okHeaders["Content-Type"] ?: "application/json; charset=utf-8"
            dataToSend.toRequestBody(contentType.toMediaType())
        } else if (httpMethod == "POST") {
            byteArrayOf().toRequestBody(null)
        } else {
            null
        }

        val okHttpRequest = Request.Builder()
            .url(url)
            .headers(okHeaders.toHeaders())
            .method(httpMethod, requestBody)
            .build()

        val response = client.newCall(okHttpRequest).execute()
        val bodyString = response.body?.string() ?: ""
        val responseCode = response.code
        val responseMessage = response.message
        val responseHeaders = response.headers.toMultimap()
        val latestUrl = response.request.url.toString()

        return Response(
            responseCode,
            responseMessage,
            responseHeaders,
            bodyString,
            latestUrl
        )
    }
}
