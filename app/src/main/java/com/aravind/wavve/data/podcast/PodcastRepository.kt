package com.aravind.wavve.data.podcast

import com.aravind.wavve.data.db.PodcastStateDao
import com.aravind.wavve.data.db.PodcastStateEntity
import com.aravind.wavve.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PodcastRepository @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val podcastStateDao: PodcastStateDao
) {

    /** Fetches every episode of Roz & Mocha, newest first. Called by the Podcasts tab. */
    suspend fun fetchRozAndMocha(): PodcastFeed = withContext(Dispatchers.IO) {
        fetchFeed(Constants.ROZ_AND_MOCHA_RSS_URL)
    }

    suspend fun fetchFeed(feedUrl: String): PodcastFeed = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(feedUrl).build()
        okHttpClient.newCall(request).execute().use { response ->
            val body = response.body ?: error("Empty podcast feed response")
            val feed = PodcastRssParser.parse(body.byteStream())
            feed.copy(episodes = feed.episodes.sortedByDescending { it.pubDateMs })
        }
    }

    /**
     * Checks whether a new episode has appeared since we last looked, updating our
     * bookmark either way. Returns the new episode if one was found (used by the
     * background worker to fire a "new episode" notification).
     */
    suspend fun checkForNewEpisode(feedUrl: String): PodcastEpisode? = withContext(Dispatchers.IO) {
        val feed = fetchFeed(feedUrl)
        val latest = feed.episodes.firstOrNull() ?: return@withContext null
        val previousState = podcastStateDao.get(feedUrl)

        podcastStateDao.upsert(
            PodcastStateEntity(
                feedUrl = feedUrl,
                lastSeenEpisodeGuid = latest.guid,
                lastCheckedAt = System.currentTimeMillis()
            )
        )

        val isNew = previousState != null && previousState.lastSeenEpisodeGuid != latest.guid
        if (isNew) latest else null
    }
}
