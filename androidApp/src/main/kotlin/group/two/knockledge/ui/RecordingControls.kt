package group.two.knockledge.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import group.two.knockledge.ui.theme.KnockledgeTheme
import group.two.knockledge.ui.theme.ParchmentCream
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

@Preview(showBackground = true, backgroundColor = 0xFF14122B)
@Composable
private fun FunctionalWaveformPreview() {
    KnockledgeTheme {
        FunctionalWaveform(
            isRecording = true,
            level = 0.65f,
            bands = List(14) { 0.9f - it * 0.06f },
        )
    }
}