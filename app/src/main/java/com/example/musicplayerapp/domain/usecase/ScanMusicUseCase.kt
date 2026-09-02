package com.example.musicplayerapp.domain.usecase

import com.example.musicplayerapp.data.repository.MusicRepository as TrackRepository
import com.example.musicplayerapp.data.model.MusicTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ScanMusicUseCase @Inject constructor(
    private val repository: TrackRepository
) {
    suspend operator fun invoke(): List<MusicTrack> = withContext(Dispatchers.IO) {
        // Actualiza DB con MediaStore
        repository.refreshTracks()

        // Retorna lista desde DB convertida a MusicTrack
        repository.getAllTracksFlow()
            .first()
            .map { trackEntity ->
                MusicTrack(
                    id = trackEntity.trackId,
                    title = trackEntity.title,
                    artist = trackEntity.artist,
                    album = trackEntity.album,
                    duration = trackEntity.duration,
                    data = trackEntity.data
                )
            }
    }
}
