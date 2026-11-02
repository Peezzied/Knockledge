package two.knockledge.pipeline

import group.two.knockledge.ai.EngineError
import group.two.knockledge.pipeline.CompositeSummarizer
import group.two.knockledge.pipeline.Summarizer
import group.two.knockledge.pipeline.Summary
import group.two.knockledge.pipeline.SummaryRequest
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CompositeSummarizerFallbackTest {

    private class FakeSummarizer(
        private val label: String,
        private val failures: List<Throwable> = emptyList(),
    ) : Summarizer {
        var callCount = 0
        override suspend fun summarize(request: SummaryRequest): Summary {
            callCount++
            if (callCount <= failures.size) throw failures[callCount - 1]
            return Summary(label, "gist", emptyList(), request.redactedCount)
        }
    }

    private val request = SummaryRequest("sample text", false, 0, 5000L)

    @Test
    fun autoModeWithNetworkFallsBackToCloudOnEngineError() = runBlocking {
        val online = FakeSummarizer("cloud", failures = listOf(RuntimeException("net")))
        val offline = FakeSummarizer("device", failures = listOf(EngineError.ModelLoadFailed("qwen2b")))
        val s = CompositeSummarizer(online = online, offline = offline, isOnline = { true }, mode = { "auto" })

        val summary = s.summarize(request)

        assertEquals("cloud", summary.title)
        assertEquals(1, offline.callCount)
        assertEquals(2, online.callCount)
    }

    @Test
    fun localModeSurfacesEngineErrorWithoutCloudCall() = runBlocking {
        val online = FakeSummarizer("cloud")
        val offline = FakeSummarizer("device", failures = listOf(EngineError.ModelMissing("qwen2b")))
        val s = CompositeSummarizer(online = online, offline = offline, isOnline = { true }, mode = { "local" })

        assertFailsWith<EngineError> { s.summarize(request) }
        assertEquals(0, online.callCount)
    }

    @Test
    fun cloudModeIsUnchanged() = runBlocking {
        val online = FakeSummarizer("cloud")
        val offline = FakeSummarizer("device")
        val s = CompositeSummarizer(online = online, offline = offline, isOnline = { true }, mode = { "online" })

        assertEquals("cloud", s.summarize(request).title)
        assertEquals(1, online.callCount)
        assertEquals(0, offline.callCount)
    }
}
