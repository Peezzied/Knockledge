package group.two.knockledge.ui

import android.animation.ValueAnimator
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import group.two.knockledge.R
import group.two.knockledge.settings.AppSettings
import group.two.knockledge.ui.theme.KnockledgeTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

/** Reserved note id for the in-memory tour sample note; must never collide with real notes. */
const val TOUR_SAMPLE_NOTE_ID: String = "__tour_sample_note__"

/** Spotlighted targets of the first-sign-in app tour, in display order. */
enum class AppTourStep {
    RECORD_BUTTON,
    RECORDINGS_LIST,
    SETTINGS_GEAR,
    BUFFER_SLIDER,
}

/** Immutable tour state: whether the overlay is visible and which step is shown. */
data class AppTourUiState(
    val active: Boolean = false,
    val stepIndex: Int = 0,
    /** True only when the tour ended through Done on the last step, not Skip. */
    val completedByDone: Boolean = false,
) {
    /** Current spotlighted step. */
    val step: AppTourStep get() = AppTourStep.entries[stepIndex.coerceIn(0, AppTourStep.entries.lastIndex)]
}

/**
 * Pure stepper for the app tour. Advances, rewinds, skips and persists the per-user
 * seen flag through [AppSettings]. No Android dependencies so it runs on the JVM.
 */
class AppTourStateMachine(
    private val settings: AppSettings,
    private val userId: String,
) {
    private val _state = MutableStateFlow(AppTourUiState())

    /** Observable tour state for the overlay. */
    val state: StateFlow<AppTourUiState> = _state.asStateFlow()

    /** Starts the tour on first sign-in when [userId] has not seen it yet. */
    fun maybeStart() {
        if (!_state.value.active && !settings.isTourSeen(userId)) start()
    }

    /** Starts or replays the tour at [stepIndex] (0 restarts from the beginning). */
    fun start(stepIndex: Int = 0) {
        _state.value = AppTourUiState(
            active = true,
            stepIndex = stepIndex.coerceIn(0, AppTourStep.entries.lastIndex),
        )
    }

    /** Advances to the next step, or finishes the tour on the last step. */
    fun next() {
        val current = _state.value
        if (!current.active) return
        if (current.stepIndex >= AppTourStep.entries.lastIndex) {
            finish(completedByDone = true)
        } else {
            _state.value = current.copy(stepIndex = current.stepIndex + 1)
        }
    }

    /** Returns to the previous step; a no-op on the first step. */
    fun back() {
        val current = _state.value
        if (current.active && current.stepIndex > 0) {
            _state.value = current.copy(stepIndex = current.stepIndex - 1)
        }
    }

    /** Skips the tour and marks it seen. */
    fun skip() = finish(completedByDone = false)

    private fun finish(completedByDone: Boolean) {
        settings.setTourSeen(userId, true)
        _state.value = AppTourUiState(completedByDone = completedByDone)
    }
}

/**
 * Holds the on-screen bounds of the spotlighted controls, reported by the real
 * composables through [androidx.compose.ui.layout.onGloballyPositioned].
 */
class AppTourTargets {
    /** Latest bounds per step, in root coordinates. */
    val bounds = mutableStateMapOf<AppTourStep, Rect>()

    /** Records [rect] as the current bounds of [step]'s target. */
    fun report(step: AppTourStep, rect: Rect) {
        bounds[step] = rect
    }
}

/**
 * Full-screen spotlight tour overlay: a midnight scrim with a cutout around the
 * current target, a pulsing brass halo, and a callout card placed above or below
 * the cutout. System Back confirms skip on the first step and rewinds afterwards.
 */
@Composable
fun AppTourOverlay(
    state: AppTourUiState,
    targets: AppTourTargets,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSkipConfirmation by remember { mutableStateOf(false) }

    BackHandler(enabled = state.active) {
        if (state.stepIndex == 0) {
            showSkipConfirmation = true
        } else {
            onBack()
        }
    }

    if (showSkipConfirmation) {
        AlertDialog(
            onDismissRequest = { showSkipConfirmation = false },
            confirmButton = {
                TextButton(onClick = {
                    showSkipConfirmation = false
                    onSkip()
                }) {
                    Text(stringResource(R.string.tour_skip_confirm_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSkipConfirmation = false }) {
                    Text(stringResource(R.string.settings_gesture_test_cancel_action))
                }
            },
            title = { Text(stringResource(R.string.tour_skip_confirm_title)) },
            text = { Text(stringResource(R.string.tour_skip_confirm_body)) },
            shape = RoundedCornerShape(4.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    val animatorsEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    val haloPulse by if (animatorsEnabled) {
        rememberInfiniteTransition(label = "tourHalo").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "tourHaloPulse",
        )
    } else {
        remember { mutableStateOf(0.5f) }
    }
    val stepTransitionSpec = tween<Float>(if (animatorsEnabled) 250 else 0)

    Crossfade(
        targetState = state.stepIndex,
        animationSpec = stepTransitionSpec,
        label = "tourStep",
        modifier = modifier.fillMaxSize(),
    ) { stepIndex ->
        val step = AppTourStep.entries[stepIndex.coerceIn(0, AppTourStep.entries.lastIndex)]
        val bounds = targets.bounds[step]

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val scrimColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
            val brass = MaterialTheme.colorScheme.primary
            val glow = MaterialTheme.colorScheme.primary

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
            ) {
                drawRect(color = scrimColor)
                if (bounds != null) {
                    val cutout = bounds.inflate(8.dp.toPx())
                    val haloRadius = maxOf(cutout.width, cutout.height) * (0.75f + 0.15f * haloPulse)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(glow.copy(alpha = 0.35f + 0.2f * haloPulse), Color.Transparent),
                            center = cutout.center,
                            radius = haloRadius,
                        ),
                        center = cutout.center,
                        radius = haloRadius,
                    )
                    drawRoundRect(
                        color = Color.Black,
                        topLeft = cutout.topLeft,
                        size = cutout.size,
                        cornerRadius = CornerRadius(12.dp.toPx()),
                        blendMode = BlendMode.Clear,
                    )
                    drawRoundRect(
                        color = brass,
                        topLeft = cutout.topLeft,
                        size = cutout.size,
                        cornerRadius = CornerRadius(12.dp.toPx()),
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }
            }

            TourCallout(
                step = step,
                stepIndex = stepIndex,
                stepCount = AppTourStep.entries.size,
                targetBounds = bounds,
                onNext = onNext,
                onSkip = onSkip,
            )
        }
    }
}

/**
 * Callout card for a single tour step, placed below the cutout when it fits and
 * above it otherwise. Grows and scrolls within half the screen for large fonts.
 */
@Composable
private fun androidx.compose.foundation.layout.BoxWithConstraintsScope.TourCallout(
    step: AppTourStep,
    stepIndex: Int,
    stepCount: Int,
    targetBounds: Rect?,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    val density = LocalDensity.current
    var calloutHeightPx by remember { mutableIntStateOf(0) }
    val gapPx = with(density) { 16.dp.toPx() }
    val screenWidthPx = constraints.maxWidth.toFloat()
    val screenHeightPx = constraints.maxHeight.toFloat()

    val yPx = when {
        targetBounds == null -> ((screenHeightPx - calloutHeightPx) / 2f).coerceAtLeast(gapPx)
        targetBounds.bottom + gapPx + calloutHeightPx <= screenHeightPx -> targetBounds.bottom + gapPx
        else -> (targetBounds.top - gapPx - calloutHeightPx).coerceAtLeast(gapPx)
    }
    val maxCalloutHeight = maxHeight * 0.6f

    Card(
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .offset { IntOffset(with(density) { 16.dp.toPx() }.roundToInt(), yPx.roundToInt()) }
            .width(with(density) { (screenWidthPx - 2 * gapPx).toDp() })
            .heightIn(max = maxCalloutHeight)
            .onSizeChanged { calloutHeightPx = it.height },
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(
                text = stringResource(tourStepTitleRes(step)),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(tourStepBodyRes(step)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.tour_step_counter, stepIndex + 1, stepCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(
                        text = stringResource(R.string.tour_skip),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onNext,
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text(
                        text = stringResource(
                            if (stepIndex == stepCount - 1) R.string.tour_done else R.string.tour_next,
                        ),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

private fun tourStepTitleRes(step: AppTourStep): Int = when (step) {
    AppTourStep.RECORD_BUTTON -> R.string.tour_step_record_title
    AppTourStep.RECORDINGS_LIST -> R.string.tour_step_list_title
    AppTourStep.SETTINGS_GEAR -> R.string.tour_step_settings_title
    AppTourStep.BUFFER_SLIDER -> R.string.tour_step_buffer_title
}

private fun tourStepBodyRes(step: AppTourStep): Int = when (step) {
    AppTourStep.RECORD_BUTTON -> R.string.tour_step_record_body
    AppTourStep.RECORDINGS_LIST -> R.string.tour_step_list_body
    AppTourStep.SETTINGS_GEAR -> R.string.tour_step_settings_body
    AppTourStep.BUFFER_SLIDER -> R.string.tour_step_buffer_body
}

@Preview(showBackground = true)
@Composable
private fun AppTourOverlayPreview() {
    KnockledgeTheme {
        Box(Modifier.fillMaxSize()) {
            AppTourOverlay(
                state = AppTourUiState(active = true, stepIndex = 0),
                targets = AppTourTargets().apply {
                    report(
                        AppTourStep.RECORD_BUTTON,
                        Rect(center = Offset(200f, 1400f), radius = 140f),
                    )
                },
                onNext = {},
                onBack = {},
                onSkip = {},
            )
        }
    }
}
