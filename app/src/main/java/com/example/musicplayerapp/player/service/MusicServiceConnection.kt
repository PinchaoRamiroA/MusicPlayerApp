package com.example.musicplayerapp.player.service

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.musicplayerapp.data.model.MusicTrack
import com.example.musicplayerapp.utils.toMediaItem
import com.example.musicplayerapp.utils.toMusicTrack
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicServiceConnection @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _currentTrack = MutableStateFlow<MusicTrack?>(null)
    val currentTrack: StateFlow<MusicTrack?> = _currentTrack.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    val playlistRec = MutableStateFlow<Long?>(null)

    private var controller: MediaController? = null
    private var progressJob: Job? = null

    private var originalList = mutableListOf<MediaItem>()
    private var currentList = mutableListOf<MediaItem>()

    /** ================================
     *  Player Controls
     *  ================================ */
    fun seekTo(position: Long) = controller?.seekTo(position)

    fun queueNext(trackId: String) {
        Log.d("MusicServiceConnection", "Queueing next track: $trackId")
        val track = currentList.find { it.mediaId == trackId } ?: return
        val currentIndex = controller?.currentMediaItemIndex ?: return

        // Evita acciones redundantes
        if (controller?.currentMediaItem?.mediaId == trackId ||
            controller?.getMediaItemAt(currentIndex + 1)?.mediaId == trackId) {
            Log.d("MusicServiceConnection", "Skipping duplicate track")
            return
        }

        val existingIndex = currentList.indexOfFirst { it.mediaId == trackId }
        if (existingIndex != -1) {
            controller?.moveMediaItem(existingIndex, ((controller?.nextMediaItemIndex
                ?: (currentIndex + 1))))
            Log.d("MusicServiceConnection", "Track ${track.mediaMetadata.title} moved from $existingIndex to ${controller?.nextMediaItemIndex}")
        } else {
            Log.d("MusicServiceConnection", "Adding track to queue: ${track.mediaId}")
            controller?.addMediaItem(currentIndex + 1, track)
        }
    }

    fun toggleShuffle() {
        if(!isConnected()) return
        _isShuffleEnabled.value = !_isShuffleEnabled.value
        val current = controller?.currentMediaItem
        val pos = controller?.currentPosition

        if (_isShuffleEnabled.value) {
            currentList.shuffle()
        } else {
            currentList = originalList.toMutableList()
        }

        controller?.setMediaItems(currentList)
        val newIndex = currentList.indexOfFirst { it.mediaId == current?.mediaId }
            .coerceAtLeast(0)
        controller?.seekTo(newIndex, pos ?: 0L)
        controller?.prepare()
        controller?.play()
    }

    fun getQueue(): List<MusicTrack> = currentList.map { it.toMusicTrack() }

    fun moveTrack(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        currentList.add(toIndex, currentList.removeAt(fromIndex))
        controller?.moveMediaItem(fromIndex, toIndex )
    }
    /** ================================
     *  Connection
     *  ================================ */
    fun connect() {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val futureController = MediaController.Builder(context, sessionToken).buildAsync()

        futureController.addListener({
            try {
                controller = futureController.get().apply {
                    addListener(controllerListener)
                }
                startProgressUpdates()
            } catch (e: Exception) {
                Log.e("MusicServiceConnection", "Controller connection failed", e)
            }
        }, Runnable::run)
    }

    fun disconnect() {
        progressJob?.cancel()
        controller?.release()
        controller = null
        resetState()
    }

    fun isConnected(): Boolean = controller?.isConnected == true

    /** ================================
     *  Playlist Management
     *  ================================ */
    fun setPlaylist(tracks: List<MusicTrack>, startIndex: Int, playlistId: Long) {
        playlistRec.value = playlistId
        originalList = tracks.map { it.toMediaItem() }.toMutableList()

        if (_isShuffleEnabled.value) {
            val shuffled = originalList.toMutableList()
            shuffled.shuffle()
            val startTrack = tracks.getOrNull(startIndex)?.toMediaItem()
            if (startTrack != null) {
                shuffled.remove(startTrack)
                shuffled.add(0, startTrack)
            }
            currentList = shuffled
            controller?.setMediaItems(currentList, 0, 0L)
        } else {
            currentList = originalList.toMutableList()
            controller?.setMediaItems(currentList, startIndex, 0L)
        }

        controller?.prepare()
        _currentTrack.value = getCurrentMetadata()
    }

    /** ================================
     *  Playback Actions
     *  ================================ */
    fun play(track: MusicTrack) {
        val index = currentList.indexOfFirst { it.mediaId == track.id }
        if (index != -1) {
            controller?.seekTo(index, 0L)
        } else {
            controller?.addMediaItem(track.toMediaItem())
            controller?.seekTo(currentList.size, 0L)
        }
        controller?.play()
    }

    fun pause() = controller?.pause()
    fun next() = controller?.seekToNext()
    fun previous() = controller?.seekToPrevious()

    /** ================================
     *  Helpers
     *  ================================ */

    private fun getCurrentMetadata(): MusicTrack? =
        controller?.currentMediaItem?.toMusicTrack()

    private fun resetState() {
        playlistRec.value = null
        _currentTrack.value = null
        _currentPosition.value = 0L
        _isPlaying.value = false
        _isShuffleEnabled.value = false
        originalList.clear()
        currentList.clear()
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                controller?.takeIf { it.isPlaying }?.let { _currentPosition.value = it.currentPosition }
                delay(500)
            }
        }
    }

    private val controllerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _currentTrack.value = mediaItem?.toMusicTrack()
            _currentPosition.value = controller?.currentPosition ?: 0L
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }
    }
}
