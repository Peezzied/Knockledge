# Quickstart: Unit 2 — Firebase, Auth & Storage

This guide defines test procedures and verification steps to validate authentication, Room persistence, audio storage, and Firestore synchronization.

---

## Prerequisites

1. Android device or virtual device running API 26+ (Android 8.0+).
2. Working network connection for initial authentication and cloud synchronization.
3. Gradle test environment (`./gradlew test`).

---

## Verification Scenarios

### Scenario 1: Authentication and Session Persistence

Prove that Google Sign-In and Email/Password registration create persistent sessions across app restarts (`FR-001`, `FR-001a`, `FR-001d`).

1. **Email Registration**:
   - Register a new account with Display Name `"Karol"`, Email `"test@knockledge.app"`, and Password `"password123"`.
   - Verify `AuthRepository.currentUser` emits a non-null `UserProfile` with `displayName == "Karol"`.
2. **Session Persistence**:
   - Terminate the application process.
   - Restart the application.
   - Verify that `AuthRepository.currentUser` restores the active session within 1 second without showing the login screen (`SC-001`).
3. **Account Collision Check**:
   - Attempt to authenticate via Google Sign-In using `"test@knockledge.app"`.
   - Verify the system throws `AuthCollisionException` and displays an error directing the user to sign in with their password (`FR-001c`).

### Scenario 2: Offline-First Audio & Note Persistence

Prove that capturing an audio snapshot persists locally in Room in $< 200\text{ ms}$ without network access (`FR-002`, `FR-003`, `SC-001`).

1. Enable Airplane Mode on the test device.
2. Trigger an audio capture export via Unit 4 (`LocalAudioRingBuffer.snapshot()`).
3. Invoke `NoteRepository.createFromSnapshot(snapshot, userId)`.
4. **Verification**:
   - Note is inserted into the local SQLite `notes` table with `syncStatus = LOCAL_ONLY`.
   - The operation completes in under 200 ms.
   - Audio file exists on device storage at `context.filesDir/audio/{noteId}.wav` and is playable.
   - The note appears immediately in `NoteRepository.observeNotes(userId)`.

### Scenario 3: Cross-Unit Pipeline Ingestion (Diarization & AI Summary)

Prove that Unit 3 transcripts and Unit 1 AI summaries attach directly to the unified Note record.

1. Generate a mock `TranscriptInput` (simulating Unit 3 output).
2. Call `NoteRepository.attachTranscript(noteId, userId, transcript)`.
3. Verify `note.transcript.sentences` and speaker labels update in Room.
4. Generate a mock `SummaryInput` (simulating Unit 1 output).
5. Call `NoteRepository.attachSummary(noteId, userId, summary)`.
6. Verify `note.title`, `note.summary.executiveSummary`, and chapter markers update in Room.

### Scenario 4: Automatic Cloud Synchronization on Reconnection

Prove that reconnecting to the internet triggers background upload of audio to Cloud Storage and note document sync to Firestore within 3 seconds (`FR-004`, `FR-005`, `SC-003`).

1. With pending offline notes created in Scenario 2, disable Airplane Mode to restore network connection.
2. The network listener triggers `SyncEngine.processQueue(userId)`.
3. **Verification**:
   - `SyncQueueItem` status transitions from `QUEUED` $\to$ `PROCESSING` $\to$ removed/completed.
   - Audio file is uploaded to Firebase Storage under `/users/{userId}/audio/{noteId}.wav`.
   - Note document is written to Cloud Firestore at `/users/{userId}/notes/{noteId}`.
   - Note `syncStatus` in Room transitions to `SYNCED`.
   - Transition begins within 3 seconds of network connection recovery.

### Scenario 5: Multi-User Local Data Isolation on Logout

Prove that logging out isolates user notes so subsequent users on the same device cannot access private recordings (`FR-006`).

1. Sign out active user `"User A"`.
2. Verify local database retains User A's recordings on disk.
3. Sign in as `"User B"`.
4. Observe `NoteRepository.observeNotes("User B")`.
5. **Verification**:
   - User B's note list displays zero notes belonging to User A.
   - Query explicitly filters `WHERE user_id = :userId`.

---

## Automated Unit & Contract Tests

Run JVM unit tests:

```bash
./gradlew test --tests "group.two.knockledge.data.*"
```

Expected output:
- `NoteRepositoryTest`: Ingestion from snapshot, transcript attachment, and summary attachment pass.
- `AuthRepositoryTest`: Google sign-in, email/password validation, and collision exceptions pass.
- `SyncQueueTest`: Offline queueing, backoff retry, and sync completion pass.
