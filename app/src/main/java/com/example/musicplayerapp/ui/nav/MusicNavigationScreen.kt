package com.example.musicplayerapp.ui.nav

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.musicplayerapp.ui.components.NowPlayingFooter
import com.example.musicplayerapp.ui.screen.*
import com.example.musicplayerapp.ui.theme.DarkColorScheme
import com.example.musicplayerapp.viewmodel.*

object MusicNavDestinations {
    const val MUSIC_LIST_ROUTE = "music_list"
    const val PLAYLISTS_ROUTE = "playlists"
    const val FAVORITES_ROUTE = "favorites"
    const val PLAYLIST_DETAIL_ROUTE = "playlist_detail"
    const val ADD_TO_PLAYLIST_ROUTE = "add_to_playlist"
    const val NOW_PLAYING_ROUTE = "now_playing"
    const val QUEUE_ROUTE = "queue"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicNavigationScreen(
    musicListViewModel: MusicListViewModel,
    playlistViewModel: PlaylistViewModel,
    favoritesViewModel: FavoritesViewModel
) {
    val navController = rememberNavController()

    val tabs = listOf(
        "Canciones" to MusicNavDestinations.MUSIC_LIST_ROUTE,
        "Listas" to MusicNavDestinations.PLAYLISTS_ROUTE,
        "Favoritos" to MusicNavDestinations.FAVORITES_ROUTE
    )

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val currentTrack by musicListViewModel.currentTrack.collectAsState()
    val isPlaying by musicListViewModel.isPlaying.collectAsState()
    val isShuffleEnabled by musicListViewModel.isShuffleModeEnabled.collectAsState()
    val currentPosition by musicListViewModel.currentPosition.collectAsState()

    MaterialTheme(colorScheme = DarkColorScheme) {
        Scaffold(
            bottomBar = {
                currentTrack?.let { track ->
                    NowPlayingFooter(
                        currentTrack = track,
                        isPlaying = isPlaying,
                        isShuffleEnabled = isShuffleEnabled,
                        onPlayPauseClick = {
                            if (isPlaying) {
                                musicListViewModel.pauseTrack()
                            } else {
                                musicListViewModel.playTrack(track)
                                musicListViewModel.seekTo(currentPosition)
                            }
                        },
                        onNextClick = { musicListViewModel.nextTrack() },
                        onPreviousClick = { musicListViewModel.previousTrack() },
                        onToggleShuffleClick = { musicListViewModel.toggleShuffle() },
                        navController = navController
                    )
                }
            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                ScrollableTabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, (title, route) ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = {
                                selectedTabIndex = index
                                navController.navigate(route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            text = {
                                Text(
                                    text = title,
                                    color = if (selectedTabIndex == index)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                            }
                        )
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = MusicNavDestinations.MUSIC_LIST_ROUTE,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(MusicNavDestinations.MUSIC_LIST_ROUTE) {
                        MusicListScreen(
                            viewModel = musicListViewModel,
                            playlistViewModel = playlistViewModel,
                            favoritesViewModel = favoritesViewModel
                        )
                    }
                    composable(MusicNavDestinations.PLAYLISTS_ROUTE) {
                        PlaylistsScreen(
                            viewModel = playlistViewModel,
                            onPlaylistClick = { playlist ->
                                navController.navigate("${MusicNavDestinations.PLAYLIST_DETAIL_ROUTE}/${playlist.playlistId}")
                            }
                        )
                    }
                    composable(MusicNavDestinations.FAVORITES_ROUTE) {
                        FavoritesScreen(favoritesViewModel = favoritesViewModel)
                    }
                    composable(MusicNavDestinations.NOW_PLAYING_ROUTE) {
                        SongInfoScreen(
                            track = currentTrack,
                            musicListViewModel = musicListViewModel,
                            playlistViewModel = playlistViewModel,
                            favoritesViewModel = favoritesViewModel,
                            navController = navController
                        )
                    }
                    composable(
                        route = "${MusicNavDestinations.PLAYLIST_DETAIL_ROUTE}/{playlistId}",
                        arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        backStackEntry.arguments?.getString("playlistId")
                            ?.toLongOrNull()
                            ?.let { playlistId ->
                                PlaylistDetailScreen(
                                    playlistId = playlistId,
                                    musicListViewModel = musicListViewModel,
                                    playlistViewModel = playlistViewModel,
                                    navController = navController,
                                    favoritesViewModel = favoritesViewModel
                                )
                            }
                    }
                    composable(
                        route = "${MusicNavDestinations.ADD_TO_PLAYLIST_ROUTE}/{playlistId}",
                        arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: return@composable
                        AddToPlaylistScreen(
                            playlistId = playlistId,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(MusicNavDestinations.QUEUE_ROUTE) {
                        val queueViewModel: QueueViewModel = hiltViewModel()
                        QueueScreen(queueViewModel)
                    }
                }
            }
        }
    }
}
