package group.two.knockledge.ui.auth

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import group.two.knockledge.ui.theme.KnockledgeTheme
import kotlin.math.sqrt

/** Number of columns the auth snapshot is sliced into for the dissolve. */
private const val GRID_COLS = 12

/** Number of rows the auth snapshot is sliced into for the dissolve. */
private const val GRID_ROWS = 20

/** Total dissolve animation duration in milliseconds. */
internal const val BRASS_DISSOLVE_DURATION_MS = 1200

/** Fraction of the animation spent on the release ripple so distant fragments still finish on time. */
private const val MAX_DELAY_FRACTION = 0.6f

/** Fraction of its size a fragment shrinks by the end of its release. */
private const val FRAGMENT_SHRINK_FRACTION = 0.55f

/** Upward drift of a released fragment, as a fraction of overlay height. */
private const val LIFT_FRACTION = 0.08f

/** Outward drift of a released fragment, as a fraction of overlay size. */
private const val DRIFT_FRACTION = 0.12f

/** Peak alpha of the brass rim shown while a fragment breaks free. */
private const val RIM_MAX_ALPHA = 0.6f

/**
 * Per-fragment animation progress for the brass dissolve.
 *
 * The fragment's release is delayed proportionally to [normalizedDistance] (0 = at the ripple
 * origin, 1 = farthest fragment), so fragments nearer the origin break away first. The result is
 * clamped to 0..1; a fragment always completes its release by global progress 1.
 *
 * @param globalProgress overall dissolve progress in 0..1.
 * @param normalizedDistance fragment distance from the ripple origin, normalised by the maximum.
 * @param maxDelayFraction largest share of the timeline a fragment may wait before releasing.
 * @return the fragment's own release progress in 0..1.
 */
internal fun fragmentReleaseProgress(
    globalProgress: Float,
    normalizedDistance: Float,
    maxDelayFraction: Float = MAX_DELAY_FRACTION,
): Float {
    val delay = normalizedDistance.coerceIn(0f, 1f) * maxDelayFraction
    return ((globalProgress - delay) / (1f - delay)).coerceIn(0f, 1f)
}

/**
 * Stateless overlay that breaks a captured auth-screen snapshot into a grid of fragments which
 * drift up and outward, shrink, and fade, rippling away from [originPx]. Drawn in a single
 * [Canvas] pass; it has no pointer handling and never intercepts touches or system Back.
 *
 * @param snapshot bitmap of the auth screen captured before it left composition.
 * @param progress overall dissolve progress in 0..1; remove the overlay when it reaches 1.
 * @param originPx ripple origin in overlay pixels, or [Offset.Unspecified] for bottom-center.
 * @param modifier modifier applied to the overlay canvas.
 */
@Composable
fun BrassDissolveOverlay(
    snapshot: ImageBitmap,
    progress: Float,
    originPx: Offset,
    modifier: Modifier = Modifier,
) {
    val rimColor = MaterialTheme.colorScheme.primary
    val gapPx = with(LocalDensity.current) { 2.dp.toPx() }
    Canvas(modifier = modifier) {
        val cellW = size.width / GRID_COLS
        val cellH = size.height / GRID_ROWS
        val origin = if (originPx == Offset.Unspecified) {
            Offset(size.width / 2f, size.height)
        } else {
            originPx
        }
        val maxDistance = sqrt(size.width * size.width + size.height * size.height)

        for (row in 0 until GRID_ROWS) {
            for (col in 0 until GRID_COLS) {
                val local = fragmentReleaseProgress(
                    progress,
                    normalizedDistance = fragmentCenter(col, row, cellW, cellH)
                        .minus(origin)
                        .getDistance() / maxDistance,
                )
                if (local >= 1f) continue

                val center = fragmentCenter(col, row, cellW, cellH)
                val away = (center - origin).let { d ->
                    if (d.getDistance() == 0f) Offset(0f, -1f) else d / d.getDistance()
                }
                val drift = away * (local * DRIFT_FRACTION * size.width) +
                    Offset(0f, -local * LIFT_FRACTION * size.height)
                val scale = 1f - FRAGMENT_SHRINK_FRACTION * local
                val inset = gapPx * local
                val dstW = cellW * scale - inset * 2f
                val dstH = cellH * scale - inset * 2f
                val dstTopLeft = center + drift - Offset(dstW / 2f, dstH / 2f)

                drawImage(
                    image = snapshot,
                    srcOffset = IntOffset((col * cellW).toInt(), (row * cellH).toInt()),
                    srcSize = IntSize(cellW.toInt(), cellH.toInt()),
                    dstOffset = IntOffset(dstTopLeft.x.toInt(), dstTopLeft.y.toInt()),
                    dstSize = IntSize(dstW.toInt().coerceAtLeast(1), dstH.toInt().coerceAtLeast(1)),
                    alpha = 1f - local,
                )
                if (local > 0f) {
                    drawRect(
                        color = rimColor.copy(alpha = RIM_MAX_ALPHA * (1f - local)),
                        topLeft = dstTopLeft,
                        size = Size(dstW, dstH),
                        style = Stroke(width = gapPx),
                    )
                }
            }
        }
    }
}

/** Center in pixels of the grid fragment at [col], [row] for cells of [cellW] x [cellH]. */
private fun fragmentCenter(col: Int, row: Int, cellW: Float, cellH: Float): Offset =
    Offset((col + 0.5f) * cellW, (row + 0.5f) * cellH)

@Preview
@Composable
private fun BrassDissolveOverlayPreview() {
    KnockledgeTheme {
        BrassDissolveOverlay(
            snapshot = ImageBitmap(GRID_COLS * 8, GRID_ROWS * 8),
            progress = 0.4f,
            originPx = Offset.Unspecified,
        )
    }
}
