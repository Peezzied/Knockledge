package group.two.knockledge

import group.two.knockledge.pipeline.AudioSegment
import group.two.knockledge.pipeline.CompositeTranscriber
import group.two.knockledge.pipeline.TranscriptResult
import group.two.knockledge.pipeline.TranscriptSentence
import group.two.knockledge.pipeline.TranscriptionEngine
import group.two.knockledge.pipeline.TranscriptionEngineType
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun result(type: TranscriptionEngineType, diarized: Boolean) = TranscriptResult(
    listOf(TranscriptSentence("Speaker", 0, 10, "hi")), "hi", diarized, "en", type,
)

private class FakeEngine(
    override val engineType: TranscriptionEngineType,
    val action: suspend () -> TranscriptResult,
) : TranscriptionEngine {
    var calls = 0
    var lastSamples = 0
    override suspend fun process(segment: AudioSegment): TranscriptResult {
        calls++
        lastSamples = segment.pcm.size
        return action()
    }
}

class TranscriberTest {
    private val speech = ShortArray(16_000) { if (it % 2 == 0) 8_000 else -8_000 }

    private fun cloud(action: suspend () -> TranscriptResult = { result(TranscriptionEngineType.CLOUD_FIREBASE_AI, true) }) =
        FakeEngine(TranscriptionEngineType.CLOUD_FIREBASE_AI, action)

    private val local get() = FakeEngine(TranscriptionEngineType.LOCAL_WHISPER) { result(TranscriptionEngineType.LOCAL_WHISPER, false) }

    @Test
    fun onlineUsesCloud() = runBlocking {
        val c = cloud()
        val l = local
        val r = CompositeTranscriber(c, l, { true }).transcribe(speech, 16_000)
        assertTrue(r.isDiarized)
        assertEquals(0, l.calls)
    }

    @Test
    fun offlineSkipsCloud() = runBlocking {
        val c = cloud()
        val r = CompositeTranscriber(c, local, { false }).transcribe(speech, 16_000)
        assertEquals(TranscriptionEngineType.LOCAL_WHISPER, r.engineType)
        assertEquals(0, c.calls)
    }

    @Test
    fun cloudErrorFallsBackToLocal() = runBlocking {
        val c = cloud { error("boom") }
        val r = CompositeTranscriber(c, local, { true }).transcribe(speech, 16_000)
        assertFalse(r.isDiarized)
    }

    @Test
    fun cloudTimeoutFallsBackToLocal() = runBlocking {
        val c = cloud { delay(5_000); result(TranscriptionEngineType.CLOUD_FIREBASE_AI, true) }
        val r = CompositeTranscriber(c, local, { true }, timeoutMillis = 50).transcribe(speech, 16_000)
        assertEquals(TranscriptionEngineType.LOCAL_WHISPER, r.engineType)
    }

    @Test
    fun silenceIsNotSentToAnyEngine() = runBlocking {
        val c = cloud()
        val l = local
        val r = CompositeTranscriber(c, l, { true }).transcribe(ShortArray(16_000), 16_000)
        assertTrue(r.sentences.isEmpty())
        assertEquals(0, c.calls + l.calls)
    }

    @Test
    fun conditionedAudioReachesEngine() = runBlocking {
        val c = cloud()
        val quiet = ShortArray(32_000) { if (it in 8_000 until 16_000) 100 else 0 }
        CompositeTranscriber(c, local, { true }).transcribe(quiet, 16_000)
        assertTrue(c.lastSamples in 1 until 32_000)
    }
}
