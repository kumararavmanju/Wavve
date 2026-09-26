package com.aravind.wavve.ui.screens.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aravind.wavve.data.db.PlaylistEntity
import com.aravind.wavve.data.db.SongEntity
import com.aravind.wavve.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {

    val playlists: StateFlow<List<PlaylistEntity>> = musicRepository.observePlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { musicRepository.createPlaylist(name) }
    }
}

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {

    fun songsIn(playlistId: Long): StateFlow<List<SongEntity>> =
        musicRepository.observeSongsInPlaylist(playlistId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun removeSong(playlistId: Long, songId: String) {
        viewModelScope.launch { musicRepository.removeFromPlaylist(playlistId, songId) }
    }
}
