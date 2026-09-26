package com.aravind.wavve.ui.screens.songs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.aravind.wavve.data.db.SongEntity
import com.aravind.wavve.ui.player.PlayerViewModel
import com.aravind.wavve.util.formatMillis

@Composable
fun SongsScreen(playerViewModel: PlayerViewModel, viewModel: SongsViewModel = hiltViewModel()) {
    val query by viewModel.query.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val library by viewModel.library.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val playlistsViewModel: com.aravind.wavve.ui.screens.playlists.PlaylistsViewModel = hiltViewModel()
    val playlists by playlistsViewModel.playlists.collectAsState()

    val listToShow = if (query.isBlank()) library else searchResults

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Songs", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            label = { Text("Search songs, artists…") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
            singleLine = true
        )

        if (isSearching) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
            }
        }

        if (!isSearching && listToShow.isEmpty()) {
            Text(
                if (query.isBlank()) "Search above to start building your library."
                else "No results for \"$query\".",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp)
            )
        }

        LazyColumn {
            items(listToShow, key = { it.songId }) { song ->
                var showMenu by remember { mutableStateOf(false) }
                Box {
                    SongRow(
                        song = song,
                        onClick = { playerViewModel.playSongList(listToShow, listToShow.indexOf(song)) },
                        trailing = {
                            Row {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(Icons.Filled.PlaylistAdd, contentDescription = "Add to playlist")
                                }
                                IconButton(onClick = { playerViewModel.playSongList(listToShow, listToShow.indexOf(song)) }) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = "Play")
                                }
                            }
                        }
                    )
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        if (playlists.isEmpty()) {
                            DropdownMenuItem(text = { Text("No playlists yet") }, onClick = { showMenu = false })
                        }
                        playlists.forEach { playlist ->
                            DropdownMenuItem(
                                text = { Text(playlist.name) },
                                onClick = {
                                    viewModel.addSongToPlaylist(playlist.playlistId, song)
                                    showMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SongRow(song: SongEntity, onClick: () -> Unit, trailing: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.artworkUrl,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                song.artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            formatMillis(song.durationSec * 1000),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (trailing != null) {
            trailing()
        } else {
            IconButton(onClick = onClick) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Play")
            }
        }
    }
}
