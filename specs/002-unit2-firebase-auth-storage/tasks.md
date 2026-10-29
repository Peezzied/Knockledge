# Tasks: Unit 2 — Firebase, Auth & Storage

**Input**: Design documents from `/specs/002-unit2-firebase-auth-storage/`
**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md)

## Format: `[ID] [P?] [Story] Description with exact file path`

- **[P]**: Can run in parallel (different files, no mutual dependencies)
- **[Story]**: User story identifier (`[US1]`, `[US2]`, `[US3]`)
- Exact file paths included in all descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization, dependency additions, and platform storage configuration.

- [x] T001 Configure Room (`2.6.1`) and Firebase BOM (`33.5.1` with `auth`, `firestore`, `storage`, and `credentials`) in `gradle/libs.versions.toml`
- [x] T002 [P] Apply Room KSP compiler and add Firebase and Room runtime dependencies to `androidApp/build.gradle.kts`
- [x] T003 [P] Configure app-private audio directory creation helper at `context.filesDir/audio/` in `androidApp/src/main/kotlin/group/two/knockledge/data/local/AudioFileStorage.kt`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core unified domain models, Room database schema, DAOs, and repository contracts.

**⚠️ CRITICAL**: Must complete before any user story implementation can begin.

- [x] T004 Create unified domain entities (`Note`, `AudioMetadata`, `TranscriptMetadata`, `Sentence`, `SummaryMetadata`, `ChapterMarker`, `UserProfile`) and enums (`AuthProvider`, `SyncStatus`, `TriggerType`, `TranscriptionEngineType`, `AiEngineType`) in `androidApp/src/main/kotlin/group/two/knockledge/data/model/NoteModels.kt`
- [x] T005 [P] Define `NoteRepository` contract interface per specification in `androidApp/src/main/kotlin/group/two/knockledge/data/NoteRepository.kt`
- [x] T006 [P] Define `AuthRepository` contract interface per specification in `androidApp/src/main/kotlin/group/two/knockledge/data/AuthRepository.kt`
- [x] T007 Create Room SQLite entities `NoteEntity` and `SyncQueueEntity` with foreign key and indices in `androidApp/src/main/kotlin/group/two/knockledge/data/local/NoteEntity.kt`
- [x] T008 [P] Implement Room TypeConverters for `List<Sentence>` and `List<ChapterMarker>` JSON serialization in `androidApp/src/main/kotlin/group/two/knockledge/data/local/NoteTypeConverters.kt`
- [x] T009 Define `NoteDao` with user-partitioned queries (`WHERE user_id = :userId`) and `SyncQueueDao` in `androidApp/src/main/kotlin/group/two/knockledge/data/local/NoteDao.kt`
- [x] T010 Create `KnockledgeDatabase` RoomDatabase definition with version 1 schema in `androidApp/src/main/kotlin/group/two/knockledge/data/local/KnockledgeDatabase.kt`
- [x] T011 [P] Create Dagger DI module `DataModule` providing Room database, DAOs, and repository bindings in `androidApp/src/main/kotlin/group/two/knockledge/di/DataModule.kt`

**Checkpoint**: Core foundation and database layers ready. User story implementation can begin.

---

## Phase 3: User Story 1 - Secure User Authentication and Session Retention (Priority: P1) 🎯 MVP

**Goal**: Authenticate users via Google Sign-In and Email/Password with mandatory login gate, session persistence across restarts, display name collection, and collision protection.
**Independent Test**: Complete login/registration flow with Google or email/password, restart application, and verify session restores within 1 second. Verify duplicate email sign-ins throw `AuthCollisionException`.

### Tests for User Story 1
- [x] T012 [P] [US1] Write JVM unit tests in `androidApp/src/test/kotlin/two/knockledge/data/AuthRepositoryTest.kt` verifying Google ID token ingestion, email format validation, password constraints, duplicate email collision rejection, and session retention.

### Implementation for User Story 1
- [x] T013 [US1] Implement `FirebaseAuthSource` wrapping Firebase Auth with Android Credential Manager for Google Sign-In and Email/Password credentials in `androidApp/src/main/kotlin/group/two/knockledge/data/remote/FirebaseAuthSource.kt`
- [x] T014 [US1] Implement `AuthRepositoryImpl` with `currentUser: StateFlow<UserProfile?>`, display name collection during registration, and collision handling in `androidApp/src/main/kotlin/group/two/knockledge/data/repository/AuthRepositoryImpl.kt`
- [x] T015 [US1] Implement `AuthViewModel` with UDF pattern exposing immutable `StateFlow<AuthState>` for sign-in, registration, and collision notices in `androidApp/src/main/kotlin/group/two/knockledge/ui/auth/AuthViewModel.kt`
- [x] T016 [US1] Build Jetpack Compose `AuthScreen` with stateless `@Preview` composables for Google and Email/Password authentication in `androidApp/src/main/kotlin/group/two/knockledge/ui/auth/AuthScreen.kt`
- [x] T017 [US1] Wire mandatory authentication gate in `MainActivity.kt` blocking recording features until user successfully authenticates in `androidApp/src/main/kotlin/group/two/knockledge/MainActivity.kt`

**Checkpoint**: User Story 1 complete. Authentication and persistent identity work end-to-end.

---

## Phase 4: User Story 2 - Offline-First Local Storage (Priority: P1)

**Goal**: Ingest audio snapshots from Unit 4, transcripts from Unit 3, and summaries from Unit 1 into Room SQLite in under 200 ms, isolated by user ID with offline accessibility.
**Independent Test**: Enable Airplane Mode, trigger note creation from an audio snapshot, and verify that note metadata and audio file persist on device in under 200 ms with offline playback.

### Tests for User Story 2
- [x] T018 [P] [US2] Write JVM unit tests in `androidApp/src/test/kotlin/two/knockledge/data/NoteRepositoryTest.kt` verifying audio snapshot ingestion, sub-200ms SQLite write latency, transcript attachment, AI summary attachment, and user query partitioning.

### Implementation for User Story 2
- [x] T019 [US2] Implement `NoteRepositoryImpl.createFromSnapshot()` to transfer audio snapshot files from cache to `filesDir/audio/{noteId}.wav` and insert `NoteEntity` (`syncStatus = LOCAL_ONLY`) in `androidApp/src/main/kotlin/group/two/knockledge/data/repository/NoteRepositoryImpl.kt`
- [x] T020 [US2] Implement `NoteRepositoryImpl.attachTranscript()` and `NoteRepositoryImpl.attachSummary()` to update note records and enqueue sync tasks in `androidApp/src/main/kotlin/group/two/knockledge/data/repository/NoteRepositoryImpl.kt`
- [x] T021 [US2] Implement `NoteRepositoryImpl.observeNotes(userId)` and `getNote(id, userId)` returning reactive flows strictly filtered by `userId` in `androidApp/src/main/kotlin/group/two/knockledge/data/repository/NoteRepositoryImpl.kt`
- [x] T022 [US2] Adapt existing `RecentsRepository` to delegate to `NoteRepository` for backward compatibility in `androidApp/src/main/kotlin/group/two/knockledge/data/Recents.kt`

**Checkpoint**: User Story 2 complete. Offline-first local storage persists and unifies data across all units.

---

## Phase 5: User Story 3 - Automatic Cloud Synchronization (Priority: P2)

**Goal**: Automatically upload local audio files to Firebase Storage and sync note records to Cloud Firestore within 3 seconds of network reconnection with retry resiliency.
**Independent Test**: Create offline notes, restore network connection, and verify audio uploaded to Firebase Storage and note documents synced to Firestore without duplicates.

### Tests for User Story 3
- [x] T023 [P] [US3] Write JVM unit tests in `androidApp/src/test/kotlin/two/knockledge/data/SyncEngineTest.kt` verifying sync queue task execution, upload retry backoff, and transition to `SYNCED`.

### Implementation for User Story 3
- [x] T024 [US3] Implement `FirebaseStorageSource` uploading audio WAV files to `/users/{userId}/audio/{noteId}.wav` and returning storage URLs in `androidApp/src/main/kotlin/group/two/knockledge/data/remote/FirebaseStorageSource.kt`
- [x] T025 [US3] Implement `FirestoreDataSource` creating and updating documents under `/users/{userId}/notes/{noteId}` per JSON schema in `androidApp/src/main/kotlin/group/two/knockledge/data/remote/FirestoreDataSource.kt`
- [x] T026 [US3] Implement `SyncEngine` observing network connectivity, processing `sync_queue` tasks, uploading audio blobs, synchronizing Firestore documents, and setting `syncStatus = SYNCED` in `androidApp/src/main/kotlin/group/two/knockledge/data/repository/SyncEngine.kt`
- [x] T027 [US3] Register `SyncEngine` in `KnockledgeApplication.kt` to trigger sync processing within 3 seconds of network availability in `androidApp/src/main/kotlin/group/two/knockledge/KnockledgeApplication.kt`

**Checkpoint**: User Story 3 complete. Automatic background cloud synchronization works reliably.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Multi-user local data partitioning, migration verification, and quickstart end-to-end validation.

- [x] T028 [P] Implement sign-out cleanup ensuring local database notes remain partitioned on disk and active state flows reset in `androidApp/src/main/kotlin/group/two/knockledge/data/repository/AuthRepositoryImpl.kt`
- [x] T029 [P] Add in-memory SQLite database helper and schema validation tests in `androidApp/src/test/kotlin/two/knockledge/data/local/KnockledgeDatabaseTest.kt`
- [ ] T030 Execute end-to-end quickstart validation scenarios from `quickstart.md` covering auth, offline persistence, and cloud sync

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Can start immediately.
- **Foundational (Phase 2)**: Depends on Phase 1 completion. Blocks all user stories.
- **User Story 1 (Phase 3)**: Depends on Phase 2 completion. Delivers MVP authentication gate.
- **User Story 2 (Phase 4)**: Depends on Phase 2 completion. Integrates with US1 `userId`.
- **User Story 3 (Phase 5)**: Depends on Phase 3 and Phase 4 completion. Syncs US2 notes for US1 users.
- **Polish (Phase 6)**: Depends on all user story phases completion.

### Parallel Opportunities

- In Phase 1: `T002` and `T003` can execute in parallel after `T001`.
- In Phase 2: `T005`, `T006`, `T008`, `T011` can execute in parallel.
- User Story tests (`T012`, `T018`, `T023`) can run in parallel before implementations.
- In Phase 6: `T028` and `T029` can run in parallel.

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (`T001`-`T003`).
2. Complete Phase 2: Foundational (`T004`-`T011`).
3. Complete Phase 3: User Story 1 (`T012`-`T017`).
4. **STOP and VALIDATE**: Verify login gate and Google/Email auth independently on device.

### Incremental Delivery

1. Foundation ready (`T001`-`T011`).
2. Add US1 Authentication (`T012`-`T017`) $\to$ Authenticated user profile active.
3. Add US2 Offline Storage (`T018`-`T022`) $\to$ Notes persist locally in Room in $< 200\text{ ms}$.
4. Add US3 Cloud Sync (`T023`-`T027`) $\to$ Notes and audio upload automatically to Firebase.
5. Polish & Verification (`T028`-`T030`) $\to$ Quality gate passes.
