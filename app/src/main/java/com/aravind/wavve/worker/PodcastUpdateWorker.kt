package com.aravind.wavve.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aravind.wavve.R
import com.aravind.wavve.data.podcast.PodcastRepository
import com.aravind.wavve.util.Constants
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Runs periodically (see WavveApp.schedulePodcastRefresh) to check the Roz & Mocha
 * RSS feed for a new episode and, if one dropped since the last check, posts a
 * notification. Because it reads the same open Simplecast feed the Podcasts tab
 * displays, no manual "add episode" step is ever needed — new episodes just appear.
 */
@HiltWorker
class PodcastUpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val podcastRepository: PodcastRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val newEpisode = podcastRepository.checkForNewEpisode(Constants.ROZ_AND_MOCHA_RSS_URL)
            if (newEpisode != null) {
                notifyNewEpisode(newEpisode.title)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun notifyNewEpisode(episodeTitle: String) {
        val context = applicationContext
        ensureChannel(context)

        val notification = NotificationCompat.Builder(context, Constants.PODCAST_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("New episode: ${Constants.ROZ_AND_MOCHA_TITLE}")
            .setContentText(episodeTitle)
            .setStyle(NotificationCompat.BigTextStyle().bigText(episodeTitle))
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context)
            .notify(Constants.PODCAST_NOTIFICATION_ID, notification)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                Constants.PODCAST_CHANNEL_ID,
                "New podcast episodes",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
        }
    }
}
