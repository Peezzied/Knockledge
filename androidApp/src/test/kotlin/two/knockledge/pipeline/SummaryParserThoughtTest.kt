package two.knockledge.pipeline

import group.two.knockledge.pipeline.SummaryParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class SummaryParserThoughtTest {

    @Test
    fun ignoresLeadingThoughtBlock() {
        val raw = """
            <think>
            The user wants a summary. Let me think about the transcript first.
            </think>
            TITLE: Project Sync
            GIST: The team aligned on the deadline.
            - Moved the demo to Friday
            - Ana owns the slides
        """.trimIndent()

        val s = SummaryParser.parse(raw, 0)

        assertEquals("Project Sync", s.title)
        assertEquals("The team aligned on the deadline.", s.gist)
        assertFalse(s.gist.contains("think"))
        assertEquals(listOf("Moved the demo to Friday", "Ana owns the slides"), s.points)
    }
}
