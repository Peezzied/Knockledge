package group.two.knockledge.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Splits an audio chunk into [COUNT] frequency bands, each 0..1 on a decibel scale.
 *
 * Uses the Goertzel algorithm (a single-bin DFT) at log-spaced center frequencies
 * from [MIN_HZ] to [MAX_HZ], so no FFT dependency is needed. Speech energy lives
 * mostly in the low bands; log spacing gives them finer resolution.
 */
object SpectrumBands {
    /** Number of frequency bands produced by [compute]. */
    const val COUNT = 14

    /** Lowest band center frequency, near the bottom of speech fundamentals. */
    const val MIN_HZ = 100.0

    /** Highest band center frequency, kept below the 16 kHz Nyquist of 8 kHz. */
    const val MAX_HZ = 7_000.0

    /** Log-spaced band center frequencies in Hz for [sampleRate], clamped below Nyquist. */
    fun centerFrequencies(sampleRate: Int): DoubleArray {
        val max = minOf(MAX_HZ, sampleRate / 2.0 * 0.9)
        return DoubleArray(COUNT) { i ->
            MIN_HZ * (max / MIN_HZ).pow(i.toDouble() / (COUNT - 1))
        }
    }

    /**
     * Returns [COUNT] band magnitudes in 0..1 for [chunk], mapped through the same
     * floor/ceiling decibel range as the overall level meter. An empty chunk gives zeros.
     */
    fun compute(
        chunk: ShortArray,
        sampleRate: Int,
        floorDb: Double = RingAudioCapturer.FLOOR_DB,
        ceilDb: Double = RingAudioCapturer.CEIL_DB,
    ): FloatArray {
        val out = FloatArray(COUNT)
        if (chunk.isEmpty()) return out
        val centers = centerFrequencies(sampleRate)
        for (b in centers.indices) {
            out[b] = bandLevel(chunk, centers[b], sampleRate, floorDb, ceilDb)
        }
        return out
    }

    /** Goertzel magnitude at [freqHz], normalised to full scale and mapped to 0..1 on a dB scale. */
    private fun bandLevel(
        chunk: ShortArray,
        freqHz: Double,
        sampleRate: Int,
        floorDb: Double,
        ceilDb: Double,
    ): Float {
        val coeff = 2.0 * cos(2.0 * PI * freqHz / sampleRate)
        var s0 = 0.0
        var s1 = 0.0
        var s2 = 0.0
        for (s in chunk) {
            s0 = s.toDouble() + coeff * s1 - s2
            s2 = s1
            s1 = s0
        }
        val magnitude = sqrt(s1 * s1 + s2 * s2 - coeff * s1 * s2) / (chunk.size * Short.MAX_VALUE)
        val db = 20.0 * log10(magnitude.coerceAtLeast(1e-6))
        return ((db - floorDb) / (ceilDb - floorDb)).toFloat().coerceIn(0f, 1f)
    }
}
