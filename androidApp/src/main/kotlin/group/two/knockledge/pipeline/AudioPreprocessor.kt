package group.two.knockledge.pipeline

/**
 * Result of the pre-processing unit.
 */
sealed interface AudioPreprocessResult {
    /**
     * Successfully conditioned audio with dead air eradicated and levels normalized.
     *
     * @property pcm The processed 16-bit mono PCM sample array at 16,000 Hz.
     * @property durationMillis Duration of the conditioned speech in milliseconds.
     * @property sampleRate Sampling rate in Hz (standardized to 16,000).
     */
    data class Success(
        val pcm: ShortArray,
        val durationMillis: Long,
        val sampleRate: Int = AudioPreprocessor.TARGET_SAMPLE_RATE,
    ) : AudioPreprocessResult {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Success) return false
            return pcm.contentEquals(other.pcm) && durationMillis == other.durationMillis && sampleRate == other.sampleRate
        }

        override fun hashCode(): Int = (pcm.contentHashCode() * 31 + durationMillis.hashCode()) * 31 + sampleRate
    }

    /**
     * Active speech content fell below the minimum viable threshold for AI processing.
     *
     * @property detectedSpeechMillis Milliseconds of speech detected before reaching threshold check.
     * @property minimumRequiredMillis Minimum speech duration required to proceed (1,000 ms).
     */
    data class InsufficientSpeech(
        val detectedSpeechMillis: Long,
        val minimumRequiredMillis: Long = 1_000L,
    ) : AudioPreprocessResult
}

/**
 * Pre-processes audio before handing it off to the AI transcription engine.
 * Normalizes input to 16 kHz mono PCM, eradicates extended dead air,
 * applies volume normalization, and verifies speech duration.
 */
interface AudioPreprocessor {
    /**
     * Processes input [pcm] audio. Returns [AudioPreprocessResult.Success] if speech >= 1.0s,
     * or [AudioPreprocessResult.InsufficientSpeech] if speech is under 1.0s.
     *
     * The input array is not mutated; a new conditioned sample array is returned upon success.
     */
    fun process(
        pcm: ShortArray,
        sampleRate: Int = TARGET_SAMPLE_RATE,
    ): AudioPreprocessResult

    /**
     * Overload supporting multi-channel audio: downmixes to mono and normalizes to 16 kHz
     * before processing.
     */
    fun process(
        pcm: ShortArray,
        sampleRate: Int = TARGET_SAMPLE_RATE,
        channels: Int = 1,
    ): AudioPreprocessResult = process(normalizeTo16kMono(pcm, sampleRate, channels), TARGET_SAMPLE_RATE)

    companion object {
        /** Standard target sample rate required by speech recognition engines (16 kHz). */
        const val TARGET_SAMPLE_RATE: Int = 16_000

        /** Minimum non-silent speech duration required to run AI models (1 second). */
        const val MIN_SPEECH_DURATION_MILLIS: Long = 1_000L

        /** Maximum silent gap allowed before truncating dead air (1.5 seconds). */
        const val MAX_DEAD_AIR_GAP_MILLIS: Long = 1_500L

        /** Ambient silence padding to retain around speech segments (300 ms). */
        const val SILENCE_PADDING_MILLIS: Long = 300L

        /** Target peak amplitude for normalization (~ -1 dBFS). */
        const val TARGET_PEAK_AMPLITUDE: Short = 29_490

        /**
         * Normalizes input audio into 16,000 Hz single-channel mono PCM.
         * If [channels] > 1, channels are downmixed to mono by averaging.
         * If [sampleRate] != 16,000, audio is resampled using linear interpolation.
         */
        fun normalizeTo16kMono(
            pcm: ShortArray,
            sampleRate: Int = TARGET_SAMPLE_RATE,
            channels: Int = 1,
        ): ShortArray {
            if (pcm.isEmpty()) return pcm

            // 1. Downmix multi-channel to mono
            val monoPcm = if (channels > 1) {
                val frames = pcm.size / channels
                val mono = ShortArray(frames)
                for (i in 0 until frames) {
                    var sum = 0
                    for (c in 0 until channels) {
                        sum += pcm[i * channels + c].toInt()
                    }
                    mono[i] = (sum / channels).toShort()
                }
                mono
            } else {
                pcm
            }

            // 2. Resample to 16 kHz if needed
            if (sampleRate == TARGET_SAMPLE_RATE || sampleRate <= 0 || monoPcm.isEmpty()) {
                return monoPcm
            }

            val targetLength = ((monoPcm.size.toLong() * TARGET_SAMPLE_RATE) / sampleRate).toInt()
            if (targetLength <= 0) return ShortArray(0)
            val resampled = ShortArray(targetLength)
            val ratio = sampleRate.toDouble() / TARGET_SAMPLE_RATE
            for (i in 0 until targetLength) {
                val srcIndex = i * ratio
                val index0 = srcIndex.toInt().coerceIn(0, monoPcm.size - 1)
                val index1 = (index0 + 1).coerceIn(0, monoPcm.size - 1)
                val frac = srcIndex - index0
                val val0 = monoPcm[index0].toDouble()
                val val1 = monoPcm[index1].toDouble()
                resampled[i] = (val0 + (val1 - val0) * frac).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    .toShort()
            }
            return resampled
        }
    }
}
