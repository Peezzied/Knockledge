package group.two.knockledge.audio

import group.two.knockledge.core.FileLocalAudioRingBuffer
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

/** Number of amplitude bins computed per recording. */
const val WAVEFORM_BIN_COUNT = 256

/**
 * Reduces 16-bit PCM [pcm] samples to [bins] normalized (0..1) peak amplitudes.
 * Returns an empty array when there is no audio.
 */
fun computeWaveformPeaks(pcm: ShortArray, bins: Int = WAVEFORM_BIN_COUNT): FloatArray {
    if (pcm.isEmpty() || bins <= 0) return FloatArray(0)
    val peaks = FloatArray(bins)
    for (bin in 0 until bins) {
        val start = (bin.toLong() * pcm.size / bins).toInt()
        val end = (((bin + 1).toLong() * pcm.size / bins).toInt()).coerceAtLeast(start + 1).coerceAtMost(pcm.size)
        var max = 0
        for (i in start until end) max = maxOf(max, abs(pcm[i].toInt()))
        peaks[bin] = max / Short.MAX_VALUE.toFloat()
    }
    val loudest = peaks.max()
    return if (loudest > 0f) FloatArray(bins) { peaks[it] / loudest } else peaks
}

/**
 * Reads a local WAV recording (PCM16 with the standard header) and returns its waveform peaks.
 * Returns an empty array if the file is missing or empty.
 */
fun readWaveformPeaks(wav: File, bins: Int = WAVEFORM_BIN_COUNT): FloatArray {
    if (!wav.exists()) return FloatArray(0)
    val bytes = wav.readBytes()
    val offset = if (bytes.size >= FileLocalAudioRingBuffer.WAV_HEADER_BYTES) FileLocalAudioRingBuffer.WAV_HEADER_BYTES else 0
    val pcm = ShortArray(((bytes.size - offset) / 2).coerceAtLeast(0))
    ByteBuffer.wrap(bytes, offset, pcm.size * 2).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(pcm)
    return computeWaveformPeaks(pcm, bins)
}
