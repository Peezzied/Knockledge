package group.two.knockledge.pipeline

interface Transcriber {
    suspend fun transcribe(pcm: ShortArray, sampleRate: Int = 16_000): TranscriptResult
}

interface TranscriptionEngine {
    val engineType: TranscriptionEngineType

    suspend fun process(segment: AudioSegment): TranscriptResult
}
