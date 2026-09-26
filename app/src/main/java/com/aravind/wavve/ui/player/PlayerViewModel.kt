package com.aravind.wavve.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.aravind.wavve.data.db.RecentSearchEntity
import com.aravind.wavve.data.db.SongEntity
import com.aravind.wavve.data.local.LocalAudioTrack
import com.aravind.wavve.data.radio.RadioStation
import com.aravind.wavve.data.repository.MusicRepository
import com.aravind.wavve.data.repository.SearchQuotaExceededException
import com.aravind.wavve.playback.EqBandInfo
import com.aravind.wavve.playback.EqualizerController
import com.aravind.wavve.playback.NowPlaying
import com.aravind.wavve.playback.PlayerManager
import com.aravind.wavve.playback.RepeatMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerManager: PlayerManager,
    val equalizerController: EqualizerController,
    private val musicRepository: MusicRepository
) : ViewModel() {

    val nowPlaying: StateFlow<NowPlaying> = playerManager.nowPlaying
    val isPlaying: StateFlow<Boolean> = playerManager.isPlaying
    val positionMs: StateFlow<Long> = playerManager.positionMs
    val durationMs: StateFlow<Long> = playerManager.durationMs
    val repeatMode: StateFlow<RepeatMode> = playerManager.repeatMode
    val shuffleEnabled: StateFlow<Boolean> = playerManager.shuffleEnabled
    val eqBands: StateFlow<List<EqBandInfo>> = equalizerController.bands
    val eqGains: StateFlow<Map<Short, Short>> = equalizerController.gains
    val eqEnabled: StateFlow<Boolean> = equalizerController.enabled

    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError

    fun clearPlaybackError() { _playbackError.value = null }

    /** Step 5: true if the currently playing song already belongs to any
     *  playlist — drives the player's +/✓ icon. Recomputes whenever the
     *  playing song changes. */
    val isCurrentSongInAnyPlaylist: StateFlow<Boolean> = nowPlaying
        .map { it.id }
        .distinctUntilChanged()
        .flatMapLatest { id ->
            if (id.isBlank()) flowOf(false) else musicRepository.observeIsSongInAnyPlaylist(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        playerManager.connect()
        viewModelScope.launch {
            while (true) {
                playerManager.pollPosition()
                delay(500)
            }
        }
    }

    fun playSong(song: SongEntity) {
        viewModelScope.launch {
            runCatching { musicRepository.resolvePlayable(song) }
                .onSuccess { playable ->
                    playerManager.playSingle(
                        id = playable.songId,
                        title = playable.title,
                        artist = playable.artist,
                        artworkUrl = playable.artworkUrl,
                        streamUrl = playable.streamUrl
                    )
                    musicRepository.recordOnlineRecent(playable)
                }
                .onFailure { e ->
                    _playbackError.value = "Couldn't play \"${song.title}\" — ${describeError(e)}"
                }
        }
    }

    fun playSongList(songs: List<SongEntity>, startIndex: Int) {
        if (songs.isEmpty()) return
        val safeStartIndex = startIndex.coerceIn(0, songs.lastIndex)
        val startSong = songs[safeStartIndex]
        val restOfList = songs.filterIndexed { index, _ -> index != safeStartIndex }

        viewModelScope.launch {
            val startPlayable = runCatching { musicRepository.resolvePlayable(startSong) }
                .onFailure { e ->
                    _playbackError.value = "Couldn't play \"${startSong.title}\" — ${describeError(e)}"
                }
                .getOrNull() ?: return@launch

            playerManager.startQueueWith(
                id = startPlayable.songId,
                title = startPlayable.title,
                artist = startPlayable.artist,
                artworkUrl = startPlayable.artworkUrl,
                item = startPlayable.toMediaItem()
            )
            musicRepository.recordOnlineRecent(startPlayable)

            var failures = 0
            restOfList.forEach { song ->
                runCatching { musicRepository.resolvePlayable(song) }
                    .onSuccess { playable ->
                        playerManager.appendToQueue(playable.toMediaItem())
                        musicRepository.recordOnlineRecent(playable)
                    }
                    .onFailure { failures++ }
            }

            if (failures > 0) {
                _playbackError.value = "Skipped $failures song(s) that couldn't be resolved."
            }
        }
    }

    private fun com.aravind.wavve.data.repository.PlayableSong.toMediaItem(): MediaItem =
        MediaItem.Builder()
            .setUri(streamUrl)
            .setMediaId(songId)
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setArtworkUri(artworkUrl?.let { android.net.Uri.parse(it) })
                    .build()
            )
            .build()

    fun playRadio(station: RadioStation) {
        playerManager.playSingle(
            id = station.id,
            title = station.name,
            artist = station.subtitle,
            artworkUrl = station.artworkUrl,
            streamUrl = station.streamUrl,
            isLive = true
        )
    }

    fun playPodcastEpisode(
        guid: String,
        title: String,
        showTitle: String,
        artworkUrl: String?,
        audioUrl: String
    ) {
        playerManager.playSingle(
            id = guid,
            title = title,
            artist = showTitle,
            artworkUrl = artworkUrl,
            streamUrl = audioUrl
        )
    }

    /** Step 7 + Step 6 + Step 5: plays a device track, records it as a Recent,
     *  AND caches it into the songs table (upsertSong) — needed so it can be
     *  looked up later when adding it to a playlist from the player screen. */
    fun playLocalTrack(track: LocalAudioTrack) {
        playerManager.playSingle(
            id = "local_${track.id}",
            title = track.title,
            artist = track.artist,
            artworkUrl = null,
            streamUrl = track.contentUri
        )
        viewModelScope.launch {
            musicRepository.recordDeviceRecent(track)
            musicRepository.upsertSong(
                SongEntity(
                    songId = "local_${track.id}",
                    title = track.title,
                    artist = track.artist,
                    artworkUrl = null,
                    durationSec = track.durationMs / 1000,
                    localContentUri = track.contentUri
                )
            )
        }
    }

    fun playRecentSearch(entry: RecentSearchEntity) {
        if (entry.source == "DEVICE") {
            val uri = entry.streamUri
            if (uri == null) {
                _playbackError.value = "Couldn't play \"${entry.title}\" — missing local file reference."
                return
            }
            playerManager.playSingle(
                id = entry.songId,
                title = entry.title,
                artist = entry.artist,
                artworkUrl = entry.artworkUrl,
                streamUrl = uri
            )
            return
        }

        viewModelScope.launch {
            val song = musicRepository.getSongById(entry.songId)
            if (song == null) {
                _playbackError.value = "Couldn't play \"${entry.title}\" — song details no longer cached."
                return@launch
            }
            playSong(song)
        }
    }

    /** Step 5: adds the currently playing song to the given playlist. Looks up
     *  the cached SongEntity by id (works for anything played via search,
     *  playlists, or the Device tab, since all three now cache into songs);
     *  falls back to building one from live playback state for the rare case
     *  it's genuinely uncached (e.g. still mid-resolve). */
    fun addCurrentSongToPlaylist(playlistId: Long) {
        viewModelScope.launch {
            val current = nowPlaying.value
            if (current.id.isBlank()) return@launch

            val song = musicRepository.getSongById(current.id) ?: SongEntity(
                songId = current.id,
                title = current.title,
                artist = current.artist,
                artworkUrl = current.artworkUrl,
                durationSec = durationMs.value / 1000
            )
            musicRepository.addToPlaylist(playlistId, song)
        }
    }

    suspend fun searchSongs(query: String): List<SongEntity> = musicRepository.searchSongs(query)

    suspend fun getTodaySearchCount(): Int = musicRepository.getTodaySearchCount()
    fun getMaxDailySearches(): Int = musicRepository.getMaxDailySearches()

    fun togglePlayPause() = playerManager.togglePlayPause()
    fun next() = playerManager.next()
    fun previous() = playerManager.previous()
    fun seekTo(ms: Long) = playerManager.seekTo(ms)
    fun toggleRepeat() = playerManager.toggleRepeat()
    fun toggleShuffle() = playerManager.toggleShuffle()

    fun setEqEnabled(on: Boolean) = equalizerController.setEnabled(on)
    fun setEqBand(band: Short, levelMb: Short) = equalizerController.setBandLevel(band, levelMb)

    fun applyEqPreset(gains: List<Int>) = equalizerController.applyPreset(gains)

    fun saveCurrentEqAsPreset(name: String) {
        viewModelScope.launch {
            musicRepository.saveEqPreset(name, equalizerController.currentGainsAsList())
        }
    }

    val savedPresets = musicRepository.observeEqPresets()

    private fun describeError(e: Throwable): String = when (e) {
        is SearchQuotaExceededException -> e.message ?: "Daily search limit reached."
        is retrofit2.HttpException ->
            if (e.code() == 500) "the streaming service is temporarily unavailable"
            else "streaming service error (HTTP ${e.code()})"
        is java.io.IOException -> "check your internet connection"
        else -> e.message ?: "unknown error"
    }
}