package group.two.knockledge.pipeline

import kotlin.math.abs
import kotlin.math.min

/**
 * Neural [AudioPreprocessor]: normalizes input audio to 16 kHz mono PCM, classifies each
 * [SileroVadConfig.frameSize] chunk as speech or not via [VadDetector], truncates non-speech
 * runs longer than [SileroVadConfig.maxDeadAirGapMillis] down to [SileroVadConfig.silencePaddingMillis],
 * and peak normalizes retained audio to [SileroVadConfig.targetPeakAmplitude].
 * Audio holding under [SileroVadConfig.minSpeechDurationMillis] of speech is rejected.
 *
 * @property vadDetector Classifier producing the per-frame speech mask.
 * @property config Thresholds and frame sizing for conditioning.
 */
class SileroAudioPreprocessor(
    override val vadDetector: VadDetector = FallbackEnergyVadDetector(),
    override val config: SileroVadConfig = SileroVadConfig(),
) : SileroAudioPreprocessorContract {

    override fun process(pcm: ShortArray, sampleRate: Int): AudioPreprocessResult {
        val normalized = AudioPreprocessor.normalizeTo16kMono(pcm, sampleRate)
        if (normalized.isEmpty()) return AudioPreprocessResult.InsufficientSpeech(0L)
        val targetRate = AudioPreprocessor.TARGET_SAMPLE_RATE
        val speech = vadDetector.detect(normalized, targetRate)
        val maxGapSamples = (config.maxDeadAirGapMillis * targetRate / 1_000).toInt()
        val paddingSamples = (config.silencePaddingMillis * targetRate / 1_000).toInt()

        val out = ShortArray(normalized.size)
        var outSize = 0
        var speechSamples = 0
        var frame = 0
        while (frame < speech.size) {
            var end = frame
            while (end < speech.size && speech[end] == speech[frame]) end++
            val from = frame * config.frameSize
            if (from >= normalized.size) break
            val to = min(normalized.size, end * config.frameSize)
            var run = to - from
            if (speech[frame]) {
                speechSamples += run
            } else if (run > maxGapSamples) {
                run = paddingSamples
            }
            normalized.copyInto(out, outSize, from, from + run)
            outSize += run
            frame = end
        }

        val speechMillis = speechSamples * 1_000L / targetRate
        if (speechMillis < config.minSpeechDurationMillis) {
            return AudioPreprocessResult.InsufficientSpeech(speechMillis)
        }

        val trimmed = out.copyOf(outSize)
        normalizePeak(trimmed)
        return AudioPreprocessResult.Success(
            pcm = trimmed,
            durationMillis = trimmed.size * 1_000L / targetRate,
            sampleRate = targetRate,
        )
    }

    /** Scales [pcm] in place so its peak lands on [SileroVadConfig.targetPeakAmplitude]. */
    private fun normalizePeak(pcm: ShortArray) {
        var peak = 0
        for (s in pcm) peak = maxOf(peak, abs(s.toInt()))
        if (peak == 0 || peak == config.targetPeakAmplitude.toInt()) return
        val gain = config.targetPeakAmplitude.toDouble() / peak
        for (i in pcm.indices) {
            pcm[i] = (pcm[i] * gain).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
    }
}
