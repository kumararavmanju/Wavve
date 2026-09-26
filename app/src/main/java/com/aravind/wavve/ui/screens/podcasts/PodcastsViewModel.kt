package com.aravind.wavve.ui.screens.podcasts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aravind.wavve.data.podcast.PodcastFeed
import com.aravind.wavve.data.podcast.PodcastRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PodcastUiState {
    data object Loading : PodcastUiState()
    data class Loaded(val feed: PodcastFeed) : PodcastUiState()
    data class Error(val message: String) : PodcastUiState()
}

@HiltViewModel
class PodcastsViewModel @Inject constructor(
    private val podcastRepository: PodcastRepository
) : ViewModel() {

    private val _state = MutableStateFlow<PodcastUiState>(PodcastUiState.Loading)
    val state: StateFlow<PodcastUiState> = _state

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = PodcastUiState.Loading
            runCatching { podcastRepository.fetchRozAndMocha() }
                .onSuccess { _state.value = PodcastUiState.Loaded(it) }
                .onFailure { _state.value = PodcastUiState.Error(it.message ?: "Couldn't load episodes") }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            runCatching { podcastRepository.fetchRozAndMocha() }
                .onSuccess { _state.value = PodcastUiState.Loaded(it) }
                .onFailure { _state.value = PodcastUiState.Error(it.message ?: "Couldn't refresh") }
            _isRefreshing.value = false
        }
    }
}
