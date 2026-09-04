package com.example.musicplayerapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.example.musicplayerapp.data.model.MusicTrack
import com.example.musicplayerapp.data.repository.MusicRepository
import com.example.musicplayerapp.domain.usecase.PlayerUseCase
import com.example.musicplayerapp.domain.usecase.ScanMusicUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class TrackSortOption(val displayName: String) {
    NOMBRE("Nombre"),
    DURACION("Duración"),
    RECIENTES("Recientes"),
    ARTISTA("Artista")
}

sealed class MusicListUiState {
    object Loading : MusicListUiState()
    data class Success(val tracks: List<MusicTrack>) : MusicListUiState()
    data class Error(val message: String) : MusicListUiState()
}

@HiltViewModel
class MusicListViewModel @Inject constructor(
    private val scanMusicUseCase: ScanMusicUseCase,
    private val playerUseCase: PlayerUseCase,
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _rawTracks = MutableStateFlow<List<MusicTrack>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOption = MutableStateFlow(TrackSortOption.NOMBRE)
    val sortOption: StateFlow<TrackSortOption> = _sortOption.asStateFlow()

    private val _isAscending = MutableStateFlow(true)
    val isAscending: StateFlow<Boolean> = _isAscending.asStateFlow()

    private val _uiState = MutableStateFlow<MusicListUiState>(MusicListUiState.Loading)
    val uiState: StateFlow<MusicListUiState> = combine(
        _uiState,
        _rawTracks,
        _searchQuery,
        _sortOption,
        _isAscending
    ) { state, rawTracks, query, sort, asc ->
        if (state is MusicListUiState.Success) {
            val filtered = if (query.isBlank()) {
                rawTracks
            } else {
                rawTracks.filter {
                    it.title.contains(query, ignoreCase = true) ||
                            it.artist.contains(query, ignoreCase = true)
                }
            }

            val sorted = when (sort) {
                TrackSortOption.NOMBRE -> filtered.sortedBy { it.title.lowercase() }
                TrackSortOption.DURACION -> filtered.sortedBy { it.duration }
                TrackSortOption.RECIENTES -> filtered.sortedBy { it.id.toLongOrNull() ?: 0L }
                TrackSortOption.ARTISTA -> filtered.sortedBy { it.artist.lowercase() }
            }

            val resultTracks = if (asc) sorted else sorted.reversed()
            MusicListUiState.Success(resultTracks)
        } else {
            state
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MusicListUiState.Loading)

    // 🎵 Flows del reproductor con valores iniciales para evitar nulls
    val currentTrack = playerUseCase.currentTrack.stateIn(viewModelScope, SharingStarted.Lazily, null)
    val currentPosition = playerUseCase.currentPosition.stateIn(viewModelScope, SharingStarted.Lazily, 0L)
    val isPlaying = playerUseCase.isPlaying.stateIn(viewModelScope, SharingStarted.Lazily, false)
    val isShuffleModeEnabled = playerUseCase.isShuffleModeEnabled.stateIn(viewModelScope, SharingStarted.Lazily, false)
    val repeatMode = playerUseCase.repeatMode.stateIn(viewModelScope, SharingStarted.Lazily, Player.REPEAT_MODE_OFF)
    val sleepTimerMinutes = playerUseCase.sleepTimerMinutes.stateIn(viewModelScope, SharingStarted.Lazily, null)
    val playlistId = playerUseCase.playlistId.stateIn(viewModelScope, SharingStarted.Lazily, -1L)

    init {
        playerUseCase.connect()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOption(option: TrackSortOption) {
        _sortOption.value = option
    }

    fun setSortOptionByString(optionName: String) {
        TrackSortOption.values().find { it.displayName.equals(optionName, ignoreCase = true) }?.let {
            _sortOption.value = it
        }
    }

    fun toggleAscending() {
        _isAscending.value = !_isAscending.value
    }

    fun playTrack(track: MusicTrack) = playerUseCase.play(track)
    fun pauseTrack() = playerUseCase.pause()
    fun nextTrack() = playerUseCase.next()
    fun previousTrack() = playerUseCase.previous()
    fun toggleShuffle() = playerUseCase.toggleShuffle()
    fun toggleRepeatMode() = playerUseCase.toggleRepeatMode()
    fun setSleepTimer(minutes: Int) = playerUseCase.setSleepTimer(minutes)
    fun cancelSleepTimer() = playerUseCase.cancelSleepTimer()

    fun updateTrackTitle(trackId: String, newTitle: String) {
        viewModelScope.launch(Dispatchers.IO) {
            musicRepository.updateTrackTitle(trackId, newTitle)
            playerUseCase.updateCurrentTrackTitle(newTitle)
        }
    }

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
                        _rawTracks.value = tracks
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
