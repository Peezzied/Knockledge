package group.two.knockledge.ui

import kotlinx.coroutines.flow.StateFlow

data class KnockRecordingUiState(
    val isRecording: Boolean = false,
    val bufferedSeconds: Int = 0,
    val maxBufferSeconds: Int = 300,
    val isGestureActive: Boolean = false,
    val isSavingSnapshot: Boolean = false,
    val lastSaveSuccess: Boolean? = null,
)

interface KnockRecordingController {
    val uiState: StateFlow<KnockRecordingUiState>
    fun startSession()
    fun stopSession()
    fun triggerManualSave()
    fun setBufferDurationSeconds(seconds: Int)
}
