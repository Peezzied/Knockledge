# Feature Specification: Unit 1 — AI Engine

**Feature Branch**: `001-unit1-ai-engine`

**Created**: 2026-10-29

**Status**: Draft

**Input**: User description: "write /speckit-specify for each units: Unit 1 AI Engine"

## User Scenarios & Testing

### User Story 1 - Online Transcript Summarization and Structuring (Priority: P1)

When connected to the internet, the user needs an accurate structured summary of recorded speech. The system sends the transcript to the cloud AI service. The cloud service returns a concise note title, an executive summary, and key discussion points.

**Why this priority**: Structured notes provide the primary user value for lecture and meeting capture.

**Independent Test**: Provide a sample text transcript with network connected. Verify that the system outputs a note title, a summary, and structured bullet points within 5 seconds.

**Acceptance Scenarios**:

1. **Given** an active network connection and a valid transcript, **When** the user saves a recording, **Then** the system produces a structured summary with title and key points within 5 seconds.
2. **Given** a multi-speaker lecture transcript, **When** cloud analysis finishes, **Then** the output displays distinct topical sections.

---

### User Story 2 - Offline On-Device Note Summarization (Priority: P2)

When the user has no internet access, the system must summarize transcripts locally. The system executes a compact on-device language model to create essential note notes without cloud servers.

**Why this priority**: Users frequently attend lectures or conferences in locations with poor or absent connectivity.

**Independent Test**: Disconnect network access and provide a transcript. Verify that the system generates a title and summary on the device.

**Acceptance Scenarios**:

1. **Given** an offline device and an existing transcript, **When** note processing starts, **Then** the on-device AI generates a note title and summary without network errors.
2. **Given** an on-device generation in progress, **When** the device battery level is normal, **Then** processing finishes in under 15 seconds.

---

### User Story 3 - Cloud Enhancement on Network Reconnection (Priority: P3)

When an offline note user reconnects to the internet, the user can refresh the local summary with deeper cloud inference.

**Why this priority**: Offline models produce basic summaries. Cloud models offer richer insights, topic chapters, and higher formatting quality.

**Independent Test**: Load a note created offline, connect to Wi-Fi, and request cloud enhancement. Verify updated title, summary, and chapters appear.

**Acceptance Scenarios**:

1. **Given** a note created offline and active network access, **When** the user selects refresh, **Then** the system replaces the brief summary with a comprehensive cloud-generated summary.

---

### Edge Cases

- What happens when a transcript is extremely short (under 10 words)? The system generates a short title and notes that the recording contains insufficient speech for a full summary.
- What happens when a transcript exceeds model context window limits? The system chunks the transcript by speaker intervals and creates a combined rolling summary.
- What happens when network connectivity drops during cloud generation? The system catches the network timeout and falls back to on-device summarization automatically.

## Requirements

### Functional Requirements

- **FR-001**: System MUST accept text transcripts containing timestamped speech segments.
- **FR-002**: System MUST generate a descriptive note title under 10 words.
- **FR-003**: System MUST produce an executive summary containing key discussed points.
- **FR-004**: System MUST generate timestamped chapter markers for major topic transitions.
- **FR-005**: System MUST detect network state and select cloud AI inference when online.
- **FR-006**: System MUST fallback to local on-device AI inference when offline.
- **FR-007**: System MUST allow users to refresh offline notes with cloud AI upon reconnection.

### Key Entities

- **Note Metadata**: Structured note information including title, summary, chapter markers, source transcript reference, generation mode, and creation timestamp.
- **Chapter Marker**: A distinct structural section containing a start timestamp, a section title, and key takeaway bullets.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Cloud metadata generation completes within 5 seconds for audio transcripts under 30 minutes in duration.
- **SC-002**: Offline on-device metadata generation completes within 15 seconds without network connectivity.
- **SC-003**: 100% of processed recordings result in a valid non-empty note title and summary.
- **SC-004**: Offline AI execution consumes less than 3% device battery for a 10-minute speech transcript.

## Assumptions

- Transcripts contain recognizable human speech with punctuation or timestamp delimiters.
- The mobile device possesses sufficient CPU/NPU capability to run the quantized on-device language model.
- Network requests use Firebase AI Logic for cloud inference.
