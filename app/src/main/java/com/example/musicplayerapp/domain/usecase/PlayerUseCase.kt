package com.example.musicplayerapp.domain.usecase

import com.example.musicplayerapp.data.model.MusicTrack
import com.example.musicplayerapp.player.service.MusicServiceConnection
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class PlayerUseCase @Inject constructor(
    private val musicServiceConnection: MusicServiceConnection
) {
    // 📡 Flujos expuestos como StateFlow inmutable
    val currentTrack: StateFlow<MusicTrack?> get() = musicServiceConnection.currentTrack
    val currentPosition: StateFlow<Long> get() = musicServiceConnection.currentPosition
    val isPlaying: StateFlow<Boolean> get() = musicServiceConnection.isPlaying
    val isShuffleModeEnabled: StateFlow<Boolean> get() = musicServiceConnection.isShuffleEnabled
    val repeatMode: StateFlow<Int> get() = musicServiceConnection.repeatMode
    val sleepTimerMinutes: StateFlow<Int?> get() = musicServiceConnection.sleepTimerMinutes
    val playlistId: StateFlow<Long?> get() = musicServiceConnection.playlistRec

    fun play(track: MusicTrack) {
        musicServiceConnection.play(track)
    }

    fun pause() {
        musicServiceConnection.pause()
    }

    fun next() {
        musicServiceConnection.next()
    }

    fun previous() {
        musicServiceConnection.previous()
    }

    fun toggleShuffle() {
        musicServiceConnection.toggleShuffle()
    }

    fun toggleRepeatMode() {
        musicServiceConnection.toggleRepeatMode()
    }

    fun setSleepTimer(minutes: Int) {
        musicServiceConnection.setSleepTimer(minutes)
    }

    fun cancelSleepTimer() {
        musicServiceConnection.cancelSleepTimer()
    }

    fun updateCurrentTrackTitle(newTitle: String) {
        musicServiceConnection.updateCurrentTrackTitle(newTitle)
    }

    fun seekTo(position: Long) {
        musicServiceConnection.seekTo(position)
    }

    fun queueNext(trackId: String) {
        if(trackId.isEmpty()) return
        musicServiceConnection.queueNext(trackId)
    }

    fun setPlaylist(tracks: List<MusicTrack>, startIndex: Int, playlistId: Long) {
        if (tracks.isNotEmpty()) {
            musicServiceConnection.setPlaylist(tracks, startIndex, playlistId)
        }
    }

    fun getQueue(): List<MusicTrack> {
        return musicServiceConnection.getQueue()
    }

    fun moveTrack(fromIndex: Int, toIndex: Int) {
        musicServiceConnection.moveTrack(fromIndex, toIndex)
    }

    fun connect() = musicServiceConnection.connect()
    fun disconnect() = musicServiceConnection.disconnect()
}
