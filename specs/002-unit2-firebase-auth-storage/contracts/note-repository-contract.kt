package group.two.knockledge.data

import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Unified Repository Contract exposed by Unit 2.
 * Serves as the persistence backbone ingesting data from Unit 4, Unit 3, and Unit 1.
 */
interface NoteRepository {
    /**
     * Observes all notes belonging to the specified user, ordered by creation date descending.
     */
    fun observeNotes(userId: String): Flow<List<Note>>

    /**
     * Fetches a specific note by ID for the active user.
     */
    suspend fun getNote(id: String, userId: String): Note?

    /**
     * Unit 4 Ingestion:
     * Creates a new persistent note from a raw audio snapshot export.
     * Moves the audio snapshot file to permanent storage and inserts the initial record.
     */
    suspend fun createFromSnapshot(snapshot: AudioSnapshotInput, userId: String): Note

    /**
     * Unit 3 Ingestion:
     * Attaches transcription and diarization results to an existing note record.
     */
    suspend fun attachTranscript(noteId: String, userId: String, transcript: TranscriptInput): Note

    /**
     * Unit 1 Ingestion:
     * Attaches AI-generated executive summary, title, and chapter markers to an existing note.
     */
    suspend fun attachSummary(noteId: String, userId: String, summary: SummaryInput): Note

    /**
     * Deletes a note locally and marks it for remote cloud deletion.
     */
    suspend fun deleteNote(id: String, userId: String)

    /**
     * Triggers synchronization of all pending queue items for the user.
     */
    suspend fun syncPending(userId: String)
}

/**
 * Data ingested from Unit 4 (Knock Recording & Audio Buffer).
 */
data class AudioSnapshotInput(
    val file: File,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val durationMs: Long,
    val sampleRate: Int = 16_000,
    val triggerType: TriggerType = TriggerType.GESTURE,
)

/**
 * Data ingested from Unit 3 (Transcription & Diarization).
 */
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

/**
 * Data ingested from Unit 1 (AI Engine).
 */
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
