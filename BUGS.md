# Reporte de Bugs y Fallos - MusicPlayerApp

Este documento proporciona una auditoría detallada e inventario técnico de los errores, fallos de arquitectura, fugas de memoria y problemas de concurrencia/UI encontrados en la aplicación **MusicPlayerApp**.

---

## 🛑 1. Bugs Críticos & Fallos de Estabilidad (Crashes)

### 1.1 `NullPointerException` al alternar la reproducción en QueueScreen
* **Ubicación:** [QueueViewModel.kt](file:///c:/dev/proyectos/MusicPlayerApp/app/src/main/java/com/example/musicplayerapp/viewmodel/QueueViewModel.kt#L53)
* **Código:**
  ```kotlin
  fun togglePlayPause() {
      if(playerUseCase.isPlaying.value){
          playerUseCase.pause()
      }else{
          playerUseCase.play(_currentTrack.value!!) // 💥 Crash si _currentTrack es null
      }
      _isPlaying.update { !it }
  }
  ```
* **Descripción:** Si el usuario presiona el botón de play/pause en la sección de reproducción de la pantalla de cola cuando `_currentTrack.value` es `null` (lo cual es su valor inicial en `QueueViewModel`), la aplicación sufre un choque inmediato por desempaquetado nulo de Kotlin (`!!`).
* **Solución recomendada:** Realizar una verificación segura `val track = _currentTrack.value ?: return` antes de ejecutar la acción.

---

### 1.2 `NullPointerException` por des-referencia forzada de `pos!!` en Shuffle
* **Ubicación:** [MusicServiceConnection.kt](file:///c:/dev/proyectos/MusicPlayerApp/app/src/main/java/com/example/musicplayerapp/player/service/MusicServiceConnection.kt#L88)
* **Código:**
  ```kotlin
  val pos = controller?.currentPosition
  ...
  controller?.seekTo(newIndex, pos!!) // 💥 Crash si el controlador no devuelve posición o pos es null
  ```
* **Descripción:** Si por alguna razón la conexión con MediaController aún no ha recuperado la posición actual (`currentPosition` retorna null), el operador `pos!!` causará un `NullPointerException`.
* **Solución recomendada:** Usar un valor por defecto o fallback: `pos ?: 0L`.

---

### 1.3 `NullPointerException` por desempaquetado `selectedTrack!!` sin protección en PlaylistDetailScreen
* **Ubicación:** [PlaylistDetailScreen.kt](file:///c:/dev/proyectos/MusicPlayerApp/app/src/main/java/com/example/musicplayerapp/ui/screen/PlaylistDetailScreen.kt#L118-L137)
* **Código:**
  ```kotlin
  if (showMenuModal) {
      TrackOptionsModal(
          track = selectedTrack!!, // 💥 Crash si showMenuModal se vuelve verdadero con selectedTrack = null
          ...
      )
  }
  ```
* **Descripción:** `selectedTrack` puede volverse nulo si la lista o el estado cambian antes de descartar el modal, provocando un error fatal al forzar `selectedTrack!!`.
* **Solución recomendada:** Condicionar la renderización del modal a `if (showMenuModal && selectedTrack != null)` o pasar `selectedTrack` nullable y manejar la nulabilidad dentro de `TrackOptionsModal` de forma segura.

---

### 1.4 `NullPointerException` en `NowPlayingFooter` al intentar reanudar
* **Ubicación:** [MusicNavigationScreen.kt](file:///c:/dev/proyectos/MusicPlayerApp/app/src/main/java/com/example/musicplayerapp/ui/nav/MusicNavigationScreen.kt#L67)
* **Código:**
  ```kotlin
  onPlayPauseClick = {
      if (isPlaying) {
          musicListViewModel.pauseTrack()
      } else {
          musicListViewModel.playTrack(currentTrack!!) // 💥 Riesgo de crash
          musicListViewModel.seekTo(currentPosition)
      }
  }
  ```
* **Descripción:** Aunque `NowPlayingFooter` valida `if (currentTrack != null)`, la recomposición o carreras en corrutinas pueden causar que `currentTrack` cambie a `null` al hacer clic en el botón.
* **Solución recomendada:** Usar `currentTrack?.let { track -> ... }`.

---

## ⚡ 2. Bugs de Sincronización de Estado e Incoherencia de UI

### 2.1 Desincronización del estado de reproducción en `QueueViewModel`
* **Ubicación:** [QueueViewModel.kt](file:///c:/dev/proyectos/MusicPlayerApp/app/src/main/java/com/example/musicplayerapp/viewmodel/QueueViewModel.kt#L24-L33)
* **Descripción:** `QueueViewModel` mantiene sus propios `MutableStateFlow` (`_queue`, `_currentTrack`, `_isPlaying`) de manera desvinculada del servicio `PlayerUseCase`/`MusicServiceConnection`. Solo actualiza la cola una vez en `init` y cuando se mueve un elemento. No se suscribe a los cambios globales del reproductor (cuando se cambia de canción, o se pausa/reanudado desde otra pantalla), provocando que la pantalla `QueueScreen` muestre información obsoleta de la canción actual y del estado de reproducción.
* **Solución recomendada:** Conectar `QueueViewModel` directamente con `playerUseCase.currentTrack` e `isPlaying` o escuchar las actualizaciones mediante corrutinas/Flows.

---

### 2.2 Desincronización del estado de Shuffle y Lista Original
* **Ubicación:** [MusicServiceConnection.kt](file:///c:/dev/proyectos/MusicPlayerApp/app/src/main/java/com/example/musicplayerapp/player/service/MusicServiceConnection.kt#L131-L141)
* **Código:**
  ```kotlin
  fun setPlaylist(tracks: List<MusicTrack>, startIndex: Int, playlistId: Long) {
      if (originalList.isNotEmpty() && _isShuffleEnabled.value) return // ⚠️ Ignora nuevas listas si el modo aleatorio está activo
      ...
  }
  ```
* **Descripción:** Si el modo Shuffle está activo, llamar a `setPlaylist()` para cambiar a otra playlist (o reproducir una canción de otra pantalla) no tiene ningún efecto y falla silenciosamente sin actualizar la lista ni informar al usuario.
* **Solución recomendada:** Resetear o reorganizar la nueva lista manteniendo el modo aleatorio de forma coherente.

---

### 2.3 Visualización vacía o bloqueada en `PlaylistSelectionModal`
* **Ubicación:** [PlaylistSelecctionModal.kt](file:///c:/dev/proyectos/MusicPlayerApp/app/src/main/java/com/example/musicplayerapp/ui/components/PlaylistSelecctionModal.kt#L25)
* **Código:**
  ```kotlin
  if (playlists.isEmpty()) return
  ```
* **Descripción:** Si el usuario no tiene ninguna playlist creada e intenta agregar una canción a una playlist desde el menú de opciones, `PlaylistSelectionModal` ejecuta `return` inmediatamente sin mostrar un diálogo informativo o sin cerrar el sheet actual, dejando la UI congelada o sin retroalimentación visual al usuario.
* **Solución recomendada:** Mostrar un estado vacío dentro del modal con un mensaje de "No hay playlists disponibles" y un botón para crear una.

---

### 2.4 Bloqueo de UI por lectura síncrona/bloqueante de base de datos en `ScanMusicUseCase`
* **Ubicación:** [ScanMusicUseCase.kt](file:///c:/dev/proyectos/MusicPlayerApp/app/src/main/java/com/example/musicplayerapp/domain/usecase/ScanMusicUseCase.kt#L16-L17)
* **Código:**
  ```kotlin
  return repository.getAllTracksFlow()
      .first() // ⚠️ Bloquea hasta el primer valor del Flow de Room
  ```
* **Descripción:** Llamar a `.first()` sobre un Flow de Room sin especificar adecuadamente el Dispatcher IO o si se invoca desde el hilo principal puede bloquear la ejecución. Además en `MusicListViewModel.kt` se inicia la primera lista fija con `playlistId = -1` reemplazando cualquier cola existente.

---

### 2.5 `IllegalStateException` por llamada a `MediaController` fuera del Main Thread
* **Ubicación:** [MusicServiceConnection.kt](file:///c:/dev/proyectos/MusicPlayerApp/app/src/main/java/com/example/musicplayerapp/player/service/MusicServiceConnection.kt#L132) / [MusicListViewModel.kt](file:///c:/dev/proyectos/MusicPlayerApp/app/src/main/java/com/example/musicplayerapp/viewmodel/MusicListViewModel.kt#L60)
* **Código:**
  ```kotlin
  java.lang.IllegalStateException: MediaController method is called from a wrong thread. See javadoc of MediaController for details.
      at androidx.media3.session.MediaController.verifyApplicationThread(MediaController.java:2113)
      at androidx.media3.session.MediaController.setMediaItems(MediaController.java:1294)
      at com.example.musicplayerapp.player.service.MusicServiceConnection.setPlaylist(MusicServiceConnection.kt:148)
  ```
* **Descripción:** Al completar la lectura asíncrona de canciones dentro de `MusicListViewModel.loadMusic()` sobre `Dispatchers.IO`, se invocaba `playerUseCase.setPlaylist(...)` directamente desde ese hilo en segundo plano. Media3 exige que todas las interacciones con `MediaController` se ejecuten exclusivamente en el hilo principal (`MainThread`), por lo cual la llamada causaba un crash fatal con `IllegalStateException`.
* **Solución recomendada:** Garantizar que la invocación a `playerUseCase.setPlaylist(...)` se ejecute sobre `withContext(Dispatchers.Main)`.

---

## 🧹 3. Código Comentado, Funcionalidades Incompletas y Fugas de Memoria

### 3.1 Funciones de borrado inactivas / código comentado
* **Ubicaciones:**
  * [PlaylistViewModel.kt](file:///c:/dev/proyectos/MusicPlayerApp/app/src/main/java/com/example/musicplayerapp/viewmodel/PlaylistViewModel.kt#L78-L83)
  * [PlaybackService.kt](file:///c:/dev/proyectos/MusicPlayerApp/app/src/main/java/com/example/musicplayerapp/player/service/PlaybackService.kt#L26)
* **Descripción:**
  * En `PlaylistViewModel.kt`, el bloque que ejecuta el borrado de la playlist y el manejo del resultado `Result` fue comentado (`// if (result.isFailure)`), haciendo que las llamadas a `deletePlaylist` no manejen errores.
  * En `PlaybackService.kt`, la variable `allTracks = mutableListOf<MusicTrack>()` nunca se llena ni actualiza, provocando que los métodos de MediaLibrary (como `onGetChildren`) retornen listas vacías a clientes MediaSession externos (e.g. Android Auto, Wear OS o Notificaciones de Media3).
  * Los botones de Configuración (Settings) y Compartir (Share) en [SongInfoScreen.kt](file:///c:/dev/proyectos/MusicPlayerApp/app/src/main/java/com/example/musicplayerapp/ui/screen/SongInfoScreen.kt#L110-L114) tienen Handlers vacíos (`/* TODO: Settings */`, `/* TODO: Share */`).

---

### 3.2 Fuga potencial de Corrutinas en `MusicServiceConnection`
* **Ubicación:** [MusicServiceConnection.kt](file:///c:/dev/proyectos/MusicPlayerApp/app/src/main/java/com/example/musicplayerapp/player/service/MusicServiceConnection.kt#L180)
* **Código:**
  ```kotlin
  progressJob = CoroutineScope(Dispatchers.Main).launch { ... }
  ```
* **Descripción:** Se crea un `CoroutineScope` desacoplado y sin Job superior ni `SupervisorJob` dentro del método `startProgressUpdates()`. Si el servicio se desconecta y se reconecta múltiples veces, se pueden acumular tareas en segundo plano consumiendo batería y recursos.
* **Solución recomendada:** Usar un `CoroutineScope` con ciclo de vida definido o usar `Job()` adecuadamente cancelado.

---

## 📱 4. Errores de Permisos y Compatibilidad Android

### 4.1 Permisos de almacenamiento en Android 13+ (Tiramisu / API 33) y Android 14+ (UPSIDE_DOWN_CAKE)
* **Ubicación:** [PermissionsManager.kt](file:///c:/dev/proyectos/MusicPlayerApp/app/src/main/java/com/example/musicplayerapp/core/utils/PermissionsManager.kt#L15-L23)
* **Descripción:** En Android 13+ (API 33) y Android 14+ (API 34), si la aplicación requiere notificaciones de reproducción para servicios en primer plano (Foreground Service), falta solicitar el permiso `Manifest.permission.POST_NOTIFICATIONS`. Además, si la app se ejecuta en Android 14, se requieren permisos de tipo `FOREGROUND_SERVICE_MEDIA_PLAYBACK` declarados en el `AndroidManifest.xml`.
* **Uso de `@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)`:**
  En varias pantallas (`MainActivity.kt`, `MusicNavigationScreen.kt`, `SongInfoScreen.kt`) se colocó la anotación `@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)` (Android 15 / API 35), restringiendo indebidamente la ejecución de funciones Composable a dispositivos de última versión o provocando fallos de compilación/ejecución en versiones anteriores.

---

## 📋 Resumen de Acciones Recomendadas

| ID | Componente | Severidad | Acción Correctora |
|---|---|---|---|
| 1.1 | `QueueViewModel` | 🔴 Alta | Agregar comprobación de nulabilidad en `togglePlayPause()`. |
| 1.2 | `MusicServiceConnection` | 🔴 Alta | Sustituir `pos!!` por `pos ?: 0L`. |
| 1.3 | `PlaylistDetailScreen` | 🔴 Alta | Proteger des-referencia de `selectedTrack!!`. |
| 2.1 | `QueueViewModel` | 🟡 Media | Observar los `StateFlow` globales de `PlayerUseCase`. |
| 2.2 | `MusicServiceConnection` | 🟡 Media | Permitir cambiar de lista/track aun cuando `isShuffleEnabled` esté activo. |
| 2.3 | `PlaylistSelectionModal` | 🟡 Media | Mostrar pantalla vacía cuando no existan playlists. |
| 3.1 | `PlaybackService` | 🟢 Baja | Implementar la fuente de datos real para `allTracks` y sincronizar con MediaLibrary. |
| 4.1 | `PermissionsManager` & Manifest | 🟡 Media | Incluir `POST_NOTIFICATIONS` y remover anotaciones innecesarias de `VANILLA_ICE_CREAM`. |
