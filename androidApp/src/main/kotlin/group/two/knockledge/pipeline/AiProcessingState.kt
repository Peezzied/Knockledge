package group.two.knockledge.pipeline

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Stages of on-demand AI processing for a note.
 */
enum class NoteAiProcessingState {
    /** Note has audio but no AI processing is in flight. */
    IDLE,

    /** Audio is currently being processed by speech recognition. */
    TRANSCRIBING,

    /** Transcript is being processed by the language model. */
    SUMMARIZING,

    /** Processing succeeded; transcript and summary are attached. */
    COMPLETED,

    /** Processing encountered an error; retry is permitted. */
    FAILED,

    /** Processing was skipped (e.g. auto AI disabled or audio skipped). */
    SKIPPED
}

/**
 * State representing on-demand AI processing progress for a note.
 */
data class NoteProcessingUiState(
    val noteId: String? = null,
    val isProcessing: Boolean = false,
    val stage: RewindStage? = null,
    val state: NoteAiProcessingState = NoteAiProcessingState.IDLE,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
)

/**
 * Single source of truth for AI processing state.
 *
 * Both producers — the automatic rewind path in `KnockledgeController` and the
 * on-demand path in `NoteProcessingViewModel` — publish here, so every UI surface
 * reacts to whichever path is running.
 */
class AiProcessingTracker {

    private val _state = MutableStateFlow(NoteProcessingUiState())

    /** Current AI processing state, collected by the UI. */
    val state: StateFlow<NoteProcessingUiState> = _state.asStateFlow()

    /** Replaces the current state. */
    fun set(next: NoteProcessingUiState) {
        _state.value = next
    }

    /** Atomically transforms the current state. */
    fun update(transform: (NoteProcessingUiState) -> NoteProcessingUiState) {
        _state.update(transform)
    }
}
