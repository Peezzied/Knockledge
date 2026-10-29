package group.two.knockledge.ui

import group.two.knockledge.RewinderController
import group.two.knockledge.RewinderState
import group.two.knockledge.data.TriggerType
import group.two.knockledge.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

interface RecordingBackend {
    val state: StateFlow<RewinderState>
    fun start()
    fun stop()
    fun save(): Boolean
}

fun RewinderController.asBackend(): RecordingBackend = object : RecordingBackend {
    override val state = this@asBackend.state
    override fun start() = this@asBackend.start()
    override fun stop() = this@asBackend.stop()
    override fun save(): Boolean {
        if (!state.value.canRewind) return false
        rewind(TriggerType.MANUAL)
        return true
    }
}

class KnockRecordingViewModel(
    private val backend: RecordingBackend,
    private val settings: AppSettings,
    scope: CoroutineScope,
) : KnockRecordingController {
    private val lastSaveSuccess = MutableStateFlow<Boolean?>(null)

    override val uiState: StateFlow<KnockRecordingUiState> =
        combine(backend.state, settings.bufferDurationSeconds, lastSaveSuccess) { rec, max, saved ->
            KnockRecordingUiState(
                isRecording = rec.isRunning,
                bufferedSeconds = (rec.bufferedMillis / 1000L).toInt(),
                maxBufferSeconds = max,
                isGestureActive = rec.isRunning,
                isSavingSnapshot = rec.pendingRewinds > 0,
                lastSaveSuccess = saved,
            )
        }.stateIn(scope, SharingStarted.Eagerly, KnockRecordingUiState())

    override fun startSession() = backend.start()

    override fun stopSession() = backend.stop()

    override fun triggerManualSave() {
        lastSaveSuccess.value = backend.save()
    }

    override fun setBufferDurationSeconds(seconds: Int) = settings.setBufferDurationSeconds(seconds)
}
