package com.aravind.wavve.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A song known to the app. songId is the Piped/YouTube video id (or "local_<id>"
 * for an on-device match), which doubles as a stable, globally-unique key we
 * can use for playlists, "now playing", etc.
 *
 * localContentUri (Step 8): set only when this song was matched to an
 * on-device file during search — when present, resolvePlayable() uses it
 * directly and skips NewPipeExtractor/Piped entirely.
 */
@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val songId: String,
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val durationSec: Long,
    val album: String? = null,
    val localContentUri: String? = null
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val playlistId: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

/** Join table: which songs belong to which playlist, and in what order. */
@Entity(tableName = "playlist_songs", primaryKeys = ["playlistId", "songId"])
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songId: String,
    val position: Int
)

@Entity(tableName = "albums")
data class AlbumEntity(
    @PrimaryKey val albumId: String,
    val title: String,
    val artist: String,
    val artworkUrl: String?
)

@Entity(tableName = "album_songs", primaryKeys = ["albumId", "songId"])
data class AlbumSongCrossRef(
    val albumId: String,
    val songId: String,
    val trackNumber: Int
)

/** A user-saved 10-band equalizer preset. Gains are stored in millibels, one per band. */
@Entity(tableName = "eq_presets")
data class EqPresetEntity(
    @PrimaryKey(autoGenerate = true) val presetId: Long = 0,
    val name: String,
    val bandGainsMb: String,
    val isBuiltIn: Boolean = false
)

/** Tracks the most-recently-seen episode GUID per podcast so the worker can detect new drops. */
@Entity(tableName = "podcast_state")
data class PodcastStateEntity(
    @PrimaryKey val feedUrl: String,
    val lastSeenEpisodeGuid: String?,
    val lastCheckedAt: Long = 0
)

/**
 * One entry in the single "Recents" pill — a song that was actually PLAYED,
 * from either an online source (source = ONLINE) or the Device screen
 * (source = DEVICE). Keyed by songId so replaying the same song just bumps
 * its timestamp instead of duplicating the row.
 */
@Entity(tableName = "recent_searches")
data class RecentSearchEntity(
    @PrimaryKey val songId: String,
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val source: String,
    val streamUri: String?,
    val searchedAt: Long
)

/** Tracks how many online searches have been made on a given day (ISO date,
 *  e.g. "2026-09-26"), so we can cap it against YouTube's free API quota
 *  and warn the user before they hit it. */
@Entity(tableName = "search_quota")
data class SearchQuotaEntity(
    @PrimaryKey val date: String,
    val count: Int
)