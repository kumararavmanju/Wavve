package com.aravind.wavve.data.newpipe

import okhttp3.OkHttpClient
import okhttp3.Request as OkHttpRequest
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request as NewPipeRequest
import org.schabi.newpipe.extractor.downloader.Response as NewPipeResponse
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Minimal OkHttp-based Downloader implementation NewPipeExtractor needs to
 * make its own HTTP calls to YouTube. Kept separate from the app's main
 * OkHttpClient(s) — this one talks directly to youtube.com/googlevideo.com,
 * not Piped instances, so it doesn't need the Piped failover interceptor.
 */
class NewPipeDownloader private constructor(
    private val client: OkHttpClient
) : Downloader() {

    companion object {
        @Volatile
        private var instance: NewPipeDownloader? = null

        fun getInstance(): NewPipeDownloader =
            instance ?: synchronized(this) {
                instance ?: NewPipeDownloader(
                    OkHttpClient.Builder()
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(20, TimeUnit.SECONDS)
                        .build()
                ).also { instance = it }
            }
    }

    @Throws(IOException::class)
    override fun execute(request: NewPipeRequest): NewPipeResponse {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val requestBuilder = OkHttpRequest.Builder().url(url)

        val body = if (dataToSend != null) {
            dataToSend.toRequestBody()
        } else null

        when (httpMethod.uppercase()) {
            "GET" -> requestBuilder.get()
            "POST" -> requestBuilder.post(body ?: okhttp3.internal.EMPTY_REQUEST)
            "HEAD" -> requestBuilder.head()
            else -> requestBuilder.method(httpMethod, body)
        }

        for ((key, values) in headers) {
            if (values.size == 1) {
                requestBuilder.header(key, values[0])
            } else {
                requestBuilder.removeHeader(key)
                for (value in values) {
                    requestBuilder.addHeader(key, value)
                }
            }
        }

        if (requestBuilder.build().header("User-Agent") == null) {
            requestBuilder.header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"
            )
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            val responseBody = response.body?.string() ?: ""
            val latestUrl = response.request.url.toString()

            return NewPipeResponse(
                response.code,
                response.message,
                response.headers.toMultimap(),
                responseBody,
                latestUrl
            )
        }
    }

    private fun ByteArray.toRequestBody(): okhttp3.RequestBody =
        okhttp3.RequestBody.create(null, this)
}