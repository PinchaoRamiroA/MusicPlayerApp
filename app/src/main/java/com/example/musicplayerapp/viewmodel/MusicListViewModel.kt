package com.example.musicplayerapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayerapp.data.model.MusicTrack
import com.example.musicplayerapp.domain.usecase.PlayerUseCase
import com.example.musicplayerapp.domain.usecase.ScanMusicUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed class MusicListUiState {
    object Loading : MusicListUiState()
    data class Success(val tracks: List<MusicTrack>) : MusicListUiState()
    data class Error(val message: String) : MusicListUiState()
}

@HiltViewModel
class MusicListViewModel @Inject constructor(
    private val scanMusicUseCase: ScanMusicUseCase,
    private val playerUseCase: PlayerUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<MusicListUiState>(MusicListUiState.Loading)
    val uiState: StateFlow<MusicListUiState> = _uiState.asStateFlow()

    // 🎵 Flows del reproductor con valores iniciales para evitar nulls
    val currentTrack = playerUseCase.currentTrack.stateIn(viewModelScope, SharingStarted.Lazily, null)
    val currentPosition = playerUseCase.currentPosition.stateIn(viewModelScope, SharingStarted.Lazily, 0L)
    val isPlaying = playerUseCase.isPlaying.stateIn(viewModelScope, SharingStarted.Lazily, false)
    val isShuffleModeEnabled = playerUseCase.isShuffleModeEnabled.stateIn(viewModelScope, SharingStarted.Lazily, false)
    val playlistId = playerUseCase.playlistId.stateIn(viewModelScope, SharingStarted.Lazily, -1L)

    init {
        playerUseCase.connect()
    }

    fun playTrack(track: MusicTrack) = playerUseCase.play(track)
    fun pauseTrack() = playerUseCase.pause()
    fun nextTrack() = playerUseCase.next()
    fun previousTrack() = playerUseCase.previous()
    fun toggleShuffle() = playerUseCase.toggleShuffle()
    fun seekTo(position: Long) = playerUseCase.seekTo(position)
    fun queueNext(trackId: String) = playerUseCase.queueNext(trackId)

    fun setPlaylist(tracks: List<MusicTrack>, startIndex: Int, playlistId: Long) {
        playerUseCase.setPlaylist(tracks, startIndex, playlistId)
    }

    fun loadMusic() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = MusicListUiState.Loading

            runCatching { scanMusicUseCase() }
                .onSuccess { tracks ->
                    if (tracks.isNotEmpty()) {
                        if (playerUseCase.playlistId.value == null || playerUseCase.currentTrack.value == null) {
                            withContext(Dispatchers.Main) {
                                playerUseCase.setPlaylist(tracks, startIndex = 0, playlistId = -1)
                            }
                        }
                        _uiState.value = MusicListUiState.Success(tracks)
                    } else {
                        _uiState.value = MusicListUiState.Error("No se encontraron canciones")
                    }
                }
                .onFailure { e ->
                    _uiState.value = MusicListUiState.Error(e.message ?: "Error desconocido")
                }
        }
    }

    override fun onCleared() {
        playerUseCase.disconnect()
        super.onCleared()
    }
}
