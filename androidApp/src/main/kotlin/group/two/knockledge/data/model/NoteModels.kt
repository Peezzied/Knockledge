package group.two.knockledge.data.model

enum class AuthProvider { GOOGLE, EMAIL_PASSWORD }

enum class SyncStatus { LOCAL_ONLY, SYNCING, SYNCED, SYNC_FAILED }

enum class TriggerType { GESTURE, MANUAL }

enum class TranscriptionEngineType { CLOUD_FIREBASE_AI, LOCAL_WHISPER }

enum class AiEngineType { CLOUD_FIREBASE_AI, LOCAL_ON_DEVICE }

enum class SyncTaskType { UPLOAD_AUDIO, SYNC_FIRESTORE }

enum class QueueStatus { QUEUED, PROCESSING, FAILED }

data class UserProfile(
    val uid: String,
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
    val provider: AuthProvider,
    val createdAtEpochMs: Long,
)

data class AudioMetadata(
    val localPath: String,
    val remoteUrl: String? = null,
    val durationMs: Long,
    val sampleRate: Int,
    val triggerType: TriggerType,
)

data class Sentence(
    val speaker: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String,
    val confidence: Float,
)

data class TranscriptMetadata(
    val fullText: String,
    val sentences: List<Sentence>,
    val isDiarized: Boolean,
    val detectedLanguage: String,
    val engineType: TranscriptionEngineType,
)

data class ChapterMarker(
    val startTimeMs: Long,
    val title: String,
    val keyBullets: List<String>,
)

data class SummaryMetadata(
    val executiveSummary: String,
    val keyPoints: List<String>,
    val chapterMarkers: List<ChapterMarker>,
    val generationMode: AiEngineType,
)

data class Note(
    val id: String,
    val userId: String,
    val title: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val audio: AudioMetadata,
    val transcript: TranscriptMetadata? = null,
    val summary: SummaryMetadata? = null,
    val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
)
