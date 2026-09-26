package com.aravind.wavve.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aravind.wavve.ui.player.MiniPlayer
import com.aravind.wavve.ui.player.PlayerScreen
import com.aravind.wavve.ui.player.PlayerViewModel
import com.aravind.wavve.ui.screens.albums.AlbumDetailScreen
import com.aravind.wavve.ui.screens.albums.AlbumsScreen
import com.aravind.wavve.ui.screens.local.LocalMusicScreen
import com.aravind.wavve.ui.screens.local.RecentsScreen
import com.aravind.wavve.ui.screens.playlists.PlaylistDetailScreen
import com.aravind.wavve.ui.screens.playlists.PlaylistsScreen
import com.aravind.wavve.ui.screens.podcasts.PodcastsScreen
import com.aravind.wavve.ui.screens.radio.LiveRadioScreen
import com.aravind.wavve.ui.screens.songs.SongsScreen

@Composable
fun WavveNavHost() {
    val navController = rememberNavController()
    val playerViewModel: PlayerViewModel = hiltViewModel()

    val snackbarHostState = remember { SnackbarHostState() }
    val playbackError by playerViewModel.playbackError.collectAsState()
    LaunchedEffect(playbackError) {
        playbackError?.let {
            snackbarHostState.showSnackbar(it)
            playerViewModel.clearPlaybackError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column {
                MiniPlayer(
                    viewModel = playerViewModel,
                    onExpand = { navController.navigate(Routes.PLAYER) }
                )
                WavveBottomBar(navController)
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            NavHost(navController = navController, startDestination = Routes.SONGS) {
                composable(Routes.SONGS) {
                    SongsScreen(playerViewModel = playerViewModel)
                }
                composable(Routes.PLAYLISTS) {
                    PlaylistsScreen(
                        onOpenPlaylist = { id -> navController.navigate(Routes.playlistDetail(id)) }
                    )
                }
                composable(
                    Routes.PLAYLIST_DETAIL,
                    arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: 0L
                    PlaylistDetailScreen(playlistId = playlistId, playerViewModel = playerViewModel)
                }
                composable(Routes.LIVE_RADIO) {
                    LiveRadioScreen(playerViewModel = playerViewModel)
                }
                composable(Routes.ALBUMS) {
                    AlbumsScreen(onOpenAlbum = { id -> navController.navigate(Routes.albumDetail(id)) })
                }
                composable(
                    Routes.ALBUM_DETAIL,
                    arguments = listOf(navArgument("albumId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val albumId = backStackEntry.arguments?.getString("albumId") ?: ""
                    AlbumDetailScreen(albumId = albumId, playerViewModel = playerViewModel)
                }
                composable(Routes.PODCASTS) {
                    PodcastsScreen(playerViewModel = playerViewModel)
                }
                composable(Routes.LOCAL_MUSIC) {
                    LocalMusicScreen(playerViewModel = playerViewModel)
                }
                composable(Routes.RECENTS) {
                    RecentsScreen(playerViewModel = playerViewModel)
                }
                composable(Routes.PLAYER) {
                    PlayerScreen(
                        viewModel = playerViewModel,
                        onCollapse = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}