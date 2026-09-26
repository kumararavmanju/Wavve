@file:OptIn(ExperimentalMaterial3Api::class)
package com.aravind.wavve.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.aravind.wavve.playback.RepeatMode
import com.aravind.wavve.ui.screens.playlists.PlaylistsViewModel
import com.aravind.wavve.util.formatMillis

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(viewModel: PlayerViewModel, onCollapse: () -> Unit) {
    val nowPlaying by viewModel.nowPlaying.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val position by viewModel.positionMs.collectAsState()
    val duration by viewModel.durationMs.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val shuffleEnabled by viewModel.shuffleEnabled.collectAsState()
    val isInAnyPlaylist by viewModel.isCurrentSongInAnyPlaylist.collectAsState()

    var showEq by remember { mutableStateOf(false) }
    var showPlaylistPicker by remember { mutableStateOf(false) }
    var isDraggingSeek by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = onCollapse) {
                Icon(Icons.Filled.ExpandMore, contentDescription = "Collapse")
            }
            Row {
                IconButton(
                    onClick = { showPlaylistPicker = true },
                    enabled = nowPlaying.id.isNotBlank() && !nowPlaying.isLiveRadio
                ) {
                    Icon(
                        imageVector = if (isInAnyPlaylist) Icons.Filled.Check else Icons.Filled.Add,
                        contentDescription = if (isInAnyPlaylist) "In a playlist" else "Add to playlist",
                        tint = if (isInAnyPlaylist) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = { showEq = true }) {
                    Icon(Icons.Filled.Equalizer, contentDescription = "Equalizer")
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = nowPlaying.artworkUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
            )

            Text(
                text = nowPlaying.title,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 24.dp)
            )
            Text(
                text = nowPlaying.artist,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (nowPlaying.isLiveRadio) {
            Text(
                text = "● LIVE",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 16.dp)
            )
        } else {
            val sliderPos = if (isDraggingSeek) dragPosition else
                if (duration > 0) position.toFloat() / duration.toFloat() else 0f

            Slider(
                value = sliderPos.coerceIn(0f, 1f),
                onValueChange = {
                    isDraggingSeek = true
                    dragPosition = it
                },
                onValueChangeFinished = {
                    viewModel.seekTo((dragPosition * duration).toLong())
                    isDraggingSeek = false
                },
                modifier = Modifier.padding(top = 16.dp)
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatMillis(position), style = MaterialTheme.typography.labelSmall)
                Text(
                    "-" + formatMillis((duration - position).coerceAtLeast(0)),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = { viewModel.toggleShuffle() }) {
                Icon(
                    Icons.Filled.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (shuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = { viewModel.previous() }) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", modifier = Modifier.padding(4.dp))
            }
            IconButton(onClick = { viewModel.togglePlayPause() }) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.padding(4.dp)
                )
            }
            IconButton(onClick = { viewModel.next() }) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Next", modifier = Modifier.padding(4.dp))
            }
            IconButton(onClick = { viewModel.toggleRepeat() }) {
                Icon(
                    if (repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                    contentDescription = "Repeat",
                    tint = if (repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    if (showEq) {
        ModalBottomSheet(onDismissRequest = { showEq = false }) {
            EqualizerSheet(viewModel = viewModel)
        }
    }

    if (showPlaylistPicker) {
        AddToPlaylistDialog(
            onDismiss = { showPlaylistPicker = false },
            onPlaylistSelected = { playlistId ->
                viewModel.addCurrentSongToPlaylist(playlistId)
                showPlaylistPicker = false
            }
        )
    }
}

@Composable
private fun AddToPlaylistDialog(
    onDismiss: () -> Unit,
    onPlaylistSelected: (Long) -> Unit,
    playlistsViewModel: PlaylistsViewModel = hiltViewModel()
) {
    val playlists by playlistsViewModel.playlists.collectAsState()
    var showCreateField by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to playlist") },
        text = {
            Column {
                if (playlists.isEmpty() && !showCreateField) {
                    Text("You don't have any playlists yet.")
                } else {
                    playlists.forEach { playlist ->
                        ListItem(
                            headlineContent = { Text(playlist.name) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onPlaylistSelected(playlist.playlistId) }
                        )
                        Divider()
                    }
                }

                if (showCreateField) {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text("New playlist name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                    )
                } else {
                    TextButton(
                        onClick = { showCreateField = true },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text("Create new playlist", modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
        },
        confirmButton = {
            if (showCreateField) {
                TextButton(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            playlistsViewModel.createPlaylist(newPlaylistName)
                        }
                        onDismiss()
                    }
                ) {
                    Text("Create & close")
                }
            } else {
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
        dismissButton = {
            if (showCreateField) {
                TextButton(onClick = { showCreateField = false; newPlaylistName = "" }) {
                    Text("Cancel")
                }
            }
        }
    )
}