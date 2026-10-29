package group.two.knockledge.data

import group.two.knockledge.data.model.AiEngineType
import group.two.knockledge.data.model.Note
import group.two.knockledge.data.model.TranscriptionEngineType
import group.two.knockledge.data.model.TriggerType
import java.io.File
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun observeNotes(userId: String): Flow<List<Note>>

    suspend fun getNote(id: String, userId: String): Note?

    suspend fun createFromSnapshot(snapshot: AudioSnapshotInput, userId: String): Note

    suspend fun attachTranscript(noteId: String, userId: String, transcript: TranscriptInput): Note

    suspend fun attachSummary(noteId: String, userId: String, summary: SummaryInput): Note

    suspend fun deleteNote(id: String, userId: String)

    suspend fun syncPending(userId: String)
}

data class AudioSnapshotInput(
    val file: File,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val durationMs: Long,
    val sampleRate: Int = 16_000,
    val triggerType: TriggerType = TriggerType.GESTURE,
)

data class TranscriptInput(
    val fullText: String,
    val sentences: List<SentenceInput>,
    val isDiarized: Boolean,
    val detectedLanguage: String,
    val engineType: TranscriptionEngineType,
)

data class SentenceInput(
    val speaker: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String,
    val confidence: Float = 1.0f,
)

data class SummaryInput(
    val title: String,
    val executiveSummary: String,
    val keyPoints: List<String>,
    val chapterMarkers: List<ChapterMarkerInput>,
    val generationMode: AiEngineType,
)

data class ChapterMarkerInput(
    val startTimeMs: Long,
    val title: String,
    val keyBullets: List<String>,
)
