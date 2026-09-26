package com.aravind.wavve.data.podcast

data class PodcastEpisode(
    val guid: String,
    val title: String,
    val description: String,
    val audioUrl: String,
    val imageUrl: String?,
    val pubDateMs: Long,
    val durationSec: Long
)

data class PodcastFeed(
    val title: String,
    val imageUrl: String?,
    val episodes: List<PodcastEpisode>
)
