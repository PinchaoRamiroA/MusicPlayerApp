package com.example.musicplayerapp.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.musicplayerapp.data.model.MusicTrack
import com.example.musicplayerapp.ui.components.*
import com.example.musicplayerapp.ui.theme.DarkColorScheme
import com.example.musicplayerapp.viewmodel.FavoritesViewModel
import com.example.musicplayerapp.viewmodel.MusicListUiState
import com.example.musicplayerapp.viewmodel.MusicListViewModel
import com.example.musicplayerapp.viewmodel.PlaylistViewModel
import kotlinx.coroutines.launch

@Composable
fun MusicListScreen(
    viewModel: MusicListViewModel = hiltViewModel(),
    playlistViewModel: PlaylistViewModel = hiltViewModel(),
    favoritesViewModel: FavoritesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val playlistId by viewModel.playlistId.collectAsState()
    val allPlaylists by playlistViewModel.uiState.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedSortOption by remember { mutableStateOf("Nombre") }
    var isAscending by remember { mutableStateOf(true) }

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
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        selectedSortOption = selectedSortOption,
                        onSortOptionChange = { selectedSortOption = it },
                        isAscending = isAscending,
                        onToggleAscending = { isAscending = !isAscending },
                        onTrackClick = onTrackClickLambda,
                        onMenuClick = onMenuClickLambda
                    )
                }
            }
        }
    }

    // Diálogos y modales
    if (isOptionsOpen) {
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
            onPlayNext = {
                selectedTrack?.let {
                    viewModel.queueNext(it.id)
                }
                isOptionsOpen = false
            },
            onDismiss = { isOptionsOpen = false }
        )
    }

    if (isPlaylistModalOpen) {
        PlaylistSelectionModal(
            playlists = allPlaylists.playlists,
            onDismiss = {
                isPlaylistModalOpen = false
                isOptionsOpen = false
            },
            onPlaylistSelected = { targetPlaylistId ->
                selectedTrack?.let {
                    playlistViewModel.addTrackToPlaylist(targetPlaylistId, it)
                }
                isPlaylistModalOpen = false
                isOptionsOpen = false
            }
        )
    }
}

@Composable
private fun MusicListContent(
    tracks: List<MusicTrack>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedSortOption: String,
    onSortOptionChange: (String) -> Unit,
    isAscending: Boolean,
    onToggleAscending: () -> Unit,
    onTrackClick: (MusicTrack) -> Unit,
    onMenuClick: (MusicTrack) -> Unit
) {
    var isSortMenuExpanded by remember { mutableStateOf(false) }
    val sortOptions = listOf("Nombre", "Duración", "Recientes", "Artista")

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val alphabetList = remember { ('A'..'Z').toList() + '#' }

    Column(modifier = Modifier.fillMaxSize()) {
        // 🔍 Buscador Superior
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Buscar canciones o artistas...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpiar búsqueda")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )

        // 🔽 Fila de Controles de Ordenamiento y Dirección
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    AssistChip(
                        onClick = { isSortMenuExpanded = true },
                        label = { Text("Orden: $selectedSortOption") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Ordenar por") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = "Desplegar") }
                    )
                    DropdownMenu(
                        expanded = isSortMenuExpanded,
                        onDismissRequest = { isSortMenuExpanded = false }
                    ) {
                        sortOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    onSortOptionChange(option)
                                    isSortMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // ⬆️⬇️ Botón de dirección de orden (Ascendente / Descendente)
                IconButton(onClick = onToggleAscending) {
                    Icon(
                        imageVector = if (isAscending) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isAscending) "Ascendente" else "Descendente",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = "${tracks.size} canciones",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        // 📜 Lista de Canciones con Barra de Navegación Lateral (Alfabeto A-Z Fast Scroll)
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 28.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(tracks, key = { index, track -> track.id }) { index, track ->
                    MusicListItem(
                        track = track,
                        onClick = { onTrackClick(track) },
                        showMenu = true,
                        onMenuClick = { onMenuClick(track) }
                    )
                }
            }

            // 🔤 Barra Lateral de Alfabeto (A-Z Fast Scroll Index Bar)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(end = 4.dp),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                alphabetList.forEach { letter ->
                    Text(
                        text = letter.toString(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        modifier = Modifier
                            .clickable {
                                // Buscar primer elemento que coincida con la letra
                                val targetIndex = tracks.indexOfFirst { track ->
                                    if (letter == '#') {
                                        track.title.firstOrNull()?.isLetter() == false
                                    } else {
                                        track.title.startsWith(letter, ignoreCase = true)
                                    }
                                }
                                if (targetIndex != -1) {
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(targetIndex)
                                    }
                                }
                            }
                            .padding(vertical = 1.dp, horizontal = 2.dp)
                    )
                }
            }
        }
    }
}

