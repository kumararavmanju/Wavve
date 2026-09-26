package com.aravind.wavve.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(song: SongEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(songs: List<SongEntity>)

    @Query("SELECT * FROM songs ORDER BY title ASC")
    fun observeAll(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE songId = :id")
    suspend fun getById(id: String): SongEntity?
}

@Dao
interface PlaylistDao {
    @Insert
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSongToPlaylist(ref: PlaylistSongCrossRef)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: String)

    @Transaction
    @Query(
        """SELECT songs.* FROM songs
           INNER JOIN playlist_songs ON songs.songId = playlist_songs.songId
           WHERE playlist_songs.playlistId = :playlistId
           ORDER BY playlist_songs.position ASC"""
    )
    fun observeSongsInPlaylist(playlistId: Long): Flow<List<SongEntity>>

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun nextPosition(playlistId: Long): Int

    /** Step 5: whether songId belongs to ANY playlist, for the player's +/✓ icon. */
    @Query("SELECT EXISTS(SELECT 1 FROM playlist_songs WHERE songId = :songId)")
    fun observeIsSongInAnyPlaylist(songId: String): Flow<Boolean>
}

@Dao
interface AlbumDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAlbum(album: AlbumEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSongToAlbum(ref: AlbumSongCrossRef)

    @Query("SELECT * FROM albums ORDER BY title ASC")
    fun observeAlbums(): Flow<List<AlbumEntity>>

    @Transaction
    @Query(
        """SELECT songs.* FROM songs
           INNER JOIN album_songs ON songs.songId = album_songs.songId
           WHERE album_songs.albumId = :albumId
           ORDER BY album_songs.trackNumber ASC"""
    )
    fun observeSongsInAlbum(albumId: String): Flow<List<SongEntity>>
}

@Dao
interface EqPresetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(preset: EqPresetEntity): Long

    @Delete
    suspend fun delete(preset: EqPresetEntity)

    @Query("SELECT * FROM eq_presets ORDER BY isBuiltIn DESC, name ASC")
    fun observeAll(): Flow<List<EqPresetEntity>>
}

@Dao
interface PodcastStateDao {
    @Query("SELECT * FROM podcast_state WHERE feedUrl = :feedUrl")
    suspend fun get(feedUrl: String): PodcastStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: PodcastStateEntity)
}

@Dao
interface RecentSearchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: RecentSearchEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<RecentSearchEntity>)

    @Query("SELECT * FROM recent_searches ORDER BY searchedAt DESC LIMIT 200")
    fun observeRecent(): Flow<List<RecentSearchEntity>>
}

@Dao
interface SearchQuotaDao {
    @Query("SELECT * FROM search_quota WHERE date = :date")
    suspend fun get(date: String): SearchQuotaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SearchQuotaEntity)
}