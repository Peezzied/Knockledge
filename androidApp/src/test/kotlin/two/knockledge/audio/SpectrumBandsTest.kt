package group.two.knockledge.audio

import kotlin.math.PI
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpectrumBandsTest {

    private fun tone(freqHz: Double, amplitude: Double = 0.5, n: Int = 1600, sampleRate: Int = 16_000) =
        ShortArray(n) { (amplitude * Short.MAX_VALUE * sin(2 * PI * freqHz * it / sampleRate)).toInt().toShort() }

    private fun nearestBand(freqHz: Double, sampleRate: Int = 16_000): Int {
        val centers = SpectrumBands.centerFrequencies(sampleRate)
        return centers.indices.minBy { kotlin.math.abs(centers[it] - freqHz) }
    }

    @Test
    fun lowTonePeaksAtNearestLowBand() {
        val bands = SpectrumBands.compute(tone(440.0), 16_000)
        val peak = bands.indices.maxBy { bands[it] }
        // 440 Hz falls between two log-spaced centers, so the peak may be either neighbour.
        assertTrue(kotlin.math.abs(peak - nearestBand(440.0)) <= 1, "peak $peak")
        assertTrue(
            bands[peak] > bands[nearestBand(4000.0)] + 0.3f,
            "440 band ${bands[peak]} vs 4000 band ${bands[nearestBand(4000.0)]}",
        )
    }

    @Test
    fun highTonePeaksAtNearestHighBand() {
        val bands = SpectrumBands.compute(tone(4000.0), 16_000)
        assertEquals(nearestBand(4000.0), bands.indices.maxBy { bands[it] })
    }

    @Test
    fun silenceGivesAllZeros() {
        val bands = SpectrumBands.compute(ShortArray(1600), 16_000)
        assertTrue(bands.all { it == 0f }, "bands ${bands.toList()}")
    }

    @Test
    fun resultSizeAndRange() {
        val bands = SpectrumBands.compute(tone(1000.0), 16_000)
        assertEquals(SpectrumBands.COUNT, bands.size)
        assertTrue(bands.all { it in 0f..1f })
        assertTrue(SpectrumBands.compute(ShortArray(0), 16_000).all { it == 0f })
    }
}
