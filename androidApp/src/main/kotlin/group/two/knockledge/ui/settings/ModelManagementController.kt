package group.two.knockledge.ui.settings

import group.two.knockledge.models.ModelFile
import group.two.knockledge.models.ModelsState
import kotlinx.coroutines.flow.StateFlow

/**
 * Contract for on-device AI model download state and management from the settings screen.
 */
interface ModelManagementController {
    /** Download / readiness state of the on-device models. */
    val modelsState: StateFlow<ModelsState>

    /** Ids of installed model files, or the assumed defaults when none are reported. */
    val installedModelIds: StateFlow<Set<String>>

    /** Saves [id] as the summary model and refreshes install state. */
    fun selectSummaryModel(id: String)

    /** Downloads the model files the current settings need. */
    fun downloadModels()

    /** Removes [file]'s download, partial or complete. */
    fun removeModel(file: ModelFile)
}
