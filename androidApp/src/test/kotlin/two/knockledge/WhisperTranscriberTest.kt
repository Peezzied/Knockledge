package group.two.knockledge

import group.two.knockledge.ai.WhisperTranscriberAdapter
import group.two.knockledge.pipeline.AudioSegment
import group.two.knockledge.pipeline.TranscriptionEngineType
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WhisperTranscriberTest {
    private val segment = AudioSegment(ShortArray(16_000), 16_000, 2_000)

    @Test
    fun mapsTextToSingleSpeaker() = runBlocking {
        val r = WhisperTranscriberAdapter { "Hello there" }.process(segment)
        assertEquals(TranscriptionEngineType.LOCAL_WHISPER, r.engineType)
        assertFalse(r.isDiarized)
        assertEquals("Hello there", r.fullText)
        val s = r.sentences.single()
        assertEquals("Speaker", s.speaker)
        assertEquals(0L, s.startTimeMs)
        assertEquals(2_000L, s.endTimeMs)
    }

    @Test
    fun blankTextGivesNoSentences() = runBlocking {
        val r = WhisperTranscriberAdapter { "  " }.process(segment)
        assertTrue(r.sentences.isEmpty())
        assertEquals("", r.fullText)
    }
}
