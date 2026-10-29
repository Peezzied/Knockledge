package group.two.knockledge.data

import group.two.knockledge.core.RewindPolicy
import java.io.File
import kotlin.math.sqrt

enum class TriggerType { GESTURE, MANUAL }

data class BufferConfiguration(
    val durationSeconds: Int,
    val sampleRate: Int = RewindPolicy.SAMPLE_RATE,
    val chunkDurationMs: Int = RewindPolicy.CHUNK_SECONDS * 1000,
    val storageDirectory: File,
) {
    init {
        require(durationSeconds in RewindPolicy.MIN_WINDOW_SECONDS..RewindPolicy.MAX_WINDOW_SECONDS)
        require(sampleRate == RewindPolicy.SAMPLE_RATE)
    }
}

data class KnockSensorSample(val x: Float, val y: Float, val z: Float, val timestampNs: Long) {
    val magnitude: Float get() = sqrt(x * x + y * y + z * z)
}

data class KnockGestureEvent(
    val firstPeakTimeMs: Long,
    val secondPeakTimeMs: Long,
    val peakMagnitude: Float,
    val triggerSource: TriggerType = TriggerType.GESTURE,
) {
    init {
        require(secondPeakTimeMs - firstPeakTimeMs in 100L..400L)
        require(peakMagnitude >= 12.0f)
    }
}

data class AudioSnapshot(
    val file: File,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val sampleRate: Int,
    val triggerType: TriggerType,
) {
    val durationMs: Long get() = endEpochMs - startEpochMs
}
