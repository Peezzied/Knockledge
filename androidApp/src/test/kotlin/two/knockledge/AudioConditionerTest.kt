package group.two.knockledge

import group.two.knockledge.audio.AudioConditioner
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

class AudioConditionerTest {
    @Test
    fun quietSpeechIsAmplifiedWithoutClipping() {
        val quiet = ShortArray(1_600) { if (it % 2 == 0) 200 else -200 }
        val out = AudioConditioner.condition(quiet, 16_000)
        assertTrue(out.maxOf { abs(it.toInt()) } in 1_000..32_767)
    }

    @Test
    fun inputIsNotMutated() {
        val input = ShortArray(1_600) { if (it % 2 == 0) 200 else -200 }
        val copy = input.copyOf()
        AudioConditioner.condition(input, 16_000)
        assertContentEquals(copy, input)
    }

    @Test
    fun leadingAndTrailingSilenceIsTrimmed() {
        val pcm = ShortArray(16_000)
        for (i in 6_000 until 10_000) pcm[i] = if (i % 2 == 0) 5_000 else -5_000
        val out = AudioConditioner.condition(pcm, 16_000)
        assertTrue(out.size in 4_000..5_000)
    }

    @Test
    fun allSilenceGivesEmpty() {
        assertTrue(AudioConditioner.condition(ShortArray(16_000), 16_000).isEmpty())
    }
}
