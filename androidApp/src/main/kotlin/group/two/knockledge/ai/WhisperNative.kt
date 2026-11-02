package group.two.knockledge.ai

/**
 * JNI binding to the whisper.cpp shared library built from `src/main/cpp`.
 * A failed [System.loadLibrary] means this device cannot run offline speech and
 * surfaces as [EngineError.UnsupportedDevice] from [ensureLoaded].
 */
object WhisperNative {
    private const val LIBRARY_NAME = "knockledge_whisper"

    private val loaded: Boolean = try {
        System.loadLibrary(LIBRARY_NAME)
        true
    } catch (_: UnsatisfiedLinkError) {
        false
    }

    /** Throws [EngineError.UnsupportedDevice] when the native library is not loadable. */
    fun ensureLoaded() {
        if (!loaded) throw EngineError.UnsupportedDevice
    }

    /** Loads the ggml Whisper model at [modelPath]; returns 0 when the file cannot be loaded. */
    external fun initContext(modelPath: String): Long

    /**
     * Transcribes 16 kHz mono float [pcm] with the model behind [ctx]. [language] is a Whisper
     * language code or "auto", [prompt] biases spelling, and [vadModelPath] points to the
     * Silero VAD file, or null to transcribe without VAD.
     */
    external fun transcribe(
        ctx: Long,
        pcm: FloatArray,
        language: String,
        prompt: String,
        vadModelPath: String?,
        threads: Int,
    ): String

    /** Releases the model behind [ctx]. */
    external fun freeContext(ctx: Long)
}
