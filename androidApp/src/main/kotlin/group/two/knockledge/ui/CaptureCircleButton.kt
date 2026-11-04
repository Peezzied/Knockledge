package group.two.knockledge.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import group.two.knockledge.ui.theme.AccentGold
import group.two.knockledge.ui.theme.AntiqueBrass
import group.two.knockledge.ui.theme.KnockledgeTheme
import group.two.knockledge.ui.theme.ObsidianMidnight
import group.two.knockledge.ui.theme.ParchmentCream
import group.two.knockledge.ui.theme.WaveformTeal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Random
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

private const val REJECTION_DURATION_MILLIS = 800
private const val REEL_ROTATION_MILLIS = 10000
private const val REEL_DEGREES_PER_SECOND = 360f * 1000f / REEL_ROTATION_MILLIS

private data class SuctionParticle(
    val initialAngleRad: Float,
    val initialRadiusDp: Float,
    val speedMultiplier: Float,
    val particleSizeDp: Float,
    val color: Color,
)

/**
 * Custom Canvas composable that draws a pixelated retro ring contour matching Figma mockup styling.
 */
@Composable
fun PixelatedRing(
    radius: Dp,
    pixelSize: Dp,
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
 * Vintage cassette tape reel hub / holder component rendered via Canvas.
 * Draws the reel hub disk, step grooves, central axle cutout, and 6 inward sprocket teeth
 * matching classic cassette tape mechanisms.
 *
 * @param modifier [Modifier] for layout sizing and positioning.
 * @param color Color of the reel disk and sprocket teeth.
 * @param shadowColor Dark shadow/border color for inner cutout depth.
 * @param rotationAngle Angle in degrees to rotate the cassette reel.
 * @param alpha Opacity modifier for suction depth effects.
 */
@Composable
fun CassetteReelHub(
    modifier: Modifier = Modifier,
    color: Color = ParchmentCream,
    shadowColor: Color = ObsidianMidnight,
    rotationAngle: Float = 0f,
    alpha: Float = 1f,
) {
    Canvas(
        modifier = modifier
            .graphicsLayer {
                rotationZ = rotationAngle
                this.alpha = alpha
            },
    ) {
        val diameter = minOf(size.width, size.height)
        val centerOffset = Offset(size.width / 2f, size.height / 2f)
        val outerRadius = diameter / 2f

        // 1. Outer Reel Rim Disk
        drawCircle(
            color = color,
            radius = outerRadius,
            center = centerOffset,
        )

        // Outer Rim Border Stroke
        drawCircle(
            color = shadowColor.copy(alpha = 0.35f),
            radius = outerRadius,
            center = centerOffset,
            style = Stroke(width = outerRadius * 0.05f),
        )

        // 2. Molded Step Groove (Concentric Inner Ring)
        val stepGrooveRadius = outerRadius * 0.72f
        drawCircle(
            color = shadowColor.copy(alpha = 0.22f),
            radius = stepGrooveRadius,
            center = centerOffset,
            style = Stroke(width = outerRadius * 0.06f),
        )

        // Inner Rim Highlight
        drawCircle(
            color = color,
            radius = stepGrooveRadius - outerRadius * 0.03f,
            center = centerOffset,
            style = Stroke(width = outerRadius * 0.02f),
        )

        // 3. Central Dark Cutout Hole (Behind teeth)
        val hubCutoutRadius = outerRadius * 0.48f
        drawCircle(
            color = shadowColor,
            radius = hubCutoutRadius,
            center = centerOffset,
        )

        // 4. 6 Inward Sprocket Teeth Lugs
        val teethPath = Path().apply {
            val numTeeth = 6
            val toothAngleSweep = 28f // degrees per tooth
            val innerTeethRadius = outerRadius * 0.28f

            for (i in 0 until numTeeth) {
                val centerAngle = i * 60f
                val startAngle = centerAngle - toothAngleSweep / 2f
                val endAngle = centerAngle + toothAngleSweep / 2f

                val startRad = Math.toRadians(startAngle.toDouble())
                val endRad = Math.toRadians(endAngle.toDouble())

                val p1X = centerOffset.x + hubCutoutRadius * cos(startRad).toFloat()
                val p1Y = centerOffset.y + hubCutoutRadius * sin(startRad).toFloat()

                val p2X = centerOffset.x + innerTeethRadius * cos(startRad).toFloat()
                val p2Y = centerOffset.y + innerTeethRadius * sin(startRad).toFloat()

                val p3X = centerOffset.x + innerTeethRadius * cos(endRad).toFloat()
                val p3Y = centerOffset.y + innerTeethRadius * sin(endRad).toFloat()

                val p4X = centerOffset.x + hubCutoutRadius * cos(endRad).toFloat()
                val p4Y = centerOffset.y + hubCutoutRadius * sin(endRad).toFloat()

                moveTo(p1X, p1Y)
                lineTo(p2X, p2Y)
                lineTo(p3X, p3Y)
                lineTo(p4X, p4Y)
                close()
            }
        }

        drawPath(
            path = teethPath,
            color = color,
        )

        // 5. Central Axle Hole Cutout
        val axleRadius = outerRadius * 0.20f
        drawCircle(
            color = shadowColor,
            radius = axleRadius,
            center = centerOffset,
        )
    }
}

/**
 * Shared central Capture Circle Button with glowing halo, particle vortex suction animation,
 * double pixelated rings, and central cassette reel hub.
 *
 * @param isRecording Whether buffer recording is currently active.
 * @param onClick Action performed when the capture button is triggered.
 * @param modifier [Modifier] for layout positioning and scaling.
 * @param isSavingSnapshot Whether a snapshot is currently saving (disables click).
 * @param canCapture Whether the buffer holds enough audio to save; taps while recording
 * with this false play a rejection flash instead of the suction animation.
 * @param captureEvents External capture signals (such as the knock gesture) that replay
 * the suction animation without a tap.
 */
@Composable
fun CaptureCircleButton(
    isRecording: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSavingSnapshot: Boolean = false,
    canCapture: Boolean = true,
    captureEvents: Flow<Unit>? = null,
) {
    val scope = rememberCoroutineScope()
    var isSinking by remember { mutableStateOf(false) }
    var isRejected by remember { mutableStateOf(false) }
    val frozenReelAngle = remember { Animatable(0f) }

    val errorColor = MaterialTheme.colorScheme.error
    val suctionAnim = remember { Animatable(0f) }
    val ringAlpha = remember { Animatable(1f) }
    val rejectionAnim = remember { Animatable(0f) }
    val particles = remember {
        val rng = Random(42L)
        val particleColors = listOf(AccentGold, AntiqueBrass, WaveformTeal, ParchmentCream)
        List(32) {
            SuctionParticle(
                initialAngleRad = rng.nextFloat() * (2f * Math.PI.toFloat()),
                initialRadiusDp = 95f + rng.nextFloat() * 55f,
                speedMultiplier = 0.85f + rng.nextFloat() * 0.45f,
                particleSizeDp = 2.5f + rng.nextFloat() * 2.5f,
                color = particleColors[rng.nextInt(particleColors.size)],
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_rings")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_scale",
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_alpha",
    )
    val glowHaloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow_halo_alpha",
    )
    val reelRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = REEL_ROTATION_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "reel_rotation",
    )

    val buttonPressScale by animateFloatAsState(
        targetValue = if (isSinking) 0.82f else 1.0f,
        animationSpec = if (isSinking) {
            tween(durationMillis = 100, easing = FastOutLinearInEasing)
        } else {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            )
        },
        label = "button_press_scale",
    )

    val suctionDepthAlpha by animateFloatAsState(
        targetValue = if (isSinking) 0.50f else 1.0f,
        animationSpec = tween(durationMillis = 100),
        label = "suction_depth_alpha",
    )

    // Full capture animation: sink, particle suction, then fade the rings back in.
    suspend fun playCaptureAnimation() {
        isSinking = true
        suctionAnim.snapTo(0f)
        suctionAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 480,
                easing = FastOutLinearInEasing,
            ),
        )
        isSinking = false
        ringAlpha.snapTo(0f)
        suctionAnim.snapTo(0f)
        ringAlpha.animateTo(1f, tween(durationMillis = 400))
    }

    LaunchedEffect(captureEvents) {
        captureEvents?.collectLatest { playCaptureAnimation() }
    }

    Box(
        modifier = modifier
            .size(240.dp)
            .graphicsLayer {
                scaleX = buttonPressScale
                scaleY = buttonPressScale
            },
        contentAlignment = Alignment.Center,
    ) {
        // Outer Radial Glowing Halo
        Canvas(
            modifier = Modifier
                .size(200.dp)
                .scale(if (isRecording) pulseScale else 1f),
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        AccentGold.copy(
                            alpha = (if (isRecording) glowHaloAlpha else 0.35f) * suctionDepthAlpha,
                        ),
                        AntiqueBrass.copy(
                            alpha = (if (isRecording) glowHaloAlpha * 0.5f else 0.15f) * suctionDepthAlpha,
                        ),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = size.width / 2f,
                ),
                radius = size.width / 2f,
            )
        }

        // Black hole particle suction overlay
        if (suctionAnim.value > 0f && suctionAnim.value < 1f) {
            val progress = suctionAnim.value
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerX = size.width / 2f
                val centerY = size.height / 2f

                particles.forEach { p ->
                    val pProgress = (progress * p.speedMultiplier).coerceIn(0f, 1f)
                    if (pProgress in 0.01f..0.99f) {
                        val currentRadiusPx = p.initialRadiusDp.dp.toPx() * (1f - pProgress.toDouble().pow(2.2).toFloat())
                        val currentAngle = p.initialAngleRad + pProgress * 1.8f
                        val px = centerX + currentRadiusPx * cos(currentAngle)
                        val py = centerY + currentRadiusPx * sin(currentAngle)
                        val alpha = (1f - pProgress * pProgress) * 0.92f
                        val radiusPx = (p.particleSizeDp.dp.toPx() * (1f - pProgress * 0.5f)).coerceAtLeast(1.5f)

                        drawCircle(
                            color = p.color.copy(alpha = alpha),
                            radius = radiusPx,
                            center = Offset(px, py),
                        )
                    }
                }
            }
        }

        // Interactive Mic Ring Button with Pixelated Rings
        Box(
            modifier = Modifier
                .size(180.dp)
                .padding(8.dp)
                .clip(CircleShape)
                .clickable(
                    enabled = !isSavingSnapshot,
                    onClick = {
                        scope.launch {
                            if (isRecording && !canCapture) {
                                frozenReelAngle.snapTo(reelRotation)
                                isRejected = true
                                launch {
                                    frozenReelAngle.animateDecay(
                                        initialVelocity = REEL_DEGREES_PER_SECOND,
                                        animationSpec = exponentialDecay(),
                                    )
                                }
                                rejectionAnim.snapTo(0f)
                                rejectionAnim.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(REJECTION_DURATION_MILLIS / 2),
                                )
                                rejectionAnim.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(REJECTION_DURATION_MILLIS / 2),
                                )
                                isRejected = false
                                return@launch
                            }
                            playCaptureAnimation()
                        }
                        onClick()
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            // Outer Pixelated Ring
            PixelatedRing(
                radius = 76.dp,
                pixelSize = 3.dp,
                color = lerp(AccentGold, errorColor, rejectionAnim.value).copy(
                    alpha = (if (isRecording) pulseAlpha else 0.5f) * suctionDepthAlpha * ringAlpha.value,
                ),
                modifier = Modifier
                    .size(160.dp)
                    .scale(if (isRecording) pulseScale else 1f),
                pullProgress = suctionAnim.value,
            )

            // Inner Pixelated Ring
            PixelatedRing(
                radius = 64.dp,
                pixelSize = 3.dp,
                color = lerp(AccentGold, errorColor, rejectionAnim.value).copy(
                    alpha = (if (isRecording) (pulseAlpha * 0.8f) else 0.35f) * suctionDepthAlpha * ringAlpha.value,
                ),
                modifier = Modifier
                    .size(136.dp)
                    .scale(if (isRecording) (pulseScale * 0.98f) else 1f),
                pullProgress = suctionAnim.value,
            )

            // Center Cassette Reel Holder - Vintage Spool Hub
            CassetteReelHub(
                color = ParchmentCream,
                shadowColor = ObsidianMidnight,
                rotationAngle = if (isRecording) (if (isRejected) frozenReelAngle.value else reelRotation) else 0f,
                alpha = suctionDepthAlpha,
                modifier = Modifier
                    .size(72.dp)
                    .scale(if (isSinking) 0.85f else 1.0f),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0E15)
@Composable
private fun CaptureCircleButtonPreview() {
    KnockledgeTheme {
        CaptureCircleButton(
            isRecording = true,
            onClick = {},
        )
    }
}
