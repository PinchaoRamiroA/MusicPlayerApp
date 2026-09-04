package com.example.musicplayerapp.data.repository

import android.util.Log
import com.example.musicplayerapp.data.database.dao.FavoriteDao
import com.example.musicplayerapp.data.database.dao.MusicTrackDao
import com.example.musicplayerapp.data.database.entities.FavoriteEntity
import com.example.musicplayerapp.data.local.MusicMediaStoreDataSource
import kotlinx.coroutines.flow.Flow
import com.example.musicplayerapp.data.database.entities.MusicTrackEntity as TrackEntity

class MusicRepository(
    private val musicTrackDao: MusicTrackDao,
    private val favoriteDao: FavoriteDao,
    private val musicMediaStoreDataSource: MusicMediaStoreDataSource
) {
    fun getAllTracks(): Flow<List<TrackEntity>> = musicTrackDao.getAllTracks()
    fun getAllTracksFlow(): Flow<List<TrackEntity>> = musicTrackDao.getAllTracks()


    suspend fun refreshTracks() {
        val tracks = musicMediaStoreDataSource.getAllTracks()
        musicTrackDao.clearTracks()
        musicTrackDao.insertTracks(tracks)
    }

    suspend fun insertTracks(tracks: List<TrackEntity>) {
        musicTrackDao.insertTracks(tracks)
    }

    suspend fun getTrackById(id: String) = musicTrackDao.getTrackById(id)

    suspend fun updateTrackTitle(trackId: String, newTitle: String) {
        musicTrackDao.updateTrackTitle(trackId, newTitle)
    }

    // Favoritos
    fun getFavorites() = favoriteDao.getFavoriteTracks()
    suspend fun addFavorite(trackId: String) = favoriteDao.addFavorite(FavoriteEntity(
        trackId = trackId
    ))
    suspend fun removeFavorite(trackId: String) = favoriteDao.removeFavorite(trackId)
    suspend fun isFavorite(trackId: String) = favoriteDao.isFavorite(trackId)
}

