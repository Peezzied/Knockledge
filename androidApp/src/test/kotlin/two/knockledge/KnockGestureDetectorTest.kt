package group.two.knockledge

import group.two.knockledge.sensor.KnockPeakMatcher
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KnockGestureDetectorTest {
    @Test
    fun doubleKnockInsideWindowTriggers() {
        val m = KnockPeakMatcher()
        assertFalse(m.onSample(13f, 1000))
        assertTrue(m.onSample(13f, 1250))
    }

    @Test
    fun singlePeakAndBelowThresholdDoNotTrigger() {
        val m = KnockPeakMatcher()
        assertFalse(m.onSample(11.9f, 0))
        assertFalse(m.onSample(11.9f, 200))
        assertFalse(m.onSample(12f, 1000))
        assertFalse(m.onSample(0f, 1200))
    }

    @Test
    fun gapOutsideWindowDoesNotTrigger() {
        val m = KnockPeakMatcher()
        m.onSample(13f, 0)
        assertFalse(m.onSample(13f, 401))
        assertTrue(m.onSample(13f, 700))
    }

    @Test
    fun bounceUnder100msIsIgnored() {
        val m = KnockPeakMatcher()
        m.onSample(13f, 0)
        assertFalse(m.onSample(14f, 60))
        assertFalse(m.onSample(14f, 99))
        assertTrue(m.onSample(13f, 100))
    }

    @Test
    fun exactBoundariesTrigger() {
        assertTrue(KnockPeakMatcher().apply { onSample(12f, 0) }.onSample(12f, 100))
        assertTrue(KnockPeakMatcher().apply { onSample(12f, 0) }.onSample(12f, 400))
    }

    @Test
    fun debounceBlocksRetriggerFor2000ms() {
        val m = KnockPeakMatcher()
        m.onSample(13f, 0)
        assertTrue(m.onSample(13f, 200))
        m.onSample(13f, 500)
        assertFalse(m.onSample(13f, 700))
        assertFalse(m.onSample(13f, 2199))
        assertFalse(m.onSample(13f, 2200))
        assertTrue(m.onSample(13f, 2400))
    }
}
