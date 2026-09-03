package com.example.musicplayerapp

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.example.musicplayerapp.core.utils.PermissionsManager
import com.example.musicplayerapp.ui.nav.MusicNavigationScreen
import com.example.musicplayerapp.viewmodel.FavoritesViewModel
import com.example.musicplayerapp.viewmodel.MusicListViewModel
import com.example.musicplayerapp.viewmodel.PlaylistViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val musicListViewModel: MusicListViewModel by viewModels()
    private val playlistViewModel: PlaylistViewModel by viewModels()
    private val favoritesViewModel: FavoritesViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            musicListViewModel.loadMusic()
        }else{
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissions()
        setContent {
            MusicNavigationScreen(
                playlistViewModel = playlistViewModel,
                musicListViewModel = musicListViewModel,
                favoritesViewModel = favoritesViewModel
            )
        }
    }

    private fun requestPermissions() {
        val perms = PermissionsManager.getAudioPermissions()
        if (!PermissionsManager.hasPermissions(this)) {
            requestPermissionLauncher.launch(perms)
        } else {
            musicListViewModel.loadMusic()
        }
    }
}