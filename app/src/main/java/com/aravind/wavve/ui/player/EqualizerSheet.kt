package com.aravind.wavve.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aravind.wavve.playback.BuiltInEqPresets
import androidx.compose.ui.draw.rotate

@Composable
fun EqualizerSheet(viewModel: PlayerViewModel) {
    val bands by viewModel.eqBands.collectAsState()
    val gains by viewModel.eqGains.collectAsState()
    val enabled by viewModel.eqEnabled.collectAsState()
    val savedPresets by viewModel.savedPresets.collectAsState(initial = emptyList())

    var showSaveDialog by remember { mutableStateOf(false) }
    var presetName by remember { mutableStateOf("") }

    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Equalizer", style = MaterialTheme.typography.titleLarge)
            Switch(checked = enabled, onCheckedChange = { viewModel.setEqEnabled(it) })
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("Presets", style = MaterialTheme.typography.labelLarge)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            items(BuiltInEqPresets.all) { (name, gains) ->
                AssistChip(onClick = { viewModel.applyEqPreset(gains) }, label = { Text(name) })
            }
            items(savedPresets) { preset ->
                AssistChip(
                    onClick = {
                        viewModel.applyEqPreset(preset.bandGainsMb.split(",").map { it.toInt() })
                    },
                    label = { Text(preset.name) }
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        if (bands.isEmpty()) {
            Text(
                "Start playing something to unlock the equalizer.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            bands.forEach { band ->
                val gain = gains[band.index] ?: 0
                val freqLabel = if (band.centerFreqHz >= 1000)
                    "${band.centerFreqHz / 1000} kHz" else "${band.centerFreqHz} Hz"

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(freqLabel, modifier = Modifier.width(64.dp), style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = gain.toFloat(),
                        onValueChange = { viewModel.setEqBand(band.index, it.toInt().toShort()) },
                        valueRange = band.minMb.toFloat()..band.maxMb.toFloat(),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${"%.1f".format(gain / 100f)}dB",
                        modifier = Modifier.width(48.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        if (showSaveDialog) {
            OutlinedTextField(
                value = presetName,
                onValueChange = { presetName = it },
                label = { Text("Preset name") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.padding(top = 8.dp)) {
                Button(onClick = {
                    if (presetName.isNotBlank()) {
                        viewModel.saveCurrentEqAsPreset(presetName)
                        presetName = ""
                        showSaveDialog = false
                    }
                }) { Text("Save") }
            }
        } else {
            Button(onClick = { showSaveDialog = true }, enabled = bands.isNotEmpty()) {
                Text("Save current as preset")
            }
        }
    }
}

/** Vertical sliders aren't built into Material3, so we rotate a horizontal one 270°. */
private fun Modifier.graphicsRotateVertical(): Modifier = this.then(
    Modifier.rotate270()

)

private fun Modifier.rotate270(): Modifier = this.rotate(270f)