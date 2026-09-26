package com.aravind.wavve

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Constraints
import com.aravind.wavve.data.newpipe.NewPipeDownloader
import com.aravind.wavve.data.piped.PipedInstanceProvider
import com.aravind.wavve.util.Constants
import com.aravind.wavve.worker.PodcastUpdateWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.schabi.newpipe.extractor.NewPipe
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class WavveApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var pipedInstanceProvider: PipedInstanceProvider

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createPlaybackNotificationChannel()
        schedulePodcastRefresh()
        refreshPipedInstances()
        initNewPipeExtractor()
    }

    /** One-time init required before any NewPipeExtractor call. Uses a plain
     *  OkHttp-based Downloader that talks directly to YouTube — separate from
     *  both the default and @PipedHttpClient OkHttpClients, since this doesn't
     *  go through any Piped instance at all. */
    private fun initNewPipeExtractor() {
        NewPipe.init(NewPipeDownloader.getInstance())
    }

    /** Fetches the live list of Piped API instances once at startup, so the
     *  OkHttp failover interceptor tries currently-up instances instead of
     *  only the static fallback list in Constants.kt. */
    private fun refreshPipedInstances() {
        applicationScope.launch {
            pipedInstanceProvider.refresh()
        }
    }

    /** Checks the Roz & Mocha feed every 30 minutes so new episodes surface without
     *  the user having to open the app or manually refresh. */
    private fun schedulePodcastRefresh() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<PodcastUpdateWorker>(30, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            Constants.PODCAST_REFRESH_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun createPlaybackNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    Constants.PLAYBACK_CHANNEL_ID,
                    "Playback",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
    }
}