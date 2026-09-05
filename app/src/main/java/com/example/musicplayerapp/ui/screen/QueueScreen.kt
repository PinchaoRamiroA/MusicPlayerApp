@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.example.musicplayerapp.ui.screen

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.example.musicplayerapp.data.model.MusicTrack
import com.example.musicplayerapp.ui.theme.DarkColorScheme
import com.example.musicplayerapp.utils.extractAlbumArt
import com.example.musicplayerapp.viewmodel.QueueViewModel
import kotlinx.coroutines.delay

@Composable
fun QueueScreen(
    viewModel: QueueViewModel = hiltViewModel()
) {
    val queue by viewModel.queue.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    MaterialTheme( colorScheme = DarkColorScheme) {
        Scaffold(
            topBar = {
                QueueTopBar(
                    queueSize = queue.size,
                    currentTrack = currentTrack
                )
            },
            containerColor = Color(0xFF0A0E27)
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Current Playing Section (if there's a current track)
                currentTrack?.let { track ->
                    CurrentPlayingSection(
                        track = track,
                        isPlaying = isPlaying,
                        onPlayPause = { viewModel.togglePlayPause() }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = Color.White.copy(alpha = 0.1f)
                    )
                }

                // Queue Header
                QueueHeader(queueSize = queue.size)

                // Reorderable Queue List
                ReorderableList(
                    items = queue,
                    currentTrackId = currentTrack?.id,
                    onMove = { from, to ->
                        viewModel.moveTrack(from, to)
                    },
                    onTrackSelect = { track ->
                        viewModel.playTrack(track)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                )
            }
        }
    }
}

@Composable
fun QueueTopBar(
    queueSize: Int,
    currentTrack: MusicTrack?
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    "Cola de reproducción",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp
                )
                if (currentTrack != null) {
                    Text(
                        "Reproduciendo: ${currentTrack.title}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        actions = {
            Badge(
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 16.dp)
            ) {
                Text(
                    text = queueSize.toString(),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
        )
    )
}

@Composable
fun CurrentPlayingSection(
    track: MusicTrack,
    isPlaying: Boolean,
    onPlayPause: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.background
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Art with Play/Pause overlay
            Box {
                Card(
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(extractAlbumArt(track.data)),
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Play/Pause Button Overlay
                FloatingActionButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = 4.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Add else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Track Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Reproduciendo ahora",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = track.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    color = Color.Gray,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Animated Playing Indicator
            if (isPlaying) {
                PlayingAnimationIndicator()
            }
        }
    }
}

@Composable
fun PlayingAnimationIndicator() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(3) { index ->
            val animationDelay = index * 100
            var isAnimating by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                delay(animationDelay.toLong())
                isAnimating = true
            }

            val height by animateFloatAsState(
                targetValue = if (isAnimating) 1f else 0.3f,
                animationSpec = spring(
                    dampingRatio = 0.7f,
                    stiffness = 300f
                ),
                label = "bar_height"
            )

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(16.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(1.5.dp)
                    )
                    .graphicsLayer {
                        scaleY = height
                    }
            )
        }
    }
}

@Composable
fun QueueHeader(queueSize: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Outlined.DateRange,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "Siguiente en la cola",
            color = Color.Gray,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "$queueSize canciones",
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}

@Composable
fun ReorderableList(
    items: List<MusicTrack>,
    currentTrackId: String?,
    onMove: (Int, Int) -> Unit,
    onTrackSelect: (MusicTrack) -> Unit,
    modifier: Modifier = Modifier
) {
    var draggedItem by remember { mutableStateOf<DraggedItem?>(null) }
    var targetIndex by remember { mutableIntStateOf(-1) }
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    var draggedTrackId by remember { mutableStateOf<String?>(null) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }

    // Lista mutable interna para respuesta visual instantánea durante el arrastre
    var listItems by remember(items.size) { mutableStateOf(items) }

    // 🎯 Auto-scroll a la canción que está sonando actualmente (solo una vez al cargar la lista)
    var hasAutoScrolled by remember { mutableStateOf(false) }
    LaunchedEffect(currentTrackId, items) {
        if (!hasAutoScrolled && currentTrackId != null && items.isNotEmpty()) {
            val targetIdx = items.indexOfFirst { it.id == currentTrackId }
            if (targetIdx >= 0) {
                listState.scrollToItem(targetIdx)
                hasAutoScrolled = true
            }
        }
    }

    MaterialTheme(colorScheme = DarkColorScheme) {
        LazyColumn(
            state = listState,
            modifier = modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(
                items = listItems,
                key = { _, track -> track.id },
                contentType = { _, _ -> "queue_track_item" }
            ) { index, track ->
                val isDragging = draggedTrackId == track.id
                val isCurrentTrack = track.id == currentTrackId

                DraggableTrackItem(
                    track = track,
                    index = index,
                    isDragging = isDragging,
                    isCurrentTrack = isCurrentTrack,
                    onDragStart = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        draggedTrackId = track.id
                        dragOffsetPx = 0f
                    },
                    onDragEnd = {
                        draggedTrackId = null
                        dragOffsetPx = 0f
                    },
                    onDragDelta = { deltaY ->
                        if (draggedTrackId == track.id) {
                            dragOffsetPx += deltaY
                            val moveThreshold = 65f
                            val currentIndex = listItems.indexOfFirst { it.id == track.id }

                            if (currentIndex != -1) {
                                if (dragOffsetPx > moveThreshold && currentIndex < listItems.lastIndex) {
                                    val nextIndex = currentIndex + 1
                                    onMove(currentIndex, nextIndex)
                                    listItems = listItems.toMutableList().apply {
                                        add(nextIndex, removeAt(currentIndex))
                                    }
                                    dragOffsetPx = 0f
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                } else if (dragOffsetPx < -moveThreshold && currentIndex > 0) {
                                    val prevIndex = currentIndex - 1
                                    onMove(currentIndex, prevIndex)
                                    listItems = listItems.toMutableList().apply {
                                        add(prevIndex, removeAt(currentIndex))
                                    }
                                    dragOffsetPx = 0f
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        }
                    },
                    onTrackSelect = onTrackSelect,
                    dragOffset = if (isDragging) Offset(0f, dragOffsetPx) else Offset.Zero,
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}

@Composable
fun DraggableTrackItem(
    track: MusicTrack,
    index: Int,
    isDragging: Boolean,
    isCurrentTrack: Boolean,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onDragDelta: (Float) -> Unit,
    onTrackSelect: (MusicTrack) -> Unit,
    dragOffset: Offset,
    modifier: Modifier = Modifier
) {
    val elevation by animateDpAsState(
        targetValue = if (isDragging) 12.dp else if (isCurrentTrack) 2.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "elevation"
    )

    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.03f else 1f,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isDragging) 0.95f else 1f,
        label = "alpha"
    )

    val backgroundColor = when {
        isDragging -> Color(0xFF2A2F5A)
        isCurrentTrack -> Color(0xFF1A1F3A).copy(alpha = 0.6f)
        else -> Color(0xFF16213E).copy(alpha = 0.2f)
    }

    val borderColor = when {
        isCurrentTrack -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        isDragging -> Color.White.copy(alpha = 0.2f)
        else -> Color.Transparent
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation)
            .scale(scale)
            .alpha(alpha)
            .zIndex(if (isDragging) 1f else 0f)
            .graphicsLayer {
                if (isDragging) {
                    translationX = dragOffset.x
                    translationY = dragOffset.y
                }
            },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isCurrentTrack) 1.dp else 0.dp,
            color = borderColor
        ),
        onClick = { onTrackSelect(track) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag Handle + Number
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .pointerInput(track.id) {
                        detectDragGestures(
                            onDragStart = { onDragStart() },
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragEnd() },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDragDelta(dragAmount.y)
                            }
                        )
                    }
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Reordenar",
                    tint = when {
                        isDragging -> Color.White
                        isCurrentTrack -> MaterialTheme.colorScheme.primary
                        else -> Color.Gray
                    },
                    modifier = Modifier.size(22.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "#${index + 1}",
                    color = when {
                        isDragging -> Color.White
                        isCurrentTrack -> MaterialTheme.colorScheme.primary
                        else -> Color.Gray
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(45.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Album Art
            Card(
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(extractAlbumArt(track.data)),
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.DarkGray)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Track Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = when {
                        isDragging -> Color.White
                        isCurrentTrack -> Color.White
                        else -> Color.White.copy(alpha = 0.9f)
                    }
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatDuration(track.duration),
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            isDragging -> Color.LightGray
                            isCurrentTrack -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            else -> Color.Gray
                        },
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = " • ",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )

                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = when {
                            isDragging -> Color.LightGray
                            isCurrentTrack -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            else -> Color.Gray
                        }
                    )
                }
            }

            // More Options
            IconButton(
                onClick = { /* Show options menu */ },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Más opciones",
                    tint = when {
                        isDragging -> Color.White
                        isCurrentTrack -> MaterialTheme.colorScheme.primary
                        else -> Color.Gray
                    },
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

data class DraggedItem(
    val index: Int,
    val track: MusicTrack,
    val offset: Offset = Offset.Zero
)

fun formatDuration(duration: Long): String {
    val minutes = duration / 60000
    val seconds = (duration % 60000) / 1000
    return String.format("%d:%02d", minutes, seconds)
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0E27)
@Composable
fun QueueScreenPreview() {
    val sampleTracks = listOf(
        MusicTrack(id = "1", title = "505", artist = "Arctic Monkeys", duration = 253000, data = "", album = ""),
        MusicTrack(id = "2", title = "Le parole lontane", artist = "Måneskin", duration = 233000, data = "", album = ""),
        MusicTrack(id = "3", title = "Planet Caravan (2009 Remaster)", artist = "Black Sabbath", duration = 269000, data = "", album = ""),
        MusicTrack(id = "4", title = "You", artist = "Radiohead", duration = 208000, data = "", album = ""),
        MusicTrack(id = "5", title = "Follow Me Around", artist = "Radiohead", duration = 326000, data = "", album = ""),
        MusicTrack(id = "6", title = "Symphony Of Destruction", artist = "Megadeth", duration = 242000, data = "", album = ""),
        MusicTrack(id = "7", title = "Radioactive", artist = "Imagine Dragons", duration = 261000, data = "", album = ""),
    )

    MaterialTheme( colorScheme = DarkColorScheme)
    {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            ReorderableList(
                items = sampleTracks,
                currentTrackId = "2", // Simulate current playing track
                onMove = { from, to ->
                    println("Moving item from $from to $to")
                },
                onTrackSelect = { track ->
                    println("Selected track: ${track.title}")
                }
            )
        }
    }
}