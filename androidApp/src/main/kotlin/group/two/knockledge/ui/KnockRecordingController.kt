package group.two.knockledge.ui

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * UI state for knock-triggered circular recording.
 */
data class KnockRecordingUiState(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    /** Seconds elapsed in the current session, excluding paused time. */
    val sessionSeconds: Int = 0,
    val bufferedSeconds: Int = 0,
    val maxBufferSeconds: Int = 300,
    val isGestureActive: Boolean = false,
    val isSavingSnapshot: Boolean = false,
    val lastSaveSuccess: Boolean? = null,
    /** True once the buffer holds at least the configured minimum audio for a save. */
    val canCapture: Boolean = false,
    val level: Float = 0f,
    /** Live frequency band levels, 0..1 each, low to high frequency. Drives the waveform bars. */
    val bands: List<Float> = emptyList(),
)

/**
 * Contract for controlling a circular audio recording session.
 */
interface KnockRecordingController {
    /** Current recording UI state. */
    val uiState: StateFlow<KnockRecordingUiState>

    /** Emits once for each knock-gesture capture so the capture button can replay its animation. */
    val gestureCaptures: SharedFlow<Unit>

    /** Starts buffering audio and gesture detection. */
    fun startSession()

    /** Pauses buffering audio and gesture detection. */
    fun pauseSession()

    /** Stops buffering audio and gesture detection. */
    fun stopSession()

    /** Saves the current buffer contents as a snapshot. */
    fun triggerManualSave()

    /** Updates the circular buffer window length. */
    fun setBufferDurationSeconds(seconds: Int)
}
