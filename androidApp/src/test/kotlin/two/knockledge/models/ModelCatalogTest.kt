package two.knockledge.models

import group.two.knockledge.models.ModelCatalog
import group.two.knockledge.models.ModelDownloader
import group.two.knockledge.models.ModelStorage
import group.two.knockledge.models.ModelStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ModelCatalogTest {

    @Test
    fun whisperByIdReturnsKnownModel() {
        assertSame(ModelCatalog.whisperTiny, ModelCatalog.whisperById("whisper_tiny"))
        assertSame(ModelCatalog.whisper, ModelCatalog.whisperById("whisper"))
    }

    @Test
    fun whisperByIdFallsBackToDefaultForUnknownId() {
        assertSame(ModelCatalog.whisper, ModelCatalog.whisperById("ggml-not-a-model"))
    }
}

class ModelStoreTranscriptionModelTest {

    private class FakeStorage(private val sizes: MutableMap<String, Long>) : ModelStorage {
        override val directory: String = "/models"

        override fun sizeOf(path: String): Long = sizes[path] ?: -1L
        override fun move(from: String, to: String): Boolean = true
        override fun delete(path: String): Boolean = true
    }

    private class FakeDownloader : ModelDownloader {
        override suspend fun download(
            url: String,
            partPath: String,
            existingBytes: Long,
            onProgress: (writtenBytes: Long) -> Unit,
        ) = Unit
    }

    private fun store(storage: FakeStorage, selected: () -> String): ModelStore =
        ModelStore(
            storage,
            FakeDownloader(),
            selectedSummaryModel = { ModelCatalog.qwen2b.id },
            selectedTranscriptionModel = selected,
        )

    @Test
    fun requiredFilesFollowSelectedWhisperModel() {
        val storage = FakeStorage(
            mutableMapOf(
                "/models/${ModelCatalog.whisperTiny.fileName}" to 100L,
                "/models/${ModelCatalog.vad.fileName}" to 100L,
                "/models/${ModelCatalog.qwen2b.fileName}" to 100L,
            ),
        )
        val store = store(storage) { ModelCatalog.whisperTiny.id }

        // The default turbo model is absent, but the selected tiny model is installed.
        assertEquals(ModelCatalog.whisperTiny, store.transcriberModel)
        assertTrue(store.isReady)

        val entries = store.entries().associateBy { it.file.id }
        assertTrue(entries.getValue(ModelCatalog.whisperTiny.id).needed)
        assertFalse(entries.getValue(ModelCatalog.whisper.id).needed)
    }

    @Test
    fun refreshPicksUpSelectionChange() {
        var selected = ModelCatalog.whisper.id
        val storage = FakeStorage(
            mutableMapOf(
                "/models/${ModelCatalog.whisper.fileName}" to 100L,
                "/models/${ModelCatalog.vad.fileName}" to 100L,
                "/models/${ModelCatalog.qwen2b.fileName}" to 100L,
            ),
        )
        val store = store(storage) { selected }

        assertTrue(store.state.value.ready)

        selected = ModelCatalog.whisperBase.id
        store.refresh()

        assertFalse(store.state.value.ready)
    }
}
