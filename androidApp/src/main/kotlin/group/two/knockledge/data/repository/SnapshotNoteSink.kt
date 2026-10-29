package group.two.knockledge.data.repository

import group.two.knockledge.data.AudioSnapshotInput
import group.two.knockledge.data.AuthRepository
import group.two.knockledge.data.NoteRepository
import group.two.knockledge.data.SentenceInput
import group.two.knockledge.data.SummaryInput
import group.two.knockledge.data.TranscriptInput
import group.two.knockledge.data.TriggerType
import group.two.knockledge.data.model.AiEngineType
import group.two.knockledge.pipeline.Summary
import group.two.knockledge.pipeline.TranscriptResult
import java.io.File
import group.two.knockledge.data.model.TriggerType as ModelTriggerType
import group.two.knockledge.data.model.TranscriptionEngineType as ModelEngineType
import group.two.knockledge.pipeline.TranscriptionEngineType as PipelineEngineType

fun interface NoteSink {
    suspend fun record(
        file: File,
        startEpochMs: Long,
        endEpochMs: Long,
        trigger: TriggerType,
        transcript: TranscriptResult,
        summary: Summary,
    )
}

class SnapshotNoteSink(
    private val notes: NoteRepository,
    private val auth: AuthRepository,
) : NoteSink {
    override suspend fun record(
        file: File,
        startEpochMs: Long,
        endEpochMs: Long,
        trigger: TriggerType,
        transcript: TranscriptResult,
        summary: Summary,
    ) {
        val user = auth.currentUser.value ?: return
        val note = notes.createFromSnapshot(
            AudioSnapshotInput(
                file = file,
                startEpochMs = startEpochMs,
                endEpochMs = endEpochMs,
                durationMs = endEpochMs - startEpochMs,
                triggerType = ModelTriggerType.valueOf(trigger.name),
            ),
            user.uid,
        )
        notes.attachTranscript(
            note.id,
            user.uid,
            TranscriptInput(
                fullText = transcript.fullText,
                sentences = transcript.sentences.map {
                    SentenceInput(it.speaker, it.startTimeMs, it.endTimeMs, it.text, it.confidence)
                },
                isDiarized = transcript.isDiarized,
                detectedLanguage = transcript.detectedLanguage,
                engineType = ModelEngineType.valueOf(transcript.engineType.name),
            ),
        )
        notes.attachSummary(
            note.id,
            user.uid,
            SummaryInput(
                title = summary.title,
                executiveSummary = summary.gist,
                keyPoints = summary.points,
                chapterMarkers = emptyList(),
                generationMode = if (transcript.engineType == PipelineEngineType.CLOUD_FIREBASE_AI) {
                    AiEngineType.CLOUD_FIREBASE_AI
                } else {
                    AiEngineType.LOCAL_ON_DEVICE
                },
            ),
        )
    }
}
