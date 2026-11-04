package two.knockledge

import group.two.knockledge.audio.SampleQueue
import group.two.knockledge.pipeline.Summaries
import group.two.knockledge.pipeline.SummarizerPrompt
import group.two.knockledge.pipeline.SummaryRequest
import group.two.knockledge.settings.SummaryLanguages
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AudioMixingTest {
    @Test
    fun sampleQueueDropsOldestWhenFull() {
        val q = SampleQueue(capacity = 4)
        q.push(shortArrayOf(1, 2, 3))
        q.push(shortArrayOf(4, 5, 6))
        assertContentEquals(shortArrayOf(3, 4), q.pop(2))
        assertContentEquals(shortArrayOf(5, 6), q.pop(10))
        assertNull(q.pop(1))
    }

    @Test
    fun filipinoRecapsAreRequestedInBothPrompts() {
        val request = SummaryRequest("Usapan tungkol sa exam.", false, 0, 180_000L)
        assertTrue(SummarizerPrompt.system(SummaryLanguages.FILIPINO).contains("Filipino (Tagalog)"))
        assertTrue(SummarizerPrompt.user(request, outputLanguage = SummaryLanguages.FILIPINO).endsWith("Isulat ang buod sa Filipino."))
        assertTrue(SummarizerPrompt.system(SummaryLanguages.ENGLISH).contains("simple English"))
        assertEquals("Walang malinaw na narinig", Summaries.nothingHeard(0, SummaryLanguages.FILIPINO).title)
    }
}
