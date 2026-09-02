package com.example.musicplayerapp.domain.usecase

import com.example.musicplayerapp.data.database.relations.PlaylistWithTracks
import com.example.musicplayerapp.data.repository.PlaylistRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

data class PlaylistUseCases @Inject constructor(
    val createPlaylist: CreatePlaylistUseCase,
    val addTrackToPlaylist: AddTrackToPlaylistUseCase,
    val getPlaylistWithTracks: GetPlaylistWithTracksUseCase,
    val getPlaylists: GetPlaylistsUseCase,
    val removeTrackFromPlaylist: RemoveTrackFromPlaylistUseCase,
    val deletePlaylist: DeletePlaylistUseCase
)

/** Crear Playlist **/
class CreatePlaylistUseCase @Inject constructor(
    private val repository: PlaylistRepository
) {
    suspend operator fun invoke(name: String): Result<Long> {
        return if (name.isBlank()) {
            Result.failure(IllegalArgumentException("El nombre de la playlist no puede estar vacío"))
        } else {
            Result.success(repository.createPlaylist(name))
        }
    }
}

/** Agregar Track a Playlist **/
class AddTrackToPlaylistUseCase @Inject constructor(
    private val repository: PlaylistRepository
) {
    suspend operator fun invoke(playlistId: Long, trackId: String): Result<Unit> {
        return try {
            repository.addTrackToPlaylist(playlistId, trackId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/** Obtener Playlist con sus Tracks **/
class GetPlaylistWithTracksUseCase @Inject constructor(
    private val repository: PlaylistRepository
) {
    operator fun invoke(playlistId: Long): Flow<PlaylistWithTracks?> =
        repository.getPlaylistWithTracks(playlistId)
}

/** Obtener todas las Playlists **/
class GetPlaylistsUseCase @Inject constructor(
    private val repository: PlaylistRepository
) {
    operator fun invoke() = repository.getAllPlaylists()
}

/** Eliminar Playlist **/
class DeletePlaylistUseCase @Inject constructor(
    private val repository: PlaylistRepository
) {
    suspend operator fun invoke(playlistId: Long): Result<Unit> {
        return try {
            repository.deletePlaylistById(playlistId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/** Quitar Track de Playlist **/
class RemoveTrackFromPlaylistUseCase @Inject constructor(
    private val repository: PlaylistRepository
) {
    suspend operator fun invoke(playlistId: Long, trackId: String): Result<Unit> {
        return try {
            repository.removeTrackFromPlaylist(playlistId, trackId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
