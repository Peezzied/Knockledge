package two.knockledge.ui.settings

import group.two.knockledge.ai.AiControls
import group.two.knockledge.models.ModelCatalog
import group.two.knockledge.models.ModelDownloader
import group.two.knockledge.models.ModelProvider
import group.two.knockledge.models.ModelStorage
import group.two.knockledge.models.ModelStore
import group.two.knockledge.models.ModelsState
import group.two.knockledge.settings.AppSettings
import group.two.knockledge.settings.MemoryStore
import group.two.knockledge.ui.settings.ModelManagementViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModelManagementViewModelTest {

    private class FakeProvider : ModelProvider {
        val stateFlow = MutableStateFlow(ModelsState(ready = false))
        var downloadCalls = 0

        override val state: StateFlow<ModelsState> = stateFlow
        override val description: String = "fake"

        override fun download() {
            downloadCalls++
        }

        override fun cancel() = Unit
    }

    private class FakeStorage(private val sizes: Map<String, Long>) : ModelStorage {
        val deleted = mutableListOf<String>()
        override val directory: String = "/models"

        override fun sizeOf(path: String): Long = sizes[path] ?: -1L
        override fun move(from: String, to: String): Boolean = true
        override fun delete(path: String): Boolean {
            deleted += path
            return true
        }
    }

    private class FakeDownloader : ModelDownloader {
        override suspend fun download(
            url: String,
            partPath: String,
            existingBytes: Long,
            onProgress: (writtenBytes: Long) -> Unit,
        ) = Unit
    }

    private val scope = CoroutineScope(Dispatchers.Unconfined)

    private fun aiControls(storage: ModelStorage): AiControls {
        val settings = AppSettings(MemoryStore(), ModelCatalog.qwen2b.id)
        val store = ModelStore(storage, FakeDownloader(), selectedSummaryModel = { settings.summaryModel.value })
        return AiControls(settings, store, system = null, totalRamBytes = 4_000_000_000L)
    }

    @Test
    fun nullDependenciesExposeReadyStateAndDefaultIds() {
        val vm = ModelManagementViewModel(models = null, ai = null, scope = scope)

        assertTrue(vm.modelsState.value.ready)
        assertEquals(
            ModelCatalog.all.map { it.id }.toSet(),
            vm.installedModelIds.value,
        )
    }

    @Test
    fun installedModelIdsReflectModelEntries() {
        val storage = FakeStorage(
            mapOf(
                "/models/${ModelCatalog.whisper.fileName}" to 100L,
                "/models/${ModelCatalog.vad.fileName}" to 100L,
            ),
        )
        val vm = ModelManagementViewModel(FakeProvider(), aiControls(storage), scope)

        assertEquals(setOf(ModelCatalog.whisper.id, ModelCatalog.vad.id), vm.installedModelIds.value)
    }

    @Test
    fun delegatesToProviderAndControls() {
        val provider = FakeProvider()
        val storage = FakeStorage(mapOf("/models/${ModelCatalog.qwen2b.fileName}" to 100L))
        val ai = aiControls(storage)
        val vm = ModelManagementViewModel(provider, ai, scope)

        vm.downloadModels()
        assertEquals(1, provider.downloadCalls)

        vm.removeModel(ModelCatalog.qwen2b)
        assertTrue(storage.deleted.any { it.contains(ModelCatalog.qwen2b.fileName) })

        vm.selectSummaryModel(ModelCatalog.qwen08b.id)
        assertEquals(ModelCatalog.qwen08b.id, ai.settings.summaryModel.value)
    }
}
