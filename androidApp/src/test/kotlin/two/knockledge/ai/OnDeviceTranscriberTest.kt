package two.knockledge.ai

import group.two.knockledge.ai.OnDeviceTranscriber
import group.two.knockledge.ai.SpeechEngine
import group.two.knockledge.models.ModelCatalog
import group.two.knockledge.models.ModelFile
import group.two.knockledge.models.ModelFiles
import group.two.knockledge.settings.TranscriptionLanguages
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OnDeviceTranscriberTest {

    private class FakeFiles(
        private val installed: Map<String, String>,
        override val transcriberModel: ModelFile = ModelCatalog.whisper,
        override val summarizerModel: ModelFile? = ModelCatalog.qwen2b,
    ) : ModelFiles {
        override val isReady: Boolean get() = true
        override fun pathOf(model: ModelFile): String? = installed[model.fileName]
    }

    private class FakeEngine(private val result: String) : SpeechEngine {
        var calls = 0
        var lastModelPath: String? = null
        var lastLanguage: String? = null
        var lastPrompt: String? = null
        var lastVadModelPath: String? = null
        var lastThreads: Int = -1

        override fun transcribe(
            modelPath: String,
            pcm: ShortArray,
            language: String,
            prompt: String,
            vadModelPath: String?,
            threads: Int,
        ): String {
            calls++
            lastModelPath = modelPath
            lastLanguage = language
            lastPrompt = prompt
            lastVadModelPath = vadModelPath
            lastThreads = threads
            return result
        }
    }

    private val whisperPath = "/models/${ModelCatalog.whisper.fileName}"
    private val vadPath = "/models/${ModelCatalog.vad.fileName}"

    private fun files(
        transcriberModel: ModelFile = ModelCatalog.whisper,
        installed: Map<String, String> = mapOf(
            ModelCatalog.whisper.fileName to whisperPath,
            ModelCatalog.whisperSmall.fileName to "/models/${ModelCatalog.whisperSmall.fileName}",
            ModelCatalog.vad.fileName to vadPath,
        ),
    ) = FakeFiles(installed, transcriberModel)

    @Test
    fun transcribePassesModelLanguagePromptAndVadToEngine() = runBlocking {
        val engine = FakeEngine("  [music] Magandang umaga. (laughter) Magandang umaga.  ")
        val t = OnDeviceTranscriber(engine, files(), threads = 3, language = { TranscriptionLanguages.FILIPINO }, taglishPrompt = "TAGLISH")

        val text = t.transcribe(ShortArray(16000) { 100 })

        assertEquals(1, engine.calls)
        assertEquals(whisperPath, engine.lastModelPath)
        assertEquals(TranscriptionLanguages.FILIPINO, engine.lastLanguage)
        assertEquals("TAGLISH", engine.lastPrompt)
        assertEquals(vadPath, engine.lastVadModelPath)
        assertEquals(3, engine.lastThreads)
        assertEquals("Magandang umaga.", text)
    }

    @Test
    fun nonFilipinoLanguageSendsEmptyPrompt() = runBlocking {
        val engine = FakeEngine("hello")
        val t = OnDeviceTranscriber(engine, files(), threads = 3, language = { TranscriptionLanguages.ENGLISH }, taglishPrompt = "TAGLISH")

        t.transcribe(ShortArray(16000) { 100 })

        assertEquals(TranscriptionLanguages.ENGLISH, engine.lastLanguage)
        assertEquals("", engine.lastPrompt)
    }

    @Test
    fun missingModelFileReturnsEmptyAndSkipsEngine() = runBlocking {
        val engine = FakeEngine("should not run")
        val t = OnDeviceTranscriber(engine, files(installed = emptyMap()), threads = 3)

        val text = t.transcribe(ShortArray(16000) { 100 })

        assertEquals("", text)
        assertEquals(0, engine.calls)
    }

    @Test
    fun selectedWhisperModelIsTheOneSentToTheEngine() = runBlocking {
        val engine = FakeEngine("ok")
        val t = OnDeviceTranscriber(
            engine,
            files(transcriberModel = ModelCatalog.whisperSmall),
            threads = 3,
        )

        t.transcribe(ShortArray(16000) { 100 })

        assertEquals("/models/${ModelCatalog.whisperSmall.fileName}", engine.lastModelPath)
    }

    @Test
    fun missingSelectedModelDoesNotFallBackToAnotherInstalledModel() = runBlocking {
        val engine = FakeEngine("should not run")
        // whisperSmall selected but only the default whisper is installed.
        val t = OnDeviceTranscriber(
            engine,
            files(
                transcriberModel = ModelCatalog.whisperSmall,
                installed = mapOf(
                    ModelCatalog.whisper.fileName to whisperPath,
                    ModelCatalog.vad.fileName to vadPath,
                ),
            ),
            threads = 3,
        )

        val text = t.transcribe(ShortArray(16000) { 100 })

        assertEquals("", text)
        assertEquals(0, engine.calls)
    }

    @Test
    fun emptyEngineResultYieldsEmptyTranscript() = runBlocking {
        val engine = FakeEngine("")
        val t = OnDeviceTranscriber(engine, files(), threads = 3)

        val text = t.transcribe(ShortArray(16000) { 100 })

        assertEquals("", text)
        assertFalse(text.contains("Transcription complete"))
        assertFalse(text.contains("Transcribed audio segment"))
    }
}
