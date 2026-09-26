package com.aravind.wavve.data.repository

import android.util.Log
import com.aravind.wavve.data.db.AlbumDao
import com.aravind.wavve.data.db.EqPresetDao
import com.aravind.wavve.data.db.EqPresetEntity
import com.aravind.wavve.data.db.PlaylistDao
import com.aravind.wavve.data.db.PlaylistEntity
import com.aravind.wavve.data.db.PlaylistSongCrossRef
import com.aravind.wavve.data.db.RecentSearchDao
import com.aravind.wavve.data.db.RecentSearchEntity
import com.aravind.wavve.data.db.SearchQuotaDao
import com.aravind.wavve.data.db.SearchQuotaEntity
import com.aravind.wavve.data.db.SongDao
import com.aravind.wavve.data.db.SongEntity
import com.aravind.wavve.data.itunes.ItunesApi
import com.aravind.wavve.data.local.LocalAudioTrack
import com.aravind.wavve.data.local.LocalMusicScanner
import com.aravind.wavve.data.newpipe.YouTubeExtractorSource
import com.aravind.wavve.data.piped.PipedApi
import com.aravind.wavve.data.piped.PipedAudioStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

data class PlayableSong(
    val songId: String,
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val durationSec: Long,
    val streamUrl: String
)

class SearchQuotaExceededException(val limit: Int) :
    Exception("Daily search limit of $limit reached. Try again tomorrow.")

@Singleton
class MusicRepository @Inject constructor(
    private val pipedApi: PipedApi,
    private val youTubeExtractorSource: YouTubeExtractorSource,
    private val itunesApi: ItunesApi,
    private val localMusicScanner: LocalMusicScanner,
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao,
    private val albumDao: AlbumDao,
    private val eqPresetDao: EqPresetDao,
    private val recentSearchDao: RecentSearchDao,
    private val searchQuotaDao: SearchQuotaDao
) {

    companion object {
        private const val MAX_BITRATE_KBPS = 120
        private const val MAX_DAILY_SEARCHES = 100
    }

    // ---- Search / browse (Songs tab) ----

    suspend fun searchSongs(query: String): List<SongEntity> = withContext(Dispatchers.IO) {
        val itunesMatch = try {
            itunesApi.search(term = query).results.firstOrNull { !it.trackName.isNullOrBlank() }
        } catch (e: Exception) {
            Log.w("MusicRepository", "iTunes lookup failed for \"$query\", using raw query", e)
            null
        }

        val effectiveQuery = if (itunesMatch != null) {
            "${itunesMatch.trackName} ${itunesMatch.artistName.orEmpty()}".trim()
        } else {
            query
        }

        val localMatches = localMusicScanner.searchLocal(effectiveQuery)
        if (localMatches.isNotEmpty()) {
            val localSongs = localMatches.map { track ->
                SongEntity(
                    songId = "local_${track.id}",
                    title = track.title,
                    artist = track.artist,
                    artworkUrl = null,
                    durationSec = track.durationMs / 1000,
                    localContentUri = track.contentUri
                )
            }
            songDao.upsertAll(localSongs)
            return@withContext localSongs
        }

        val today = LocalDate.now().toString()
        val currentCount = searchQuotaDao.get(today)?.count ?: 0
        if (currentCount >= MAX_DAILY_SEARCHES) {
            throw SearchQuotaExceededException(MAX_DAILY_SEARCHES)
        }

        val results = pipedApi.search(effectiveQuery, filter = "music_songs").items
            .filter { it.type == "stream" && it.videoId != null }
            .map {
                SongEntity(
                    songId = it.videoId!!,
                    title = it.title.orEmpty(),
                    artist = it.uploaderName.orEmpty(),
                    artworkUrl = it.thumbnail,
                    durationSec = it.duration
                )
            }
        songDao.upsertAll(results)
        searchQuotaDao.upsert(SearchQuotaEntity(date = today, count = currentCount + 1))

        results
    }

    suspend fun getTodaySearchCount(): Int = withContext(Dispatchers.IO) {
        searchQuotaDao.get(LocalDate.now().toString())?.count ?: 0
    }

    fun getMaxDailySearches(): Int = MAX_DAILY_SEARCHES

    fun observeLibrary(): Flow<List<SongEntity>> = songDao.observeAll()

    suspend fun getSongById(songId: String): SongEntity? = withContext(Dispatchers.IO) {
        songDao.getById(songId)
    }

    /** Step 5: used so a song played from a path that doesn't already cache it
     *  (e.g. Device tab playback) can still be looked up later — needed for
     *  the "add to playlist" flow to have full song metadata. */
    suspend fun upsertSong(song: SongEntity) = withContext(Dispatchers.IO) {
        songDao.upsert(song)
    }

    suspend fun recordOnlineRecent(song: PlayableSong) = withContext(Dispatchers.IO) {
        recentSearchDao.upsert(
            RecentSearchEntity(
                songId = song.songId,
                title = song.title,
                artist = song.artist,
                artworkUrl = song.artworkUrl,
                source = "ONLINE",
                streamUri = null,
                searchedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun recordDeviceRecent(track: LocalAudioTrack) = withContext(Dispatchers.IO) {
        recentSearchDao.upsert(
            RecentSearchEntity(
                songId = "local_${track.id}",
                title = track.title,
                artist = track.artist,
                artworkUrl = null,
                source = "DEVICE",
                streamUri = track.contentUri,
                searchedAt = System.currentTimeMillis()
            )
        )
    }

    fun observeRecentSearches(): Flow<List<RecentSearchEntity>> = recentSearchDao.observeRecent()

    suspend fun resolvePlayable(song: SongEntity): PlayableSong = withContext(Dispatchers.IO) {
        if (song.localContentUri != null) {
            return@withContext PlayableSong(
                songId = song.songId,
                title = song.title,
                artist = song.artist,
                artworkUrl = song.artworkUrl,
                durationSec = song.durationSec,
                streamUrl = song.localContentUri
            )
        }

        val streams = try {
            youTubeExtractorSource.getStreams(song.songId)
        } catch (e: Exception) {
            Log.w("MusicRepository", "NewPipeExtractor failed for ${song.songId}, falling back to Piped", e)
            pipedApi.getStreams(song.songId)
        }

        val playableStreams = streams.audioStreams.filter { !it.url.isNullOrBlank() }
        val best = selectByBitrateCap(playableStreams)
            ?: error("No playable audio stream for ${song.title}")

        Log.d(
            "MusicRepository",
            "Selected ${best.bitrate}kbps stream for ${song.songId} (cap: ${MAX_BITRATE_KBPS}kbps)"
        )

        PlayableSong(
            songId = song.songId,
            title = song.title,
            artist = song.artist,
            artworkUrl = song.artworkUrl,
            durationSec = song.durationSec,
            streamUrl = best.url!!
        )
    }

    private fun selectByBitrateCap(streams: List<PipedAudioStream>): PipedAudioStream? {
        val withinCap = streams.filter { it.bitrate in 1..MAX_BITRATE_KBPS }
        return withinCap.maxByOrNull { it.bitrate }
            ?: streams.maxByOrNull { it.bitrate }
    }

    // ---- Playlists ----

    fun observePlaylists(): Flow<List<PlaylistEntity>> = playlistDao.observePlaylists()

    fun observeSongsInPlaylist(playlistId: Long): Flow<List<SongEntity>> =
        playlistDao.observeSongsInPlaylist(playlistId)

    suspend fun createPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        playlistDao.insertPlaylist(PlaylistEntity(name = name))
    }

    suspend fun addToPlaylist(playlistId: Long, song: SongEntity) = withContext(Dispatchers.IO) {
        songDao.upsert(song)
        val position = playlistDao.nextPosition(playlistId)
        playlistDao.addSongToPlaylist(PlaylistSongCrossRef(playlistId, song.songId, position))
    }

    suspend fun removeFromPlaylist(playlistId: Long, songId: String) = withContext(Dispatchers.IO) {
        playlistDao.removeSongFromPlaylist(playlistId, songId)
    }

    /** Step 5: powers the player's +/✓ icon — true if songId is in any playlist. */
    fun observeIsSongInAnyPlaylist(songId: String): Flow<Boolean> =
        playlistDao.observeIsSongInAnyPlaylist(songId)

    // ---- Albums ----

    fun observeAlbums() = albumDao.observeAlbums()
    fun observeSongsInAlbum(albumId: String) = albumDao.observeSongsInAlbum(albumId)

    // ---- EQ presets ----

    fun observeEqPresets(): Flow<List<EqPresetEntity>> = eqPresetDao.observeAll()

    suspend fun saveEqPreset(name: String, bandGainsMb: List<Int>): Long = withContext(Dispatchers.IO) {
        eqPresetDao.upsert(
            EqPresetEntity(name = name, bandGainsMb = bandGainsMb.joinToString(","))
        )
    }

    suspend fun deleteEqPreset(preset: EqPresetEntity) = withContext(Dispatchers.IO) {
        eqPresetDao.delete(preset)
    }
}