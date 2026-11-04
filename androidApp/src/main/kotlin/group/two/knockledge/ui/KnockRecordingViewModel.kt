package group.two.knockledge.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import group.two.knockledge.KnockledgeController
import group.two.knockledge.data.TriggerType
import group.two.knockledge.settings.AppSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * [ViewModel] combining session state from [KnockledgeController] and [AppSettings] into [KnockRecordingUiState].
 */
class KnockRecordingViewModel(
    private val controller: KnockledgeController,
    private val settings: AppSettings,
) : ViewModel(), KnockRecordingController {
    private val lastSaveSuccess = MutableStateFlow<Boolean?>(null)

    private val sessionSeconds = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            controller.state
                .map { it.isRunning to it.isPaused }
                .distinctUntilChanged()
                .collectLatest { (running, paused) ->
                    if (!running && !paused) sessionSeconds.value = 0
                    while (running && !paused) {
                        delay(1000L)
                        sessionSeconds.value++
                    }
                }
        }
    }

    /** Combined recording, buffer, and save-status state for the UI. */
    override val uiState: StateFlow<KnockRecordingUiState> =
        combine(controller.state, settings.bufferDurationSeconds, lastSaveSuccess, sessionSeconds) { rec, max, saved, session ->
            KnockRecordingUiState(
                isRecording = rec.isRunning,
                isPaused = rec.isPaused,
                sessionSeconds = session,
                bufferedSeconds = (rec.bufferedMillis / 1000L).toInt(),
                maxBufferSeconds = max,
                isGestureActive = rec.isGestureActive,
                isSavingSnapshot = rec.pendingRewinds > 0,
                lastSaveSuccess = saved,
                canCapture = rec.canRewind,
                level = rec.level,
                bands = rec.bands,
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, KnockRecordingUiState())

    /** Knock-gesture capture signals forwarded from the session controller. */
    override val gestureCaptures: SharedFlow<Unit> = controller.gestureCaptures

    /** Starts the recording session. */
    override fun startSession() = controller.start()

    /** Pauses the recording session. */
    override fun pauseSession() = controller.pause()

    /** Stops the recording session. */
    override fun stopSession() = controller.stop()

    /** Saves a snapshot of the current buffer and records the outcome. */
    override fun triggerManualSave() {
        if (!controller.state.value.canRewind) {
            lastSaveSuccess.value = false
            return
        }
        controller.rewind(TriggerType.MANUAL)
        lastSaveSuccess.value = true
    }

    /** Persists the requested buffer window length via [AppSettings]. */
    override fun setBufferDurationSeconds(seconds: Int) = settings.setBufferDurationSeconds(seconds)
}
