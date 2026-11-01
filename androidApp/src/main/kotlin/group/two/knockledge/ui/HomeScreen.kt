package group.two.knockledge.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FabPosition
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import group.two.knockledge.R
import group.two.knockledge.audio.AudioPlaybackState
import group.two.knockledge.data.model.AiEngineType
import group.two.knockledge.data.model.AudioMetadata
import group.two.knockledge.data.model.ChapterMarker
import group.two.knockledge.data.model.Note
import group.two.knockledge.data.model.SummaryMetadata
import group.two.knockledge.data.model.TriggerType
import group.two.knockledge.data.remote.GoogleIdTokenClient
import group.two.knockledge.di.KnockledgeComponent
import group.two.knockledge.ui.auth.AuthScreen
import group.two.knockledge.ui.auth.AuthViewModel
import group.two.knockledge.ui.playback.AudioPlaybackControls
import group.two.knockledge.ui.playback.NotePlaybackViewModel
import group.two.knockledge.ui.processing.NoteProcessingControls
import group.two.knockledge.ui.processing.NoteProcessingUiState
import group.two.knockledge.ui.processing.NoteProcessingViewModel
import group.two.knockledge.ui.settings.SettingsViewModel
import group.two.knockledge.ui.tabs.SettingsTab
import group.two.knockledge.ui.theme.AccentGold
import group.two.knockledge.ui.theme.AntiqueBrass
import group.two.knockledge.ui.theme.KnockledgeTheme
import group.two.knockledge.ui.theme.ObsidianMidnight
import group.two.knockledge.ui.theme.ParchmentCream
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Settings Gear Icon for the top left header.
 */
@Composable
fun SettingsGearIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val stroke = 2.dp.toPx()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val outerR = size.width * 0.36f
        val innerR = size.width * 0.18f

        drawCircle(
            color = tint,
            radius = outerR,
            center = Offset(cx, cy),
            style = Stroke(width = stroke),
        )
        drawCircle(
            color = tint,
            radius = innerR,
            center = Offset(cx, cy),
            style = Stroke(width = stroke),
        )
        for (i in 0 until 6) {
            val angle = Math.toRadians((i * 60).toDouble())
            val startX = (cx + (outerR - 1.dp.toPx()) * kotlin.math.cos(angle)).toFloat()
            val startY = (cy + (outerR - 1.dp.toPx()) * kotlin.math.sin(angle)).toFloat()
            val endX = (cx + (outerR + 3.5.dp.toPx()) * kotlin.math.cos(angle)).toFloat()
            val endY = (cy + (outerR + 3.5.dp.toPx()) * kotlin.math.sin(angle)).toFloat()
            drawLine(
                color = tint,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
fun SearchLensIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(20.dp)) {
        val stroke = 2.dp.toPx()
        val w = size.width
        val h = size.height
        val radius = w * 0.32f

        drawCircle(
            color = tint,
            radius = radius,
            center = Offset(w * 0.42f, h * 0.42f),
            style = Stroke(width = stroke),
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.65f, h * 0.65f),
            end = Offset(w * 0.88f, h * 0.88f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
fun ClearCrossIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(18.dp)) {
        val stroke = 2.dp.toPx()
        val w = size.width
        val h = size.height

        drawLine(
            color = tint,
            start = Offset(w * 0.25f, h * 0.25f),
            end = Offset(w * 0.75f, h * 0.75f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.75f, h * 0.25f),
            end = Offset(w * 0.25f, h * 0.75f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
fun PlayTriangleIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.3f, h * 0.2f)
            lineTo(w * 0.8f, h * 0.5f)
            lineTo(w * 0.3f, h * 0.8f)
            close()
        }
        drawPath(path, color = tint)
    }
}

@Composable
fun PauseBarsIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(20.dp)) {
        val stroke = 3.dp.toPx()
        val w = size.width
        val h = size.height

        drawLine(
            color = tint,
            start = Offset(w * 0.35f, h * 0.25f),
            end = Offset(w * 0.35f, h * 0.75f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.65f, h * 0.25f),
            end = Offset(w * 0.65f, h * 0.75f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
fun TrashBinIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(20.dp)) {
        val stroke = 1.75.dp.toPx()
        val w = size.width
        val h = size.height

        drawLine(
            color = tint,
            start = Offset(w * 0.2f, h * 0.28f),
            end = Offset(w * 0.8f, h * 0.28f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawArc(
            color = tint,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.38f, h * 0.16f),
            size = Size(w * 0.24f, h * 0.22f),
            style = Stroke(width = stroke),
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.26f, h * 0.32f),
            size = Size(w * 0.48f, h * 0.52f),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
            style = Stroke(width = stroke),
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.41f, h * 0.42f),
            end = Offset(w * 0.41f, h * 0.72f),
            strokeWidth = stroke,
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.59f, h * 0.42f),
            end = Offset(w * 0.59f, h * 0.72f),
            strokeWidth = stroke,
        )
    }
}

@Composable
fun BackArrowNavIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(20.dp)) {
        val strokeWidth = 2.dp.toPx()
        val w = size.width
        val h = size.height
        drawLine(
            color = tint,
            start = Offset(w * 0.85f, h * 0.5f),
            end = Offset(w * 0.15f, h * 0.5f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.15f, h * 0.5f),
            end = Offset(w * 0.45f, h * 0.2f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.15f, h * 0.5f),
            end = Offset(w * 0.45f, h * 0.8f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun rememberFormattedDateTitle(epochMs: Long): String {
    val formatter = remember(epochMs) {
        SimpleDateFormat("MMM d, h.mm a", Locale.US)
    }
    return formatter.format(Date(epochMs))
}

@Composable
private fun rememberFormattedSubtext(epochMs: Long, durationMs: Long): String {
    val dateFmt = remember(epochMs) {
        SimpleDateFormat("M/d/yy", Locale.US)
    }
    val durSec = durationMs / 1000L
    val durFmt = String.format(Locale.US, "%02d:%02d", durSec / 60, durSec % 60)
    return "$durFmt  ${dateFmt.format(Date(epochMs))}"
}

@Composable
fun NoteListItem(
    note: Note,
    isSelected: Boolean,
    isPlayingThisNote: Boolean,
    onSelectNote: (Note) -> Unit,
    onTogglePlayNote: (Note) -> Unit,
    onDeleteNote: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val formattedTitle = if (note.title.isNotBlank()) note.title else rememberFormattedDateTitle(note.createdAtEpochMs)
    val subtext = rememberFormattedSubtext(note.createdAtEpochMs, note.audio.durationMs)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelectNote(note) },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable { onTogglePlayNote(note) },
                contentAlignment = Alignment.Center,
            ) {
                if (isPlayingThisNote) {
                    PauseBarsIcon(tint = MaterialTheme.colorScheme.onSurface)
                } else {
                    PlayTriangleIcon(tint = MaterialTheme.colorScheme.onSurface)
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formattedTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtext,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            IconButton(
                onClick = { onDeleteNote(note.id) },
                modifier = Modifier.size(36.dp),
            ) {
                TrashBinIcon(tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
            }
        }
    }
}

/**
 * Custom Canvas composable that draws a pixelated retro ring contour matching Figma mockup styling.
 */
@Composable
fun PixelatedRing(
    radius: androidx.compose.ui.unit.Dp,
    pixelSize: androidx.compose.ui.unit.Dp,
    color: Color,
    modifier: Modifier = Modifier,
    isDotted: Boolean = false,
    dotCount: Int = 16,
    waveProgress: Float = 0f,
    isWaving: Boolean = false,
    pullProgress: Float = 0f,
) {
    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val effectiveRadiusDp = radius * (1f - 0.75f * pullProgress)
        val radiusPx = effectiveRadiusDp.toPx()
        val stepPx = pixelSize.toPx()
        val steps = if (isDotted) dotCount else (2 * Math.PI * radiusPx / stepPx).toInt().coerceAtLeast(32)
        val alphaMultiplier = 1f - 0.4f * pullProgress

        for (i in 0 until steps) {
            val angle = (2 * Math.PI * i / steps).toFloat()
            val rawX = radiusPx * kotlin.math.cos(angle)
            val rawY = radiusPx * kotlin.math.sin(angle)
            val snappedX = (rawX / stepPx).toInt() * stepPx
            val snappedY = (rawY / stepPx).toInt() * stepPx

            val dotScale = if (isWaving) {
                val baseRadiusPx = radius.toPx().coerceAtLeast(1f)
                val normalizedDiag = (snappedX + snappedY) / (1.414f * baseRadiusPx)
                val wavePhase = (2 * Math.PI * (waveProgress - normalizedDiag * 0.5f)).toFloat()
                (1f + 0.25f * kotlin.math.sin(wavePhase)) * (1f - 0.3f * pullProgress)
            } else {
                1f - 0.3f * pullProgress
            }

            val currentDotSize = stepPx * dotScale

            drawRect(
                color = color.copy(alpha = color.alpha * alphaMultiplier),
                topLeft = Offset(
                    centerX + snappedX - currentDotSize / 2f,
                    centerY + snappedY - currentDotSize / 2f,
                ),
                size = Size(currentDotSize, currentDotSize),
            )
        }
    }
}

/**
 * Floating Recording Button reproducing the RecordTab circle button verbatim with a diagonal wave dot animation
 * and a center pull animation on click before navigating to RecordScreen.
 */
@Composable
fun RecordFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val pullProgress = remember { Animatable(0f) }
    var isTriggered by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "diagonalWave")

    val waveProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "waveProgress",
    )

    Surface(
        onClick = {
            if (!isTriggered) {
                isTriggered = true
                scope.launch {
                    val pullJob = launch {
                        pullProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(
                                durationMillis = 220,
                                easing = FastOutLinearInEasing,
                            ),
                        )
                    }
                    kotlinx.coroutines.delay(140L)
                    onClick()
                    pullJob.join()
                }
            }
        },
        modifier = modifier.size(88.dp),
        shape = CircleShape,
        color = ObsidianMidnight,
        shadowElevation = 12.dp,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            PixelatedRing(
                radius = 36.dp,
                pixelSize = 3.dp,
                color = AccentGold.copy(alpha = 0.85f),
                modifier = Modifier.size(76.dp),
                isDotted = true,
                dotCount = 16,
                waveProgress = waveProgress,
                isWaving = true,
                pullProgress = pullProgress.value,
            )
            PixelatedRing(
                radius = 28.dp,
                pixelSize = 3.dp,
                color = AccentGold.copy(alpha = 0.55f),
                modifier = Modifier.size(60.dp),
                isDotted = true,
                dotCount = 12,
                waveProgress = waveProgress,
                isWaving = true,
                pullProgress = pullProgress.value,
            )
            Box(
                modifier = Modifier
                    .width(12.dp)
                    .height(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(ParchmentCream),
            )
        }
    }
}

/**
 * Main application Home Screen hosting the recordings list content.
 */
@Composable
fun HomeScreen(
    notes: List<Note>,
    selectedNote: Note?,
    playbackState: AudioPlaybackState,
    processingUiState: NoteProcessingUiState,
    onSelectNote: (Note) -> Unit,
    onDeleteNote: (String) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSelectChapter: (ChapterMarker) -> Unit,
    onProcessNote: (String) -> Unit,
    onManualSave: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            RecordFloatingActionButton(
                onClick = onManualSave,
            )
        },
    ) { innerPadding ->
        HomeRecordingsContent(
            notes = notes,
            selectedNote = selectedNote,
            playbackState = playbackState,
            processingUiState = processingUiState,
            onSelectNote = onSelectNote,
            onDeleteNote = onDeleteNote,
            onTogglePlayPause = onTogglePlayPause,
            onSeekTo = onSeekTo,
            onSelectChapter = onSelectChapter,
            onProcessNote = onProcessNote,
            modifier = Modifier.padding(innerPadding),
            onNavigateToSettings = onNavigateToSettings,
        )
    }
}

/**
 * Migrated Home Screen Recordings view content.
 */
@Composable
fun HomeRecordingsContent(
    notes: List<Note>,
    selectedNote: Note?,
    playbackState: AudioPlaybackState,
    processingUiState: NoteProcessingUiState,
    onSelectNote: (Note) -> Unit,
    onDeleteNote: (String) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSelectChapter: (ChapterMarker) -> Unit,
    onProcessNote: (String) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToSettings: () -> Unit = {},
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredNotes = remember(notes, searchQuery) {
        notes.filter { note ->
            searchQuery.isBlank() ||
                note.title.contains(searchQuery, ignoreCase = true) ||
                (note.transcript?.fullText?.contains(searchQuery, ignoreCase = true) == true)
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            // Top Header Row: Recordings title on left, Settings gear icon on top-right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Recordings",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier.size(44.dp),
                ) {
                    SettingsGearIcon(
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Search all recordings",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                },
                leadingIcon = {
                    SearchLensIcon(tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            ClearCrossIcon(tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
            )



            // Note List or Empty State
            if (filteredNotes.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.no_notes_found),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                ) {
                    items(
                        items = filteredNotes,
                        key = { it.id },
                    ) { note ->
                        val isSelected = selectedNote?.id == note.id
                        val isPlayingThisNote = isSelected && playbackState.isPlaying

                        NoteListItem(
                            note = note,
                            isSelected = isSelected,
                            isPlayingThisNote = isPlayingThisNote,
                            onSelectNote = { selected ->
                                onSelectNote(selected)
                            },
                            onTogglePlayNote = { selected ->
                                if (selectedNote?.id != selected.id) {
                                    onSelectNote(selected)
                                }
                                onTogglePlayPause()
                            },
                            onDeleteNote = onDeleteNote,
                        )
                    }

                    // Selected Note Detail Section
                    if (selectedNote != null) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Playback: ${selectedNote.title.ifBlank { rememberFormattedDateTitle(selectedNote.createdAtEpochMs) }}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            AudioPlaybackControls(
                                state = playbackState,
                                chapters = selectedNote.summary?.chapterMarkers ?: emptyList(),
                                transcript = selectedNote.transcript,
                                onTogglePlayPause = onTogglePlayPause,
                                onSeekTo = onSeekTo,
                                onSelectChapter = onSelectChapter,
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            NoteProcessingControls(
                                uiState = processingUiState,
                                onProcessNote = { onProcessNote(selectedNote.id) },
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                ),
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Text(
                                        text = stringResource(R.string.ai_intelligence_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                    )

                                    Text(
                                        text = stringResource(R.string.summary_label),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    val summary = selectedNote.summary
                                    if (summary != null) {
                                        Text(
                                            text = summary.executiveSummary,
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                        if (summary.keyPoints.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = stringResource(R.string.key_bullet_points),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                            )
                                            summary.keyPoints.forEach { point ->
                                                Text(
                                                    text = "• $point",
                                                    style = MaterialTheme.typography.bodySmall,
                                                )
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = stringResource(R.string.no_summary),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )

                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Full application shell host binding all subsystem ViewModels and passing state into [HomeScreen].
 */
@Composable
fun LiveTestAppShell(
    component: KnockledgeComponent,
    hasRecordAudioPermission: Boolean,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    val graph = remember(component) { component.graph() }
    val dataGraph = remember(component) { component.data() }
    val appSettings = remember(component) { component.appSettings() }
    val audioPlayer = remember(component) { component.audioPlayer() }
    val pipeline = remember(component) { component.pipeline() }
    val aiNotifier = remember(component) { component.aiNotificationNotifier() }

    val recordingViewModel = remember(component, graph, appSettings) {
        KnockRecordingViewModel(graph.controller, appSettings)
    }
    val playbackViewModel = remember(component, audioPlayer) {
        NotePlaybackViewModel(audioPlayer)
    }
    val processingViewModel = remember(component, dataGraph, pipeline, aiNotifier) {
        NoteProcessingViewModel(dataGraph.notes, dataGraph.auth, pipeline, aiNotifier)
    }
    val settingsViewModel = remember(component, appSettings) {
        SettingsViewModel(appSettings)
    }
    val context = LocalContext.current
    val authViewModel = remember(component, dataGraph, context) {
        AuthViewModel(dataGraph.auth, googleToken = {
            val appContext = context.applicationContext
            val webClientIdRes = appContext.resources.getIdentifier("default_web_client_id", "string", appContext.packageName)
            val webClientId = if (webClientIdRes != 0) appContext.getString(webClientIdRes) else ""
            if (webClientId.isBlank()) {
                throw IllegalStateException("Google Sign-In is not configured: default_web_client_id missing in google-services.json")
            }
            GoogleIdTokenClient(webClientId).fetch(context)
        })
    }

    val authState by authViewModel.state.collectAsState()
    val currentUser = authState.user

    if (currentUser == null) {
        Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
            AuthScreen(
                state = authState,
                onGoogle = authViewModel::signInWithGoogle,
                onSignIn = { email, pass -> authViewModel.signInWithEmail(email, pass) },
                onRegister = { name, email, pass -> authViewModel.register(name, email, pass) },
                modifier = Modifier.padding(innerPadding),
            )
        }
    } else {
        val activeUserId = currentUser.uid
        val notesFlow = remember(dataGraph, activeUserId) { dataGraph.notes.observeNotes(activeUserId) }
        val notes by notesFlow.collectAsState(initial = emptyList())

        var selectedNoteId by remember { mutableStateOf<String?>(null) }
        val selectedNote = notes.firstOrNull { it.id == selectedNoteId }
        val playbackState by playbackViewModel.playbackState.collectAsState()
        val processingUiState by processingViewModel.processingState.collectAsState()
        val settingsUiState by settingsViewModel.uiState.collectAsState()
        val recordingUiState by recordingViewModel.uiState.collectAsState()
        val autoAiEnabled by appSettings.autoAiProcessingEnabled.collectAsState()

        val navController = rememberNavController()

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(ObsidianMidnight),
        ) {
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.fillMaxSize(),
            ) {
                composable(
                    route = "home",
                    enterTransition = {
                        (scaleIn(
                            initialScale = 0.90f,
                            transformOrigin = TransformOrigin(0.5f, 0.92f),
                            animationSpec = tween(320, easing = FastOutSlowInEasing),
                        ) + fadeIn(animationSpec = tween(320, easing = FastOutSlowInEasing)))
                    },
                    exitTransition = {
                        (scaleOut(
                            targetScale = 0.90f,
                            transformOrigin = TransformOrigin(0.5f, 0.92f),
                            animationSpec = tween(280, easing = FastOutLinearInEasing),
                        ) + fadeOut(animationSpec = tween(280, easing = FastOutLinearInEasing)))
                    },
                ) {
                    HomeScreen(
                        notes = notes,
                        selectedNote = selectedNote,
                        playbackState = playbackState,
                        processingUiState = processingUiState,
                        onSelectNote = { note ->
                            selectedNoteId = note.id
                            playbackViewModel.loadNote(note)
                        },
                        onDeleteNote = { noteId ->
                            scope.launch {
                                dataGraph.notes.deleteNote(noteId, activeUserId)
                            }
                        },
                        onTogglePlayPause = playbackViewModel::togglePlayPause,
                        onSeekTo = playbackViewModel::seekTo,
                        onSelectChapter = playbackViewModel::selectChapter,
                        onProcessNote = processingViewModel::processNote,
                        onManualSave = {
                            navController.navigate("record")
                        },
                        onNavigateToSettings = {
                            navController.navigate("settings")
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                composable(
                    route = "record",
                    enterTransition = {
                        if (recordingUiState.isRecording) {
                            (scaleIn(
                                initialScale = 0.02f,
                                transformOrigin = TransformOrigin(0.5f, 0.92f),
                                animationSpec = tween(
                                    durationMillis = 420,
                                    easing = CubicBezierEasing(0.12f, 0.8f, 0.15f, 1.0f),
                                ),
                            ) + fadeIn(
                                animationSpec = tween(
                                    durationMillis = 250,
                                    easing = LinearEasing,
                                ),
                            ))
                        } else {
                            fadeIn(
                                animationSpec = tween(
                                    durationMillis = 200,
                                    easing = LinearEasing,
                                ),
                            )
                        }
                    },
                    exitTransition = {
                        if (recordingUiState.isRecording) {
                            (scaleOut(
                                targetScale = 0.02f,
                                transformOrigin = TransformOrigin(0.5f, 0.92f),
                                animationSpec = tween(
                                    durationMillis = 320,
                                    easing = FastOutLinearInEasing,
                                ),
                            ) + fadeOut(
                                animationSpec = tween(
                                    durationMillis = 320,
                                    easing = FastOutLinearInEasing,
                                ),
                            ))
                        } else {
                            fadeOut(
                                animationSpec = tween(
                                    durationMillis = 200,
                                    easing = FastOutLinearInEasing,
                                ),
                            )
                        }
                    },
                    popExitTransition = {
                        if (recordingUiState.isRecording) {
                            (scaleOut(
                                targetScale = 0.02f,
                                transformOrigin = TransformOrigin(0.5f, 0.92f),
                                animationSpec = tween(
                                    durationMillis = 320,
                                    easing = FastOutLinearInEasing,
                                ),
                            ) + fadeOut(
                                animationSpec = tween(
                                    durationMillis = 320,
                                    easing = FastOutLinearInEasing,
                                ),
                            ))
                        } else {
                            fadeOut(
                                animationSpec = tween(
                                    durationMillis = 200,
                                    easing = FastOutLinearInEasing,
                                ),
                            )
                        }
                    },
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        RecordScreen(
                            recordingViewModel = recordingViewModel,
                            hasRecordAudioPermission = hasRecordAudioPermission,
                            onRequestPermission = onRequestPermission,
                            onNavigateToHome = { navController.popBackStack() },
                            autoAiEnabled = autoAiEnabled,
                            onAutoAiChanged = appSettings::setAutoAiProcessingEnabled,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                composable("settings") {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        SettingsTab(
                            settingsUiState = settingsUiState,
                            authState = authState,
                            autoAiEnabled = autoAiEnabled,
                            onAutoAiChanged = appSettings::setAutoAiProcessingEnabled,
                            onBufferDurationChanged = settingsViewModel::setBufferDurationSeconds,
                            onKnockThresholdChanged = settingsViewModel::setKnockSensorThreshold,
                            onSignOut = authViewModel::signOut,
                            onNavigateToHome = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    KnockledgeTheme {
        val navController = rememberNavController()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianMidnight),
        ) {
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.fillMaxSize(),
            ) {
                composable(
                    route = "home",
                    enterTransition = {
                        (scaleIn(
                            initialScale = 0.90f,
                            transformOrigin = TransformOrigin(0.5f, 0.92f),
                            animationSpec = tween(320, easing = FastOutSlowInEasing),
                        ) + fadeIn(animationSpec = tween(320, easing = FastOutSlowInEasing)))
                    },
                    exitTransition = {
                        (scaleOut(
                            targetScale = 0.90f,
                            transformOrigin = TransformOrigin(0.5f, 0.92f),
                            animationSpec = tween(280, easing = FastOutLinearInEasing),
                        ) + fadeOut(animationSpec = tween(280, easing = FastOutLinearInEasing)))
                    },
                ) {
                    HomeScreen(
                        notes = emptyList(),
                        selectedNote = null,
                        playbackState = AudioPlaybackState(),
                        processingUiState = NoteProcessingUiState(),
                        onSelectNote = {},
                        onDeleteNote = {},
                        onTogglePlayPause = {},
                        onSeekTo = {},
                        onSelectChapter = {},
                        onProcessNote = {},
                        onManualSave = { navController.navigate("record") },
                        onNavigateToSettings = {},
                    )
                }

                composable(
                    route = "record",
                    enterTransition = {
                        fadeIn(
                            animationSpec = tween(
                                durationMillis = 200,
                                easing = LinearEasing,
                            ),
                        )
                    },
                    exitTransition = {
                        fadeOut(
                            animationSpec = tween(
                                durationMillis = 200,
                                easing = FastOutLinearInEasing,
                            ),
                        )
                    },
                    popExitTransition = {
                        fadeOut(
                            animationSpec = tween(
                                durationMillis = 200,
                                easing = FastOutLinearInEasing,
                            ),
                        )
                    },
                ) {
                    RecordScreen(
                        recordingUiState = KnockRecordingUiState(
                            isRecording = true,
                            bufferedSeconds = 60,
                            maxBufferSeconds = 300,
                            level = 0.5f,
                        ),
                        hasRecordAudioPermission = true,
                        onStartSession = {},
                        onStopSession = {},
                        onManualSave = {},
                        onRequestPermission = {},
                        onNavigateToHome = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun RecordFloatingActionButtonPreview() {
    var clickedCount by remember { mutableStateOf(0) }
    KnockledgeTheme {
        Column(
            modifier = Modifier
                .size(160.dp)
                .background(ParchmentCream),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = if (clickedCount > 0) "Triggered ($clickedCount)" else "Tap FAB",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            RecordFloatingActionButton(
                onClick = { clickedCount++ },
            )
        }
    }
}

