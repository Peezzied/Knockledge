package two.knockledge.pipeline

import group.two.knockledge.R
import group.two.knockledge.ai.EngineError
import group.two.knockledge.models.ModelCatalog
import group.two.knockledge.ui.processing.EngineErrorMessages
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EngineErrorMappingTest {

    @Test
    fun everyEngineErrorMapsToAStringResource() {
        val cases = mapOf(
            EngineError.ModelMissing("whisper") to R.string.engine_error_model_missing,
            EngineError.ModelLoadFailed("qwen2b") to R.string.engine_error_load_failed,
            EngineError.UnsupportedDevice to R.string.engine_error_unsupported_device,
            EngineError.OutOfMemory to R.string.engine_error_out_of_memory,
        )
        for ((error, expectedRes) in cases) {
            assertEquals(expectedRes, EngineErrorMessages.resId(error), "$error")
        }
    }

    @Test
    fun modelMissingMessageNamesTheModel() {
        for (key in listOf(
            ModelCatalog.whisper.id,
            ModelCatalog.whisper.fileName,
            ModelCatalog.qwen2b.fileName.substringBeforeLast('.'),
        )) {
            val args = EngineErrorMessages.formatArgs(EngineError.ModelMissing(key))
            assertEquals(1, args.size)
            assertTrue(args[0].toString().isNotBlank())
        }
        assertEquals(
            ModelCatalog.whisper.label,
            EngineErrorMessages.formatArgs(EngineError.ModelMissing(ModelCatalog.whisper.id))[0],
        )
    }
}
