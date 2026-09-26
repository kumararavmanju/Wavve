package com.aravind.wavve.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
        AlbumEntity::class,
        AlbumSongCrossRef::class,
        EqPresetEntity::class,
        PodcastStateEntity::class,
        RecentSearchEntity::class,
        SearchQuotaEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun albumDao(): AlbumDao
    abstract fun eqPresetDao(): EqPresetDao
    abstract fun podcastStateDao(): PodcastStateDao
    abstract fun recentSearchDao(): RecentSearchDao
    abstract fun searchQuotaDao(): SearchQuotaDao
}