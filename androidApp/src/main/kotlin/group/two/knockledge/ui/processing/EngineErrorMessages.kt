package group.two.knockledge.ui.processing

import android.content.Context
import androidx.annotation.StringRes
import group.two.knockledge.R
import group.two.knockledge.ai.EngineError
import group.two.knockledge.models.ModelCatalog

/** Maps [EngineError] failures to the user-facing string resources they surface as. */
object EngineErrorMessages {

    /** The string resource [error] should show. */
    @StringRes
    fun resId(error: EngineError): Int = when (error) {
        is EngineError.ModelMissing -> R.string.engine_error_model_missing
        is EngineError.ModelLoadFailed -> R.string.engine_error_load_failed
        EngineError.UnsupportedDevice -> R.string.engine_error_unsupported_device
        EngineError.OutOfMemory -> R.string.engine_error_out_of_memory
    }

    /** Format arguments for [resId]: the model's label for [EngineError.ModelMissing]. */
    fun formatArgs(error: EngineError): Array<out Any> = when (error) {
        is EngineError.ModelMissing -> arrayOf(modelLabel(error.modelId))
        else -> emptyArray()
    }

    /** The fully resolved message for [error]. */
    fun message(context: Context, error: EngineError): String =
        context.getString(resId(error), *formatArgs(error))

    /**
     * [key] is a catalog id, a file name, or a file name without extension depending on
     * which layer reported the failure.
     */
    private fun modelLabel(key: String): String =
        ModelCatalog.all.firstOrNull {
            it.id == key || it.fileName == key || it.fileName.substringBeforeLast('.') == key
        }?.label ?: key
}
