package com.aravind.wavve.ui.screens.podcasts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.aravind.wavve.data.podcast.PodcastEpisode
import com.aravind.wavve.ui.player.PlayerViewModel
import com.aravind.wavve.util.Constants
import com.aravind.wavve.util.formatMillis
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PodcastsScreen(playerViewModel: PlayerViewModel, viewModel: PodcastsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Podcasts", style = MaterialTheme.typography.headlineMedium)
            IconButton(onClick = { viewModel.refresh() }) {
                if (isRefreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                }
            }
        }
        Text(
            "New ${Constants.ROZ_AND_MOCHA_TITLE} episodes appear here automatically — no need to refresh.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )

        when (val s = state) {
            is PodcastUiState.Loading -> {
                Row(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator()
                }
            }
            is PodcastUiState.Error -> {
                Text(
                    "Couldn't load episodes: ${s.message}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
            is PodcastUiState.Loaded -> {
                LazyColumn {
                    items(s.feed.episodes, key = { it.guid }) { episode ->
                        EpisodeRow(
                            episode = episode,
                            showTitle = s.feed.title.ifBlank { Constants.ROZ_AND_MOCHA_TITLE },
                            onClick = {
                                playerViewModel.playPodcastEpisode(
                                    guid = episode.guid,
                                    title = episode.title,
                                    showTitle = s.feed.title.ifBlank { Constants.ROZ_AND_MOCHA_TITLE },
                                    artworkUrl = episode.imageUrl,
                                    audioUrl = episode.audioUrl
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)

@Composable
private fun EpisodeRow(episode: PodcastEpisode, showTitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = episode.imageUrl,
            contentDescription = null,
            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp))
        )
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(episode.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                text = buildString {
                    if (episode.pubDateMs > 0) append(dateFormat.format(Date(episode.pubDateMs)))
                    if (episode.durationSec > 0) {
                        if (isNotEmpty()) append("  •  ")
                        append(formatMillis(episode.durationSec * 1000))
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
