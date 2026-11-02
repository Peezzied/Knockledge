# Feature Specification: On-Device AI Engines

**Feature Branch**: `012-on-device-ai-engines`

**Created**: 2026-11-02

**Status**: Draft

**Input**: User description: "Real on-device transcription and summarization for Knockledge on Android. Offline mode must produce a real transcript and a real recap from a saved clip, fully on the phone. Transcription runs whisper.cpp through a JNI binding. Summarization runs LiteRT-LM with .litertlm models. Cloud behavior must not change."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Offline Transcript of a Saved Clip (Priority: P1)

A student saves a knock-triggered clip with no network connection. The app turns the clip into a real transcript in Filipino, English or Taglish, using the language and Whisper model chosen in Settings. Silence is skipped so it is not turned into invented text.

**Why this priority**: Today Offline mode returns placeholder text. A real offline transcript is the core value of local AI and the input to every other offline feature.

**Independent Test**: Put the phone in airplane mode, set execution mode to Offline, save a clip of a spoken Filipino sentence, and check that the note shows a transcript of what was said.

**Acceptance Scenarios**:

1. **Given** Offline mode, an installed default Whisper model and a clip of clear Filipino speech, **When** the clip is processed, **Then** the note contains a transcript that matches the spoken words and not a placeholder.
2. **Given** a clip with long silences between sentences, **When** it is processed, **Then** the transcript contains no invented text for the silent parts.
3. **Given** transcription language set to English, **When** the clip is processed, **Then** the transcript is produced using the English setting.
4. **Given** a clip with no speech at all, **When** it is processed, **Then** the note records that nothing was heard instead of showing invented text.

---

### User Story 2 - Offline Recap of a Transcript (Priority: P1)

After the offline transcript is ready, the student gets a structured recap of it, written in the recap language chosen in Settings, with no network use.

**Why this priority**: The recap is the main product output. Without it, offline mode only gives raw text.

**Independent Test**: In airplane mode with a non-empty transcript, trigger processing and check that a recap appears in each of the three recap languages.

**Acceptance Scenarios**:

1. **Given** Offline mode, an installed summary model and a non-empty transcript, **When** processing finishes, **Then** the note has a recap generated from that transcript.
2. **Given** recap language set to Filipino, English or "match speakers", **When** the recap is generated, **Then** it is written in that language.
3. **Given** an empty or blank transcript, **When** processing finishes, **Then** the note records that nothing was heard and no recap is invented.

---

### User Story 3 - Choose and Use the Selected Whisper Model (Priority: P2)

The student picks one of the four Whisper models in Settings (large-v3-turbo, small, base, tiny). The model they selected is the one that runs, and the app tells them smaller models are faster but weaker at Filipino.

**Why this priority**: Selection and download already exist in Settings. This story makes the choice real.

**Independent Test**: Install two different Whisper models, run the same clip with each selected in turn, and check that each run uses the selected model.

**Acceptance Scenarios**:

1. **Given** two installed Whisper models, **When** the user selects one and processes a clip, **Then** the transcript comes from the selected model.
2. **Given** a selected model that is not installed, **When** the user processes a clip offline, **Then** the app tells the user which model to download and keeps the recording.

---

### User Story 4 - Safe Failure and Fallback (Priority: P2)

When a model file is missing, truncated or fails to load, the student sees a clear, actionable message. The recording is kept and nothing fake is shown. In Auto mode with a network, the app uses the cloud instead.

**Why this priority**: Local models can fail on weak phones or after interrupted downloads. A lost clip or a fake transcript damages trust.

**Independent Test**: Delete or truncate the selected model file, process a clip offline, and check that the recording is kept and a message names the problem.

**Acceptance Scenarios**:

1. **Given** a truncated or missing model file, **When** a clip is processed offline, **Then** the app shows a clear message, keeps the recording and shows no transcript text.
2. **Given** Auto mode and a working network, **When** the local engine cannot run, **Then** processing continues in the cloud.
3. **Given** Cloud mode, **When** a clip is processed, **Then** behavior is unchanged from today.

---

### Edge Cases

- A phone with about 4 GB RAM or less must not run speech and summary models in memory at the same time.
- A device with less memory than needed for the default summary model falls back to the lighter summary model.
- A clip longer than the rewind window setting is processed up to the window length only.
- A user leaves the app while processing runs. Processing must not freeze the interface and must finish or fail cleanly.
- A phone with an unsupported processor type must show a clear "offline AI not supported on this device" message instead of crashing.
- Very low battery or low storage must not corrupt the saved recording.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: In Offline mode, and in Auto mode when no network is available, the system MUST produce a transcript from the saved clip using only on-device processing.
- **FR-002**: The system MUST use the Whisper model selected in Settings, and the transcription language selected in Settings.
- **FR-003**: The system MUST skip non-speech segments before transcription using the voice activity model the app already downloads.
- **FR-004**: The system MUST produce a recap from a non-empty transcript using only on-device processing, in the recap language selected in Settings.
- **FR-005**: The system MUST NOT show placeholder or invented text as a transcript or recap in any case.
- **FR-006**: When a required model file is missing, truncated or fails to load, the system MUST keep the recording and show a clear message that names the problem and how to fix it.
- **FR-007**: The system MUST run model loading and processing off the main thread and release speech and summary models after each run so both are never loaded together.
- **FR-008**: The model catalog MUST list only model files that the on-device engines can load. The catalog MUST contain no model formats the engines cannot open.
- **FR-009**: The system MUST offer a lighter summary model for devices with less memory and choose it automatically below the memory threshold.
- **FR-010**: Settings MUST label the small, base and tiny Whisper models as faster but weaker at Filipino.
- **FR-011**: Cloud-mode behavior, existing settings, model selection, model download and note storage MUST remain unchanged.
- **FR-012**: The system MUST show a clear "not supported on this device" message when the device cannot run the on-device engines.
- **FR-013**: Processing logic, model selection and failure handling MUST be testable without a device, with the native engines behind interfaces the existing pipeline already uses.

### Key Entities

- **Transcription model**: One of four selectable Whisper models, with an id, label, file, download size and install state. Exactly one is selected at a time.
- **Summary model**: A downloadable on-device language model chosen by device memory or by the user in Settings.
- **Voice activity model**: The downloaded model that separates speech from silence before transcription.
- **Processing result**: The transcript or recap for a clip, or the failure reason when the models cannot run.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A 30-second clear Filipino clip is transcribed offline with the default model and returns a transcript, not placeholder text, in every one of 5 test runs.
- **SC-002**: The same 30-second clip is transcribed with each of the small, base and tiny models and each returns a transcript that identifies the spoken topic.
- **SC-003**: A recap is produced offline for a non-empty transcript in each of the 3 recap languages, with no network use.
- **SC-004**: With airplane mode on, saving a clip in Auto mode finishes with a transcript and a recap in every one of 5 test runs.
- **SC-005**: A clip with no speech produces "nothing heard" and no invented text in every one of 5 test runs.
- **SC-006**: With the selected model file deleted or truncated, 100% of test runs keep the recording and show a clear message, and none crash.
- **SC-007**: The interface stays responsive while processing runs: no frozen screen longer than 1 second during a processing run.
- **SC-008**: Speech and summary models are never loaded at the same time during a processing run.
- **SC-009**: Fast, device-free tests cover model selection, missing-file handling and the fallback rules, and pass in routine verification.

## Assumptions

- Users already have the selected models installed through the existing Settings download flow. Downloading models is not part of this feature.
- The default Whisper model (large-v3-turbo) is the quality baseline. The small, base and tiny models are acceptable when they return a recognizable transcript of a clear reference clip. No numeric accuracy target applies.
- Target devices are 64-bit ARM phones with about 4 GB RAM or more. Other processor types are out of scope and show the not-supported message.
- Whisper transcription is built on whisper.cpp with a JNI binding, and the voice activity step uses the Silero file the app downloads. Summarization uses LiteRT-LM with `.litertlm` models. These choices came from the team before planning.
- The summary model catalog will change from GGUF files to `.litertlm` files. The exact models and URLs are chosen at planning time after checking what the `litert-community` collection offers.
- Adding whisper.cpp source or binaries is a new external dependency and needs the team's approval at planning time, per the constitution.
- The constitution lists Firebase AI as the AI inference stack and does not mention on-device inference. This feature likely needs a constitution amendment to add on-device engines. Amendments need explicit user approval.
- Cloud processing, speaker diarization, streaming transcription, iOS, new Whisper model sizes and settings UI redesign are out of scope.
- This feature supersedes the on-device parts of specs 003 and 009 where they assumed engines that do not exist in the current build.
