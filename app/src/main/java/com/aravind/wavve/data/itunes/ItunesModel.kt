package com.aravind.wavve.data.itunes

data class ItunesTrack(
    val trackName: String? = null,
    val artistName: String? = null,
    val collectionName: String? = null,
    val artworkUrl100: String? = null,
    val trackTimeMillis: Long? = null
)

data class ItunesSearchResponse(
    val resultCount: Int = 0,
    val results: List<ItunesTrack> = emptyList()
)