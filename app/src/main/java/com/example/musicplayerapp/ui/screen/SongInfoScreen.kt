package com.example.musicplayerapp.ui.screen

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
    var showEditTitleDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var isRepeatActive by remember { mutableStateOf(false) }
    var selectedTimerMinutes by remember { mutableStateOf<Int?>(null) }

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
        } else {
            Image(
                painter = painterResource(id = R.drawable.ic_music_note),
                contentDescription = "Album Art",
                modifier = Modifier.size(250.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        if (track == null) {
            Text(
                "No hay canción seleccionada",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            return
        }

        // Info de la canción con botón de editar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = { showEditTitleDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Editar nombre",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Text(
            text = track.artist,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Controles de acciones rápidas (Favorito, Repetir en bucle, Temporizador, Compartir, Playlist)
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = { favoritesViewModel.toggleFavorite(track.id) }) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorito",
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }

            // Reproducir en círculo / bucle
            IconButton(onClick = { isRepeatActive = !isRepeatActive }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reproducir en círculo",
                    tint = if (isRepeatActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }

            // Temporizador de apagado
            IconButton(onClick = { showSleepTimerDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Temporizador de apagado",
                    tint = if (selectedTimerMinutes != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }

            // Compartir canción vía WhatsApp y otras apps
            IconButton(onClick = {
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, "Escuchando: ${track.title} - ${track.artist}")
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, "Compartir canción vía..."))
            }) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Compartir",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // Añadir a playlist
            IconButton(onClick = { showPlaylistModal = true }) {
                Icon(
                    imageVector = Icons.Default.AddCircle,
                    contentDescription = "Añadir a playlist",
                    tint = MaterialTheme.colorScheme.onSurface
                )
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
            Text(formatTime(if (track.duration < 0) 0 else currentPosition), color = MaterialTheme.colorScheme.onSurface)
            Text(formatTime(track.duration), color = MaterialTheme.colorScheme.onSurface)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Controles principales
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { musicListViewModel.toggleShuffle() }) {
                Icon(
                    painter = painterResource(id = R.drawable.shuffle_24px),
                    contentDescription = "Aleatorio",
                    tint = if (musicListViewModel.isShuffleModeEnabled.collectAsState().value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = { musicListViewModel.previousTrack() }) {
                Icon(
                    painter = painterResource(id = R.drawable.skip_previous),
                    contentDescription = "Anterior",
                    tint = MaterialTheme.colorScheme.onSurface
                )
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
            IconButton(onClick = { musicListViewModel.nextTrack() }) {
                Icon(
                    painter = painterResource(id = R.drawable.skip_next),
                    contentDescription = "Siguiente",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = {
                navController.navigate(MusicNavDestinations.QUEUE_ROUTE)
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.queue_music),
                    contentDescription = "Cola de reproducción",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Modal para añadir a playlist
        if (showPlaylistModal) {
            Log.d("SongInfoScreen", "Playlist modal: ${track.title}")
            PlaylistSelectionModal(
                playlists = allPlaylists,
                onDismiss = { showPlaylistModal = false },
                onPlaylistSelected = { playlistId ->
                    playlistViewModel.addTrackToPlaylist(playlistId, track)
                    showPlaylistModal = false
                }
            )
        }

        // Diálogo para editar nombre de la canción
        if (showEditTitleDialog) {
            var newTitle by remember { mutableStateOf(track.title) }
            AlertDialog(
                onDismissRequest = { showEditTitleDialog = false },
                title = { Text("Editar nombre de la canción") },
                text = {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Nombre de la canción") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        Log.d("SongInfoScreen", "Nuevo título ingresado: $newTitle")
                        showEditTitleDialog = false
                    }) {
                        Text("Guardar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditTitleDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        // Diálogo para temporizador de apagado (Sleep Timer)
        if (showSleepTimerDialog) {
            val timerOptions = listOf(15, 30, 45, 60)
            AlertDialog(
                onDismissRequest = { showSleepTimerDialog = false },
                title = { Text("Temporizador de apagado") },
                text = {
                    Column {
                        Text(
                            text = if (selectedTimerMinutes != null) "Temporizador activo: $selectedTimerMinutes min" else "Selecciona el tiempo para detener la música:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        timerOptions.forEach { minutes ->
                            TextButton(
                                onClick = {
                                    selectedTimerMinutes = minutes
                                    Log.d("SongInfoScreen", "Alarma de apagado programada: $minutes min")
                                    showSleepTimerDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "$minutes minutos",
                                    color = if (selectedTimerMinutes == minutes) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        if (selectedTimerMinutes != null) {
                            TextButton(
                                onClick = {
                                    selectedTimerMinutes = null
                                    showSleepTimerDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Desactivar temporizador", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showSleepTimerDialog = false }) {
                        Text("Cerrar")
                    }
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

