package group.two.knockledge.ui.processing

import group.two.knockledge.pipeline.NoteProcessingUiState
import kotlinx.coroutines.flow.StateFlow

/**
 * Controller interface for triggering on-demand transcription and summarization from the UI.
 */
interface NoteProcessingController {
    /**
     * Current AI processing state for the active or selected note.
     */
    val processingState: StateFlow<NoteProcessingUiState>

    /**
     * Initiates transcription and summarization for the specified note.
     *
     * @param noteId Unique identifier of the note with captured audio.
     */
    fun processNote(noteId: String)

    /**
     * Cancels in-flight processing; no transcript or summary is attached.
     */
    fun stopProcessing()

    /**
     * Resets or clears the current processing state.
     */
    fun resetState()
}
