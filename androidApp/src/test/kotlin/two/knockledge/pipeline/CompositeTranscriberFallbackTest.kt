package two.knockledge.pipeline

import group.two.knockledge.ai.EngineError
import group.two.knockledge.pipeline.AudioSegment
import group.two.knockledge.pipeline.CompositeTranscriber
import group.two.knockledge.pipeline.TranscriptResult
import group.two.knockledge.pipeline.TranscriptionEngine
import group.two.knockledge.pipeline.TranscriptionEngineType
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CompositeTranscriberFallbackTest {

    private class FakeEngine(
        private val type: TranscriptionEngineType,
        private val failures: List<Throwable> = emptyList(),
    ) : TranscriptionEngine {
        var callCount = 0
        override val engineType: TranscriptionEngineType get() = type
        override suspend fun process(segment: AudioSegment): TranscriptResult {
            callCount++
            if (callCount <= failures.size) throw failures[callCount - 1]
            return TranscriptResult(emptyList(), "test", false, "en", type)
        }
    }

    private val pcm = ShortArray(16000) { 100 }

    @Test
    fun autoModeWithNetworkFallsBackToCloudOnEngineError() = runBlocking {
        // The cloud's first attempt fails, the offline engine then fails with an
        // EngineError, and the cloud retry carries the rewind.
        val online = FakeEngine(TranscriptionEngineType.CLOUD_FIREBASE_AI, failures = listOf(RuntimeException("net")))
        val offline = FakeEngine(TranscriptionEngineType.LOCAL_WHISPER, failures = listOf(EngineError.ModelLoadFailed("whisper")))
        val t = CompositeTranscriber(online = online, offline = offline, isOnline = { true }, mode = { "auto" })

        val result = t.transcribe(pcm, 16000)

        assertEquals(TranscriptionEngineType.CLOUD_FIREBASE_AI, result.engineType)
        assertEquals(1, offline.callCount)
        assertEquals(2, online.callCount)
    }

    @Test
    fun localModeSurfacesEngineErrorWithoutCloudCall() = runBlocking {
        val online = FakeEngine(TranscriptionEngineType.CLOUD_FIREBASE_AI)
        val offline = FakeEngine(TranscriptionEngineType.LOCAL_WHISPER, failures = listOf(EngineError.ModelMissing("whisper")))
        val t = CompositeTranscriber(online = online, offline = offline, isOnline = { true }, mode = { "local" })

        assertFailsWith<EngineError> { t.transcribe(pcm, 16000) }
        assertEquals(0, online.callCount)
    }

    @Test
    fun cloudModeIsUnchanged() = runBlocking {
        val online = FakeEngine(TranscriptionEngineType.CLOUD_FIREBASE_AI)
        val offline = FakeEngine(TranscriptionEngineType.LOCAL_WHISPER)
        val t = CompositeTranscriber(online = online, offline = offline, isOnline = { true }, mode = { "online" })

        val result = t.transcribe(pcm, 16000)

        assertEquals(TranscriptionEngineType.CLOUD_FIREBASE_AI, result.engineType)
        assertEquals(1, online.callCount)
        assertEquals(0, offline.callCount)
    }
}
