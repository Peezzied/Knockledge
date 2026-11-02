package group.two.knockledge.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import group.two.knockledge.ai.AiControls
import group.two.knockledge.models.ModelCatalog
import group.two.knockledge.models.ModelFile
import group.two.knockledge.models.ModelProvider
import group.two.knockledge.models.ModelsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel bridging the settings screen to [ModelProvider] downloads and [AiControls] model actions.
 * Both dependencies are nullable because platforms may ship without on-device models.
 */
class ModelManagementViewModel(
    private val models: ModelProvider?,
    private val ai: AiControls?,
    scope: CoroutineScope? = null,
) : ViewModel(), ModelManagementController {

    private val coroutineScope = scope ?: viewModelScope

    /** Live model download state, or a ready placeholder when the platform has no models. */
    override val modelsState: StateFlow<ModelsState> =
        models?.state ?: MutableStateFlow(ModelsState(ready = true))

    /** Installed model ids, recomputed whenever [modelsState] changes. */
    override val installedModelIds: StateFlow<Set<String>> =
        modelsState.map { installedIds() }
            .stateIn(coroutineScope, SharingStarted.Eagerly, installedIds())

    /** Delegates summary model selection to [AiControls]. */
    override fun selectSummaryModel(id: String) {
        ai?.selectSummaryModel(id)
    }

    /** Delegates transcription model selection to [AiControls]. */
    override fun selectTranscriptionModel(id: String) {
        ai?.selectTranscriptionModel(id)
    }

    /** Starts the model download via [ModelProvider]. */
    override fun downloadModels() {
        models?.download()
    }

    /** Removes [file] via [AiControls]. */
    override fun removeModel(file: ModelFile) {
        ai?.removeModel(file)
    }

    private fun installedIds(): Set<String> =
        ai?.modelEntries().orEmpty()
            .filter { it.installed }
            .map { it.file.id }
            .toSet()
            .ifEmpty { DEFAULT_INSTALLED_MODEL_IDS }

    companion object {
        /** Assumed installed ids when no entries are reported, for example on desktop previews. */
        private val DEFAULT_INSTALLED_MODEL_IDS = ModelCatalog.all.map { it.id }.toSet()
    }
}
