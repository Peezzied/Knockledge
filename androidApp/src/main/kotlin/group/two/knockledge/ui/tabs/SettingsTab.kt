package group.two.knockledge.ui.tabs

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import group.two.knockledge.R
import group.two.knockledge.core.RewindPolicy
import group.two.knockledge.ui.BackArrowNavIcon
import group.two.knockledge.ui.auth.AuthState
import group.two.knockledge.ui.settings.SettingsUiState
import group.two.knockledge.ui.theme.AccentGold
import group.two.knockledge.ui.theme.AntiqueBrass
import group.two.knockledge.ui.theme.BurnishedBronze
import group.two.knockledge.ui.theme.KiwiSoda
import group.two.knockledge.ui.theme.KnockledgeTheme
import group.two.knockledge.ui.theme.MutedSand
import group.two.knockledge.ui.theme.ObsidianMidnight
import group.two.knockledge.ui.theme.OutlineVariant
import group.two.knockledge.ui.theme.ParchmentCream
import group.two.knockledge.ui.theme.SurfaceContainerHigh
import group.two.knockledge.ui.theme.SurfaceIndigo
import group.two.knockledge.ui.theme.TextInk
import group.two.knockledge.ui.theme.VcrOsdMono
import kotlinx.coroutines.delay

/**
 * Settings tab and screen built according to the Knockledge Figma design.
 * Features M3 container components, obsidian & brass styling, and interactive controls.
 *
 * @param settingsUiState Immutable settings UI state.
 * @param authState Current user authentication state.
 * @param autoAiEnabled Whether automatic AI processing is active.
 * @param onAutoAiChanged Callback when automatic AI processing is toggled.
 * @param onBufferDurationChanged Callback when buffer duration is adjusted.
 * @param onKnockThresholdChanged Callback when knock sensitivity is modified.
 * @param onExecutionModeChanged Callback when AI execution mode is changed ("online", "local", "auto").
 * @param onRemoteSyncChanged Callback when remote database synchronization is toggled.
 * @param onSignOut Callback when user initiates sign-out.
 * @param onNavigateToHome Callback for back navigation, if applicable.
 * @param storageConsumptionMb Cached audio storage consumption in MB.
 * @param modifier Modifier for root container layout.
 */
@Composable
fun SettingsTab(
    settingsUiState: SettingsUiState,
    authState: AuthState,
    autoAiEnabled: Boolean,
    onAutoAiChanged: (Boolean) -> Unit,
    onBufferDurationChanged: (Int) -> Unit,
    onKnockThresholdChanged: (Float) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    onExecutionModeChanged: (String) -> Unit = {},
    onRemoteSyncChanged: (Boolean) -> Unit = {},
    onNavigateToHome: (() -> Unit)? = null,
    storageConsumptionMb: Float = 128f,
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (onNavigateToHome != null) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(SurfaceIndigo)
                            .border(1.dp, OutlineVariant, RoundedCornerShape(4.dp))
                            .clickable(onClick = onNavigateToHome),
                        contentAlignment = Alignment.Center,
                    ) {
                        BackArrowNavIcon(tint = ParchmentCream)
                    }
                }

                Text(
                    text = stringResource(R.string.settings_tab_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = ParchmentCream,
                    fontSize = 32.sp,
                )
            }

            // Cards Stack
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 1. Buffer Duration Card
                BufferDurationCard(
                    bufferDurationSeconds = settingsUiState.bufferDurationSeconds,
                    onBufferDurationChanged = onBufferDurationChanged,
                )

                // 2. Storage Consumption Card
                StorageConsumptionCard(
                    storageConsumptionMb = storageConsumptionMb,
                )

                // 3. Automatic AI Processing Card
                AutomaticAiCard(
                    autoAiEnabled = autoAiEnabled,
                    onAutoAiChanged = onAutoAiChanged,
                )

                // 4. AI Execution Mode Card
                AiExecutionModeCard(
                    currentMode = settingsUiState.transcriptionMode,
                    onExecutionModeChanged = onExecutionModeChanged,
                )

                // 5. Gesture Sensitivity Card
                GestureSensitivityCard(
                    knockSensorThreshold = settingsUiState.knockSensorThreshold,
                    onKnockThresholdChanged = onKnockThresholdChanged,
                )

                // 6. Gesture Test Card
                GestureTestCard()

                // 7. Active Account Card
                ActiveAccountCard(
                    authState = authState,
                    remoteSyncEnabled = settingsUiState.remoteSyncEnabled,
                    onRemoteSyncChanged = onRemoteSyncChanged,
                )

                // 8. Sign Out Button
                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, AntiqueBrass),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = SurfaceIndigo,
                        contentColor = ParchmentCream,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.sign_out),
                        fontFamily = KiwiSoda,
                        fontSize = 16.sp,
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
            val text = if (minutes == 1) {
                stringResource(R.string.settings_buffer_minute_kept, minutes)
            } else {
                stringResource(R.string.settings_buffer_minutes_kept, minutes)
            }
            Text(
                text = text,
                fontFamily = KiwiSoda,
                fontSize = 18.sp,
                color = ParchmentCream,
            )

            Text(
                text = "1m  5m",
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
                fontFamily = KiwiSoda,
                fontSize = 18.sp,
                color = ParchmentCream,
            )

            Text(
                text = stringResource(R.string.settings_storage_cached_audio),
                style = MaterialTheme.typography.bodyMedium,
                color = MutedSand,
            )
        }

        LinearProgressIndicator(
            progress = { (storageConsumptionMb / 512f).coerceIn(0.1f, 1.0f) },
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
                fontFamily = KiwiSoda,
                fontSize = 16.sp,
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
 * Card for selecting AI execution mode (Cloud vs Offline).
 */
@Composable
private fun AiExecutionModeCard(
    currentMode: String,
    onExecutionModeChanged: (String) -> Unit,
) {
    SettingsCardContainer {
        Text(
            text = stringResource(R.string.settings_execution_header),
            style = MaterialTheme.typography.labelMedium,
            color = AccentGold,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val isCloud = currentMode != "local"
            OptionSegmentButton(
                label = stringResource(R.string.settings_execution_cloud),
                selected = isCloud,
                onClick = { onExecutionModeChanged("online") },
                modifier = Modifier.weight(1f),
            )
            OptionSegmentButton(
                label = stringResource(R.string.settings_execution_offline),
                selected = !isCloud,
                onClick = { onExecutionModeChanged("local") },
                modifier = Modifier.weight(1f),
            )
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Low = 6.0f, Medium = 4.0f, High = 2.0f
            val isLow = knockSensorThreshold >= 5.0f
            val isHigh = knockSensorThreshold <= 2.5f
            val isMedium = !isLow && !isHigh

            OptionSegmentButton(
                label = stringResource(R.string.settings_sensitivity_low),
                selected = isLow,
                onClick = { onKnockThresholdChanged(6.0f) },
                modifier = Modifier.weight(1f),
            )
            OptionSegmentButton(
                label = stringResource(R.string.settings_sensitivity_medium),
                selected = isMedium,
                onClick = { onKnockThresholdChanged(4.0f) },
                modifier = Modifier.weight(1f),
            )
            OptionSegmentButton(
                label = stringResource(R.string.settings_sensitivity_high),
                selected = isHigh,
                onClick = { onKnockThresholdChanged(2.0f) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Interactive gesture testing card supporting simulated double knock taps.
 */
@Composable
private fun GestureTestCard() {
    var isTriggered by remember { mutableStateOf(false) }

    LaunchedEffect(isTriggered) {
        if (isTriggered) {
            delay(1500)
            isTriggered = false
        }
    }

    SettingsCardContainer {
        Text(
            text = stringResource(R.string.settings_gesture_test_header),
            style = MaterialTheme.typography.labelMedium,
            color = AccentGold,
        )

        // Gesture Test Surface Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(ObsidianMidnight)
                .border(1.dp, AntiqueBrass, RoundedCornerShape(4.dp))
                .clickable { isTriggered = true },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.settings_gesture_test_prompt),
                fontFamily = KiwiSoda,
                fontSize = 16.sp,
                color = ParchmentCream,
            )
        }

        // Gesture Feedback Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (isTriggered) {
                    stringResource(R.string.settings_gesture_test_detected)
                } else {
                    stringResource(R.string.settings_gesture_test_awaiting)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MutedSand,
            )

            val pillBg by animateColorAsState(
                targetValue = if (isTriggered) AccentGold else AntiqueBrass,
                animationSpec = tween(200),
                label = "PillBg",
            )

            Surface(
                color = pillBg,
                shape = CircleShape,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = if (isTriggered) {
                        stringResource(R.string.settings_gesture_test_triggered)
                    } else {
                        stringResource(R.string.settings_gesture_test_ready)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = TextInk,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/**
 * Card displaying active user account details and remote database sync toggle.
 */
@Composable
private fun ActiveAccountCard(
    authState: AuthState,
    remoteSyncEnabled: Boolean,
    onRemoteSyncChanged: (Boolean) -> Unit,
) {
    SettingsCardContainer {
        Text(
            text = stringResource(R.string.settings_account_header),
            style = MaterialTheme.typography.labelMedium,
            color = AccentGold,
        )

        val user = authState.user
        val name = user?.displayName?.ifBlank { "Jordan Doe" } ?: "Jordan Doe"
        val email = user?.email?.ifBlank { "jordan@example.com" } ?: "jordan@example.com"
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
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AccentGold),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initials.uppercase(),
                    fontFamily = KiwiSoda,
                    fontSize = 16.sp,
                    color = TextInk,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontFamily = KiwiSoda,
                    fontSize = 16.sp,
                    color = ParchmentCream,
                )
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedSand,
                )
            }
        }

        // Database Sync Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.settings_sync_label),
                fontFamily = KiwiSoda,
                fontSize = 16.sp,
                color = ParchmentCream,
            )

            Switch(
                checked = remoteSyncEnabled,
                onCheckedChange = onRemoteSyncChanged,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = TextInk,
                    checkedTrackColor = AccentGold,
                    uncheckedThumbColor = MutedSand,
                    uncheckedTrackColor = SurfaceContainerHigh,
                    uncheckedBorderColor = OutlineVariant,
                ),
            )
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
 * Option segment button for mode / sensitivity toggles.
 */
@Composable
private fun OptionSegmentButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) AntiqueBrass else Color.Transparent,
        animationSpec = tween(200),
        label = "BgColor",
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) TextInk else ParchmentCream,
        animationSpec = tween(200),
        label = "TextColor",
    )

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .border(1.dp, AntiqueBrass, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontFamily = KiwiSoda,
            fontSize = 16.sp,
            color = textColor,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsTabPreview() {
    KnockledgeTheme {
        SettingsTab(
            settingsUiState = SettingsUiState(
                bufferDurationSeconds = 300,
                knockSensorThreshold = 4.0f,
                transcriptionMode = "online",
            ),
            authState = AuthState(),
            autoAiEnabled = true,
            onAutoAiChanged = {},
            onBufferDurationChanged = {},
            onKnockThresholdChanged = {},
            onSignOut = {},
        )
    }
}
