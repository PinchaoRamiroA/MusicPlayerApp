package com.example.musicplayerapp.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.musicplayerapp.data.model.MusicTrack
import com.example.musicplayerapp.ui.components.*
import com.example.musicplayerapp.ui.theme.DarkColorScheme
import com.example.musicplayerapp.viewmodel.FavoritesViewModel
import com.example.musicplayerapp.viewmodel.MusicListUiState
import com.example.musicplayerapp.viewmodel.MusicListViewModel
import com.example.musicplayerapp.viewmodel.PlaylistViewModel
import com.example.musicplayerapp.ui.components.TrackOptionsModal

@Composable
fun MusicListScreen(
    viewModel: MusicListViewModel = hiltViewModel(),
    playlistViewModel: PlaylistViewModel = hiltViewModel(),
    favoritesViewModel: FavoritesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val playlistId by viewModel.playlistId.collectAsState()
    val allPlaylists by playlistViewModel.uiState.collectAsState()

    var selectedTrack by remember { mutableStateOf<MusicTrack?>(null) }
    var isOptionsOpen by remember { mutableStateOf(false) }
    var isPlaylistModalOpen by remember { mutableStateOf(false) }

    MaterialTheme(colorScheme = DarkColorScheme) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (uiState) {
                is MusicListUiState.Loading -> LoadingContent()
                is MusicListUiState.Error -> ErrorContent((uiState as MusicListUiState.Error).message)
                is MusicListUiState.Success -> {
                    val tracks = (uiState as MusicListUiState.Success).tracks
                    val onTrackClickLambda = remember(tracks) {
                        { track: MusicTrack ->
                            viewModel.setPlaylist(tracks, tracks.indexOf(track), -1)
                            viewModel.playTrack(track)
                        }
                    }
                    val onMenuClickLambda = remember {
                        { track: MusicTrack ->
                            selectedTrack = track
                            isOptionsOpen = true
                        }
                    }
                    MusicListContent(
                        tracks = tracks,
                        onTrackClick = onTrackClickLambda,
                        onMenuClick = onMenuClickLambda
                    )
                }
            }
        }
    }

    // Dialogos ↓↓↓
    if (isOptionsOpen)
        TrackOptionsModal(
            track = selectedTrack,
            onAddToFavorites = {
                selectedTrack?.let { favoritesViewModel.toggleFavorite(it.id) }
                isOptionsOpen = false
                               },
            onAddToPlaylist = { isPlaylistModalOpen = true },
            onRemoveFromPlaylist = {
                selectedTrack?.let {
                    playlistViewModel.removeTrackFromPlaylist(playlistId ?: 0, it.id)
                }
                isOptionsOpen = false
            },
            onPlayNext = { selectedTrack?.let {
                viewModel.queueNext(selectedTrack!!.id)
            } },
            onDismiss = { isOptionsOpen = false }
        )


    if (isPlaylistModalOpen)
        PlaylistSelectionModal(
            playlists = allPlaylists.playlists,
            onDismiss = {
                isPlaylistModalOpen = false
                isOptionsOpen = false
                        },
            onPlaylistSelected = { playlistId ->
                selectedTrack?.let {
                    playlistViewModel.addTrackToPlaylist(playlistId, it)
                }
                isPlaylistModalOpen = false
                isOptionsOpen = false
            }
        )
}

@Composable
private fun MusicListContent(
    tracks: List<MusicTrack>,
    onTrackClick: (MusicTrack) -> Unit,
    onMenuClick: (MusicTrack) -> Unit
) {
    val trackCount by remember(tracks) { derivedStateOf { tracks.size } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        stickyHeader {
            Text(
                text = "$trackCount songs",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(vertical = 8.dp)
            )
        }
        items(tracks, key = { it.id }) { track ->
            MusicListItem(
                track = track,
                onClick = { onTrackClick(track) },
                showMenu = true,
                onMenuClick = { onMenuClick(track) }
            )
        }
    }
}
