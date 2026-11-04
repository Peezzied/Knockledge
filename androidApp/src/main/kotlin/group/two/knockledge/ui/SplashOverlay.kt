package group.two.knockledge.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import group.two.knockledge.R
import group.two.knockledge.ui.theme.KnockledgeTheme

/**
 * Brand hold drawn over the app shell right after the system splash: the splash mark with one
 * brass "knock" ring expanding from it, then a fade-out. [onFirstFrame] fires once the overlay is
 * composed so the caller can release the system splash onto an identical frame; [onFinished] fires
 * when the overlay can be removed. With animations disabled in system settings it finishes at once.
 */
@Composable
fun SplashOverlay(
    animationsEnabled: Boolean,
    onFirstFrame: () -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ring = remember { Animatable(0f) }
    val fade = remember { Animatable(1f) }
    val rippleMillis = integerResource(R.integer.splash_ripple_millis)
    val fadeMillis = integerResource(R.integer.splash_fade_millis)

    LaunchedEffect(Unit) {
        onFirstFrame()
        if (animationsEnabled) {
            ring.animateTo(1f, tween(rippleMillis, easing = FastOutSlowInEasing))
            fade.animateTo(0f, tween(fadeMillis))
        }
        onFinished()
    }

    SplashContent(ringProgress = ring.value, alpha = fade.value, modifier = modifier)
}

/** Stateless splash frame: [ringProgress] 0..1 grows the ring and fades its stroke out. */
@Composable
private fun SplashContent(ringProgress: Float, alpha: Float, modifier: Modifier = Modifier) {
    val ringColor = MaterialTheme.colorScheme.primary
    val markSize = dimensionResource(R.dimen.splash_mark_size)
    val ringMax = dimensionResource(R.dimen.splash_ring_max_size)
    val ringStroke = dimensionResource(R.dimen.splash_ring_stroke)
    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(alpha)
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(ringMax)) {
            val minRadius = markSize.toPx() / 2
            val radius = minRadius + (ringMax.toPx() / 2 - minRadius) * ringProgress
            drawCircle(
                color = ringColor.copy(alpha = 1f - ringProgress),
                radius = radius,
                center = Offset(size.width / 2, size.height / 2),
                style = Stroke(width = ringStroke.toPx()),
            )
        }
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(markSize * 3),
        )
    }
}

@Preview
@Composable
private fun SplashContentPreview() {
    KnockledgeTheme { SplashContent(ringProgress = 0.4f, alpha = 1f) }
}
