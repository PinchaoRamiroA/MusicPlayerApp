package com.example.musicplayerapp.ui.screen

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.musicplayerapp.R
import com.example.musicplayerapp.data.model.MusicTrack
import com.example.musicplayerapp.ui.components.PlaylistSelectionModal
import com.example.musicplayerapp.ui.nav.MusicNavDestinations
import com.example.musicplayerapp.utils.extractAlbumArt
import com.example.musicplayerapp.viewmodel.FavoritesViewModel
import com.example.musicplayerapp.viewmodel.MusicListViewModel
import com.example.musicplayerapp.viewmodel.PlaylistViewModel

import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import android.provider.Settings

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Composable
fun SongInfoScreen(
    track: MusicTrack?,
    navController: NavController,
    playlistViewModel: PlaylistViewModel = hiltViewModel(),
    musicListViewModel: MusicListViewModel = hiltViewModel(),
    favoritesViewModel: FavoritesViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isFavorite = favoritesViewModel.isFavorite(track?.id ?: "")
    val isPlaying = musicListViewModel.isPlaying.collectAsState().value
    val currentPosition = musicListViewModel.currentPosition.collectAsState().value

    var showPlaylistModal by remember { mutableStateOf(false) }
    val allPlaylists = playlistViewModel.uiState.collectAsState().value.playlists

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val albumArt = remember(track?.data) {
            track?.data?.let { extractAlbumArt(it) }
        }

        if (albumArt != null) {
            Image(
                bitmap = albumArt.asImageBitmap(),
                contentDescription = "Album Art",
                modifier = Modifier
                    .size(250.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }else{
            Image(
                painter = painterResource(id = R.drawable.ic_music_note),
                contentDescription = "Album Art",
                modifier = Modifier
                    .size(250.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        if (track == null) {
            Text("No hay canción seleccionada", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            return
        }

        // Info de la canción
        Text(track.title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        Text(track.artist, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))

        Spacer(modifier = Modifier.height(16.dp))

        // Controles superiores
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = { favoritesViewModel.toggleFavorite(track.id)} ) {
                Icon(
                    imageVector = if (favoritesViewModel.isFavorite(track.id)) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = { showPlaylistModal = true }) {
                Icon(Icons.Default.AddCircle, contentDescription = "Add to Playlist", tint = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = {
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, "Escuchando: ${track.title} - ${track.artist}")
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, "Compartir canción"))
            }) {
                Icon(Icons.Default.Share, contentDescription = "share", tint = MaterialTheme.colorScheme.onSurface)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Progreso
        if (track.duration > 0) {
            Slider(
                enabled = track.data.isNotEmpty(),
                value = currentPosition.coerceIn(0L, track.duration).toFloat(),
                onValueChange = { musicListViewModel.seekTo(it.toLong()) },
                valueRange = 0f..track.duration.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                ),
                modifier = Modifier.fillMaxWidth()

            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatTime(if(track.duration < 0) 0 else currentPosition), color = MaterialTheme.colorScheme.onSurface)
            Text(formatTime(track.duration), color = MaterialTheme.colorScheme.onSurface)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Controles principales
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { musicListViewModel.toggleShuffle()}) {
                Icon(
                    painter = painterResource(id = R.drawable.shuffle_24px),
                    contentDescription = "Shuffle",
                    tint = if (musicListViewModel.isShuffleModeEnabled.collectAsState().value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = {musicListViewModel.previousTrack()}) {
                Icon(painter = painterResource(id = R.drawable.skip_previous), contentDescription = "Previous", tint = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(
                onClick = {
                    if (isPlaying) {
                        musicListViewModel.pauseTrack()
                    } else {
                        musicListViewModel.playTrack(track)
                        musicListViewModel.seekTo(musicListViewModel.currentPosition.value)
                    }
                },
                modifier = Modifier
                    .size(64.dp)
                    .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape)
            ) {
                Icon(
                    painter = if (isPlaying) painterResource(id = R.drawable.pause) else painterResource(id = R.drawable.play_arrow),
                    contentDescription = "Play/Pause",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
            IconButton(onClick = {musicListViewModel.nextTrack()}) {
                Icon(painter = painterResource(id = R.drawable.skip_next), contentDescription = "Next", tint = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = {
                navController.navigate(MusicNavDestinations.QUEUE_ROUTE)
            }) {
                Icon(painter = painterResource(id = R.drawable.queue_music), contentDescription = "Queue", tint = MaterialTheme.colorScheme.onSurface)
            }
        }

        if (showPlaylistModal) {
            Log.d("MusicListScreen", "Playlist modal: ${track.title}")
            PlaylistSelectionModal(
                playlists = allPlaylists,
                onDismiss = { showPlaylistModal = false },
                onPlaylistSelected = { playlistId ->
                    playlistViewModel.addTrackToPlaylist(playlistId, track)
                    showPlaylistModal = false
                }
            )
        }

    }
}

fun formatTime(millis: Long): String {
    val minutes = millis / 60000
    val seconds = (millis % 60000) / 1000
    return "%02d:%02d".format(minutes, seconds)
}
