package com.aravind.wavve.ui.navigation

object Routes {
    const val SONGS = "songs"
    const val PLAYLISTS = "playlists"
    const val PLAYLIST_DETAIL = "playlist/{playlistId}"
    const val LIVE_RADIO = "live_radio"
    const val ALBUMS = "albums"
    const val ALBUM_DETAIL = "album/{albumId}"
    const val PODCASTS = "podcasts"
    const val PLAYER = "player"
    const val LOCAL_MUSIC = "local_music"
    const val RECENTS = "recents"

    fun playlistDetail(id: Long) = "playlist/$id"
    fun albumDetail(id: String) = "album/$id"
}

data class BottomTab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)