package two.knockledge.models

import group.two.knockledge.models.ModelCatalog
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ModelCatalogFormatTest {

    @Test
    fun qwenModelsAreLitertlmFromLitertCommunity() {
        for (model in ModelCatalog.qwenModels) {
            assertTrue(model.fileName.endsWith(".litertlm"), "${model.id} fileName: ${model.fileName}")
            assertTrue(
                model.url.startsWith("https://huggingface.co/litert-community/"),
                "${model.id} url: ${model.url}",
            )
        }
    }

    @Test
    fun whisperModelsAreGgmlBins() {
        for (model in ModelCatalog.whisperModels) {
            assertTrue(model.fileName.endsWith(".bin"), "${model.id} fileName: ${model.fileName}")
        }
    }

    @Test
    fun catalogContainsNoGguf() {
        for (model in ModelCatalog.all) {
            assertFalse(model.fileName.endsWith(".gguf"), "${model.id} fileName: ${model.fileName}")
        }
    }
}
