package com.example.musicplayerapp.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
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

    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val isAscending by viewModel.isAscending.collectAsState()

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
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        selectedSortOption = sortOption.displayName,
                        onSortOptionChange = { viewModel.setSortOptionByString(it) },
                        isAscending = isAscending,
                        onToggleAscending = { viewModel.toggleAscending() },
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

    // ⚡ Pre-calcular mapa de letra a índice O(1) para navegación ultra-rápida sin lag en UI thread
    val letterIndexMap = remember(tracks) {
        val map = mutableMapOf<Char, Int>()
        tracks.forEachIndexed { index, track ->
            val firstChar = track.title.firstOrNull()?.uppercaseChar()
            if (firstChar != null) {
                if (firstChar.isLetter() && firstChar in 'A'..'Z') {
                    map.putIfAbsent(firstChar, index)
                } else {
                    map.putIfAbsent('#', index)
                }
            }
        }
        map
    }

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
                itemsIndexed(
                    items = tracks,
                    key = { _, track -> track.id },
                    contentType = { _, _ -> "music_track_item" }
                ) { index, track ->
                    MusicListItem(
                        track = track,
                        onClick = { onTrackClick(track) },
                        showMenu = true,
                        onMenuClick = { onMenuClick(track) }
                    )
                }
            }

            if (selectedSortOption.equals("Nombre", ignoreCase = true)) {
                // 🔤 Barra Lateral de Alfabeto (Solo cuando el orden es por Nombre)
                val alphabetScrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(22.dp)
                        .padding(vertical = 8.dp, horizontal = 2.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .verticalScroll(alphabetScrollState)
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    alphabetList.forEach { letter ->
                        Text(
                            text = letter.toString(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable {
                                    val targetIndex = letterIndexMap[letter] ?: -1
                                    if (targetIndex != -1) {
                                        coroutineScope.launch {
                                            listState.scrollToItem(targetIndex)
                                        }
                                    }
                                }
                                .padding(vertical = 2.dp, horizontal = 4.dp)
                        )
                    }
                }
            } else {
                // 🎚️ Barra de Desplazamiento Arrastrable (Thumb Fast Scroll) para Duración, Recientes y Artista
                var containerHeightPx by remember { mutableFloatStateOf(1f) }
                val totalItems = tracks.size

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(24.dp)
                        .padding(vertical = 8.dp, horizontal = 4.dp)
                        .onGloballyPositioned { coordinates ->
                            containerHeightPx = coordinates.size.height.toFloat().coerceAtLeast(1f)
                        }
                        .pointerInput(totalItems) {
                            var lastTargetIndex = -1
                            detectVerticalDragGestures { change, _ ->
                                change.consume()
                                val touchY = change.position.y.coerceIn(0f, containerHeightPx)
                                val fraction = touchY / containerHeightPx
                                val targetIndex = ((totalItems - 1) * fraction).toInt().coerceIn(0, (totalItems - 1).coerceAtLeast(0))
                                if (totalItems > 0 && targetIndex != lastTargetIndex) {
                                    lastTargetIndex = targetIndex
                                    coroutineScope.launch {
                                        listState.scrollToItem(targetIndex)
                                    }
                                }
                            }
                        }
                ) {
                    // Pista visual translúcida de la barra
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(12.dp)
                            )
                    )

                    // Tirador / Botón de arrastre (Thumb)
                    val firstVisibleIndex = remember { derivedStateOf { listState.firstVisibleItemIndex } }
                    val thumbFraction = if (totalItems > 0) (firstVisibleIndex.value.toFloat() / totalItems.toFloat()).coerceIn(0f, 0.9f) else 0f

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.15f)
                            .align(Alignment.TopCenter)
                            .offset(y = with(androidx.compose.ui.platform.LocalDensity.current) { (thumbFraction * containerHeightPx).toDp() })
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(12.dp)
                            )
                    )
                }
            }
        }
    }
}

