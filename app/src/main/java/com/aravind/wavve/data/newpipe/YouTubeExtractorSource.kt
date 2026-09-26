package com.aravind.wavve.data.newpipe

import com.aravind.wavve.data.piped.PipedAudioStream
import com.aravind.wavve.data.piped.PipedStreamsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamExtractor
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extracts playable audio streams directly from YouTube via NewPipeExtractor —
 * no hosted Piped instance involved. This is the PRIMARY extraction path;
 * Piped (via MusicRepository's existing failover) remains the fallback if
 * this throws (e.g. YouTube changed something and NewPipeExtractor's parsing
 * broke — see build.gradle comment on checking for library updates).
 */
@Singleton
class YouTubeExtractorSource @Inject constructor() {

    suspend fun getStreams(videoId: String): PipedStreamsResponse = withContext(Dispatchers.IO) {
        val url = "https://www.youtube.com/watch?v=$videoId"
        val extractor: StreamExtractor = ServiceList.YouTube.getStreamExtractor(url)
        extractor.fetchPage()

        val audioStreams = extractor.audioStreams.map { stream ->
            PipedAudioStream(
                url = stream.content,
                format = stream.format?.name,
                quality = stream.averageBitrate.toString() + "kbps",
                bitrate = stream.averageBitrate,
                mimeType = stream.format?.mimeType,
                codec = stream.codec
            )
        }

        PipedStreamsResponse(
            title = extractor.name,
            uploader = extractor.uploaderName,
            thumbnailUrl = extractor.thumbnails.maxByOrNull { it.height }?.url,
            duration = extractor.length,
            audioStreams = audioStreams
        )
    }
}