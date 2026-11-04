package group.two.knockledge.ui.playback

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.compose.ui.unit.sp
import group.two.knockledge.R
import group.two.knockledge.audio.AudioPlaybackState
import group.two.knockledge.audio.readWaveformPeaks
import group.two.knockledge.data.model.AudioMetadata
import group.two.knockledge.data.model.ChapterMarker
import group.two.knockledge.data.model.Note
import group.two.knockledge.data.model.Sentence
import group.two.knockledge.data.model.SummaryMetadata
import group.two.knockledge.data.model.TranscriptMetadata
import group.two.knockledge.data.model.TranscriptionEngineType
import group.two.knockledge.data.model.TriggerType
import group.two.knockledge.data.model.AiEngineType
import group.two.knockledge.pipeline.RewindStage
import group.two.knockledge.pipeline.NoteAiProcessingState
import group.two.knockledge.pipeline.NoteProcessingUiState
import group.two.knockledge.ui.theme.AccentGold
import group.two.knockledge.ui.theme.AntiqueBrass
import group.two.knockledge.ui.theme.BurnishedBronze
import group.two.knockledge.ui.theme.ErrorRed
import group.two.knockledge.ui.theme.KnockledgeTheme
import group.two.knockledge.ui.theme.MutedSand
import group.two.knockledge.ui.theme.OutlineVariant
import group.two.knockledge.ui.theme.ParchmentCream
import group.two.knockledge.ui.theme.SurfaceContainerHigh
import group.two.knockledge.ui.theme.SurfaceContainerHighest
import group.two.knockledge.ui.theme.SurfaceIndigo
import group.two.knockledge.ui.theme.TextInk
import group.two.knockledge.ui.theme.VcrOsdMono
import group.two.knockledge.ui.theme.WaveformTeal
import group.two.knockledge.ui.theme.speakerClothTexture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sin

/**
 * Tab options for switching between the AI summary and speaker transcript.
 */
enum class AudioPlaybackTab {
    SUMMARY,
    TRANSCRIPT,
}

/**
 * Stateful [AudioPlaybackScreen] host that connects [AudioPlaybackViewModel] and reactive
 * playback state to the stateless playback UI.
 *
 * @param note Note entity containing audio metadata, transcript, and summary.
 * @param playbackViewModel ViewModel managing audio playback state and controls.
 * @param onBackClick Navigation callback to return to the previous screen.
 * @param modifier Modifier for styling the root layout.
 * @param onDeleteClick Optional callback to delete the current note.
 * @param onRename Optional callback to rename the current note.
 * @param onProcessWithAi Optional callback to trigger on-demand AI processing for this note.
 * @param processingUiState Optional AI processing UI state for progress tracking.
 * @param initialTab Default tab (TRANSCRIPT or SUMMARY) to show when screen opens.
 */
@Composable
fun AudioPlaybackScreen(
    note: Note,
    playbackViewModel: AudioPlaybackViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDeleteClick: (() -> Unit)? = null,
    onRename: ((String) -> Unit)? = null,
    onProcessWithAi: (() -> Unit)? = null,
    onStopAiProcessing: (() -> Unit)? = null,
    processingUiState: NoteProcessingUiState? = null,
    initialTab: AudioPlaybackTab = AudioPlaybackTab.TRANSCRIPT,
) {
    LaunchedEffect(note.id) {
        playbackViewModel.loadNote(note, autoPlay = false)
    }

    val view = LocalView.current
    DisposableEffect(view, playbackViewModel) {
        val owner = view.findViewTreeLifecycleOwner()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) playbackViewModel.pausePlayback()
        }
        owner?.lifecycle?.addObserver(observer)
        onDispose {
            owner?.lifecycle?.removeObserver(observer)
            playbackViewModel.onCleared()
        }
    }

    val playbackState by playbackViewModel.playbackState.collectAsState()

    AudioPlaybackScreen(
        note = note,
        playbackState = playbackState,
        onTogglePlayPause = playbackViewModel::togglePlayPause,
        onSeekTo = playbackViewModel::seekTo,
        onSelectChapter = playbackViewModel::selectChapter,
        onBackClick = onBackClick,
        modifier = modifier,
        onDeleteClick = onDeleteClick,
        onRename = onRename,
        onProcessWithAi = onProcessWithAi,
        onStopAiProcessing = onStopAiProcessing,
        processingUiState = processingUiState,
        initialTab = initialTab,
    )
}

/**
 * High-end playback screen merging wireframe navigation with tactile audio controls.
 *
 * Visual hierarchy:
 * 1. Top bar: Back navigation, note timestamp or title, delete icon.
 * 2. Segmented Pill Tabs: Summary and Transcript switcher.
 * 3. Scrollable Body: AI Executive summary / key points, or speaker transcript with active bubble highlighting.
 * 4. Docked Audio Playback Component: Chapter pill, waveform scrubber with red needle, and squircle controls.
 *
 * @param note Note entity containing audio metadata, transcript, and summary.
 * @param playbackState Current reactive audio playback state.
 * @param onTogglePlayPause Action toggling audio play and pause.
 * @param onSeekTo Action seeking playback position in milliseconds.
 * @param onSelectChapter Action selecting and seeking to a chapter start time.
 * @param onBackClick Action navigating back to the previous screen.
 * @param onDeleteClick Optional action deleting the current note.
 * @param onRename Optional action renaming the current note.
 * @param onProcessWithAi Optional action triggering AI processing for this note.
 * @param processingUiState Optional AI processing progress state for this note.
 * @param modifier Modifier for styling the root layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlaybackScreen(
    note: Note,
    playbackState: AudioPlaybackState,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSelectChapter: (ChapterMarker) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDeleteClick: (() -> Unit)? = null,
    onRename: ((String) -> Unit)? = null,
    onProcessWithAi: (() -> Unit)? = null,
    onStopAiProcessing: (() -> Unit)? = null,
    processingUiState: NoteProcessingUiState? = null,
    initialTab: AudioPlaybackTab = AudioPlaybackTab.TRANSCRIPT,
) {
    var selectedTab by remember { mutableStateOf(initialTab) }
    var showChapterSheet by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var currentTitle by remember(note.title) { mutableStateOf(note.title) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val chapters = note.summary?.chapterMarkers.orEmpty()
    val scope = rememberCoroutineScope()

    val isProcessingThisNote = processingUiState?.noteId == note.id && processingUiState.isProcessing
    val isFailedThisNote = processingUiState?.noteId == note.id && processingUiState.state == NoteAiProcessingState.FAILED
    val isSkippedThisNote = processingUiState?.noteId == note.id && processingUiState.state == NoteAiProcessingState.SKIPPED
    val stageThisNote = if (isProcessingThisNote) processingUiState.stage else null
    val errorMessageThisNote = if (isFailedThisNote || isSkippedThisNote) processingUiState.errorMessage else null
    val peaks by produceState(FloatArray(0), note.audio.localPath) {
        value = withContext(Dispatchers.IO) { readWaveformPeaks(File(note.audio.localPath)) }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .speakerClothTexture()
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            AudioPlaybackTopBar(
                createdAtEpochMs = note.createdAtEpochMs,
                title = currentTitle,
                onBackClick = onBackClick,
                onRenameClick = { showRenameDialog = true },
                onDeleteClick = onDeleteClick,
                onProcessWithAi = onProcessWithAi,
                isProcessed = note.summary != null || note.transcript != null,
            )
        },
        containerColor = Color.Transparent,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 1. Audio Playback Dock (Edge-to-edge, matching screen width)
            AudioPlaybackDock(
                state = playbackState,
                peaks = peaks,
                chapters = chapters,
                onTogglePlayPause = onTogglePlayPause,
                onSeekTo = onSeekTo,
                onOpenChapters = { showChapterSheet = true },
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Tabs and Content container with horizontal padding
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp),
            ) {
                // Segmented Control Tabs (Summary | Transcript)
                SegmentedPillTabs(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Tab Content (Summary or Transcript)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    when (selectedTab) {
                        AudioPlaybackTab.SUMMARY -> {
                            AudioSummaryView(
                                summary = note.summary,
                                onProcessWithAi = onProcessWithAi,
                                onStopProcessing = onStopAiProcessing,
                                onDelete = onDeleteClick,
                                isProcessing = isProcessingThisNote,
                                processingStage = stageThisNote,
                                isFailed = isFailedThisNote,
                                isSkipped = isSkippedThisNote,
                                errorMessage = errorMessageThisNote,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        AudioPlaybackTab.TRANSCRIPT -> {
                            AudioTranscriptView(
                                transcript = note.transcript,
                                currentPositionMs = playbackState.currentPositionMs,
                                onSeekTo = onSeekTo,
                                onProcessWithAi = onProcessWithAi,
                                onStopProcessing = onStopAiProcessing,
                                onDelete = onDeleteClick,
                                isProcessing = isProcessingThisNote,
                                processingStage = stageThisNote,
                                isFailed = isFailedThisNote,
                                isSkipped = isSkippedThisNote,
                                errorMessage = errorMessageThisNote,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet for Chapter Navigation
    if (showChapterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showChapterSheet = false },
            sheetState = sheetState,
            containerColor = SurfaceContainerHighest,
            contentColor = ParchmentCream,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
                )
            },
        ) {
            ChaptersBottomSheetContent(
                chapters = chapters,
                activeChapter = playbackState.activeChapter,
                onSelectChapter = { chapter ->
                    onSelectChapter(chapter)
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            showChapterSheet = false
                        }
                    }
                },
                modifier = Modifier.padding(bottom = 24.dp),
            )
        }
    }

    // Rename Dialog
    if (showRenameDialog) {
        var textInput by remember { mutableStateOf(currentTitle) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Note", color = ParchmentCream) },
            text = {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            currentTitle = textInput
                            onRename?.invoke(textInput)
                        }
                        showRenameDialog = false
                    },
                ) {
                    Text("Save", color = AccentGold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel", color = MutedSand)
                }
            },
            containerColor = SurfaceContainerHighest,
            shape = RoundedCornerShape(28.dp),
        )
    }
}

/**
 * Clean top bar showing back navigation, formatted date timestamp, and action menu.
 */
@Composable
private fun AudioPlaybackTopBar(
    createdAtEpochMs: Long,
    title: String,
    onBackClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: (() -> Unit)?,
    onProcessWithAi: (() -> Unit)?,
    isProcessed: Boolean,
    modifier: Modifier = Modifier,
) {
    val formattedDate = remember(createdAtEpochMs) {
        val formatter = SimpleDateFormat("MMM dd, yyyy  h:mm a", Locale.US)
        formatter.format(Date(createdAtEpochMs)).uppercase(Locale.US)
    }
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Transparent back button (no background color or border)
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.size(40.dp),
        ) {
            CanvasBackArrowIcon(tint = MaterialTheme.colorScheme.onBackground)
        }

        Text(
            text = title.ifBlank { formattedDate },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        )

        // Vertical 3 dots menu button (transparent background/border)
        Box {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(40.dp),
            ) {
                CanvasVerticalDotsIcon(tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f))
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                containerColor = SurfaceContainerHighest,
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.ai_reprocess),
                            color = if (isProcessed) ParchmentCream else MutedSand,
                        )
                    },
                    onClick = {
                        showMenu = false
                        onProcessWithAi?.invoke()
                    },
                    enabled = isProcessed,
                )
                DropdownMenuItem(
                    text = { Text("Rename", color = ParchmentCream) },
                    onClick = {
                        showMenu = false
                        onRenameClick()
                    },
                )
                if (onDeleteClick != null) {
                    DropdownMenuItem(
                        text = { Text("Delete", color = ErrorRed) },
                        onClick = {
                            showMenu = false
                            onDeleteClick()
                        },
                    )
                }
            }
        }
    }
}

/**
 * Mode Toggle Segmented Control (Summary / Transcript) copied verbatim from AuthScreen.
 */
@Composable
private fun SegmentedPillTabs(
    selectedTab: AudioPlaybackTab,
    onTabSelected: (AudioPlaybackTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val buttonShape = MaterialTheme.shapes.extraSmall

    val summaryBgColor by animateColorAsState(
        targetValue = if (selectedTab == AudioPlaybackTab.SUMMARY) AntiqueBrass else Color.Transparent,
        label = "summaryBgColor",
    )
    val summaryTextColor by animateColorAsState(
        targetValue = if (selectedTab == AudioPlaybackTab.SUMMARY) TextInk else ParchmentCream,
        label = "summaryTextColor",
    )
    val transcriptBgColor by animateColorAsState(
        targetValue = if (selectedTab == AudioPlaybackTab.TRANSCRIPT) AntiqueBrass else Color.Transparent,
        label = "transcriptBgColor",
    )
    val transcriptTextColor by animateColorAsState(
        targetValue = if (selectedTab == AudioPlaybackTab.TRANSCRIPT) TextInk else ParchmentCream,
        label = "transcriptTextColor",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(SurfaceIndigo, buttonShape)
            .border(1.dp, AntiqueBrass.copy(alpha = 0.4f), buttonShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .background(summaryBgColor, buttonShape)
                .clickable {
                    onTabSelected(AudioPlaybackTab.SUMMARY)
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.tab_summary),
                style = MaterialTheme.typography.labelLarge,
                color = summaryTextColor,
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .background(transcriptBgColor, buttonShape)
                .clickable {
                    onTabSelected(AudioPlaybackTab.TRANSCRIPT)
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.transcript_label),
                style = MaterialTheme.typography.labelLarge,
                color = transcriptTextColor,
            )
        }
    }
}

/**
 * Inline banner displayed during active AI processing when tab content exists.
 */
@Composable
private fun AiProcessingBanner(
    processingStage: RewindStage?,
    onStopProcessing: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
        border = BorderStroke(1.dp, AccentGold.copy(alpha = 0.5f)),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = AccentGold,
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val stageText = if (processingStage == RewindStage.Transcribing) {
                        stringResource(R.string.processing_transcribing)
                    } else {
                        stringResource(R.string.processing_summarizing)
                    }
                    Text(
                        text = stageText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = ParchmentCream,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.badge_processing),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = WaveformTeal,
                    )
                    if (onStopProcessing != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = onStopProcessing,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.processing_stop),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = AccentGold,
                trackColor = AntiqueBrass.copy(alpha = 0.2f),
            )
        }
    }
}

/**
 * Inline banner displayed when AI processing failed or was skipped.
 */
@Composable
private fun AiStatusBanner(
    isSkipped: Boolean,
    errorMessage: String?,
    onRetry: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val isLengthError = errorMessage?.contains("speech", ignoreCase = true) == true ||
        errorMessage?.contains("length", ignoreCase = true) == true ||
        errorMessage?.contains("1.0s", ignoreCase = true) == true ||
        errorMessage?.contains("short", ignoreCase = true) == true ||
        errorMessage?.contains("long", ignoreCase = true) == true ||
        errorMessage?.contains("duration", ignoreCase = true) == true ||
        errorMessage?.contains("insufficient", ignoreCase = true) == true

    val title = when {
        isSkipped -> stringResource(R.string.processing_skipped)
        isLengthError -> stringResource(R.string.processing_failed_length_title)
        else -> stringResource(R.string.processing_failed_error_title)
    }

    val defaultMsg = when {
        isSkipped -> stringResource(R.string.processing_skipped_subtitle)
        isLengthError -> stringResource(R.string.processing_failed_length_subtitle)
        else -> stringResource(R.string.processing_failed)
    }

    val message = errorMessage ?: defaultMsg

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSkipped) SurfaceContainerHigh else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
        ),
        border = BorderStroke(1.dp, if (isSkipped) OutlineVariant else MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSkipped) ParchmentCream else MaterialTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSand,
                )
            }
            if (onRetry != null || (isSkipped && onDelete != null)) {
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    if (onRetry != null) {
                        OutlinedButton(
                            onClick = onRetry,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.retry),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    if (isSkipped && onDelete != null) {
                        TextButton(onClick = onDelete) {
                            Text(
                                text = stringResource(R.string.delete_note),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Shared tactile empty or processing state view for Summary and Transcript tabs
 * when AI results are missing or currently generating.
 */
@Composable
private fun AiEmptyOrProcessingView(
    title: String,
    subtitle: String,
    onProcessWithAi: (() -> Unit)?,
    onStopProcessing: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    isProcessing: Boolean,
    processingStage: RewindStage?,
    isFailed: Boolean,
    errorMessage: String?,
    modifier: Modifier = Modifier,
    isSkipped: Boolean = false,
) {
    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                shape = CircleShape,
                color = when {
                    isProcessing -> AntiqueBrass.copy(alpha = 0.15f)
                    isFailed -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    isSkipped -> AntiqueBrass.copy(alpha = 0.1f)
                    else -> AntiqueBrass.copy(alpha = 0.15f)
                },
                modifier = Modifier.size(64.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    when {
                        isProcessing -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = AccentGold,
                                strokeWidth = 2.5.dp,
                            )
                        }
                        isFailed -> {
                            CanvasSparkleIcon(tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp))
                        }
                        isSkipped -> {
                            CanvasSparkleIcon(tint = MutedSand, modifier = Modifier.size(32.dp))
                        }
                        else -> {
                            CanvasSparkleIcon(tint = AccentGold, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            when {
                isProcessing -> {
                    val stageText = if (processingStage == RewindStage.Transcribing) {
                        stringResource(R.string.processing_transcribing)
                    } else {
                        stringResource(R.string.processing_summarizing)
                    }
                    Text(
                        text = stageText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ParchmentCream,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.badge_processing),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = WaveformTeal,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = AccentGold,
                        trackColor = AntiqueBrass.copy(alpha = 0.2f),
                    )
                    if (onStopProcessing != null) {
                        Spacer(modifier = Modifier.height(20.dp))
                        OutlinedButton(
                            onClick = onStopProcessing,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, OutlineVariant),
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(48.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.processing_stop),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                isFailed || isSkipped -> {
                    val isLengthError = errorMessage?.contains("speech", ignoreCase = true) == true ||
                        errorMessage?.contains("length", ignoreCase = true) == true ||
                        errorMessage?.contains("1.0s", ignoreCase = true) == true ||
                        errorMessage?.contains("short", ignoreCase = true) == true ||
                        errorMessage?.contains("long", ignoreCase = true) == true ||
                        errorMessage?.contains("duration", ignoreCase = true) == true ||
                        errorMessage?.contains("insufficient", ignoreCase = true) == true

                    val errorTitle = when {
                        isSkipped -> stringResource(R.string.processing_skipped)
                        isLengthError -> stringResource(R.string.processing_failed_length_title)
                        else -> stringResource(R.string.processing_failed_error_title)
                    }

                    val defaultSubtitle = when {
                        isSkipped -> stringResource(R.string.processing_skipped_subtitle)
                        isLengthError -> stringResource(R.string.processing_failed_length_subtitle)
                        else -> stringResource(R.string.processing_failed)
                    }

                    val displayMessage = errorMessage ?: defaultSubtitle

                    val badgeText = when {
                        isSkipped -> stringResource(R.string.badge_skipped)
                        isLengthError -> stringResource(R.string.badge_length_error)
                        else -> stringResource(R.string.badge_failed)
                    }

                    Text(
                        text = errorTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSkipped) MutedSand else MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = displayMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedSand,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSkipped) MutedSand else MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { onProcessWithAi?.invoke() },
                        enabled = onProcessWithAi != null,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentGold,
                            contentColor = TextInk,
                        ),
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(48.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            CanvasSparkleIcon(tint = TextInk, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.retry),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    if (isSkipped && onDelete != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(onClick = onDelete) {
                            Text(
                                text = stringResource(R.string.delete_note),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }

                else -> {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ParchmentCream,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedSand,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { onProcessWithAi?.invoke() },
                        enabled = onProcessWithAi != null,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentGold,
                            contentColor = TextInk,
                            disabledContainerColor = AccentGold.copy(alpha = 0.4f),
                            disabledContentColor = TextInk.copy(alpha = 0.5f),
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 2.dp,
                            pressedElevation = 0.dp,
                        ),
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(48.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            CanvasSparkleIcon(tint = TextInk, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.process_with_ai),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Summary view showing executive takeaways and bullet points.
 */
@Composable
private fun AudioSummaryView(
    summary: SummaryMetadata?,
    onProcessWithAi: (() -> Unit)?,
    onStopProcessing: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    isProcessing: Boolean,
    processingStage: RewindStage?,
    isFailed: Boolean,
    errorMessage: String?,
    modifier: Modifier = Modifier,
    isSkipped: Boolean = false,
) {
    if (summary == null || (summary.executiveSummary.isBlank() && summary.keyPoints.isEmpty())) {
        AiEmptyOrProcessingView(
            title = stringResource(R.string.no_summary),
            subtitle = stringResource(R.string.summary_empty_subtitle),
            onProcessWithAi = onProcessWithAi,
            onStopProcessing = onStopProcessing,
            onDelete = onDelete,
            isProcessing = isProcessing,
            processingStage = processingStage,
            isFailed = isFailed,
            isSkipped = isSkipped,
            errorMessage = errorMessage,
            modifier = modifier,
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        if (isProcessing) {
            item {
                AiProcessingBanner(processingStage = processingStage, onStopProcessing = onStopProcessing)
            }
        } else if (isFailed || isSkipped) {
            item {
                AiStatusBanner(
                    isSkipped = isSkipped,
                    errorMessage = errorMessage,
                    onRetry = onProcessWithAi,
                    onDelete = onDelete,
                )
            }
        }

        if (summary.executiveSummary.isNotBlank()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
                    border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.4f)),
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = stringResource(R.string.summary_label).uppercase(Locale.US),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = AccentGold,
                            letterSpacing = 1.sp,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = summary.executiveSummary,
                            style = MaterialTheme.typography.bodyLarge,
                            color = ParchmentCream,
                            lineHeight = 24.sp,
                        )
                    }
                }
            }
        }

        if (summary.keyPoints.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.key_bullet_points),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = ParchmentCream,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                )
            }

            itemsIndexed(summary.keyPoints) { index, point ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh.copy(alpha = 0.7f)),
                    border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.25f)),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = AntiqueBrass.copy(alpha = 0.2f),
                            modifier = Modifier.size(24.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AntiqueBrass,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = point,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ParchmentCream,
                            lineHeight = 22.sp,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Transcript view displaying speaker names, timestamps, and active speech bubble highlighting.
 */
@Composable
private fun AudioTranscriptView(
    transcript: TranscriptMetadata?,
    currentPositionMs: Long,
    onSeekTo: (Long) -> Unit,
    onProcessWithAi: (() -> Unit)?,
    onStopProcessing: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    isProcessing: Boolean,
    processingStage: RewindStage?,
    isFailed: Boolean,
    errorMessage: String?,
    modifier: Modifier = Modifier,
    isSkipped: Boolean = false,
) {
    val sentences = transcript?.sentences.orEmpty().ifEmpty {
        if (!transcript?.fullText.isNullOrBlank()) {
            listOf(
                Sentence(
                    speaker = "Speaker",
                    startTimeMs = 0L,
                    endTimeMs = 0L,
                    text = transcript.fullText,
                    confidence = 1.0f,
                )
            )
        } else {
            emptyList()
        }
    }

    if (sentences.isEmpty()) {
        AiEmptyOrProcessingView(
            title = stringResource(R.string.no_transcript),
            subtitle = stringResource(R.string.transcript_empty_subtitle),
            onProcessWithAi = onProcessWithAi,
            onStopProcessing = onStopProcessing,
            onDelete = onDelete,
            isProcessing = isProcessing,
            processingStage = processingStage,
            isFailed = isFailed,
            isSkipped = isSkipped,
            errorMessage = errorMessage,
            modifier = modifier,
        )
        return
    }

    val listState = rememberLazyListState()

    // Smooth scroll to keep the active sentence visible as playback runs
    val activeIndex = sentences.indexOfFirst { sentence ->
        val index = sentences.indexOf(sentence)
        if (sentence.endTimeMs > sentence.startTimeMs) {
            currentPositionMs in sentence.startTimeMs..sentence.endTimeMs
        } else {
            val nextStart = sentences.getOrNull(index + 1)?.startTimeMs ?: Long.MAX_VALUE
            currentPositionMs in sentence.startTimeMs until nextStart
        }
    }

    val headerOffset = if (isProcessing || isFailed || isSkipped) 1 else 0

    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) {
            listState.animateScrollToItem(activeIndex + headerOffset)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
    ) {
        if (isProcessing) {
            item {
                AiProcessingBanner(processingStage = processingStage, onStopProcessing = onStopProcessing)
            }
        } else if (isFailed || isSkipped) {
            item {
                AiStatusBanner(
                    isSkipped = isSkipped,
                    errorMessage = errorMessage,
                    onRetry = onProcessWithAi,
                    onDelete = onDelete,
                )
            }
        }

        itemsIndexed(sentences) { index, sentence ->
            val isActive = if (sentence.endTimeMs > sentence.startTimeMs) {
                currentPositionMs in sentence.startTimeMs..sentence.endTimeMs
            } else {
                val nextStart = sentences.getOrNull(index + 1)?.startTimeMs ?: Long.MAX_VALUE
                currentPositionMs in sentence.startTimeMs until nextStart
            }

            TranscriptBubbleItem(
                sentence = sentence,
                isActive = isActive,
                onClick = { onSeekTo(sentence.startTimeMs) },
            )
        }
    }
}

/**
 * Speech bubble item matching the inspiration visual design.
 * Active sentence is wrapped in a light squircle card; inactive text is softly faded.
 */
@Composable
private fun TranscriptBubbleItem(
    sentence: Sentence,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val speakerLabel = sentence.speaker.ifBlank { "Speaker" }

    if (isActive) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(16.dp),
            color = BurnishedBronze,
            shadowElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = speakerLabel.uppercase(Locale.US),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AccentGold,
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AccentGold.copy(alpha = 0.15f),
                        modifier = Modifier.clickable(onClick = onClick),
                    ) {
                        Text(
                            text = formatTimeMs(sentence.startTimeMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentGold,
                            fontFamily = VcrOsdMono,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = sentence.text,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = ParchmentCream,
                    lineHeight = 24.sp,
                )
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = speakerLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedSand.copy(alpha = 0.5f),
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ParchmentCream.copy(alpha = 0.08f),
                    modifier = Modifier.clickable(onClick = onClick),
                ) {
                    Text(
                        text = formatTimeMs(sentence.startTimeMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = ParchmentCream.copy(alpha = 0.7f),
                        fontFamily = VcrOsdMono,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = sentence.text,
                style = MaterialTheme.typography.bodyMedium,
                color = ParchmentCream.copy(alpha = 0.45f),
                lineHeight = 22.sp,
            )
        }
    }
}

/**
 * Docked audio playback component fixed to the bottom of the screen.
 * Houses the active chapter pill, waveform scrubber, time counters, and squircle controls.
 *
 * @param peaks Normalized (0..1) waveform amplitude peaks; empty for remote-only audio.
 */
@Composable
private fun AudioPlaybackDock(
    state: AudioPlaybackState,
    peaks: FloatArray,
    chapters: List<ChapterMarker>,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onOpenChapters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFF181614),
        tonalElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF22201C), Color(0xFF171512)),
                    )
                )
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Chapter Pill Indicator using Material 3 FilterChip
            val chapterTitle = state.activeChapter?.title ?: chapters.firstOrNull()?.title
            val chapterIndex = chapters.indexOfFirst { it.startTimeMs == state.activeChapter?.startTimeMs }

            val chapterText = if (!chapterTitle.isNullOrBlank()) {
                val prefix = if (chapterIndex >= 0) "Ch. ${chapterIndex + 1} • " else ""
                "$prefix$chapterTitle"
            } else if (chapters.isNotEmpty()) {
                stringResource(R.string.chapters_sheet_title)
            } else {
                null
            }

            if (chapterText != null) {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    FilterChip(
                        selected = true,
                        onClick = onOpenChapters,
                        label = {
                            Text(
                                text = chapterText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        trailingIcon = {
                            CanvasDropDownIcon(
                                tint = AccentGold,
                                modifier = Modifier.size(10.dp),
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SurfaceContainerHigh,
                            selectedLabelColor = AccentGold,
                            selectedTrailingIconColor = AccentGold,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = true,
                            borderColor = OutlineVariant,
                            selectedBorderColor = AntiqueBrass.copy(alpha = 0.5f),
                        ),
                        shape = RoundedCornerShape(14.dp),
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Waveform Scrubber with Chapter Notches and Red Playhead Needle (Edge-to-edge)
            WaveformScrubberBar(
                currentPositionMs = state.currentPositionMs,
                peaks = peaks,
                durationMs = state.durationMs,
                chapters = chapters,
                onSeekTo = onSeekTo,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp),
            )

            // Timestamps (Current position / Duration)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = formatTimeMs(state.currentPositionMs),
                    fontFamily = VcrOsdMono,
                    fontSize = 12.sp,
                    color = MutedSand,
                )
                Text(
                    text = formatTimeMs(state.durationMs),
                    fontFamily = VcrOsdMono,
                    fontSize = 12.sp,
                    color = MutedSand,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3 Control Buttons (Rewind Icon Button, Play/Pause Squircle, Forward Icon Button)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 1. Rewind 15s Icon Button
                IconButton(
                    onClick = {
                        val newPos = (state.currentPositionMs - 15_000L).coerceAtLeast(0L)
                        onSeekTo(newPos)
                    },
                    modifier = Modifier.size(48.dp),
                ) {
                    CanvasReplay15Icon(tint = ParchmentCream)
                }

                // 2. Center Play / Pause Squircle Button (Colorized, Square with Tactile LED)
                SquircleActionButton(
                    onClick = onTogglePlayPause,
                    containerColor = AccentGold,
                    contentColor = TextInk,
                    borderColor = AccentGold,
                    showActiveLed = state.isPlaying,
                    modifier = Modifier.size(56.dp),
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = TextInk,
                            strokeWidth = 2.dp,
                        )
                    } else if (state.isPlaying) {
                        CanvasPauseBarsIcon(tint = TextInk)
                    } else {
                        CanvasPlayTriangleIcon(tint = TextInk)
                    }
                }

                // 3. Forward 15s Icon Button
                IconButton(
                    onClick = {
                        val maxPos = if (state.durationMs > 0) state.durationMs else Long.MAX_VALUE
                        val newPos = (state.currentPositionMs + 15_000L).coerceAtMost(maxPos)
                        onSeekTo(newPos)
                    },
                    modifier = Modifier.size(48.dp),
                ) {
                    CanvasForward15Icon(tint = ParchmentCream)
                }
            }
        }
    }
}

/**
 * Tactile squircle button with smooth 18dp corner radius and active LED dot.
 */
@Composable
private fun SquircleActionButton(
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    borderColor: Color? = null,
    showActiveLed: Boolean = false,
    content: @Composable () -> Unit,
) {
    val ledDotColor by animateColorAsState(
        targetValue = if (showActiveLed) Color(0xFFFFAB00) else Color(0xFF1B1833).copy(alpha = 0.25f),
        label = "dockLedDotColorAnimation",
    )
    val ledBorderColor by animateColorAsState(
        targetValue = if (showActiveLed) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.18f),
        label = "dockLedBorderColorAnimation",
    )

    Surface(
        onClick = onClick,
        modifier = modifier.clip(RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        contentColor = contentColor,
        border = borderColor?.let { BorderStroke(1.dp, it) } ?: BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.35f)),
        shadowElevation = 2.dp,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            content()

            // Tactile LED dot indicator in top-right corner
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(ledDotColor)
                    .border(BorderStroke(0.5.dp, ledBorderColor), CircleShape),
            )
        }
    }
}

/**
 * Interactive waveform scrubber with vertical bars, chapter divider tick marks,
 * and a red scrubber pin indicator matching Image 2.
 *
 * @param peaks Normalized (0..1) amplitude peaks; when empty a synthetic profile is drawn.
 */
@Composable
private fun WaveformScrubberBar(
    currentPositionMs: Long,
    peaks: FloatArray,
    durationMs: Long,
    chapters: List<ChapterMarker>,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val needleColor = ErrorRed
    val activeBarColor = ParchmentCream.copy(alpha = 0.85f)
    val inactiveBarColor = MutedSand.copy(alpha = 0.35f)
    val tickColor = AccentGold

    val progress = remember(currentPositionMs, durationMs) {
        if (durationMs > 0L) {
            (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    Box(
        modifier = modifier
            .pointerInput(durationMs) {
                detectTapGestures { offset ->
                    if (durationMs > 0) {
                        val targetProgress = (offset.x / size.width).coerceIn(0f, 1f)
                        val targetMs = (targetProgress * durationMs).toLong()
                        onSeekTo(targetMs)
                    }
                }
            }
            .pointerInput(durationMs) {
                detectDragGestures(
                    onDragStart = { offset ->
                        if (durationMs > 0) {
                            val targetProgress = (offset.x / size.width).coerceIn(0f, 1f)
                            val targetMs = (targetProgress * durationMs).toLong()
                            onSeekTo(targetMs)
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        if (durationMs > 0) {
                            val targetProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                            val targetMs = (targetProgress * durationMs).toLong()
                            onSeekTo(targetMs)
                        }
                    },
                )
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val totalWidth = size.width
            val totalHeight = size.height
            val barSpacing = 4.dp.toPx()
            val barWidth = 2.dp.toPx()
            val totalBars = (totalWidth / (barWidth + barSpacing)).toInt().coerceAtLeast(10)
            val baselineY = totalHeight * 0.95f

            val chapterRatios = if (durationMs > 0) {
                chapters.map { (it.startTimeMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) }
            } else {
                emptyList()
            }

            // Draw waveform vertical bars
            for (i in 0 until totalBars) {
                val barProgress = i.toFloat() / totalBars.toFloat()
                val x = i * (barWidth + barSpacing) + barWidth / 2f

                // Deterministic waveform profile
                val heightRatio = if (peaks.isNotEmpty()) {
                    peaks[(i * peaks.size / totalBars).coerceAtMost(peaks.lastIndex)].coerceIn(0.08f, 1f)
                } else {
                    val sineFactor = (sin(i * 0.28) * 0.45 + 0.55).toFloat()
                    (sineFactor * 0.85f + (i % 3) * 0.08f).coerceIn(0.2f, 0.98f)
                }
                val barHeight = totalHeight * 0.90f * heightRatio

                val isPastPlayhead = barProgress <= progress
                val isChapterTick = chapterRatios.any { (it - barProgress) in -0.015f..0.015f }

                val color = when {
                    isChapterTick -> tickColor
                    isPastPlayhead -> activeBarColor
                    else -> inactiveBarColor
                }

                drawLine(
                    color = color,
                    start = Offset(x, baselineY),
                    end = Offset(x, baselineY - barHeight),
                    strokeWidth = if (isChapterTick) barWidth * 1.5f else barWidth,
                    cap = StrokeCap.Butt,
                )
            }

            // Draw dotted/solid baseline
            drawLine(
                color = inactiveBarColor,
                start = Offset(0f, baselineY),
                end = Offset(totalWidth, baselineY),
                strokeWidth = 1.dp.toPx(),
            )

            // Draw Red Playhead Needle & Circular Dot Pin (Inspiration Image 2)
            val needleX = totalWidth * progress
            drawLine(
                color = needleColor,
                start = Offset(needleX, baselineY + 2.dp.toPx()),
                end = Offset(needleX, 6.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawCircle(
                color = needleColor,
                radius = 4.5.dp.toPx(),
                center = Offset(needleX, 6.dp.toPx()),
            )
        }
    }
}

/**
 * Bottom sheet content displaying the list of chapters and jump triggers.
 */
@Composable
private fun ChaptersBottomSheetContent(
    chapters: List<ChapterMarker>,
    activeChapter: ChapterMarker?,
    onSelectChapter: (ChapterMarker) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        Text(
            text = stringResource(R.string.chapters_sheet_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = ParchmentCream,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        if (chapters.isEmpty()) {
            Text(
                text = stringResource(R.string.no_chapters),
                style = MaterialTheme.typography.bodyMedium,
                color = MutedSand,
                modifier = Modifier.padding(vertical = 12.dp),
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(chapters) { index, chapter ->
                    val isActive = chapter.startTimeMs == activeChapter?.startTimeMs

                    OutlinedCard(
                        onClick = { onSelectChapter(chapter) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = if (isActive) SurfaceIndigo else SurfaceContainerHigh,
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isActive) AntiqueBrass else OutlineVariant.copy(alpha = 0.3f),
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "CHAPTER ${index + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isActive) AccentGold else MutedSand,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = chapter.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ParchmentCream,
                                )
                            }
                            Text(
                                text = formatTimeMs(chapter.startTimeMs),
                                fontFamily = VcrOsdMono,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isActive) AccentGold else MutedSand,
                            )
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Canvas Vector Icons (Sharp, Self-Contained, No External Asset Dependencies)
// -----------------------------------------------------------------------------

@Composable
private fun CanvasVerticalDotsIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val radius = 2.dp.toPx()
        val cx = size.width / 2f
        val cy1 = size.height * 0.25f
        val cy2 = size.height * 0.5f
        val cy3 = size.height * 0.75f

        drawCircle(color = tint, radius = radius, center = Offset(cx, cy1))
        drawCircle(color = tint, radius = radius, center = Offset(cx, cy2))
        drawCircle(color = tint, radius = radius, center = Offset(cx, cy3))
    }
}

@Composable
private fun CanvasSparkleIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(28.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.5f, 0f)
            quadraticTo(w * 0.5f, h * 0.5f, w, h * 0.5f)
            quadraticTo(w * 0.5f, h * 0.5f, w * 0.5f, h)
            quadraticTo(w * 0.5f, h * 0.5f, 0f, h * 0.5f)
            quadraticTo(w * 0.5f, h * 0.5f, w * 0.5f, 0f)
            close()
        }
        drawPath(path, color = tint)
    }
}

@Composable
private fun CanvasDropDownIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(12.dp)) {
        val path = Path().apply {
            moveTo(size.width * 0.15f, size.height * 0.35f)
            lineTo(size.width * 0.85f, size.height * 0.35f)
            lineTo(size.width * 0.5f, size.height * 0.75f)
            close()
        }
        drawPath(path, color = tint)
    }
}

@Composable
private fun CanvasBackArrowIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val stroke = 2.dp.toPx()
        val w = size.width
        val h = size.height
        drawLine(
            color = tint,
            start = Offset(w * 0.85f, h * 0.5f),
            end = Offset(w * 0.15f, h * 0.5f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.15f, h * 0.5f),
            end = Offset(w * 0.45f, h * 0.2f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.15f, h * 0.5f),
            end = Offset(w * 0.45f, h * 0.8f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

/**
 * Formats milliseconds into mm:ss string format.
 */
fun formatTimeMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
}


@Composable
private fun CanvasPlayTriangleIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val path = Path().apply {
            moveTo(size.width * 0.3f, size.height * 0.2f)
            lineTo(size.width * 0.8f, size.height * 0.5f)
            lineTo(size.width * 0.3f, size.height * 0.8f)
            close()
        }
        drawPath(path, color = tint)
    }
}

@Composable
private fun CanvasPauseBarsIcon(tint: Color, modifier: Modifier = Modifier) {
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
private fun CanvasReplay15Icon(tint: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(36.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 2.dp.toPx()
            val w = size.width
            val h = size.height
            val center = Offset(w / 2f, h / 2f)
            val radius = w * 0.38f

            drawArc(
                color = tint,
                startAngle = 120f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )

            val arrowX = center.x + radius * kotlin.math.cos(Math.toRadians(120.0)).toFloat()
            val arrowY = center.y + radius * sin(Math.toRadians(120.0)).toFloat()
            val path = Path().apply {
                moveTo(arrowX - 5f, arrowY - 6f)
                lineTo(arrowX + 4f, arrowY - 2f)
                lineTo(arrowX - 3f, arrowY + 6f)
                close()
            }
            drawPath(path, color = tint)
        }
        Text(
            text = "15",
            fontFamily = VcrOsdMono,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = tint,
        )
    }
}

@Composable
private fun CanvasForward15Icon(tint: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(36.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 2.dp.toPx()
            val w = size.width
            val h = size.height
            val center = Offset(w / 2f, h / 2f)
            val radius = w * 0.38f

            drawArc(
                color = tint,
                startAngle = 300f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )

            val arrowX = center.x + radius * kotlin.math.cos(Math.toRadians(210.0)).toFloat()
            val arrowY = center.y + radius * sin(Math.toRadians(210.0)).toFloat()
            val path = Path().apply {
                moveTo(arrowX + 5f, arrowY - 6f)
                lineTo(arrowX - 4f, arrowY - 2f)
                lineTo(arrowX + 3f, arrowY + 6f)
                close()
            }
            drawPath(path, color = tint)
        }
        Text(
            text = "15",
            fontFamily = VcrOsdMono,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = tint,
        )
    }
}

// -----------------------------------------------------------------------------
// Stateless Compose Previews
// -----------------------------------------------------------------------------

@Preview(name = "Audio Playback Screen - Transcript Tab", showBackground = true)
@Composable
fun AudioPlaybackScreenTranscriptPreview() {
    val sampleNote = Note(
        id = "preview_1",
        userId = "user_preview",
        title = "Marketing Strategy Review",
        createdAtEpochMs = 1738421160000L, // Feb 01, 2025 2:46 PM
        updatedAtEpochMs = 1738421160000L,
        audio = AudioMetadata(
            localPath = "/mock/audio.wav",
            durationMs = 25_000L,
            sampleRate = 16000,
            triggerType = TriggerType.MANUAL,
        ),
        transcript = TranscriptMetadata(
            fullText = "Today I'm going to talk about new marketing strategies for business owners. First, let's get to know... My name is David Crowie and I am current hmm... Financial consultant with 20 years of experience in business support.",
            sentences = listOf(
                Sentence(
                    speaker = "Speaker 1",
                    startTimeMs = 0L,
                    endTimeMs = 5000L,
                    text = "Today I'm going to talk about new marketing strategies for business owners.",
                    confidence = 0.95f,
                ),
                Sentence(
                    speaker = "Speaker 1",
                    startTimeMs = 5000L,
                    endTimeMs = 18000L,
                    text = "First, let's get to know... My name is David Crowie and I am current hmm...",
                    confidence = 0.98f,
                ),
                Sentence(
                    speaker = "Speaker 2",
                    startTimeMs = 18000L,
                    endTimeMs = 25000L,
                    text = "Financial consultant with 20 years of experience in business support.",
                    confidence = 0.92f,
                ),
            ),
            isDiarized = true,
            detectedLanguage = "en",
            engineType = TranscriptionEngineType.CLOUD_FIREBASE_AI,
        ),
        summary = SummaryMetadata(
            executiveSummary = "Overview of marketing tactics and introduction of consulting services.",
            keyPoints = listOf(
                "Discussion of modern multi-channel marketing campaigns.",
                "Consultant background verification.",
            ),
            chapterMarkers = listOf(
                ChapterMarker(
                    startTimeMs = 0L,
                    title = "Introduction & Welcome",
                    keyBullets = listOf("Welcoming attendees"),
                ),
                ChapterMarker(
                    startTimeMs = 18000L,
                    title = "Consultant Credentials",
                    keyBullets = listOf("20 years background"),
                ),
            ),
            generationMode = AiEngineType.CLOUD_FIREBASE_AI,
        ),
    )

    val sampleState = AudioPlaybackState(
        currentPositionMs = 8000L,
        durationMs = 25000L,
        isPlaying = true,
        activeChapter = sampleNote.summary?.chapterMarkers?.firstOrNull(),
    )

    KnockledgeTheme(darkTheme = true) {
        AudioPlaybackScreen(
            note = sampleNote,
            playbackState = sampleState,
            onTogglePlayPause = {},
            onSeekTo = {},
            onSelectChapter = {},
            onBackClick = {},
            onDeleteClick = {},
        )
    }
}

@Preview(name = "Audio Playback Screen - Summary Tab", showBackground = true)
@Composable
fun AudioPlaybackScreenSummaryPreview() {
    val sampleNote = Note(
        id = "preview_2",
        userId = "user_preview",
        title = "FEB 01, 2025 2:46 PM",
        createdAtEpochMs = 1738421160000L,
        updatedAtEpochMs = 1738421160000L,
        audio = AudioMetadata(
            localPath = "/mock/audio.wav",
            durationMs = 30_000L,
            sampleRate = 16000,
            triggerType = TriggerType.MANUAL,
        ),
        summary = SummaryMetadata(
            executiveSummary = "A thorough strategic session focusing on customer acquisition cost reduction and enterprise client onboarding.",
            keyPoints = listOf(
                "Lower customer acquisition costs by 18% in Q2.",
                "Streamline enterprise customer onboarding with automated pipelines.",
                "Schedule weekly synchronization with sales directors.",
            ),
            chapterMarkers = emptyList(),
            generationMode = AiEngineType.LOCAL_ON_DEVICE,
        ),
    )

    val sampleState = AudioPlaybackState(
        currentPositionMs = 12000L,
        durationMs = 30000L,
        isPlaying = false,
    )

    KnockledgeTheme(darkTheme = true) {
        AudioPlaybackScreen(
            note = sampleNote,
            playbackState = sampleState,
            onTogglePlayPause = {},
            onSeekTo = {},
            onSelectChapter = {},
            onBackClick = {},
            onDeleteClick = {},
            initialTab = AudioPlaybackTab.SUMMARY,
        )
    }
}

@Preview(name = "Audio Playback Screen - Unprocessed Note", showBackground = true)
@Composable
fun AudioPlaybackScreenUnprocessedPreview() {
    val sampleNote = Note(
        id = "preview_unprocessed",
        userId = "user_preview",
        title = "FEB 01, 2025 2:46 PM",
        createdAtEpochMs = 1738421160000L,
        updatedAtEpochMs = 1738421160000L,
        audio = AudioMetadata(
            localPath = "/mock/audio.wav",
            durationMs = 45_000L,
            sampleRate = 16000,
            triggerType = TriggerType.MANUAL,
        ),
        transcript = null,
        summary = null,
    )

    val sampleState = AudioPlaybackState(
        currentPositionMs = 0L,
        durationMs = 45000L,
        isPlaying = false,
    )

    KnockledgeTheme(darkTheme = true) {
        AudioPlaybackScreen(
            note = sampleNote,
            playbackState = sampleState,
            onTogglePlayPause = {},
            onSeekTo = {},
            onSelectChapter = {},
            onBackClick = {},
            onDeleteClick = {},
            onProcessWithAi = {},
            initialTab = AudioPlaybackTab.SUMMARY,
        )
    }
}

@Preview(name = "Audio Playback Screen - AI Processing State", showBackground = true)
@Composable
fun AudioPlaybackScreenProcessingPreview() {
    val sampleNote = Note(
        id = "preview_processing",
        userId = "user_preview",
        title = "FEB 01, 2025 2:46 PM",
        createdAtEpochMs = 1738421160000L,
        updatedAtEpochMs = 1738421160000L,
        audio = AudioMetadata(
            localPath = "/mock/audio.wav",
            durationMs = 45_000L,
            sampleRate = 16000,
            triggerType = TriggerType.MANUAL,
        ),
        transcript = null,
        summary = null,
    )

    val sampleState = AudioPlaybackState(
        currentPositionMs = 0L,
        durationMs = 45000L,
        isPlaying = false,
    )

    val processingState = NoteProcessingUiState(
        noteId = "preview_processing",
        isProcessing = true,
        stage = RewindStage.Transcribing,
    )

    KnockledgeTheme(darkTheme = true) {
        AudioPlaybackScreen(
            note = sampleNote,
            playbackState = sampleState,
            onTogglePlayPause = {},
            onSeekTo = {},
            onSelectChapter = {},
            onBackClick = {},
            onDeleteClick = {},
            onProcessWithAi = {},
            processingUiState = processingState,
            initialTab = AudioPlaybackTab.SUMMARY,
        )
    }
}
