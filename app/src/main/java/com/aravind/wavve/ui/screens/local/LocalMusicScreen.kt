package com.aravind.wavve.ui.screens.local

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.aravind.wavve.ui.player.PlayerViewModel

@Composable
fun LocalMusicScreen(
    playerViewModel: PlayerViewModel,
    viewModel: LocalMusicViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val tracks by viewModel.tracks.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val showDialog by viewModel.showScanOptionsDialog.collectAsState()

    val audioPermission = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, audioPermission) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) viewModel.loadInitialIfNeeded()
    }

    LaunchedEffect(Unit) {
        if (hasPermission) {
            viewModel.loadInitialIfNeeded()
        } else {
            permissionLauncher.launch(audioPermission)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("On-Device Music", style = MaterialTheme.typography.titleLarge)
            IconButton(
                onClick = {
                    if (hasPermission) {
                        viewModel.onRefreshClicked()
                    } else {
                        permissionLauncher.launch(audioPermission)
                    }
                }
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh local music")
            }
        }

        when {
            !hasPermission -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Wavve needs permission to see music stored on your device.")
                    Button(
                        onClick = { permissionLauncher.launch(audioPermission) },
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        Text("Grant permission")
                    }
                }
            }
            isScanning -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Text("Scanning device storage…", modifier = Modifier.padding(top = 12.dp))
                }
            }
            tracks.isEmpty() -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("No local music found. Tap refresh to scan your device.")
                }
            }
            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(tracks, key = { it.id }) { track ->
                        ListItem(
                            headlineContent = { Text(track.title) },
                            supportingContent = { Text(track.artist) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { playerViewModel.playLocalTrack(track) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (showDialog) {
        ScanOptionsDialog(
            onDismiss = { viewModel.dismissScanOptionsDialog() },
            onConfirm = { includeWhatsApp, includeRecordings ->
                viewModel.confirmScan(includeWhatsApp, includeRecordings)
            }
        )
    }
}

@Composable
private fun ScanOptionsDialog(
    onDismiss: () -> Unit,
    onConfirm: (includeWhatsApp: Boolean, includeRecordings: Boolean) -> Unit
) {
    var includeWhatsApp by remember { mutableStateOf(false) }
    var includeRecordings by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Scan for music") },
        text = {
            Column {
                Text("Regular music is always included. Also include:")
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = includeWhatsApp, onCheckedChange = { includeWhatsApp = it })
                    Text("WhatsApp audio")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = includeRecordings, onCheckedChange = { includeRecordings = it })
                    Text("Locally recorded audio")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(includeWhatsApp, includeRecordings) }) {
                Text("Scan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}