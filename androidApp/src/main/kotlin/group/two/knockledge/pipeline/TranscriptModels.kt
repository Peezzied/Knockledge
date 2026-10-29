package group.two.knockledge.pipeline

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
