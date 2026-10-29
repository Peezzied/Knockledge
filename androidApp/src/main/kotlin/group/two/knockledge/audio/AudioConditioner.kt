package group.two.knockledge.audio

import group.two.knockledge.core.AudioGain
import kotlin.math.sqrt

object AudioConditioner {
    private const val FRAME_MS = 20
    private const val SPEECH_RMS = 330.0

    fun condition(pcm: ShortArray, sampleRate: Int): ShortArray {
        val gained = pcm.copyOf()
        AudioGain.normalizeInPlace(gained)
        val frame = sampleRate * FRAME_MS / 1000
        val frames = (gained.size + frame - 1) / frame
        val active = (0 until frames).filter { rms(gained, it * frame, minOf(gained.size, (it + 1) * frame)) >= SPEECH_RMS }
        if (active.isEmpty()) return ShortArray(0)
        return gained.copyOfRange(active.first() * frame, minOf(gained.size, (active.last() + 1) * frame))
    }

    private fun rms(pcm: ShortArray, from: Int, to: Int): Double {
        var sum = 0.0
        for (i in from until to) sum += pcm[i].toDouble() * pcm[i]
        return sqrt(sum / (to - from))
    }
}
