package com.aravind.wavve.data.piped

import com.google.gson.annotations.SerializedName

/** A single result row from Piped's /search endpoint. */
data class PipedSearchItem(
    val url: String? = null,               // e.g. "/watch?v=VIDEOID"
    val title: String? = null,
    val thumbnail: String? = null,
    val uploaderName: String? = null,
    @SerializedName("uploaderUrl") val uploaderUrl: String? = null,
    val duration: Long = 0,                // seconds
    val type: String? = null               // "stream" | "channel" | "playlist"
) {
    val videoId: String?
        get() = url?.substringAfter("v=", "")?.takeIf { it.isNotBlank() }
}

data class PipedSearchResponse(
    val items: List<PipedSearchItem> = emptyList(),
    val nextpage: String? = null
)

/** A single selectable audio (or muxed) format from /streams/{id}. */
data class PipedAudioStream(
    val url: String? = null,
    val format: String? = null,
    val quality: String? = null,
    val bitrate: Int = 0,
    val mimeType: String? = null,
    val codec: String? = null
)

data class PipedStreamsResponse(
    val title: String? = null,
    val uploader: String? = null,
    val thumbnailUrl: String? = null,
    val duration: Long = 0,
    val audioStreams: List<PipedAudioStream> = emptyList()
)
