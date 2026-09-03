package com.example.musicplayerapp.core.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object PermissionsManager {

    /**
     * Obtiene la lista de permisos que deben solicitarse dinámicamente
     * dependiendo de la versión de Android.
     */
    fun getAudioPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ usa permisos específicos para audio y notificaciones
            arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            // Android 12 e inferiores usan READ_EXTERNAL_STORAGE
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    /**
     * Verifica si todos los permisos necesarios ya fueron concedidos.
     */
    fun hasPermissions(context: Context): Boolean {
        return getAudioPermissions().all { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
    }
}
