package group.two.knockledge.ai

import group.two.knockledge.models.ModelEntry
import group.two.knockledge.models.ModelFile
import group.two.knockledge.models.ModelStore
import group.two.knockledge.settings.AppSettings

/** What Settings and the model picker read and change. Shared by both apps. */
/**
 * Controls AI-related settings and interactions.
 */
class AiControls(
    val settings: AppSettings,
    private val models: ModelStore,
    private val system: SystemModelEngine?,
    private val totalRamBytes: Long,
    /** False where the platform cannot capture its own playback. */
    val supportsDeviceAudio: Boolean = false,
) {
    /** Whether automatic AI processing runs immediately when saving an audio buffer. */
    val autoAiProcessingEnabled = settings.autoAiProcessingEnabled

    /** Sets the automatic AI processing preference. */
    fun setAutoAiProcessingEnabled(enabled: Boolean) = settings.setAutoAiProcessingEnabled(enabled)

    /** Saves [id] as the summary model and re-reads which model files are installed. */
    /**
 * Selects the summary model by its ID and refreshes the model store.
 */
fun selectSummaryModel(id: String) {
        settings.setSummaryModel(id)
        models.refresh()
    }

    /** Saves [id] as the transcription model and re-reads which model files are installed. */
    fun selectTranscriptionModel(id: String) {
        settings.setTranscriptionLocalModel(id)
        models.refresh()
    }

    /** Every catalogue model with its install state, for the storage list in Settings. */
    /**
 * Returns all model entries with their installation state.
 */
fun modelEntries(): List<ModelEntry> = models.entries()

    /** Deletes [file]'s download, partial or complete, and re-reads what is stored. */
    /**
 * Removes the specified model file from storage.
 */
fun removeModel(file: ModelFile) = models.remove(file)

    companion object {
        /** Renders [bytes] as "N MB" or "N.N GB" for the picker's detail line. */
        fun gigabytes(bytes: Long): String {
            val mb = bytes / 1_000_000L
            return if (mb >= 1000) "${mb / 1000}.${(mb % 1000) / 100} GB" else "$mb MB"
        }
    }
}

