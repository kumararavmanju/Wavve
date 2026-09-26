package com.aravind.wavve.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class NowPlaying(
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    val artworkUrl: String? = null,
    val isLiveRadio: Boolean = false
)

enum class RepeatMode { OFF, ONE, ALL }

/**
 * App-wide singleton that owns the MediaController connection to PlaybackService and
 * republishes playback state as StateFlows the Compose UI can collect directly — this
 * is what both the mini player and the full player screen observe.
 */
@Singleton
class PlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var controller: MediaController? = null

    private val _nowPlaying = MutableStateFlow(NowPlaying())
    val nowPlaying: StateFlow<NowPlaying> = _nowPlaying

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled

    fun connect(onReady: () -> Unit = {}) {
        if (controller != null) { onReady(); return }
        val sessionToken = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java)
        )
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        future.addListener({
            controller = future.get()
            attachListener()
            onReady()
        }, MoreExecutors.directExecutor())
    }

    private fun attachListener() {
        val c = controller ?: return
        c.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                _nowPlaying.value = _nowPlaying.value.copy(
                    title = mediaMetadata.title?.toString().orEmpty(),
                    artist = mediaMetadata.artist?.toString().orEmpty(),
                    artworkUrl = mediaMetadata.artworkUri?.toString()
                )
                _durationMs.value = c.duration.coerceAtLeast(0)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _durationMs.value = c.duration.coerceAtLeast(0)
            }
        })
    }

    /** Call periodically (e.g. every 500ms from a UI-side ticker) to refresh the seek bar. */
    fun pollPosition() {
        controller?.let {
            _positionMs.value = it.currentPosition.coerceAtLeast(0)
            _durationMs.value = it.duration.coerceAtLeast(0)
        }
    }

    fun playSingle(id: String, title: String, artist: String, artworkUrl: String?, streamUrl: String, isLive: Boolean = false) {
        val item = MediaItem.Builder()
            .setUri(streamUrl)
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setArtworkUri(artworkUrl?.let { android.net.Uri.parse(it) })
                    .build()
            )
            .build()
        _nowPlaying.value = NowPlaying(id, title, artist, artworkUrl, isLive)
        controller?.apply {
            setMediaItem(item)
            prepare()
            play()
        }
    }

    fun playQueue(items: List<MediaItem>, startIndex: Int = 0) {
        controller?.apply {
            setMediaItems(items, startIndex, 0L)
            prepare()
            play()
        }
    }

    /** Starts playback immediately with just ONE item as the whole queue.
     *  Used so a tapped song in a list plays right away, instead of waiting
     *  for every song in the list to resolve first. Call appendToQueue()
     *  afterward, per song, as the rest of the list resolves in the background. */
    fun startQueueWith(id: String, title: String, artist: String, artworkUrl: String?, item: MediaItem) {
        _nowPlaying.value = NowPlaying(id, title, artist, artworkUrl, isLiveRadio = false)
        controller?.apply {
            setMediaItem(item)
            prepare()
            play()
        }
    }

    /** Appends one resolved song to the end of the currently playing queue,
     *  without interrupting whatever's already playing. */
    fun appendToQueue(item: MediaItem) {
        controller?.addMediaItem(item)
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun next() { controller?.seekToNextMediaItem() }
    fun previous() { controller?.seekToPreviousMediaItem() }
    fun seekTo(ms: Long) { controller?.seekTo(ms) }

    fun toggleRepeat() {
        val next = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _repeatMode.value = next
        controller?.repeatMode = when (next) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
    }

    fun toggleShuffle() {
        val enabled = !_shuffleEnabled.value
        _shuffleEnabled.value = enabled
        controller?.shuffleModeEnabled = enabled
    }

    // Note: the EQ's audio session id is captured inside PlaybackService itself via
    // ExoPlayer's AnalyticsListener (see PlaybackService.onCreate) — session id isn't
    // part of the MediaController API surface, so it can't be read from here.
}