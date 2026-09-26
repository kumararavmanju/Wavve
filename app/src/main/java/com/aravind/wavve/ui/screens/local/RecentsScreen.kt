package com.aravind.wavve.ui.screens.local

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aravind.wavve.data.db.RecentSearchEntity
import com.aravind.wavve.ui.player.PlayerViewModel

@Composable
fun RecentsScreen(
    playerViewModel: PlayerViewModel,
    viewModel: RecentsViewModel = hiltViewModel()
) {
    val recents by viewModel.recents.collectAsState()
    val quotaLabel by viewModel.quotaLabel.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshQuotaLabel()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Recents", style = MaterialTheme.typography.titleLarge)
            Text(
                quotaLabel,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (recents.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Songs you search for or play from your device will show up here.")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(recents, key = { it.songId }) { entry ->
                    RecentRow(entry = entry, onClick = { playerViewModel.playRecentSearch(entry) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun RecentRow(entry: RecentSearchEntity, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(entry.title) },
        supportingContent = { Text(entry.artist) },
        leadingContent = {
            Icon(
                imageVector = if (entry.source == "DEVICE") Icons.Filled.PhoneAndroid else Icons.Filled.CloudQueue,
                contentDescription = if (entry.source == "DEVICE") "On device" else "Online"
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    )
}