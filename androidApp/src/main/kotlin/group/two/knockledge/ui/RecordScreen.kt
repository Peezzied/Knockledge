package group.two.knockledge.ui

import androidx.compose.animation.VectorConverter
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import group.two.knockledge.R
import group.two.knockledge.data.model.Note
import group.two.knockledge.pipeline.NoteProcessingUiState
import group.two.knockledge.ui.theme.AccentGold
import group.two.knockledge.ui.theme.AntiqueBrass
import group.two.knockledge.ui.theme.KnockledgeTheme
import group.two.knockledge.ui.theme.ObsidianMidnight
import group.two.knockledge.ui.theme.ParchmentCream
import group.two.knockledge.ui.theme.SurfaceContainerHigh
import group.two.knockledge.ui.theme.TextInk
import group.two.knockledge.ui.theme.WaveformTeal
import group.two.knockledge.ui.theme.speakerClothTexture
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.Random
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

private const val EVICTION_BLINK_PULSE_COUNT = 2
private const val EVICTION_BLINK_DURATION_MILLIS = 150
private const val PAUSED_BLINK_DURATION_MILLIS = 500
private const val PAUSED_BLINK_MIN_ALPHA = 0.2f
private const val CAPTURE_CARD_ARRIVAL_OFFSET_DP = 56
private const val CAPTURE_CARD_ARRIVAL_SCALE = 0.92f
private const val CAPTURE_CARD_BEHIND_OFFSET_DP = 34
private const val CAPTURE_CARD_BEHIND_SCALE = 0.94f
private const val CAPTURE_CARD_BEHIND_ALPHA = 0.60f
private const val CAPTURE_CARD_RECEDE_DURATION_MILLIS = 300

/**
 * Transparent top bar for [RecordScreen] displaying the session timer and compact Auto AI toggle.
 * The record dot and duration text blink while [isPaused] is true.
 */
@Composable
fun RecordTopBar(
    sessionSeconds: Int,
    autoAiEnabled: Boolean,
    onAutoAiChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isPaused: Boolean = false,
) {
    val durationAlpha = if (isPaused) {
        rememberInfiniteTransition(label = "dur_paused_blink").animateFloat(
            initialValue = 1f,
            targetValue = PAUSED_BLINK_MIN_ALPHA,
            animationSpec = infiniteRepeatable(
                animation = tween(PAUSED_BLINK_DURATION_MILLIS, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "dur_paused_blink_alpha",
        ).value
    } else {
        1f
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color.Black,
            border = BorderStroke(1.dp, Color(0xFF33312B)),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error)
                        .alpha(durationAlpha),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "DUR ",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF8A8275),
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                )
                Text(
                    text = formatTimerSeconds(sessionSeconds),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.alpha(durationAlpha),
                )
            }
        }

        FilterChip(
            selected = autoAiEnabled,
            onClick = { onAutoAiChanged(!autoAiEnabled) },
            label = {
                Text(
                    text = if (autoAiEnabled) "AI: ON" else "AI: OFF",
                    style = MaterialTheme.typography.labelSmall,
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                containerColor = ObsidianMidnight,
                labelColor = ParchmentCream,
                selectedContainerColor = AccentGold,
                selectedLabelColor = TextInk,
            ),
            border = FilterChipDefaults.filterChipBorder(
                borderColor = AccentGold.copy(alpha = 0.5f),
                selectedBorderColor = AntiqueBrass,
                enabled = true,
                selected = autoAiEnabled,
            ),
        )
    }
}

/**
 * Recording screen following Figma design
 * with pixelated retro ring contour, central mic ring acting as the Capture trigger,
 * glowing ambient halo, black hole particle vortex suction animation, compact Auto AI toggle,
 * functional live sound wave, and stacked capture cards.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun RecordScreen(
    recordingUiState: KnockRecordingUiState,
    hasRecordAudioPermission: Boolean,
    onStartSession: () -> Unit,
    onPauseSession: () -> Unit = {},
    onStopSession: () -> Unit,
    onManualSave: () -> Unit,
    onRequestPermission: () -> Unit,
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier,
    autoAiEnabled: Boolean = true,
    onAutoAiChanged: (Boolean) -> Unit = {},
    onDurationChange: (Int) -> Unit = {},
    onSimulateKnock: () -> Unit = {},
    notes: List<Note> = emptyList(),
    onSelectNote: (Note) -> Unit = {},
    captureEvents: Flow<Unit>? = null,
    processingUiState: NoteProcessingUiState? = null,
) {
    val haptic = LocalHapticFeedback.current
    var isButtonInPosition by remember { mutableStateOf(recordingUiState.isRecording) }
    var isContentVisible by remember { mutableStateOf(recordingUiState.isRecording) }

    val buttonScale by animateFloatAsState(
        targetValue = if (isButtonInPosition) 1f else 0.38f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "button_entrance_scale",
    )

    val buttonOffsetY by animateFloatAsState(
        targetValue = if (isButtonInPosition) 0f else 320f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "button_entrance_offset_y",
    )

    val contentAlpha by animateFloatAsState(
        targetValue = if (isContentVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 350,
            easing = FastOutSlowInEasing,
        ),
        label = "content_alpha",
    )

    val contentOffsetY by animateFloatAsState(
        targetValue = if (isContentVisible) 0f else 16f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "content_offset_y",
    )

    LaunchedEffect(recordingUiState.isRecording) {
        if (recordingUiState.isRecording) {
            isButtonInPosition = true
            isContentVisible = true
        } else {
            isButtonInPosition = true
            delay(320L)
            isContentVisible = true
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianMidnight)
            .speakerClothTexture(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            AccentGold.copy(alpha = 0.08f),
                            Color.Transparent,
                            AccentGold.copy(alpha = 0.06f),
                        ),
                    ),
                ),
        )

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                RecordTopBar(
                    sessionSeconds = recordingUiState.sessionSeconds,
                    autoAiEnabled = autoAiEnabled,
                    onAutoAiChanged = onAutoAiChanged,
                    isPaused = recordingUiState.isPaused,
                    modifier = Modifier.graphicsLayer {
                        alpha = contentAlpha
                        translationY = -contentOffsetY.dp.toPx()
                    },
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {

                // Permission Banner
                if (!hasRecordAudioPermission) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                alpha = contentAlpha
                                translationY = contentOffsetY.dp.toPx()
                            },
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.permission_required),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Button(
                                onClick = onRequestPermission,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                ),
                            ) {
                                Text(text = stringResource(R.string.grant_permission))
                            }
                        }
                    }
                }

                // Central Capture Circle Button
                CaptureCircleButton(
                    isRecording = recordingUiState.isRecording,
                    isSavingSnapshot = recordingUiState.isSavingSnapshot,
                    canCapture = recordingUiState.canCapture,
                    captureEvents = captureEvents,
                    onClick = {
                        if (recordingUiState.isRecording) {
                            onManualSave()
                            if (recordingUiState.canCapture) {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            }
                        } else {
                            onStartSession()
                        }
                    },
                    modifier = Modifier.graphicsLayer {
                        translationY = buttonOffsetY.dp.toPx()
                        scaleX = buttonScale
                        scaleY = buttonScale
                    },
                )

                // Remaining content following the central mic ring button
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = contentAlpha
                            translationY = contentOffsetY.dp.toPx()
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = when {
                                recordingUiState.isRecording -> stringResource(R.string.recording_status_active)
                                recordingUiState.isPaused -> stringResource(R.string.recording_status_paused)
                                else -> stringResource(R.string.recording_status_inactive)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = ParchmentCream,
                            textAlign = TextAlign.Center,
                        )

                        BufferStatusText(
                            bufferedSeconds = recordingUiState.bufferedSeconds,
                            maxBufferSeconds = recordingUiState.maxBufferSeconds,
                        )

                        Text(
                            text = if (recordingUiState.isGestureActive) {
                                stringResource(R.string.recording_gesture_status_active)
                            } else {
                                stringResource(R.string.recording_gesture_status_inactive)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = AccentGold,
                            textAlign = TextAlign.Center,
                        )
                    }

                    FunctionalWaveform(
                        isRecording = recordingUiState.isRecording,
                        level = recordingUiState.level,
                        bands = recordingUiState.bands,
                    )

                    Text(
                        text = stringResource(R.string.recording_hint_text),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AccentGold,
                        textAlign = TextAlign.Center,
                    )

                    StackedCaptureDeck(
                        notes = notes,
                        onSelectNote = onSelectNote,
                        onNavigateToHome = onNavigateToHome,
                        processingUiState = processingUiState,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action buttons: Pause when recording; Resume + Stop Icon when paused
                    when {
                        recordingUiState.isRecording -> {
                            Button(
                                onClick = onPauseSession,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(4.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ObsidianMidnight,
                                    contentColor = ParchmentCream,
                                ),
                                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                    brush = Brush.horizontalGradient(listOf(AccentGold, AntiqueBrass)),
                                ),
                            ) {
                                Text(text = stringResource(R.string.pause_session))
                            }
                        }
                        recordingUiState.isPaused -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Button(
                                    onClick = onStartSession,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(4.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AntiqueBrass,
                                        contentColor = TextInk,
                                    ),
                                ) {
                                    Text(text = stringResource(R.string.resume_session))
                                }

                                IconButton(
                                    onClick = onStopSession,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(ObsidianMidnight, RoundedCornerShape(4.dp))
                                        .border(
                                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                                brush = Brush.horizontalGradient(listOf(AccentGold, AntiqueBrass)),
                                            ),
                                            shape = RoundedCornerShape(4.dp),
                                        ),
                                ) {
                                    CanvasStopSquareIcon(tint = ParchmentCream)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

/**
 * Buffer status label that pulses [MaterialTheme.colorScheme.error] when the ring buffer
 * evicts the oldest audio (buffered seconds drop while staying above zero). Drops to zero
 * mark a snapshot save and do not blink.
 */
@Composable
private fun BufferStatusText(
    bufferedSeconds: Int,
    maxBufferSeconds: Int,
    modifier: Modifier = Modifier,
) {
    val errorColor = MaterialTheme.colorScheme.error
    val blinkColor = remember {
        Animatable(AccentGold, Color.VectorConverter(AccentGold.colorSpace))
    }
    var previousBufferedSeconds by remember { mutableIntStateOf(bufferedSeconds) }

    LaunchedEffect(bufferedSeconds) {
        if (bufferedSeconds < previousBufferedSeconds && bufferedSeconds > 0) {
            repeat(EVICTION_BLINK_PULSE_COUNT) {
                blinkColor.animateTo(errorColor, tween(EVICTION_BLINK_DURATION_MILLIS))
                blinkColor.animateTo(AccentGold, tween(EVICTION_BLINK_DURATION_MILLIS))
            }
        }
        previousBufferedSeconds = bufferedSeconds
    }

    Text(
        text = stringResource(
            R.string.recording_buffer_status,
            formatBufferTime(bufferedSeconds),
            formatBufferTime(maxBufferSeconds),
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = blinkColor.value,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

@Composable
fun CaptureRowCard(
    title: String,
    date: String,
    duration: String,
    status: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceContainerHigh, RoundedCornerShape(8.dp))
            .border(
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                    brush = Brush.horizontalGradient(listOf(AccentGold, AntiqueBrass)),
                ),
                shape = RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AccentGold.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.size(14.dp)) {
                    val triangle = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(size.width, size.height / 2f)
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(triangle, color = AccentGold)
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = ParchmentCream,
                )
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AccentGold,
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = duration,
                style = MaterialTheme.typography.bodyMedium,
                color = ParchmentCream,
            )
            Text(
                text = status,
                style = MaterialTheme.typography.labelMedium,
                color = AccentGold,
            )
        }
    }
}

/**
 * Two-card deck showing the newest capture in front and the previous one receded behind it.
 * When a new note arrives, the incoming front card drops in with a settle spring while the
 * displaced card slides down, shrinks, and dims into the behind slot.
 */
@Composable
fun StackedCaptureDeck(
    notes: List<Note> = emptyList(),
    onSelectNote: (Note) -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    modifier: Modifier = Modifier,
    processingUiState: NoteProcessingUiState? = null,
) {
    val frontNote = notes.firstOrNull()
    val behindNote = notes.getOrNull(1)

    // Track note ids so the arrival and recede animations fire only on real
    // captures, never on first composition or content-only updates.
    val cardArrival = remember { Animatable(1f) }
    val cardRecede = remember { Animatable(1f) }
    var previousFrontId by remember { mutableStateOf(frontNote?.id) }
    var previousBehindId by remember { mutableStateOf(behindNote?.id) }

    LaunchedEffect(frontNote?.id) {
        if (frontNote?.id != previousFrontId) {
            previousFrontId = frontNote?.id
            if (frontNote != null) {
                cardArrival.snapTo(0f)
                cardArrival.animateTo(
                    1f,
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                )
            }
        }
    }

    LaunchedEffect(behindNote?.id) {
        if (behindNote?.id != previousBehindId) {
            previousBehindId = behindNote?.id
            if (behindNote != null) {
                cardRecede.snapTo(0f)
                cardRecede.animateTo(
                    1f,
                    tween(CAPTURE_CARD_RECEDE_DURATION_MILLIS, easing = FastOutSlowInEasing),
                )
            }
        }
    }

    if (frontNote == null) return

    val frontTitle = frontNote.title.ifBlank { "Untitled Note" }
    val frontDate = formatNoteDate(frontNote.createdAtEpochMs)
    val frontDuration = formatNoteDuration(frontNote.audio.durationMs)
    val frontStatus = stringResource(captureStatusLabelRes(frontNote, processingUiState))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 28.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        if (behindNote != null) {
            CaptureRowCard(
                title = behindNote.title.ifBlank { "Untitled Note" },
                date = formatNoteDate(behindNote.createdAtEpochMs),
                duration = formatNoteDuration(behindNote.audio.durationMs),
                status = stringResource(captureStatusLabelRes(behindNote, processingUiState)),
                onClick = {
                    onNavigateToHome()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        val progress = cardRecede.value
                        alpha = CAPTURE_CARD_BEHIND_ALPHA +
                            (1f - CAPTURE_CARD_BEHIND_ALPHA) * (1f - progress)
                        translationY = CAPTURE_CARD_BEHIND_OFFSET_DP.dp.toPx() * progress
                        val scale = CAPTURE_CARD_BEHIND_SCALE +
                            (1f - CAPTURE_CARD_BEHIND_SCALE) * (1f - progress)
                        scaleX = scale
                        scaleY = scale
                    },
            )
        }

        CaptureRowCard(
            title = frontTitle,
            date = frontDate,
            duration = frontDuration,
            status = frontStatus,
            onClick = {
                onSelectNote(frontNote)
            },
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    val progress = cardArrival.value
                    alpha = progress
                    translationY = (1f - progress) * CAPTURE_CARD_ARRIVAL_OFFSET_DP.dp.toPx()
                    val scale = CAPTURE_CARD_ARRIVAL_SCALE +
                        (1f - CAPTURE_CARD_ARRIVAL_SCALE) * progress
                    scaleX = scale
                    scaleY = scale
                },
        )
    }
}

/** A capture shows as processed once AI output is attached or the shared AI tracker succeeded for it. */
private fun captureStatusLabelRes(note: Note, processingUiState: NoteProcessingUiState?): Int =
    if (note.summary != null || note.transcript != null ||
        (processingUiState?.noteId == note.id && processingUiState.isSuccess)
    ) {
        R.string.processed_label
    } else {
        R.string.unprocessed_label
    }

private fun formatNoteDate(epochMs: Long): String {
    val sdf = java.text.SimpleDateFormat("MMMdd HH:mm", Locale.US)
    return sdf.format(java.util.Date(epochMs)).uppercase(Locale.US)
}

private fun formatNoteDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000L).toInt()
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", m, s)
}

private fun formatTimerSeconds(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
}

private fun formatBufferTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return if (m > 0) String.format(Locale.US, "%d:%02d", m, s) else "${s}s"
}

/**
 * Convenience stateful overload for [RecordScreen].
 */
@Composable
fun RecordScreen(
    recordingViewModel: KnockRecordingController,
    hasRecordAudioPermission: Boolean,
    onRequestPermission: () -> Unit,
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier,
    autoAiEnabled: Boolean = true,
    onAutoAiChanged: (Boolean) -> Unit = {},
    notes: List<Note> = emptyList(),
    onSelectNote: (Note) -> Unit = {},
    processingUiState: NoteProcessingUiState? = null,
) {
    val recordingUiState by recordingViewModel.uiState.collectAsState()

    RecordScreen(
        recordingUiState = recordingUiState,
        hasRecordAudioPermission = hasRecordAudioPermission,
        onStartSession = {
            if (hasRecordAudioPermission) {
                recordingViewModel.startSession()
            } else {
                onRequestPermission()
            }
        },
        onPauseSession = recordingViewModel::pauseSession,
        onStopSession = {
            recordingViewModel.stopSession()
            onNavigateToHome()
        },
        onManualSave = recordingViewModel::triggerManualSave,
        onRequestPermission = onRequestPermission,
        onNavigateToHome = onNavigateToHome,
        modifier = modifier,
        autoAiEnabled = autoAiEnabled,
        onAutoAiChanged = onAutoAiChanged,
        onDurationChange = recordingViewModel::setBufferDurationSeconds,
        onSimulateKnock = recordingViewModel::triggerManualSave,
        notes = notes,
        onSelectNote = onSelectNote,
        captureEvents = recordingViewModel.gestureCaptures,
        processingUiState = processingUiState,
    )
}

@Preview(showBackground = true)
@Composable
private fun RecordScreenPreview() {
    KnockledgeTheme {
        RecordScreen(
            recordingUiState = KnockRecordingUiState(
                isRecording = true,
                sessionSeconds = 222,
                bufferedSeconds = 222,
                maxBufferSeconds = 300,
                level = 0.65f,
                bands = List(14) { 0.9f - it * 0.06f },
            ),
            hasRecordAudioPermission = true,
            onStartSession = {},
            onStopSession = {},
            onManualSave = {},
            onRequestPermission = {},
            onNavigateToHome = {},
            notes = listOf(
                Note(
                    id = "preview-note",
                    userId = "preview-user",
                    title = "Photosynthesis",
                    createdAtEpochMs = 0L,
                    updatedAtEpochMs = 0L,
                    audio = group.two.knockledge.data.model.AudioMetadata(
                        localPath = "",
                        durationMs = 25_000L,
                        sampleRate = 16000,
                        triggerType = group.two.knockledge.data.model.TriggerType.MANUAL,
                    ),
                ),
            ),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0E15)
@Composable
private fun RecordTopBarPreview() {
    KnockledgeTheme {
        RecordTopBar(
            sessionSeconds = 222,
            autoAiEnabled = true,
            onAutoAiChanged = {},
            isPaused = true,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0E15)
@Composable
private fun BufferStatusTextPreview() {
    KnockledgeTheme {
        BufferStatusText(
            bufferedSeconds = 222,
            maxBufferSeconds = 300,
        )
    }
}

@Composable
private fun CanvasStopSquareIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.25f, h * 0.25f),
            size = Size(w * 0.5f, h * 0.5f),
            cornerRadius = CornerRadius(2.dp.toPx()),
        )
    }
}


