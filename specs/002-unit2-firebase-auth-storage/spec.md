# Feature Specification: Unit 2 — Firebase, Auth & Storage

**Feature Branch**: `002-unit2-firebase-auth-storage`

**Created**: 2026-10-29

**Status**: Draft

**Input**: User description: "write /speckit-specify for each units: Unit 2 Firebase Auth and Storage"

## Clarifications

### Session 2026-10-29

- Q: Which authentication method must the application implement for email accounts? (FR-001) → A: Email and password authentication.
- Q: Must users authenticate before accessing recording features, or does the application support guest mode? (FR-001) → A: Mandatory authentication before accessing recording features.
- Q: How must the application resolve duplicate email sign-ins between Google and email providers? (FR-001) → A: Display an error directing the user to their original sign-in provider.
- Q: Must email registration collect a user display name alongside email and password? (FR-001) → A: Collect a display name during email registration.
- Q: How must the application handle local note recordings when a user signs out? (FR-006) → A: Retain local files partitioned by user ID and filter by active user.

## User Scenarios & Testing

### User Story 1 - Secure User Authentication and Session Retention (Priority: P1)

A user opens the application and signs in securely using Google Sign-In or email and password. The application remembers the user session across restarts so the user can immediately access and save lecture notes without repeated logins.

**Why this priority**: User identity is required to isolate private audio recordings, manage database permissions, and synchronize notes safely across devices.

**Independent Test**: Complete a login flow with Google or email/password, terminate the application, and reopen it. Verify that the session remains active and the user notes load automatically.

**Acceptance Scenarios**:

1. **Given** unauthenticated state, **When** the user signs in with Google or registers with display name, email, and password, **Then** the system authorizes access and creates a persistent user profile.
2. **Given** an authenticated session, **When** the application restarts, **Then** the application restores user context within 1 second.
3. **Given** unauthenticated state, **When** the user launches the application, **Then** the system presents the sign-in screen and prevents access to recording features until authentication succeeds.
4. **Given** an existing email and password account, **When** the user attempts Google sign-in with the same email, **Then** the system displays an error directing the user to sign in with their password.

---

### User Story 2 - Offline-First Local Storage (Priority: P1)

When offline, the user saves audio clips and metadata. The system stores everything locally on the device disk and internal database. The user can view, organize, and listen to all local recordings without internet access.

**Why this priority**: The circular buffer capture must not drop recordings due to absent or unstable network connections.

**Independent Test**: Enable airplane mode, trigger a note save, and open the note detail. Verify that audio playback and note details work locally.

**Acceptance Scenarios**:

1. **Given** offline mode, **When** a save trigger executes, **Then** the system writes the audio file and note metadata to local storage within 200 milliseconds.
2. **Given** existing local recordings, **When** the user browses the notes list offline, **Then** all saved recordings appear with complete titles and timestamps.

---

### User Story 3 - Automatic Cloud Synchronization (Priority: P2)

When an internet connection becomes available, the system uploads newly saved local audio clips and synchronizes note metadata to the cloud database.

**Why this priority**: Cloud backup secures recordings against device loss and enables access from other devices.

**Independent Test**: Create a note offline, connect the device to the internet, and verify the record and audio appear in the cloud database and cloud storage.

**Acceptance Scenarios**:

1. **Given** pending local notes and restored network connection, **When** synchronization starts, **Then** the system uploads audio files and syncs metadata records in the background.
2. **Given** network disconnection during an upload, **When** connection returns, **Then** the system resumes upload progress without creating duplicate records.

---

### Edge Cases

- What happens when a user logs out? The system cancels active network uploads and retains local audio files and database records partitioned by user ID, accessible only when that user authenticates again.
- What happens when local storage space is nearly full? The system warns the user before buffer allocation and prioritizes existing saved clips over unconfirmed cache data.
- What happens when a cloud document conflicts with a local modification? The system applies a last-write-wins timestamp policy to resolve conflict without user intervention.
- What happens when an unauthenticated user opens the app without internet connectivity? The system shows the sign-in screen with an offline notice requiring network connectivity to complete initial authentication.
- What happens when a user attempts sign-in with Google but already has an email/password account with the same email? The system displays an error message directing the user to sign in with their email and password.

## Requirements

### Functional Requirements

- **FR-001**: System MUST authenticate users via Google Sign-In and email with password, and maintain secure persistent user session tokens.
- **FR-001a**: System MUST validate email format and minimum password requirements locally before credential submission.
- **FR-001b**: System MUST require authentication before granting access to recording features and local note data.
- **FR-001c**: System MUST prevent duplicate accounts for a single email address and display a directive error message when an incoming sign-in provider conflicts with an existing provider.
- **FR-001d**: System MUST collect a display name during email registration and extract the display name during Google Sign-In.
- **FR-002**: System MUST store captured audio clips and note metadata in local device storage immediately upon capture.
- **FR-003**: System MUST associate each saved note and audio clip with the authenticated user ID.
- **FR-004**: System MUST synchronize note metadata to the remote cloud database whenever network connectivity is active.
- **FR-005**: System MUST upload audio recording files to secure cloud storage whenever network connectivity is active.
- **FR-006**: System MUST enforce access control rules so that users access only their own private recordings and notes, partitioning local storage queries and cloud records strictly by user ID.
- **FR-007**: System MUST track synchronization status for each item and retry failed uploads automatically.

### Key Entities

- **User Profile**: Represents user account information, including unique user identifier, display name, email address, provider type, and session validity.
- **Note Record**: Represents saved lecture notes, containing note identifier, user identifier, local file path, remote storage reference, title, summary, and sync status.
- **Sync Queue Item**: Represents pending cloud tasks with item identifier, action type, retry count, and sync state.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Local note persistence completes in under 200 milliseconds following a save trigger.
- **SC-002**: Local notes and audio files remain 100% accessible and playable in complete offline conditions.
- **SC-003**: Cloud sync initiates within 3 seconds of network connection recovery.
- **SC-004**: Zero user data loss occurs across unexpected app terminations or network failures.

## Assumptions

- The device operating system provides sandboxed internal file storage for audio files.
- Firebase Auth handles identity verification and session renewal.
- Cloud Firestore and Firebase Storage enforce security rules scoped to the user UID.
