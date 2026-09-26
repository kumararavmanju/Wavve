package com.aravind.wavve.ui.screens.playlists

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aravind.wavve.ui.player.PlayerViewModel
import com.aravind.wavve.ui.screens.songs.SongRow

@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    playerViewModel: PlayerViewModel,
    viewModel: PlaylistDetailViewModel = hiltViewModel()
) {
    val songsFlow = remember(playlistId) { viewModel.songsIn(playlistId) }
    val songs by songsFlow.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Playlist", style = MaterialTheme.typography.headlineMedium)

        if (songs.isEmpty()) {
            Text(
                "No songs yet. Add some from the Songs tab.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp)
            )
        }

        LazyColumn(modifier = Modifier.padding(top = 12.dp)) {
            items(songs, key = { it.songId }) { song ->
                SongRow(
                    song = song,
                    onClick = { playerViewModel.playSongList(songs, songs.indexOf(song)) },
                    trailing = {
                        IconButton(onClick = { viewModel.removeSong(playlistId, song.songId) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove")
                        }
                    }
                )
            }
        }
    }
}
