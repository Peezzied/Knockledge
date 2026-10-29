package group.two.knockledge.pipeline

/**
 * Core interface exposed by Unit 3 for audio transcription and diarization.
 */
interface Transcriber {
    /**
     * Ingests a raw PCM16 audio buffer and produces a structured transcription result.
     *
     * @param pcm Array of 16-bit PCM audio samples at 16 kHz.
     * @param sampleRate Audio sample rate in Hz (expected 16,000).
     * @return Structured [TranscriptResult] containing sentences and metadata.
     */
    suspend fun transcribe(pcm: ShortArray, sampleRate: Int = 16_000): TranscriptResult
}

/**
 * Underlying engine abstraction implemented by online and offline providers.
 */
interface TranscriptionEngine {
    val engineType: TranscriptionEngineType

    suspend fun process(segment: AudioSegment): TranscriptResult
}

enum class TranscriptionEngineType {
    CLOUD_FIREBASE_AI,
    LOCAL_WHISPER,
}

data class AudioSegment(
    val pcm: ShortArray,
    val sampleRate: Int,
    val durationMillis: Long,
    val isGainNormalized: Boolean = false,
)

data class TranscriptSentence(
    val speaker: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String,
    val confidence: Float = 1.0f,
)

data class TranscriptResult(
    val sentences: List<TranscriptSentence>,
    val fullText: String,
    val isDiarized: Boolean,
    val detectedLanguage: String,
    val engineType: TranscriptionEngineType,
)
