package com.pis.tvplayer.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pis.tvplayer.data.PlaylistRepository
import com.pis.tvplayer.data.RemoteConfig
import com.pis.tvplayer.data.model.Playlist
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    private val config = RemoteConfig(app)
    private val repository = PlaylistRepository(app, config)

    sealed interface UiState {
        data object Loading : UiState
        data class Ready(val playlist: Playlist, val fromCache: Boolean) : UiState
        data class Empty(val reason: String) : UiState
    }

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun loadPlaylist() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            _state.value = when (val result = repository.load()) {
                is PlaylistRepository.Result.Live ->
                    toReady(result.playlist, fromCache = false)

                is PlaylistRepository.Result.Cached ->
                    toReady(result.playlist, fromCache = true)

                is PlaylistRepository.Result.Empty ->
                    UiState.Empty(result.reason)
            }
        }
    }

    private fun toReady(playlist: Playlist, fromCache: Boolean): UiState =
        if (playlist.items.isEmpty()) {
            UiState.Empty("Playlist contains no items")
        } else {
            UiState.Ready(playlist, fromCache)
        }
}
