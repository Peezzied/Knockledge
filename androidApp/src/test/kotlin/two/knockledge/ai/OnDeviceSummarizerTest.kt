package two.knockledge.ai

import group.two.knockledge.ai.OnDeviceSummarizer
import group.two.knockledge.ai.TextEngine
import group.two.knockledge.models.ModelCatalog
import group.two.knockledge.models.ModelFile
import group.two.knockledge.models.ModelFiles
import group.two.knockledge.pipeline.PromptProvider
import group.two.knockledge.pipeline.Summarizer
import group.two.knockledge.pipeline.Summary
import group.two.knockledge.pipeline.SummaryRequest
import group.two.knockledge.settings.SummaryLanguages
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OnDeviceSummarizerTest {

    private class FakeFiles(
        private val installed: Map<String, String>,
        override val transcriberModel: ModelFile = ModelCatalog.whisper,
        override val summarizerModel: ModelFile? = ModelCatalog.qwen2b,
    ) : ModelFiles {
        override val isReady: Boolean get() = true
        override fun pathOf(model: ModelFile): String? = installed[model.fileName]
    }

    private class FakeEngine(private val result: String) : TextEngine {
        var calls = 0
        var lastModelPath: String? = null
        var lastSystemPrompt: String? = null
        var lastUserPrompt: String? = null

        override fun complete(
            modelPath: String,
            systemPrompt: String,
            userPrompt: String,
            maxTokens: Int,
            temperature: Float,
            topP: Float,
            topK: Int,
        ): String {
            calls++
            lastModelPath = modelPath
            lastSystemPrompt = systemPrompt
            lastUserPrompt = userPrompt
            return result
        }
    }

    private class FakePrompts : PromptProvider {
        override val geminiTranscribePrompt: String = "GEMINI"
        override val taglishPrompt: String = "TAGLISH"
        override fun summarizerSystem(outputLanguage: String): String = "SYS-$outputLanguage"
        override fun summarizerUser(request: SummaryRequest, maxChars: Int, outputLanguage: String): String =
            "USER-$outputLanguage:${request.filteredTranscript}"
    }

    private class FakeFallback : Summarizer {
        var calls = 0
        override suspend fun summarize(request: SummaryRequest): Summary {
            calls++
            return Summary("fallback", "fallback gist", emptyList(), request.redactedCount)
        }
    }

    private val qwenPath = "/models/${ModelCatalog.qwen2b.fileName}"

    private fun files(
        summarizerModel: ModelFile? = ModelCatalog.qwen2b,
        installed: Map<String, String> = mapOf(
            ModelCatalog.whisper.fileName to "/models/${ModelCatalog.whisper.fileName}",
            ModelCatalog.vad.fileName to "/models/${ModelCatalog.vad.fileName}",
            ModelCatalog.qwen2b.fileName to qwenPath,
        ),
    ) = FakeFiles(installed, summarizerModel = summarizerModel)

    private fun request(transcript: String = "Nag-usap kami tungkol sa proyekto bukas.") =
        SummaryRequest(transcript, false, 0, 30_000L)

    private val engineResult = "TITLE: Meeting\nGIST: They discussed the project.\n- Point one"

    @Test
    fun passesSystemAndUserPromptsPerLanguage() = runBlocking {
        for (lang in listOf(SummaryLanguages.AUTO, SummaryLanguages.FILIPINO, SummaryLanguages.ENGLISH)) {
            val engine = FakeEngine(engineResult)
            val s = OnDeviceSummarizer(
                engine = engine,
                files = files(),
                threads = 3,
                outputLanguage = { lang },
                promptProvider = FakePrompts(),
            )
            val summary = s.summarize(request())
            assertEquals(1, engine.calls)
            assertEquals("SYS-$lang", engine.lastSystemPrompt)
            assertTrue(engine.lastUserPrompt!!.startsWith("USER-$lang:"))
            assertEquals("Meeting", summary.title)
            assertEquals("They discussed the project.", summary.gist)
        }
    }

    @Test
    fun blankTranscriptReturnsNothingHeardAndSkipsEngine() = runBlocking {
        val engine = FakeEngine(engineResult)
        val s = OnDeviceSummarizer(
            engine = engine,
            files = files(),
            threads = 3,
            outputLanguage = { SummaryLanguages.ENGLISH },
            promptProvider = FakePrompts(),
        )
        val summary = s.summarize(request("   "))
        assertEquals("Nothing clear heard", summary.title)
        assertEquals(0, engine.calls)
    }

    @Test
    fun missingSummarizerModelRunsFallback() = runBlocking {
        val engine = FakeEngine(engineResult)
        val fallback = FakeFallback()

        // Selected model not installed.
        var s = OnDeviceSummarizer(
            engine = engine,
            files = files(installed = mapOf(ModelCatalog.whisper.fileName to "/models/${ModelCatalog.whisper.fileName}")),
            threads = 3,
            fallback = fallback,
            promptProvider = FakePrompts(),
        )
        s.summarize(request())
        assertEquals(1, fallback.calls)
        assertEquals(0, engine.calls)

        // No summarizer model selected at all.
        s = OnDeviceSummarizer(
            engine = engine,
            files = files(summarizerModel = null),
            threads = 3,
            fallback = fallback,
            promptProvider = FakePrompts(),
        )
        s.summarize(request())
        assertEquals(2, fallback.calls)
        assertEquals(0, engine.calls)
    }
}
