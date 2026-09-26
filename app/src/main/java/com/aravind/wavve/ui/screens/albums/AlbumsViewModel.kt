package com.aravind.wavve.ui.screens.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aravind.wavve.data.db.AlbumEntity
import com.aravind.wavve.data.db.SongEntity
import com.aravind.wavve.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AlbumsViewModel @Inject constructor(
    musicRepository: MusicRepository
) : ViewModel() {
    val albums: StateFlow<List<AlbumEntity>> = musicRepository.observeAlbums()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {
    fun songsIn(albumId: String): StateFlow<List<SongEntity>> =
        musicRepository.observeSongsInAlbum(albumId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
