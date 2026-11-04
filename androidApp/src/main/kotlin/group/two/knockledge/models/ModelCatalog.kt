package group.two.knockledge.models

/** One downloadable model file. */
data class ModelFile(
    val id: String,
    val label: String,
    val url: String,
    val fileName: String,
    val approxBytes: Long,
)

/**
 * Model choices, all free and from Hugging Face. Shared by Android and iOS.
 *
 * - Speech: Whisper large-v3-turbo (q5_0) is the default; small/base/tiny (q5_1) are
 *   smaller and faster but much weaker on Filipino.
 * - Voice activity: Silero VAD, so silence is skipped instead of hallucinated.
 * - Summary: Qwen3.5 `.litertlm` from litert-community, run by LiteRT-LM.
 *   2B on devices with about 4 GB RAM or more, 0.8B below that.
 */
object ModelCatalog {
    val whisper = ModelFile(
        id = "whisper",
        label = "Whisper large-v3-turbo",
        url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-large-v3-turbo-q5_0.bin",
        fileName = "ggml-large-v3-turbo-q5_0.bin",
        approxBytes = 574_000_000L,
    )

    val whisperSmall = ModelFile(
        id = "whisper_small",
        label = "Whisper small",
        url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small-q5_1.bin",
        fileName = "ggml-small-q5_1.bin",
        approxBytes = 181_000_000L,
    )

    val whisperBase = ModelFile(
        id = "whisper_base",
        label = "Whisper base",
        url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base-q5_1.bin",
        fileName = "ggml-base-q5_1.bin",
        approxBytes = 57_000_000L,
    )

    val whisperTiny = ModelFile(
        id = "whisper_tiny",
        label = "Whisper tiny",
        url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny-q5_1.bin",
        fileName = "ggml-tiny-q5_1.bin",
        approxBytes = 31_000_000L,
    )

    val vad = ModelFile(
        id = "vad",
        label = "Silero VAD",
        url = "https://huggingface.co/ggml-org/whisper-vad/resolve/main/ggml-silero-v6.2.0.bin",
        fileName = "ggml-silero-v6.2.0.bin",
        approxBytes = 885_000L,
    )

    val qwen2b = ModelFile(
        id = "qwen2b",
        label = "Qwen3.5 2B",
        url = "https://huggingface.co/litert-community/Qwen3.5-2B/resolve/main/Qwen3.5-2B_int8.litertlm",
        fileName = "Qwen3.5-2B_int8.litertlm",
        approxBytes = 2_120_000_000L,
    )

    val qwen08b = ModelFile(
        id = "qwen08b",
        label = "Qwen3.5 0.8B",
        url = "https://huggingface.co/litert-community/Qwen3.5-0.8B/resolve/main/Qwen3.5-0.8B_int8.litertlm",
        fileName = "Qwen3.5-0.8B_int8.litertlm",
        approxBytes = 965_000_000L,
    )

    val all: List<ModelFile> = listOf(whisper, whisperSmall, whisperBase, whisperTiny, vad, qwen2b, qwen08b)
    val whisperModels: List<ModelFile> = listOf(whisper, whisperSmall, whisperBase, whisperTiny)
    val qwenModels: List<ModelFile> = listOf(qwen2b, qwen08b)

    /** A "4 GB" device reports roughly 3.5 to 3.8 GB of total memory. */
    fun summarizerFor(totalRamBytes: Long): ModelFile =
        if (totalRamBytes >= 3_400_000_000L) qwen2b else qwen08b

    /** Looks up a downloadable Qwen model by [ModelFile.id]; null for unknown ids. */
    fun qwenById(id: String): ModelFile? = qwenModels.firstOrNull { it.id == id }

    /** Looks up a Whisper model by [ModelFile.id]; unknown ids fall back to the default [whisper]. */
    fun whisperById(id: String): ModelFile = whisperModels.firstOrNull { it.id == id } ?: whisper
}

/** Ids for the summary model setting. Qwen ids match [ModelFile.id]. */
object SummaryModelIds {
    const val QWEN_2B = "qwen2b"
    const val QWEN_08B = "qwen08b"
}

