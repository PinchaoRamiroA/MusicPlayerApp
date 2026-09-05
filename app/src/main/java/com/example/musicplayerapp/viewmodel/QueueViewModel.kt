package com.example.musicplayerapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayerapp.data.model.MusicTrack
import com.example.musicplayerapp.domain.usecase.PlayerUseCase
import com.example.musicplayerapp.player.service.MusicServiceConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QueueViewModel @Inject constructor(
    private val playerUseCase: PlayerUseCase,
) : ViewModel() {

    private val _queue = MutableStateFlow<List<MusicTrack>>(emptyList())
    val queue: StateFlow<List<MusicTrack>> = _queue

    val currentTrack: StateFlow<MusicTrack?> = playerUseCase.currentTrack
    val isPlaying: StateFlow<Boolean> = playerUseCase.isPlaying

    init {
        Log.d("QueueViewModel", "init called")
        loadQueue()
    }

    fun loadQueue() {
        Log.d("QueueViewModel", "Loading queue...")
        viewModelScope.launch {
            _queue.update { playerUseCase.getQueue() }
        }
        Log.d("QueueViewModel", "Queue loaded: ${_queue.value}")
    }

    fun moveTrack(fromIndex: Int, toIndex: Int) {
        Log.d("QueueViewModel", "Moving track from $fromIndex to $toIndex")
        playerUseCase.moveTrack(fromIndex, toIndex)
    }

    fun togglePlayPause() {
        if (playerUseCase.isPlaying.value) {
            playerUseCase.pause()
        } else {
            val track = playerUseCase.currentTrack.value ?: return
            playerUseCase.play(track)
        }
    }

    fun playTrack(track: MusicTrack) {
        playerUseCase.play(track)
    }
}
