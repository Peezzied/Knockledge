import group.two.knockledge.KnockledgeController
import group.two.knockledge.audio.AudioSource
import group.two.knockledge.audio.InputStatus
import group.two.knockledge.audio.RingAudioCapturer
import group.two.knockledge.audio.SessionHooks
import group.two.knockledge.core.FileLocalAudioRingBuffer
import group.two.knockledge.data.TriggerType
import group.two.knockledge.data.repository.NoteSink
import group.two.knockledge.pipeline.AiProcessingTracker
import group.two.knockledge.pipeline.DefaultAudioPreprocessor
import group.two.knockledge.pipeline.NoteAiProcessingState
import group.two.knockledge.pipeline.PlaceholderSummarizer
import group.two.knockledge.pipeline.PlaceholderTranscriber
import group.two.knockledge.pipeline.RewindPipeline
import group.two.knockledge.pipeline.Summary
import group.two.knockledge.pipeline.Transcriber
import group.two.knockledge.pipeline.TranscriptResult
import group.two.knockledge.sensor.KnockGestureDetector
import group.two.knockledge.settings.AppSettings
import group.two.knockledge.settings.MemoryStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class KnockledgeControllerAutoAiTest {

    private val tempDir: File = Files.createTempDirectory("controller_test").toFile()

    private class RecordingNoteSink : NoteSink {
        var recordedAudioOnlyCount = 0
        var recordedFullCount = 0
        var attachAiCount = 0
        var attachedNoteId: String? = null
        var lastAudioOnlyNoteId: String? = null
        var lastTrigger: TriggerType? = null

        override suspend fun record(
            file: File,
            startEpochMs: Long,
            endEpochMs: Long,
            trigger: TriggerType,
            transcript: TranscriptResult,
            summary: Summary,
        ) {
            recordedFullCount++
            lastTrigger = trigger
        }

        override suspend fun recordAudioOnly(
            file: File,
            startEpochMs: Long,
            endEpochMs: Long,
            trigger: TriggerType,
        ): String? {
            recordedAudioOnlyCount++
            lastTrigger = trigger
            return "note-$recordedAudioOnlyCount".also { lastAudioOnlyNoteId = it }
        }

        override suspend fun attachAi(noteId: String, transcript: TranscriptResult, summary: Summary) {
            attachAiCount++
            attachedNoteId = noteId
        }
    }

    private class NoOpAudioSource : AudioSource {
        override fun frames(sampleRate: Int): Flow<ShortArray> = kotlinx.coroutines.flow.flow { emit(ShortArray(1_600) { 100 }) }
        override fun status(): InputStatus = InputStatus("mic", null)
    }

    private class NoOpHooks : SessionHooks {
        override fun onSessionStarted() = Unit
        override fun onSessionStopped() = Unit
    }

    private class ThrowingTranscriber : Transcriber {
        override suspend fun transcribe(pcm: ShortArray, sampleRate: Int): TranscriptResult =
            throw RuntimeException("model exploded")
    }

    private class FakeDetector : KnockGestureDetector {
        override val isListening: Boolean = false
        override fun startListening(onKnockDetected: () -> Boolean) = Unit
        override fun stopListening() = Unit
    }

    @AfterTest
    fun cleanup() {
        tempDir.deleteRecursively()
    }

    @Test
    fun rewindSavesAudioOnlyWhenAutoAiIsDisabled() = runBlocking {
        val sink = RecordingNoteSink()
        val settings = AppSettings(MemoryStore(), "qwen2b")
        settings.setAutoAiProcessingEnabled(false)

        val ringBuffer = FileLocalAudioRingBuffer(tempDir, 60, 16_000)
        ringBuffer.write(ShortArray(160_000))

        val controller = KnockledgeController(
            capturer = RingAudioCapturer(NoOpAudioSource(), ringBuffer),
            knockDetector = FakeDetector(),
            hooks = NoOpHooks(),
            pipeline = RewindPipeline(
                transcriber = PlaceholderTranscriber(),
                summarizer = PlaceholderSummarizer(),
                preprocessor = DefaultAudioPreprocessor(),
            ),
            noteSink = sink,
            autoAiProcessingEnabled = settings.autoAiProcessingEnabled,
        )

        controller.start()
        withTimeout(5_000) { while (!controller.state.value.canRewind) delay(10) }
        controller.rewind(TriggerType.GESTURE)
        withTimeout(5_000) { while (controller.state.value.pendingRewinds > 0) delay(10) }

        assertEquals(1, sink.recordedAudioOnlyCount)
        assertEquals(0, sink.recordedFullCount)
        assertEquals(TriggerType.GESTURE, sink.lastTrigger)
    }

    @Test
    fun rewindExecutesPipelineWhenAutoAiIsEnabled() = runBlocking {
        val sink = RecordingNoteSink()
        val settings = AppSettings(MemoryStore(), "qwen2b")
        settings.setAutoAiProcessingEnabled(true)

        val ringBuffer = FileLocalAudioRingBuffer(tempDir, 60, 16_000)
        ringBuffer.write(ShortArray(160_000) { 2_000 })

        val controller = KnockledgeController(
            capturer = RingAudioCapturer(NoOpAudioSource(), ringBuffer),
            knockDetector = FakeDetector(),
            hooks = NoOpHooks(),
            pipeline = RewindPipeline(
                transcriber = PlaceholderTranscriber(),
                summarizer = PlaceholderSummarizer(),
                preprocessor = DefaultAudioPreprocessor(),
            ),
            noteSink = sink,
            autoAiProcessingEnabled = settings.autoAiProcessingEnabled,
        )

        controller.start()
        withTimeout(5_000) { while (!controller.state.value.canRewind) delay(10) }
        controller.rewind(TriggerType.MANUAL)
        withTimeout(5_000) { while (controller.state.value.pendingRewinds > 0) delay(10) }

        assertEquals(1, sink.recordedAudioOnlyCount)
        assertEquals(0, sink.recordedFullCount)
        assertEquals(1, sink.attachAiCount)
        assertEquals(sink.lastAudioOnlyNoteId, sink.attachedNoteId)
        assertEquals(TriggerType.MANUAL, sink.lastTrigger)
    }

    @Test
    fun rewindSavesAudioOnlyWhenPipelineThrows() = runBlocking {
        val sink = RecordingNoteSink()
        val settings = AppSettings(MemoryStore(), "qwen2b")
        settings.setAutoAiProcessingEnabled(true)

        val ringBuffer = FileLocalAudioRingBuffer(tempDir, 60, 16_000)
        ringBuffer.write(ShortArray(160_000) { 2_000 })

        val controller = KnockledgeController(
            capturer = RingAudioCapturer(NoOpAudioSource(), ringBuffer),
            knockDetector = FakeDetector(),
            hooks = NoOpHooks(),
            pipeline = RewindPipeline(
                transcriber = ThrowingTranscriber(),
                summarizer = PlaceholderSummarizer(),
                preprocessor = DefaultAudioPreprocessor(),
            ),
            noteSink = sink,
            autoAiProcessingEnabled = settings.autoAiProcessingEnabled,
        )

        controller.start()
        withTimeout(5_000) { while (!controller.state.value.canRewind) delay(10) }
        controller.rewind(TriggerType.GESTURE)
        withTimeout(5_000) {
            while (controller.state.value.error == null && sink.recordedAudioOnlyCount == 0) delay(10)
        }

        assertEquals(1, sink.recordedAudioOnlyCount)
        assertEquals(0, sink.recordedFullCount)
        assertEquals(0, sink.attachAiCount)
    }

    @Test
    fun rewindKeepsAudioOnlyNoteOnInsufficientSpeech() = runBlocking {
        val sink = RecordingNoteSink()
        val settings = AppSettings(MemoryStore(), "qwen2b")
        settings.setAutoAiProcessingEnabled(true)

        val ringBuffer = FileLocalAudioRingBuffer(tempDir, 60, 16_000)
        ringBuffer.write(ShortArray(160_000))

        val controller = KnockledgeController(
            capturer = RingAudioCapturer(NoOpAudioSource(), ringBuffer),
            knockDetector = FakeDetector(),
            hooks = NoOpHooks(),
            pipeline = RewindPipeline(
                transcriber = ThrowingTranscriber(),
                summarizer = PlaceholderSummarizer(),
                preprocessor = DefaultAudioPreprocessor(),
            ),
            noteSink = sink,
            autoAiProcessingEnabled = settings.autoAiProcessingEnabled,
        )

        controller.start()
        withTimeout(5_000) { while (!controller.state.value.canRewind) delay(10) }
        controller.rewind(TriggerType.GESTURE)
        withTimeout(5_000) {
            while (controller.state.value.error == null && sink.recordedAudioOnlyCount == 0) delay(10)
        }

        assertEquals(1, sink.recordedAudioOnlyCount)
        assertEquals(0, sink.recordedFullCount)
        assertEquals(0, sink.attachAiCount)
    }

    @Test
    fun rewindPublishesCompletedStateToSharedAiTracker() = runBlocking {
        val sink = RecordingNoteSink()
        val settings = AppSettings(MemoryStore(), "qwen2b")
        settings.setAutoAiProcessingEnabled(true)
        val tracker = AiProcessingTracker()

        val ringBuffer = FileLocalAudioRingBuffer(tempDir, 60, 16_000)
        ringBuffer.write(ShortArray(160_000) { 2_000 })

        val controller = KnockledgeController(
            capturer = RingAudioCapturer(NoOpAudioSource(), ringBuffer),
            knockDetector = FakeDetector(),
            hooks = NoOpHooks(),
            pipeline = RewindPipeline(
                transcriber = PlaceholderTranscriber(),
                summarizer = PlaceholderSummarizer(),
                preprocessor = DefaultAudioPreprocessor(),
            ),
            noteSink = sink,
            autoAiProcessingEnabled = settings.autoAiProcessingEnabled,
            aiTracker = tracker,
        )

        controller.start()
        withTimeout(5_000) { while (!controller.state.value.canRewind) delay(10) }
        controller.rewind(TriggerType.MANUAL)
        withTimeout(5_000) { while (controller.state.value.pendingRewinds > 0) delay(10) }

        val aiState = tracker.state.value
        assertEquals("note-1", aiState.noteId)
        assertEquals(NoteAiProcessingState.COMPLETED, aiState.state)
        kotlin.test.assertFalse(aiState.isProcessing)
        kotlin.test.assertTrue(aiState.isSuccess)
    }

    @Test
    fun rewindTriggersAiNotificationOnCompletion(): Unit = runBlocking {
        var notifiedTitle: String? = null
        var notifiedSummary: String? = null
        val testNotifier = object : group.two.knockledge.notification.AiNotificationNotifier {
            override fun notifyAiComplete(title: String, summaryText: String) {
                notifiedTitle = title
                notifiedSummary = summaryText
            }
        }

        val settings = AppSettings(MemoryStore(), "qwen2b")
        settings.setAutoAiProcessingEnabled(true)

        val ringBuffer = FileLocalAudioRingBuffer(tempDir, 60, 16_000)
        ringBuffer.write(ShortArray(160_000) { 2_000 })

        val controller = KnockledgeController(
            capturer = RingAudioCapturer(NoOpAudioSource(), ringBuffer),
            knockDetector = FakeDetector(),
            hooks = NoOpHooks(),
            pipeline = RewindPipeline(
                transcriber = PlaceholderTranscriber(),
                summarizer = PlaceholderSummarizer(),
                preprocessor = DefaultAudioPreprocessor(),
            ),
            noteSink = RecordingNoteSink(),
            autoAiProcessingEnabled = settings.autoAiProcessingEnabled,
            notifier = testNotifier,
        )

        controller.start()
        withTimeout(5_000) { while (!controller.state.value.canRewind) delay(10) }
        controller.rewind(TriggerType.MANUAL)
        withTimeout(5_000) { while (controller.state.value.pendingRewinds > 0) delay(10) }

        assertEquals("Untitled rewind", notifiedTitle)
        kotlin.test.assertNotNull(notifiedSummary)
    }
}
