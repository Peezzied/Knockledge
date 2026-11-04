package group.two.knockledge.ai

import group.two.knockledge.models.ModelCatalog
import group.two.knockledge.models.ModelFiles
import group.two.knockledge.models.SummaryModelIds
import group.two.knockledge.pipeline.PlaceholderSummarizer
import group.two.knockledge.pipeline.PromptProvider
import group.two.knockledge.pipeline.Summaries
import group.two.knockledge.pipeline.Summarizer
import group.two.knockledge.pipeline.SummarizerPrompt
import group.two.knockledge.pipeline.Summary
import group.two.knockledge.pipeline.SummaryParser
import group.two.knockledge.pipeline.SummaryRequest
import group.two.knockledge.pipeline.TranscriptCleaner
import group.two.knockledge.pipeline.TranscriptionHints
import group.two.knockledge.settings.SummaryLanguages
import group.two.knockledge.settings.TranscriptionLanguages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


/**
 * whisper.cpp, one call per rewind. Implementations load the model, transcribe,
 * free it, and throw [EngineError] on failure.
 */
interface SpeechEngine {
    /**
     * Returns the transcript of [pcm]. [language] is one of [TranscriptionLanguages], [prompt]
     * biases spelling, and [vadModelPath] trims silence when the file exists.
     */
    fun transcribe(
        modelPath: String,
        pcm: ShortArray,
        language: String,
        prompt: String,
        vadModelPath: String?,
        threads: Int,
    ): String
}

/**
 * LiteRT-LM, one completion per rewind. The runtime applies the model's own chat template,
 * so the engine takes the system and user text separately. Implementations create a fresh
 * engine and conversation per call, close both before returning, and throw [EngineError]
 * on failure.
 */
interface TextEngine {
    /**
     * Returns the completion for [userPrompt] under [systemPrompt], at most [maxTokens]
     * new tokens, sampling set by [temperature], [topP] and [topK].
     */
    fun complete(
        modelPath: String,
        systemPrompt: String,
        userPrompt: String,
        maxTokens: Int,
        temperature: Float,
        topP: Float,
        topK: Int,
    ): String
}

/** Use the big cores; more threads than that just fight each other on phones. */
fun inferenceThreads(cpuCores: Int): Int = (cpuCores / 2).coerceIn(2, 4)

/**
 * Whisper large-v3-turbo with Silero VAD. Language comes from Settings: Filipino (with a
 * Taglish prompt), English, or auto-detect.
 * Loaded per rewind and freed right after, so it never sits in memory next to Qwen.
 */
class OnDeviceTranscriber(
    private val engine: SpeechEngine,
    private val files: ModelFiles,
    private val threads: Int,
    private val language: () -> String = { TranscriptionLanguages.FILIPINO },
    private val taglishPrompt: String = TranscriptionHints.TAGLISH_PROMPT,
) {
    /** Returns the cleaned transcript, or an empty string when the Whisper file is not installed. */
    suspend fun transcribe(pcm: ShortArray): String =
        withContext(Dispatchers.Default) {
            val model = files.pathOf(files.transcriberModel)
                ?: return@withContext ""
            val lang = language()
            val raw = engine.transcribe(
                modelPath = model,
                pcm = pcm,
                language = lang,
                prompt = if (lang == TranscriptionLanguages.FILIPINO) taglishPrompt else "",
                vadModelPath = files.pathOf(ModelCatalog.vad),
                threads = threads,
            )
            TranscriptCleaner.clean(raw)
        }
}

/**
 * Summarizes with the model picked in Settings: a litert-community Qwen3.5 `.litertlm`
 * (the default). The recap language comes from Settings too.
 */
class OnDeviceSummarizer(
    private val engine: TextEngine,
    private val files: ModelFiles,
    private val threads: Int,
    private val selected: () -> String = { files.summarizerModel?.id ?: SummaryModelIds.QWEN_2B },
    private val fallback: Summarizer = PlaceholderSummarizer(),
    private val outputLanguage: () -> String = { SummaryLanguages.AUTO },
    private val promptProvider: PromptProvider = SummarizerPrompt,
) : Summarizer {
    /**
     * Falls back to [fallback] when Whisper or a summarizer file is missing.
     */
    override suspend fun summarize(request: SummaryRequest): Summary = withContext(Dispatchers.Default) {
        val language = outputLanguage()
        // Without Whisper there is no transcript to summarize: show the setup placeholder.
        if (files.pathOf(files.transcriberModel) == null) return@withContext fallback.summarize(request)
        if (request.filteredTranscript.isBlank()) return@withContext Summaries.nothingHeard(request.redactedCount, language)

        val qwen = files.summarizerModel ?: return@withContext fallback.summarize(request)
        val path = files.pathOf(qwen) ?: return@withContext fallback.summarize(request)
        runQwen(path, request, language)
    }

    private fun runQwen(modelPath: String, request: SummaryRequest, language: String): Summary {
        val raw = engine.complete(
            modelPath = modelPath,
            systemPrompt = promptProvider.summarizerSystem(language),
            userPrompt = promptProvider.summarizerUser(request, outputLanguage = language),
            maxTokens = MAX_TOKENS,
            // Qwen3.5 non-thinking settings, a little cooler for factual recaps.
            temperature = 0.7f,
            topP = 0.8f,
            topK = 20,
        )
        return SummaryParser.parse(raw, request.redactedCount)
    }

    private companion object {
        const val MAX_TOKENS = 320
    }
}

