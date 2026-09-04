package com.example.musicplayerapp.utils

import android.content.Context
import android.content.Intent
import com.example.musicplayerapp.data.model.MusicTrack

object ShareUtils {
    fun shareTrack(context: Context, track: MusicTrack) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(
                Intent.EXTRA_TEXT,
                "🎵 Escuchando: \"${track.title}\" de ${track.artist} en MusicPlayerApp"
            )
            type = "text/plain"
        }
        val chooserIntent = Intent.createChooser(shareIntent, "Compartir canción vía...")
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooserIntent)
    }
}
