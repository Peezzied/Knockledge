package two.knockledge

import group.two.knockledge.KnockledgeController
import group.two.knockledge.audio.AudioSource
import group.two.knockledge.audio.NoopSessionHooks
import group.two.knockledge.audio.RingAudioCapturer
import group.two.knockledge.core.LocalAudioRingBuffer
import group.two.knockledge.data.TriggerType
import group.two.knockledge.pipeline.DefaultAudioPreprocessor
import group.two.knockledge.pipeline.PlaceholderSummarizer
import group.two.knockledge.pipeline.PlaceholderTranscriber
import group.two.knockledge.pipeline.RewindPipeline
import group.two.knockledge.sensor.KnockGestureDetector
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class KnockledgeSaveThresholdTest {
    private class DummyBuffer(var currentMillis: Long = 0L) : LocalAudioRingBuffer {
        override fun write(chunk: ShortArray, length: Int) {}
        override fun setWindowSeconds(seconds: Int) {}
        override fun bufferedMillis(): Long = currentMillis
        override fun snapshot(): File = File.createTempFile("dummy", ".wav")
        override fun clear() {}
    }

    private class DummySource(val chunks: Int = 0) : AudioSource {
        override fun frames(sampleRate: Int) = flow {
            repeat(chunks) { emit(ShortArray(1600) { 100 }) }
        }
    }

    @Test
    fun canRewindObeysDynamicThreshold() = runBlocking {
        val dummyBuffer = DummyBuffer(4_000L) // 4 seconds buffered
        val minSaveSeconds = MutableStateFlow(5) // Requires 5 seconds
        val pipeline = RewindPipeline(PlaceholderTranscriber(), PlaceholderSummarizer(), DefaultAudioPreprocessor())

        val dummyDetector = object : KnockGestureDetector {
            override val isListening: Boolean = false
            override fun startListening(onKnockDetected: () -> Boolean) = Unit
            override fun stopListening() = Unit
        }
        val controller = KnockledgeController(
            capturer = RingAudioCapturer(DummySource(chunks = 1), dummyBuffer),
            knockDetector = dummyDetector,
            hooks = NoopSessionHooks,
            pipeline = pipeline,
            minAudioSaveSeconds = minSaveSeconds,
        )

        controller.start()
        // Wait until the emitted chunk publishes the buffered amount into state.
        withTimeout(5_000L) {
            while (controller.state.value.bufferedMillis == 0L) delay(10L)
        }
        assertFalse("At 4s with 5s threshold, canRewind must be false", controller.state.value.canRewind)

        // With threshold updated to 4, the 4s buffer unlocks.
        minSaveSeconds.value = 4
        assertTrue("At 4s with 4s threshold, canRewind must be true", controller.state.value.canRewind)
        controller.stop()
    }

    @Test
    fun rewindReportsRejectionBelowSaveThreshold() = runBlocking {
        val dummyBuffer = DummyBuffer(4_000L)
        val minSaveSeconds = MutableStateFlow(5)
        val pipeline = RewindPipeline(PlaceholderTranscriber(), PlaceholderSummarizer(), DefaultAudioPreprocessor())
        val dummyDetector = object : KnockGestureDetector {
            override val isListening: Boolean = false
            override fun startListening(onKnockDetected: () -> Boolean) = Unit
            override fun stopListening() = Unit
        }
        val controller = KnockledgeController(
            capturer = RingAudioCapturer(DummySource(chunks = 1), dummyBuffer),
            knockDetector = dummyDetector,
            hooks = NoopSessionHooks,
            pipeline = pipeline,
            minAudioSaveSeconds = minSaveSeconds,
        )

        controller.start()
        withTimeout(5_000L) {
            while (controller.state.value.bufferedMillis == 0L) delay(10L)
        }

        // The return value is the signal the detector uses to gate vibration.
        assertFalse("Below the save threshold, rewind must report rejection", controller.rewind(TriggerType.GESTURE))
        minSaveSeconds.value = 4
        assertTrue("At the save threshold, rewind must report acceptance", controller.rewind(TriggerType.GESTURE))
        controller.stop()
    }
}
