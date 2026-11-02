package group.two.knockledge.ai

/**
 * Failures the on-device engines throw. The pipeline maps each type to a user message and,
 * in Auto mode with a network, falls back to the cloud engine.
 */
sealed class EngineError(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /** The model file is absent, empty or truncated. [modelId] is the [ModelFile.id] to download. */
    class ModelMissing(val modelId: String) : EngineError("Model file missing: $modelId")

    /** The model file exists but the engine rejected it. */
    class ModelLoadFailed(val modelId: String, cause: Throwable? = null) :
        EngineError("Model failed to load: $modelId", cause)

    /** The device cannot run the on-device engines (not arm64, or the native library is absent). */
    object UnsupportedDevice : EngineError("Offline AI is not supported on this device")

    /** Loading or running the model ran out of memory. */
    object OutOfMemory : EngineError("The device ran out of memory running the on-device model")
}
