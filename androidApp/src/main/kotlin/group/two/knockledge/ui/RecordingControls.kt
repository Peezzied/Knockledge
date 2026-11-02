package group.two.knockledge.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import group.two.knockledge.R
import group.two.knockledge.ui.theme.AccentGold
import group.two.knockledge.ui.theme.AntiqueBrass
import group.two.knockledge.ui.theme.KnockledgeTheme
import group.two.knockledge.ui.theme.ObsidianMidnight
import group.two.knockledge.ui.theme.ParchmentCream
import group.two.knockledge.ui.theme.TextInk
import group.two.knockledge.ui.theme.WaveformPink
import group.two.knockledge.ui.theme.WaveformTeal

/**
 * Functional sound wave visualization with high-density thin vertical bars reacting live to
 * [bands]: each bar's height follows the live audio energy in one frequency region, low
 * frequencies on the left. Falls back to [level] when no band data is available yet.
 */
@Composable
fun FunctionalWaveform(
    isRecording: Boolean,
    level: Float,
    modifier: Modifier = Modifier,
    bands: List<Float> = emptyList(),
) {
    val barCount = 42

    val maxBarHeight = 180.dp
    val minBarHeight = 4.dp

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(maxBarHeight)
            .padding(vertical = 4.dp),
    ) {
        val totalWidth = size.width
        val canvasHeight = size.height
        val centerY = canvasHeight / 2f
        val strokeWidthPx = 2.5.dp.toPx()
        val spacingPx = (totalWidth - (barCount * strokeWidthPx)) / (barCount + 1)
        val minHeightPx = minBarHeight.toPx()
        val maxHeightPx = maxBarHeight.toPx()

        for (i in 0 until barCount) {
            val x = spacingPx + i * (strokeWidthPx + spacingPx) + strokeWidthPx / 2f
            val targetHeightRatio = if (isRecording) {
                val energy = if (bands.isNotEmpty()) {
                    // Bar i samples the band list by linear interpolation across its range.
                    val pos = i.toFloat() / (barCount - 1) * (bands.size - 1)
                    val lo = pos.toInt().coerceIn(0, bands.size - 1)
                    val hi = (lo + 1).coerceAtMost(bands.size - 1)
                    bands[lo] + (bands[hi] - bands[lo]) * (pos - lo)
                } else {
                    level
                }
                energy.coerceIn(0.05f, 1.0f)
            } else {
                0.05f
            }

            val barHeightPx = (maxHeightPx * targetHeightRatio).coerceAtLeast(minHeightPx)
            val startY = centerY - barHeightPx / 2f
            val endY = centerY + barHeightPx / 2f

            val barColor = when {
                i in 3..5 || i in 28..32 -> WaveformPink
                i in 9..12 || i in 35..38 -> WaveformTeal
                else -> ParchmentCream
            }

            drawLine(
                color = barColor,
                start = Offset(x, startY),
                end = Offset(x, endY),
                strokeWidth = strokeWidthPx,
                cap = StrokeCap.Round,
            )
        }
    }
}

/**
 * Recording status, manual-save button, and buffer-length segmented selector.
 */
@Composable
fun RecordingControls(
    state: KnockRecordingUiState,
    onManualSave: () -> Unit,
    onDurationChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(
                R.string.recording_buffer_status,
                formatBufferTime(state.bufferedSeconds),
                formatBufferTime(state.maxBufferSeconds),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = AccentGold,
        )

        FunctionalWaveform(
            isRecording = state.isRecording,
            level = state.level,
            bands = state.bands,
        )

        Text(
            text = stringResource(R.string.buffer_window_selector_title),
            style = MaterialTheme.typography.labelMedium,
            color = AccentGold,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
        ) {
            val minutesList = listOf(1, 2, 3, 4, 5)
            val selectedMinutes = (state.maxBufferSeconds / 60).coerceIn(1, 5)

            minutesList.forEach { mins ->
                val isSelected = mins == selectedMinutes
                FilterChip(
                    selected = isSelected,
                    onClick = { onDurationChange(mins * 60) },
                    label = { Text("${mins}m") },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = ObsidianMidnight,
                        labelColor = ParchmentCream,
                        selectedContainerColor = AccentGold,
                        selectedLabelColor = TextInk,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = AccentGold,
                        selectedBorderColor = AntiqueBrass,
                        enabled = true,
                        selected = isSelected,
                    ),
                )
            }
        }

        Button(
            onClick = onManualSave,
            enabled = state.isRecording && !state.isSavingSnapshot,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = AntiqueBrass,
                contentColor = TextInk,
            ),
        ) {
            Text(if (state.isSavingSnapshot) "Saving..." else stringResource(R.string.save_snapshot))
        }

        when (state.lastSaveSuccess) {
            true -> Text("Saved successfully", style = MaterialTheme.typography.bodySmall, color = WaveformTeal)
            false -> Text("Not enough audio buffered yet", style = MaterialTheme.typography.bodySmall, color = WaveformPink)
            null -> Unit
        }
    }
}

private fun formatBufferTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return if (m > 0) String.format(java.util.Locale.US, "%d:%02d", m, s) else "${s}s"
}

@Preview(showBackground = true)
@Composable
private fun RecordingControlsRecordingPreview() {
    KnockledgeTheme {
        RecordingControls(
            KnockRecordingUiState(
                isRecording = true,
                bufferedSeconds = 95,
                maxBufferSeconds = 300,
                isGestureActive = true,
                level = 0.65f,
                bands = List(14) { 0.9f - it * 0.06f },
            ),
            onManualSave = {},
            onDurationChange = {},
        )
    }
}
