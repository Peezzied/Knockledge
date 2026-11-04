package group.two.knockledge.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import group.two.knockledge.R
import group.two.knockledge.ai.AiControls
import group.two.knockledge.core.RewindPolicy
import group.two.knockledge.models.ModelCatalog
import group.two.knockledge.models.ModelFile
import group.two.knockledge.settings.AppSettings
import group.two.knockledge.settings.InputSources
import group.two.knockledge.settings.SummaryLanguages
import group.two.knockledge.settings.TranscriptionLanguages
import group.two.knockledge.sensor.AndroidKnockGestureDetector
import group.two.knockledge.sensor.KnockGestureDetector
import group.two.knockledge.ui.BackArrowNavIcon
import group.two.knockledge.ui.auth.AuthState
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import group.two.knockledge.ui.theme.AccentGold
import group.two.knockledge.ui.theme.AntiqueBrass
import group.two.knockledge.ui.theme.BurnishedBronze
import group.two.knockledge.ui.theme.KnockledgeTheme
import group.two.knockledge.ui.theme.MutedSand
import group.two.knockledge.ui.theme.ObsidianMidnight
import group.two.knockledge.ui.theme.OutlineVariant
import group.two.knockledge.ui.theme.ParchmentCream
import group.two.knockledge.ui.theme.SurfaceContainerHigh
import group.two.knockledge.ui.theme.SurfaceIndigo
import group.two.knockledge.ui.theme.TextInk
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Maximum scale reduction applied to the account card as it is covered. */
private const val ACCOUNT_CARD_MIN_SCALE_DELTA = 0.08f

/** Cached-audio size, in MB, at which the storage bar reads as full. */
private const val STORAGE_BAR_MAX_MB = 512f

/** Minimum bar fill so a non-empty cache still shows a sliver. */
private const val STORAGE_BAR_MIN_FILL = 0.1f

/**
 * Settings screen built according to the Knockledge Figma design.
 * Features M3 container components, obsidian & brass styling, and interactive controls including
 * selecting and downloading AI models for offline processing via separate transcription and summarization bottom sheets.
 *
 * @param settingsUiState Immutable settings UI state.
 * @param authState Current user authentication state.
 * @param autoAiEnabled Whether automatic AI processing is active.
 * @param onAutoAiChanged Callback when automatic AI processing is toggled.
 * @param onBufferDurationChanged Callback when buffer duration is adjusted.
 * @param onKnockThresholdChanged Callback when knock sensitivity is modified.
 * @param onExecutionModeChanged Callback when AI execution mode is changed ("online", "local", "auto").
 * @param onMinAudioSaveChanged Callback when the minimum saved-audio duration is adjusted.
 * @param onInputSourceChanged Callback when the audio input source is changed.
 * @param onTranscriptionLanguageChanged Callback when the speech transcription language is changed.
 * @param onSummaryLanguageChanged Callback when the recap summary language is changed.
 * @param onTranscriptionModelChanged Callback when the selected offline transcription model is changed.
 * @param onRemoteSyncChanged Callback when remote database synchronization is toggled.
 * @param onSyncNow Callback when manual synchronization is requested.
 * @param onSummaryModelChanged Callback when selected offline summary model is changed.
 * @param onDownloadModel Callback when downloading an offline model file.
 * @param onRemoveModel Callback when removing an installed offline model file.
 * @param installedModelIds Set of model IDs currently installed on-device.
 * @param downloadProgress Download progress ratio (0.0 to 1.0) when downloading.
 * @param downloadingModelId ID of model currently downloading, if any.
 * @param onSignOut Callback when user initiates sign-out.
 * @param onNavigateToHome Callback for back navigation, if applicable.
 * @param storageConsumptionMb Cached audio storage consumption in MB.
 * @param modifier Modifier for root container layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsUiState: SettingsUiState,
    authState: AuthState,
    autoAiEnabled: Boolean,
    onAutoAiChanged: (Boolean) -> Unit,
    onBufferDurationChanged: (Int) -> Unit,
    onKnockThresholdChanged: (Float) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    onExecutionModeChanged: (String) -> Unit = {},
    onMinAudioSaveChanged: (Int) -> Unit = {},
    onInputSourceChanged: (String) -> Unit = {},
    onTranscriptionLanguageChanged: (String) -> Unit = {},
    onSummaryLanguageChanged: (String) -> Unit = {},
    onTranscriptionModelChanged: (String) -> Unit = {},
    onRemoteSyncChanged: (Boolean) -> Unit = {},
    onSyncNow: () -> Unit = {},
    onSummaryModelChanged: (String) -> Unit = {},
    onDownloadModel: (ModelFile) -> Unit = {},
    onRemoveModel: (ModelFile) -> Unit = {},
    installedModelIds: Set<String> = emptySet(),
    downloadProgress: Float? = null,
    downloadingModelId: String? = null,
    onNavigateToHome: (() -> Unit)? = null,
    storageConsumptionMb: Float = 0f,
    isSessionRunning: Boolean = false,
    onPurgeSession: () -> Unit = {},
) {
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var accountCardHeightPx by remember { mutableStateOf(0) }

    val density = LocalDensity.current
    val collapseThresholdPx = with(density) { 56.dp.toPx() }
    val collapseProgress = if (collapseThresholdPx > 0f) {
        (scrollState.value.toFloat() / collapseThresholdPx).coerceIn(0f, 1f)
    } else {
        0f
    }

    LaunchedEffect(scrollState.isScrollInProgress) {
        val height = accountCardHeightPx
        val value = scrollState.value
        if (!scrollState.isScrollInProgress && value in 1 until height) {
            scrollState.animateScrollTo(if (value * 2 <= height) 0 else height)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(ObsidianMidnight),
    ) {
        // Ambient Glow Highlights
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(AccentGold.copy(alpha = 0.15f), Color.Transparent),
                    center = Offset(-100f, -100f),
                    radius = 500f,
                ),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(AntiqueBrass.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(size.width + 100f, size.height - 200f),
                    radius = 600f,
                ),
            )
        }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                val progress = FastOutSlowInEasing.transform(collapseProgress)
                val topBarHeight = (96f - 40f * progress).dp
                val horizontalBias = -1f + progress
                val verticalBias = 1f - progress
                val titleStartPadding = (16f * (1f - progress)).dp
                val titleBottomPadding = (12f * (1f - progress)).dp
                val titleScale = 1f - 0.28f * progress

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(topBarHeight)
                        .padding(horizontal = 8.dp),
                ) {
                    // Back button row
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (onNavigateToHome != null) {
                            IconButton(
                                onClick = onNavigateToHome,
                            ) {
                                BackArrowNavIcon(tint = ParchmentCream)
                            }
                        }
                    }

                    // Collapsible Header Title
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineLarge,
                        color = ParchmentCream,
                        modifier = Modifier
                            .align(BiasAlignment(horizontalBias, verticalBias))
                            .padding(start = titleStartPadding, bottom = titleBottomPadding)
                            .graphicsLayer {
                                scaleX = titleScale
                                scaleY = titleScale
                                transformOrigin = TransformOrigin(
                                    pivotFractionX = 0.5f * progress,
                                    pivotFractionY = 0.5f,
                                )
                            },
                    )
                }
            },
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                ) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = SurfaceContainerHigh,
                        contentColor = ParchmentCream,
                        actionColor = AccentGold,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.border(1.dp, AntiqueBrass, RoundedCornerShape(4.dp)),
                    )
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState),
            ) {
                // Cards Stack
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // 1. Active Account & Sign Out Hero Card
                    Box(
                        modifier = Modifier
                            .onSizeChanged { accountCardHeightPx = it.height }
                            .graphicsLayer {
                                // Pin the card so later cards slide over it, fading and shrinking it as it is covered.
                                val progress = if (accountCardHeightPx > 0) {
                                    (scrollState.value.toFloat() / accountCardHeightPx).coerceIn(0f, 1f)
                                } else {
                                    0f
                                }
                                translationY = scrollState.value.toFloat()
                                alpha = 1f - progress
                                scaleX = 1f - progress * ACCOUNT_CARD_MIN_SCALE_DELTA
                                scaleY = scaleX
                            },
                    ) {
                        ActiveAccountCard(
                            authState = authState,
                            remoteSyncEnabled = settingsUiState.remoteSyncEnabled,
                            onRemoteSyncChanged = onRemoteSyncChanged,
                            onSyncNow = onSyncNow,
                            onSignOut = onSignOut,
                        )
                    }
    
                    // 2. Buffer Duration Card
                    BufferDurationCard(
                        bufferDurationSeconds = settingsUiState.bufferDurationSeconds,
                        onBufferDurationChanged = onBufferDurationChanged,
                    )

                    // 2b. Audio Card (minimum save duration + input source)
                    AudioCard(
                        minAudioSaveSeconds = settingsUiState.minAudioSaveSeconds,
                        inputSource = settingsUiState.inputSource,
                        onMinAudioSaveChanged = onMinAudioSaveChanged,
                        onInputSourceChanged = onInputSourceChanged,
                    )
    
                    // 3. Storage Consumption Card
                    StorageConsumptionCard(
                        storageConsumptionMb = storageConsumptionMb,
                    )
    
                    // 4. Automatic AI Processing Card
                    AutomaticAiCard(
                        autoAiEnabled = autoAiEnabled,
                        onAutoAiChanged = onAutoAiChanged,
                    )
    
                    // 5. AI Execution Mode Card with split transcription & summarization bottom sheets
                    AiExecutionModeCard(
                        currentMode = settingsUiState.transcriptionMode,
                        transcriptionModel = settingsUiState.transcriptionLocalModel,
                        summaryModel = settingsUiState.summaryModel,
                        onExecutionModeChanged = onExecutionModeChanged,
                        onTranscriptionModelChanged = onTranscriptionModelChanged,
                        onSummaryModelChanged = onSummaryModelChanged,
                        onDownloadModel = onDownloadModel,
                        onRemoveModel = onRemoveModel,
                        installedModelIds = installedModelIds,
                        downloadProgress = downloadProgress,
                        downloadingModelId = downloadingModelId,
                    )

                    // 5b. Language Card (transcription + summary languages)
                    LanguageCard(
                        transcriptionLanguage = settingsUiState.transcriptionLanguage,
                        summaryLanguage = settingsUiState.summaryLanguage,
                        onTranscriptionLanguageChanged = onTranscriptionLanguageChanged,
                        onSummaryLanguageChanged = onSummaryLanguageChanged,
                    )

                    // 6. Gesture Sensitivity Card
                    GestureSensitivityCard(
                        knockSensorThreshold = settingsUiState.knockSensorThreshold,
                        onKnockThresholdChanged = onKnockThresholdChanged,
                    )
    
                    // 7. Gesture Test Card
                    GestureTestCard(
                        isSessionRunning = isSessionRunning,
                        onPurgeSession = onPurgeSession,
                        onShowSnackbar = { msg ->
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(msg)
                            }
                        },
                    )
                }
            }
        }
    }
}

/**
 * Card displaying and adjusting circular buffer window duration.
 */
@Composable
private fun BufferDurationCard(
    bufferDurationSeconds: Int,
    onBufferDurationChanged: (Int) -> Unit,
) {
    SettingsCardContainer {
        Text(
            text = stringResource(R.string.settings_buffer_duration_header),
            style = MaterialTheme.typography.labelMedium,
            color = AccentGold,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val minutes = bufferDurationSeconds / 60
            val seconds = bufferDurationSeconds % 60
            val text = if (seconds == 0) {
                if (minutes == 1) {
                    stringResource(R.string.settings_buffer_minute_kept, minutes)
                } else {
                    stringResource(R.string.settings_buffer_minutes_kept, minutes)
                }
            } else {
                if (minutes > 0) {
                    "$minutes min $seconds sec kept"
                } else {
                    "$seconds sec kept"
                }
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = ParchmentCream,
            )

            val detailedText = if (seconds == 0) {
                "${minutes}m 00s"
            } else {
                "${minutes}m ${seconds.toString().padStart(2, '0')}s"
            }
            Text(
                text = detailedText,
                style = MaterialTheme.typography.bodyMedium,
                color = MutedSand,
            )
        }

        Slider(
            value = bufferDurationSeconds.toFloat(),
            onValueChange = { onBufferDurationChanged(it.toInt()) },
            valueRange = RewindPolicy.MIN_WINDOW_SECONDS.toFloat()..RewindPolicy.MAX_WINDOW_SECONDS.toFloat(),
            steps = (RewindPolicy.MAX_WINDOW_SECONDS - RewindPolicy.MIN_WINDOW_SECONDS) / RewindPolicy.CHUNK_SECONDS - 1,
            colors = SliderDefaults.colors(
                thumbColor = AccentGold,
                activeTrackColor = AccentGold,
                inactiveTrackColor = BurnishedBronze,
            ),
        )
    }
}

/**
 * Card for audio capture settings: the minimum duration saved on a knock and the input source.
 */
@Composable
private fun AudioCard(
    minAudioSaveSeconds: Int,
    inputSource: String,
    onMinAudioSaveChanged: (Int) -> Unit,
    onInputSourceChanged: (String) -> Unit,
) {
    SettingsCardContainer {
        Text(
            text = stringResource(R.string.settings_audio_header),
            style = MaterialTheme.typography.labelMedium,
            color = AccentGold,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.settings_audio_min_save_label),
                style = MaterialTheme.typography.labelLarge,
                color = ParchmentCream,
            )
            Text(
                text = stringResource(R.string.settings_audio_min_save_value, minAudioSaveSeconds),
                style = MaterialTheme.typography.bodyMedium,
                color = MutedSand,
            )
        }

        Slider(
            value = minAudioSaveSeconds.toFloat(),
            onValueChange = { onMinAudioSaveChanged(it.toInt()) },
            valueRange = AppSettings.MIN_AUDIO_SAVE_MIN.toFloat()..AppSettings.MIN_AUDIO_SAVE_MAX.toFloat(),
            steps = AppSettings.MIN_AUDIO_SAVE_MAX - AppSettings.MIN_AUDIO_SAVE_MIN - 1,
            colors = SliderDefaults.colors(
                thumbColor = AccentGold,
                activeTrackColor = AccentGold,
                inactiveTrackColor = BurnishedBronze,
            ),
        )

        Text(
            text = stringResource(R.string.settings_audio_input_label),
            style = MaterialTheme.typography.labelLarge,
            color = ParchmentCream,
        )

        val inputIds = listOf(InputSources.MIC, InputSources.MIC_AND_DEVICE)
        SegmentedButtonGroup(
            options = listOf(
                stringResource(R.string.settings_input_mic),
                stringResource(R.string.settings_input_mic_device),
            ),
            selectedIndex = inputIds.indexOf(inputSource).coerceAtLeast(0),
            onOptionSelected = { index -> onInputSourceChanged(inputIds[index]) },
        )
    }
}

/**
 * Card for language settings: the spoken language Whisper transcribes and the recap language.
 */
@Composable
private fun LanguageCard(
    transcriptionLanguage: String,
    summaryLanguage: String,
    onTranscriptionLanguageChanged: (String) -> Unit,
    onSummaryLanguageChanged: (String) -> Unit,
) {
    SettingsCardContainer {
        Text(
            text = stringResource(R.string.settings_language_header),
            style = MaterialTheme.typography.labelMedium,
            color = AccentGold,
        )

        Text(
            text = stringResource(R.string.settings_language_transcription_label),
            style = MaterialTheme.typography.labelLarge,
            color = ParchmentCream,
        )

        val transcriptionIds = listOf(
            TranscriptionLanguages.FILIPINO,
            TranscriptionLanguages.ENGLISH,
            TranscriptionLanguages.AUTO,
        )
        SegmentedButtonGroup(
            options = listOf(
                stringResource(R.string.settings_lang_filipino),
                stringResource(R.string.settings_lang_english),
                stringResource(R.string.settings_lang_auto_detect),
            ),
            selectedIndex = transcriptionIds.indexOf(transcriptionLanguage).coerceAtLeast(0),
            onOptionSelected = { index -> onTranscriptionLanguageChanged(transcriptionIds[index]) },
        )

        Text(
            text = stringResource(R.string.settings_language_summary_label),
            style = MaterialTheme.typography.labelLarge,
            color = ParchmentCream,
        )

        val summaryIds = listOf(
            SummaryLanguages.AUTO,
            SummaryLanguages.FILIPINO,
            SummaryLanguages.ENGLISH,
        )
        SegmentedButtonGroup(
            options = listOf(
                stringResource(R.string.settings_lang_match_speakers),
                stringResource(R.string.settings_lang_filipino),
                stringResource(R.string.settings_lang_english),
            ),
            selectedIndex = summaryIds.indexOf(summaryLanguage).coerceAtLeast(0),
            onOptionSelected = { index -> onSummaryLanguageChanged(summaryIds[index]) },
        )
    }
}

/**
 * Card displaying audio storage consumption statistics.
 */
@Composable
private fun StorageConsumptionCard(
    storageConsumptionMb: Float,
) {
    SettingsCardContainer {
        Text(
            text = stringResource(R.string.settings_storage_header),
            style = MaterialTheme.typography.labelMedium,
            color = AccentGold,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${storageConsumptionMb.toInt()} MB",
                style = MaterialTheme.typography.titleMedium,
                color = ParchmentCream,
            )

            Text(
                text = stringResource(R.string.settings_storage_cached_audio),
                style = MaterialTheme.typography.bodyMedium,
                color = MutedSand,
            )
        }

        LinearProgressIndicator(
            progress = { (storageConsumptionMb / STORAGE_BAR_MAX_MB).coerceIn(STORAGE_BAR_MIN_FILL, 1.0f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = AccentGold,
            trackColor = BurnishedBronze,
            strokeCap = StrokeCap.Round,
        )
    }
}

/**
 * Card for toggling automatic background AI processing.
 */
@Composable
private fun AutomaticAiCard(
    autoAiEnabled: Boolean,
    onAutoAiChanged: (Boolean) -> Unit,
) {
    SettingsCardContainer {
        Text(
            text = stringResource(R.string.settings_ai_header),
            style = MaterialTheme.typography.labelMedium,
            color = AccentGold,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.settings_ai_toggle_label),
                style = MaterialTheme.typography.labelLarge,
                color = ParchmentCream,
            )

            Switch(
                checked = autoAiEnabled,
                onCheckedChange = onAutoAiChanged,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = TextInk,
                    checkedTrackColor = AccentGold,
                    uncheckedThumbColor = MutedSand,
                    uncheckedTrackColor = SurfaceContainerHigh,
                    uncheckedBorderColor = OutlineVariant,
                ),
            )
        }

        Text(
            text = stringResource(R.string.settings_ai_toggle_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MutedSand,
        )
    }
}

/**
 * Card for selecting AI execution mode (Auto, Cloud or Offline) with two distinct menu items:
 * one for offline transcription model selection (Whisper) and one for offline summarization model selection (Qwen).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiExecutionModeCard(
    currentMode: String,
    transcriptionModel: String,
    summaryModel: String,
    onExecutionModeChanged: (String) -> Unit,
    onTranscriptionModelChanged: (String) -> Unit,
    onSummaryModelChanged: (String) -> Unit,
    onDownloadModel: (ModelFile) -> Unit,
    onRemoveModel: (ModelFile) -> Unit,
    installedModelIds: Set<String>,
    downloadProgress: Float?,
    downloadingModelId: String?,
) {
    var showTranscriptionSheet by remember { mutableStateOf(false) }
    var showSummarySheet by remember { mutableStateOf(false) }

    SettingsCardContainer {
        Text(
            text = stringResource(R.string.settings_execution_header),
            style = MaterialTheme.typography.labelMedium,
            color = AccentGold,
        )

        val modes = listOf(AppSettings.MODE_AUTO, AppSettings.MODE_ONLINE, AppSettings.MODE_LOCAL)
        SegmentedButtonGroup(
            options = listOf(
                stringResource(R.string.settings_execution_auto),
                stringResource(R.string.settings_execution_cloud),
                stringResource(R.string.settings_execution_offline),
            ),
            selectedIndex = modes.indexOf(currentMode).coerceAtLeast(0),
            onOptionSelected = { index -> onExecutionModeChanged(modes[index]) },
        )

        val usesOfflineModels = currentMode == AppSettings.MODE_LOCAL || currentMode == AppSettings.MODE_AUTO

        AnimatedVisibility(
            visible = usesOfflineModels,
            enter = fadeIn(animationSpec = tween(250)) + expandVertically(animationSpec = tween(250)),
            exit = fadeOut(animationSpec = tween(200)) + shrinkVertically(animationSpec = tween(200)),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = OutlineVariant,
                )

                // Menu Item 1: Offline Transcription Model (Whisper)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { showTranscriptionSheet = true }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_transcription_models_menu_title),
                            style = MaterialTheme.typography.labelLarge,
                            color = ParchmentCream,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.settings_transcription_models_menu_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedSand,
                        )
                    }

                    CanvasChevronRightIcon(
                        tint = AccentGold,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 2.dp),
                    color = OutlineVariant.copy(alpha = 0.5f),
                )

                // Menu Item 2: Offline Summarization Models (Qwen 2B / 0.8B)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { showSummarySheet = true }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_summary_models_menu_title),
                            style = MaterialTheme.typography.labelLarge,
                            color = ParchmentCream,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.settings_summary_models_menu_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedSand,
                        )
                    }

                    CanvasChevronRightIcon(
                        tint = AccentGold,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }

    // Modal Bottom Sheet 1: Offline Transcription Model (Whisper)
    if (showTranscriptionSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { showTranscriptionSheet = false },
            sheetState = sheetState,
            containerColor = SurfaceIndigo,
            contentColor = ParchmentCream,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(AccentGold.copy(alpha = 0.5f)),
                )
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.settings_transcription_models_sheet_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = ParchmentCream,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.settings_transcription_models_sheet_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSand,
                    )
                }

                HorizontalDivider(color = OutlineVariant)

                ModelCatalog.whisperModels.forEach { file ->
                    val isInstalled = installedModelIds.contains(file.id)
                    val isDownloading = downloadingModelId == file.id

                    OfflineModelItem(
                        modelFile = file,
                        description = stringResource(R.string.settings_transcription_model_desc),
                        isInstalled = isInstalled,
                        isDownloading = isDownloading,
                        downloadProgress = if (isDownloading) downloadProgress else null,
                        isSelected = transcriptionModel == file.id,
                        selectable = true,
                        onSelect = { onTranscriptionModelChanged(file.id) },
                        onDownload = { onDownloadModel(file) },
                        onRemove = { onRemoveModel(file) },
                    )
                }

                Text(
                    text = stringResource(R.string.settings_transcription_models_filipino_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSand,
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { showTranscriptionSheet = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, AntiqueBrass),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = ObsidianMidnight,
                        contentColor = ParchmentCream,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.settings_sheet_done),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }

    // Modal Bottom Sheet 2: Offline Summarization Models (Qwen 2B / 0.8B)
    if (showSummarySheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { showSummarySheet = false },
            sheetState = sheetState,
            containerColor = SurfaceIndigo,
            contentColor = ParchmentCream,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(AccentGold.copy(alpha = 0.5f)),
                )
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.settings_summary_models_sheet_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = ParchmentCream,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.settings_summary_models_sheet_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSand,
                    )
                }

                HorizontalDivider(color = OutlineVariant)

                val summaryModelList = listOf(
                    ModelCatalog.qwen2b to stringResource(R.string.settings_summary_qwen2b_desc),
                    ModelCatalog.qwen08b to stringResource(R.string.settings_summary_qwen08b_desc),
                )

                summaryModelList.forEach { (file, description) ->
                    val isInstalled = installedModelIds.contains(file.id)
                    val isDownloading = downloadingModelId == file.id
                    val isSelectedSummary = summaryModel == file.id

                    OfflineModelItem(
                        modelFile = file,
                        description = description,
                        isInstalled = isInstalled,
                        isDownloading = isDownloading,
                        downloadProgress = if (isDownloading) downloadProgress else null,
                        isSelected = isSelectedSummary,
                        selectable = true,
                        onSelect = { onSummaryModelChanged(file.id) },
                        onDownload = { onDownloadModel(file) },
                        onRemove = { onRemoveModel(file) },
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { showSummarySheet = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, AntiqueBrass),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = ObsidianMidnight,
                        contentColor = ParchmentCream,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.settings_sheet_done),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

/**
 * Item component for an individual offline AI model with selection, download, and status indicators.
 */
@Composable
private fun OfflineModelItem(
    modelFile: ModelFile,
    description: String,
    isInstalled: Boolean,
    isDownloading: Boolean,
    downloadProgress: Float?,
    isSelected: Boolean,
    selectable: Boolean,
    onSelect: () -> Unit,
    onDownload: () -> Unit,
    onRemove: () -> Unit,
) {
    val sizeText = AiControls.gigabytes(modelFile.approxBytes)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .border(
                width = 1.dp,
                color = if (isSelected) AntiqueBrass else OutlineVariant,
                shape = RoundedCornerShape(4.dp),
            )
            .clickable(enabled = selectable, onClick = onSelect),
        color = if (isSelected) SurfaceContainerHigh else ObsidianMidnight,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    if (selectable) {
                        RadioButton(
                            selected = isSelected,
                            onClick = onSelect,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = AccentGold,
                                unselectedColor = MutedSand,
                            ),
                        )
                    }

                    Column {
                        Text(
                            text = modelFile.label,
                            style = MaterialTheme.typography.titleSmall,
                            color = ParchmentCream,
                        )
                        Text(
                            text = "$description · $sizeText",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedSand,
                        )
                    }
                }

                // Status Badge or Download/Remove Button
                if (isDownloading) {
                    val percent = ((downloadProgress ?: 0f) * 100).toInt()
                    Text(
                        text = stringResource(R.string.settings_model_downloading_status, percent),
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentGold,
                    )
                } else if (isInstalled) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Surface(
                            color = AntiqueBrass.copy(alpha = 0.2f),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, AntiqueBrass),
                        ) {
                            Text(
                                text = if (isSelected) {
                                    stringResource(R.string.settings_model_active_badge)
                                } else {
                                    stringResource(R.string.settings_model_installed_badge)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentGold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        IconButton(
                            onClick = onRemove,
                            modifier = Modifier.size(28.dp),
                        ) {
                            CanvasTrashIcon(tint = MutedSand)
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = onDownload,
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, AntiqueBrass),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ParchmentCream,
                        ),
                    ) {
                        Text(
                            text = stringResource(R.string.settings_download_model_btn, sizeText),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                }
            }

            if (isDownloading) {
                LinearProgressIndicator(
                    progress = { downloadProgress ?: 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp)),
                    color = AccentGold,
                    trackColor = BurnishedBronze,
                )
            }
        }
    }
}

/**
 * Card for selecting gesture accelerometer sensitivity threshold.
 */
@Composable
private fun GestureSensitivityCard(
    knockSensorThreshold: Float,
    onKnockThresholdChanged: (Float) -> Unit,
) {
    SettingsCardContainer {
        Text(
            text = stringResource(R.string.settings_sensitivity_header),
            style = MaterialTheme.typography.labelMedium,
            color = AccentGold,
        )

        val isLow = knockSensorThreshold >= 5.0f
        val isHigh = knockSensorThreshold <= 2.5f
        val selectedIndex = when {
            isLow -> 0
            isHigh -> 2
            else -> 1
        }

        SegmentedButtonGroup(
            options = listOf(
                stringResource(R.string.settings_sensitivity_low),
                stringResource(R.string.settings_sensitivity_medium),
                stringResource(R.string.settings_sensitivity_high),
            ),
            selectedIndex = selectedIndex,
            onOptionSelected = { index ->
                when (index) {
                    0 -> onKnockThresholdChanged(6.0f)
                    1 -> onKnockThresholdChanged(4.0f)
                    2 -> onKnockThresholdChanged(2.0f)
                }
            },
        )
    }
}

/**
 * Interactive gesture testing card supporting physical accelerometer detection and simulated double knock taps.
 */
@Composable
private fun GestureTestCard(
    isSessionRunning: Boolean,
    onPurgeSession: () -> Unit,
    onShowSnackbar: (String) -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isGestureTestActive by remember { mutableStateOf(false) }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var isTriggered by remember { mutableStateOf(false) }

    val knockDetector: KnockGestureDetector = remember {
        val app = context.applicationContext as? Application
        AndroidKnockGestureDetector(app)
    }

    LaunchedEffect(isTriggered) {
        if (isTriggered) {
            delay(1500)
            isTriggered = false
        }
    }

    val knockSnackbarMsg = stringResource(R.string.settings_gesture_test_snackbar_knock)
    val handleKnock: () -> Unit = remember(knockSnackbarMsg, onShowSnackbar) {
        {
            isTriggered = true
            onShowSnackbar(knockSnackbarMsg)
        }
    }

    DisposableEffect(isGestureTestActive) {
        if (isGestureTestActive) {
            knockDetector.startListening {
                coroutineScope.launch {
                    handleKnock()
                }
            }
        } else {
            knockDetector.stopListening()
        }
        onDispose {
            knockDetector.stopListening()
        }
    }

    SettingsCardContainer {
        Text(
            text = stringResource(R.string.settings_gesture_test_header),
            style = MaterialTheme.typography.labelMedium,
            color = AccentGold,
        )

        // Activation Switch Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (isGestureTestActive) {
                        isGestureTestActive = false
                    } else {
                        showConfirmationDialog = true
                    }
                },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_gesture_test_activate_switch),
                    style = MaterialTheme.typography.titleMedium,
                    color = ParchmentCream,
                )
                Text(
                    text = stringResource(R.string.settings_gesture_test_activate_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSand,
                )
            }
            Switch(
                checked = isGestureTestActive,
                onCheckedChange = { checked ->
                    if (checked) {
                        showConfirmationDialog = true
                    } else {
                        isGestureTestActive = false
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ObsidianMidnight,
                    checkedTrackColor = AccentGold,
                    uncheckedThumbColor = MutedSand,
                    uncheckedTrackColor = SurfaceIndigo,
                ),
            )
        }

        // Gesture Test Surface Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (isGestureTestActive) ObsidianMidnight else SurfaceIndigo)
                .border(
                    1.dp,
                    if (isGestureTestActive) AntiqueBrass else OutlineVariant,
                    RoundedCornerShape(4.dp),
                )
                .clickable {
                    if (isGestureTestActive) {
                        handleKnock()
                    } else {
                        showConfirmationDialog = true
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (isGestureTestActive) {
                    stringResource(R.string.settings_gesture_test_prompt)
                } else {
                    stringResource(R.string.settings_gesture_test_inactive_prompt)
                },
                style = MaterialTheme.typography.labelLarge,
                color = if (isGestureTestActive) ParchmentCream else MutedSand,
            )
        }

        // Gesture Feedback Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = when {
                    !isGestureTestActive -> stringResource(R.string.settings_gesture_test_inactive_prompt)
                    isTriggered -> stringResource(R.string.settings_gesture_test_detected)
                    else -> stringResource(R.string.settings_gesture_test_awaiting)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MutedSand,
            )

            val pillBg by animateColorAsState(
                targetValue = when {
                    !isGestureTestActive -> BurnishedBronze
                    isTriggered -> AccentGold
                    else -> AntiqueBrass
                },
                animationSpec = tween(200),
                label = "PillBg",
            )

            Surface(
                color = pillBg,
                shape = CircleShape,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = when {
                        !isGestureTestActive -> stringResource(R.string.settings_gesture_test_inactive_status)
                        isTriggered -> stringResource(R.string.settings_gesture_test_triggered)
                        else -> stringResource(R.string.settings_gesture_test_active_status)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = TextInk,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }

    // Confirmation Prompt Dialog
    if (showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmationDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.settings_gesture_test_confirm_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = AccentGold,
                )
            },
            text = {
                Text(
                    text = if (isSessionRunning) {
                        stringResource(R.string.settings_gesture_test_confirm_message)
                    } else {
                        stringResource(R.string.settings_gesture_test_activate_desc)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = ParchmentCream,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onPurgeSession()
                        isGestureTestActive = true
                        showConfirmationDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentGold,
                        contentColor = TextInk,
                    ),
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.settings_gesture_test_confirm_action),
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showConfirmationDialog = false },
                    border = BorderStroke(1.dp, AntiqueBrass),
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.settings_gesture_test_cancel_action),
                        color = ParchmentCream,
                    )
                }
            },
            containerColor = ObsidianMidnight,
            shape = RoundedCornerShape(4.dp),
        )
    }
}

/**
 * Calculates Gravatar image URL from an email address using MD5 hashing.
 */
private fun gravatarUrl(email: String, size: Int = 128): String {
    val cleanEmail = email.trim().lowercase()
    val md5 = MessageDigest.getInstance("MD5")
        .digest(cleanEmail.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
    return "https://www.gravatar.com/avatar/$md5?s=$size&d=identicon"
}

/**
 * Asynchronously loads a network image bitmap off the main thread using platform APIs.
 */
@Composable
private fun rememberNetworkImage(url: String?): ImageBitmap? {
    var bitmapState by remember(url) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(url) {
        if (url.isNullOrBlank()) {
            bitmapState = null
            return@LaunchedEffect
        }
        withContext(Dispatchers.IO) {
            try {
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 3000
                    readTimeout = 3000
                    doInput = true
                    instanceFollowRedirects = true
                }
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    connection.inputStream.use { stream ->
                        val decoded = BitmapFactory.decodeStream(stream)
                        bitmapState = decoded?.asImageBitmap()
                    }
                }
            } catch (_: Exception) {
                bitmapState = null
            }
        }
    }

    return bitmapState
}

/**
 * Hero card displaying active user account details, remote database sync toggle, manual sync action, and sign out action.
 * Styled with a distinct deep burnished brass container (PrimaryContainer) and glowing gold accents to distinguish the profile section.
 */
@Composable
private fun ActiveAccountCard(
    authState: AuthState,
    remoteSyncEnabled: Boolean,
    onRemoteSyncChanged: (Boolean) -> Unit,
    onSyncNow: () -> Unit = {},
    onSignOut: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = ParchmentCream,
        ),
        border = BorderStroke(1.5.dp, AccentGold),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_account_header),
                style = MaterialTheme.typography.labelMedium,
                color = AccentGold,
                fontWeight = FontWeight.Bold,
            )

            val user = authState.user
            val name = user?.displayName?.ifBlank { "Jordan Doe" } ?: "Jordan Doe"
            val email = user?.email?.ifBlank { "jordan@example.com" } ?: "jordan@example.com"
            val photoUrl = user?.photoUrl?.toString()?.ifBlank { null } ?: gravatarUrl(email)
            val avatarBitmap = rememberNetworkImage(photoUrl)

            val initials = name.split(" ")
                .mapNotNull { it.firstOrNull() }
                .take(2)
                .joinToString("")
                .ifBlank { "JD" }

            // User Avatar Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(AccentGold)
                        .border(1.dp, ParchmentCream, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (avatarBitmap != null) {
                        Image(
                            bitmap = avatarBitmap,
                            contentDescription = stringResource(R.string.settings_account_header),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                        )
                    } else {
                        Text(
                            text = initials.uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextInk,
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        color = ParchmentCream,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedSand,
                    )
                }
            }

            HorizontalDivider(
                color = AccentGold.copy(alpha = 0.35f),
                thickness = 1.dp,
            )

            // Database Sync Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.settings_sync_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = ParchmentCream,
                )

                Switch(
                    checked = remoteSyncEnabled,
                    onCheckedChange = onRemoteSyncChanged,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TextInk,
                        checkedTrackColor = AccentGold,
                        uncheckedThumbColor = MutedSand,
                        uncheckedTrackColor = ObsidianMidnight,
                        uncheckedBorderColor = AccentGold.copy(alpha = 0.5f),
                    ),
                )
            }

            // Sync Now Action Button
            Button(
                onClick = onSyncNow,
                enabled = remoteSyncEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGold,
                    contentColor = TextInk,
                    disabledContainerColor = BurnishedBronze.copy(alpha = 0.4f),
                    disabledContentColor = MutedSand.copy(alpha = 0.6f),
                ),
            ) {
                Text(
                    text = stringResource(R.string.settings_sync_now_btn),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Sign Out Action Button
            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, AccentGold),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = SurfaceIndigo,
                    contentColor = AccentGold,
                ),
            ) {
                Text(
                    text = stringResource(R.string.sign_out),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

/**
 * Reusable container wrapper for settings card modules.
 */
@Composable
private fun SettingsCardContainer(
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceIndigo,
        ),
        border = BorderStroke(1.dp, OutlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

/**
 * Segmented button group for option selection in Settings.
 *
 * @param options List of option label strings.
 * @param selectedIndex Index of the currently selected option.
 * @param onOptionSelected Callback invoked when an option at index is selected.
 * @param modifier Modifier applied to the button group row.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SegmentedButtonGroup(
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Longer labels get wider segments so words such as "Auto-detect" do not break mid-word.
    val segmentWeights = options.map { it.length.coerceAtLeast(6).toFloat() }
    // 12.dp horizontal padding leaves too little room for the longest labels; keep vertical as is.
    val contentPadding = PaddingValues(
        horizontal = 6.dp,
        vertical = SegmentedButtonDefaults.ContentPadding.calculateTopPadding(),
    )

    SingleChoiceSegmentedButtonRow(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        options.forEachIndexed { index, option ->
            val selected = selectedIndex == index
            // SegmentedButton always reserves an 18.dp icon + 8.dp gap, so a plain Surface is used
            // to keep the label centered without that dead space.
            Surface(
                selected = selected,
                onClick = { onOptionSelected(index) },
                modifier = Modifier
                    .weight(segmentWeights[index])
                    .fillMaxHeight()
                    .semantics { role = Role.RadioButton },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                color = if (selected) AntiqueBrass else Color.Transparent,
                contentColor = if (selected) TextInk else ParchmentCream,
                border = SegmentedButtonDefaults.borderStroke(
                    color = if (selected) AntiqueBrass else OutlineVariant,
                ),
            ) {
                Box(
                    modifier = Modifier.padding(contentPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun CanvasChevronRightIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(16.dp)) {
        val strokeWidth = 2.dp.toPx()
        val w = size.width
        val h = size.height

        val path = Path().apply {
            moveTo(w * 0.35f, h * 0.2f)
            lineTo(w * 0.65f, h * 0.5f)
            lineTo(w * 0.35f, h * 0.8f)
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

@Composable
private fun CanvasTrashIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(16.dp)) {
        val strokeWidth = 1.5.dp.toPx()
        val w = size.width
        val h = size.height

        drawLine(
            color = tint,
            start = Offset(w * 0.15f, h * 0.25f),
            end = Offset(w * 0.85f, h * 0.25f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.35f, h * 0.15f),
            end = Offset(w * 0.65f, h * 0.15f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        val path = Path().apply {
            moveTo(w * 0.25f, h * 0.25f)
            lineTo(w * 0.3f, h * 0.85f)
            lineTo(w * 0.7f, h * 0.85f)
            lineTo(w * 0.75f, h * 0.25f)
        }
        drawPath(path, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    KnockledgeTheme {
        SettingsScreen(
            settingsUiState = SettingsUiState(
                bufferDurationSeconds = 300,
                minAudioSaveSeconds = 15,
                inputSource = InputSources.MIC_AND_DEVICE,
                transcriptionMode = AppSettings.MODE_LOCAL,
                transcriptionLocalModel = ModelCatalog.whisper.id,
                transcriptionLanguage = TranscriptionLanguages.FILIPINO,
                summaryLanguage = SummaryLanguages.AUTO,
                summaryModel = "qwen2b",
            ),
            authState = AuthState(),
            autoAiEnabled = true,
            onAutoAiChanged = {},
            onBufferDurationChanged = {},
            onKnockThresholdChanged = {},
            onSignOut = {},
            onNavigateToHome = {},
        )
    }
}
