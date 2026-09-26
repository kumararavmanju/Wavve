package com.aravind.wavve.ui.screens.local

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aravind.wavve.data.local.LocalAudioTrack
import com.aravind.wavve.data.local.LocalMusicScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocalMusicViewModel @Inject constructor(
    private val scanner: LocalMusicScanner
) : ViewModel() {

    private val _tracks = MutableStateFlow<List<LocalAudioTrack>>(emptyList())
    val tracks: StateFlow<List<LocalAudioTrack>> = _tracks

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    /** True while the "include WhatsApp / recordings?" dialog should be shown. */
    private val _showScanOptionsDialog = MutableStateFlow(false)
    val showScanOptionsDialog: StateFlow<Boolean> = _showScanOptionsDialog

    private var hasScannedOnce = false

    /** Called once permission is confirmed granted, on first screen entry only —
     *  doesn't show the options dialog, just loads whatever was previously
     *  scanned with default (music-only) options, so the screen isn't empty
     *  on first visit. Subsequent scans go through the refresh button + dialog. */
    fun loadInitialIfNeeded() {
        if (hasScannedOnce) return
        hasScannedOnce = true
        runScan(includeWhatsApp = false, includeRecordings = false)
    }

    fun onRefreshClicked() {
        _showScanOptionsDialog.value = true
    }

    fun dismissScanOptionsDialog() {
        _showScanOptionsDialog.value = false
    }

    fun confirmScan(includeWhatsApp: Boolean, includeRecordings: Boolean) {
        _showScanOptionsDialog.value = false
        runScan(includeWhatsApp, includeRecordings)
    }

    private fun runScan(includeWhatsApp: Boolean, includeRecordings: Boolean) {
        viewModelScope.launch {
            _isScanning.value = true
            _tracks.value = scanner.scan(includeWhatsApp, includeRecordings)
            _isScanning.value = false
        }
    }
}