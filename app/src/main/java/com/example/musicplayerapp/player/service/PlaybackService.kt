package com.example.musicplayerapp.player.service

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.example.musicplayerapp.MainActivity
import com.example.musicplayerapp.data.model.MusicTrack
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

class PlaybackService : MediaLibraryService() {

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaLibrarySession

    // Playlist actual
    private val allTracks = mutableListOf<MusicTrack>()

    override fun onCreate() {
        super.onCreate()
        Log.d("PlaybackService", "Create a service playback")

        player = ExoPlayer.Builder(this).build()

        mediaSession = MediaLibrarySession.Builder(this, player, SessionCallback())
            .setSessionActivity(getSessionActivityIntent())
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession =
        mediaSession

    override fun onDestroy() {
        mediaSession.release()
        player.release()
        super.onDestroy()
    }

    private fun getSessionActivityIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    private inner class SessionCallback : MediaLibrarySession.Callback {

        @OptIn(UnstableApi::class)
        override fun onAddMediaItems(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            val mapped = mediaItems.map {
                it.buildUpon()
                    .setMediaId(it.mediaId)
                    .setUri(it.localConfiguration?.uri)
                    .setMediaMetadata(it.mediaMetadata)
                    /*.setUri(it.playbackProperties?.uri)*/
                    .build()
            }.toMutableList()
            return Futures.immediateFuture(mapped)
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val rootItem = MediaItem.Builder()
                .setMediaId("root")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("Root Library")
                        .build()
                )
                .build()

            return Futures.immediateFuture(
                LibraryResult.ofItem(rootItem, params)
            )
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val count = player.mediaItemCount
            val items = (0 until count).map { index ->
                player.getMediaItemAt(index)
            }

            return Futures.immediateFuture(
                LibraryResult.ofItemList(ImmutableList.copyOf(items), params)
            )
        }
    }
}