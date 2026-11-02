package two.knockledge.audio

import group.two.knockledge.audio.computeWaveformPeaks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WaveformPeaksTest {
    @Test
    fun emptyPcmGivesNoPeaks() {
        assertTrue(computeWaveformPeaks(ShortArray(0), 4).isEmpty())
    }

    @Test
    fun peaksAreNormalizedPerBin() {
        val pcm = shortArrayOf(100, -200, 400, -800)
        val peaks = computeWaveformPeaks(pcm, 2)
        assertEquals(0.25f, peaks[0], 0.001f)
        assertEquals(1f, peaks[1], 0.001f)
    }
}
