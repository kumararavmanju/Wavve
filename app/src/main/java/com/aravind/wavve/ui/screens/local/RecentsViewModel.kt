package com.aravind.wavve.ui.screens.local

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aravind.wavve.data.db.RecentSearchEntity
import com.aravind.wavve.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecentsViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {

    val recents: StateFlow<List<RecentSearchEntity>> = musicRepository.observeRecentSearches()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _quotaLabel = MutableStateFlow("")
    val quotaLabel: StateFlow<String> = _quotaLabel

    init {
        refreshQuotaLabel()
    }

    fun refreshQuotaLabel() {
        viewModelScope.launch {
            val used = musicRepository.getTodaySearchCount()
            val max = musicRepository.getMaxDailySearches()
            _quotaLabel.value = "Searches today: $used/$max"
        }
    }
}