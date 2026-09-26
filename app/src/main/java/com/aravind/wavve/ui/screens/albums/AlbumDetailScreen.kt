package com.aravind.wavve.ui.screens.albums

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
fun AlbumDetailScreen(
    albumId: String,
    playerViewModel: PlayerViewModel,
    viewModel: AlbumDetailViewModel = hiltViewModel()
) {
    val songsFlow = remember(albumId) { viewModel.songsIn(albumId) }
    val songs by songsFlow.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Album", style = MaterialTheme.typography.headlineMedium)

        LazyColumn(modifier = Modifier.padding(top = 12.dp)) {
            items(songs, key = { it.songId }) { song ->
                SongRow(
                    song = song,
                    onClick = { playerViewModel.playSongList(songs, songs.indexOf(song)) }
                )
            }
        }
    }
}
