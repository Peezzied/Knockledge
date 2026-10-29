# Research: Unit 2 — Firebase, Auth & Storage

This document captures technology selections, architecture decisions, and contract unification strategies for Unit 2.

---

## 1. Authentication Engine & Multi-Provider Strategy

- **Decision**: Use Firebase Authentication with Google Sign-In and Email/Password, integrated through Android Credential Manager API (`androidx.credentials`).
- **Rationale**: 
  - Standardizes identity management directly on Firebase Auth.
  - Supports both Google Sign-In and Email/Password with native Android UI and autofill.
  - Enforces mandatory authentication at app launch, ensuring all downstream records (`Note`, `SyncQueueItem`) contain a guaranteed non-null `userId`.
  - Handles duplicate email collisions by rejecting incoming provider sign-in and directing the user to their original registration method (`FR-001c`).
- **Alternatives Considered**:
  - *Legacy GoogleSignInClient*: Deprecated on modern Android in favor of `CredentialManager`.
  - *FirebaseUI-Android*: Adds excessive external dependencies and UI styling conflicts with Jetpack Compose.
  - *Anonymous Auth with Account Linking*: Adds unnecessary state machine complexity and migration risks for a 3-day hackathon MVP.

---

## 2. Local Persistence Engine

- **Decision**: Android Jetpack Room with Kotlin Coroutines and immutable `Flow` streams.
- **Rationale**:
  - Mandated by the project constitution (Principle: Persistence via Room).
  - Provides compile-time SQLite query verification and zero runtime reflection overhead.
  - Integrates seamlessly with MVVM Unidirectional Data Flow (UDF) via `StateFlow` in ViewModels.
  - Allows strict user-level query partitioning (`WHERE user_id = :userId`) to ensure private data isolation across sign-outs.
- **Alternatives Considered**:
  - *SQLDelight*: Requires separate multiplatform Gradle plugin configuration and schema migration tooling.
  - *Raw SQLiteOpenHelper*: High boilerplate and prone to syntax errors.
  - *SharedPreferences / Proto DataStore*: Unsuitable for relational queries, pagination, and structured note records.

---

## 3. Remote Cloud Synchronization & Storage

- **Decision**: Cloud Firestore for structured metadata and Firebase Cloud Storage for raw audio recordings, rooted under `/users/{userId}/`.
- **Rationale**:
  - Firestore offline persistence provides native synchronization and conflict resolution (last-write-wins).
  - Security rules enforce strict user boundary isolation:
    - Firestore: `match /users/{userId}/notes/{noteId} { allow read, write: if request.auth.uid == userId; }`
    - Storage: `match /users/{userId}/audio/{noteId}.wav { allow read, write: if request.auth.uid == userId; }`
  - Audio files are stored on disk in app-private storage (`context.filesDir/audio/`) and referenced via local URIs, with remote download URLs populated upon cloud sync.
- **Alternatives Considered**:
  - *Realtime Database*: Lower query flexibility than Firestore; lacks collection-group indexing.
  - *Direct S3 / Cloudflare R2*: Adds third-party SDK dependencies and custom auth tokens, violating hackathon simplicity rules.

---

## 4. Background Sync Queue & Resiliency

- **Decision**: Coroutine-based `SyncQueueManager` backed by a Room `sync_queue` table, triggered on network state restoration.
- **Rationale**:
  - Guarantees zero data loss if connection drops mid-upload.
  - Each item tracks `retryCount`, `status` (`QUEUED`, `IN_PROGRESS`, `FAILED`), and `taskType` (`UPLOAD_AUDIO`, `SYNC_FIRESTORE`).
  - Network listener initiates sync processing within 3 seconds of connection recovery (`SC-003`).
- **Alternatives Considered**:
  - *WorkManager exclusively*: High overhead for immediate sync upon reconnection; Room queue with coroutines provides immediate execution with simpler testability on JVM.

---

## 5. Cross-Unit Domain Model Unification

- **Decision**: Establish the unified `Note` Aggregate Root in `group.two.knockledge.data.model`, providing formal ingestion adapters for Unit 4, Unit 3, and Unit 1.
- **Rationale**:
  - Unit 4 produces an audio capture (`AudioSnapshot`).
  - Unit 3 produces speech recognition sentences (`TranscriptResult`).
  - Unit 1 produces titles, executive summaries, and chapters (`AiSummaryResult`).
  - Unit 2 binds all three subsystems into a unified lifecycle:
    1. `createFromSnapshot(snapshot)` -> saves local audio file, initializes `Note` in Room.
    2. `attachTranscript(noteId, transcript)` -> updates transcript in Room, queues Firestore sync.
    3. `attachSummary(noteId, summary)` -> updates summary in Room, completes note generation.
- **Alternatives Considered**:
  - *Scattered per-unit storage*: Each unit having its own database or files leads to sync desynchronization, orphaned audio clips, and broken security isolation.
