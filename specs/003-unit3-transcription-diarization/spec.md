# Feature Specification: Unit 3 — Transcription & Diarization

**Feature Branch**: `003-unit3-transcription-diarization`

**Created**: 2026-10-29

**Status**: Draft

**Input**: User description: "write /speckit-specify for each units: Unit 3 Transcription and Diarization"

## Clarifications

### Session 2026-10-29

- Q: Which cloud service will the system use for online buffered audio transcription and speaker diarization? → A: Firebase AI (Gemini 1.5 Flash) ingesting buffered audio for automatic language detection (including Taglish) and native speaker diarization.
- Q: How should the system handle speaker diarization when transcribing offline without network access? → A: Offline transcription produces a plain text transcript without speaker separation (single "Speaker" label) to conserve device memory and fit the 3-day MVP scope.
- Q: How should the system respond when an online transcription request fails or times out? → A: Switch immediately to the local on-device Whisper engine to generate a plain text transcript without delay.
- Q: Should the system execute transcription automatically after every successful audio cache save? → A: No. Unit 3 exposes and scaffolds the speech transcription interface; automatic trigger orchestration upon cache save is handled by pipeline integration.

## User Scenarios & Testing

### User Story 1 - Online Speech Transcription with Speaker Diarization (Priority: P1)

When connected to the internet, the user expects fast and accurate transcription of recorded audio. The system processes the audio through noise reduction and submits the buffered audio to Firebase AI (Gemini 1.5 Flash). The output contains readable text with automatic language detection (including English and Taglish) separated by identified speaker turns.

**Why this priority**: Accurate text transcription with distinct speaker labeling forms the foundation for summaries and study notes.

**Independent Test**: Feed a 30-second multi-speaker audio clip with network connected. Verify that the output transcript shows correct text and separated speaker segments within 15 seconds.

**Acceptance Scenarios**:

1. **Given** an online connection and recorded audio, **When** speech processing runs, **Then** the system outputs a transcript with distinct speaker labels.
2. **Given** a 30-second audio clip, **When** cloud speech processing finishes, **Then** total transcription time does not exceed 15 seconds.

---

### User Story 2 - Offline On-Device Speech Transcription (Priority: P2)

When the user records audio without internet access, the system runs an on-device speech-to-text model. The user obtains a usable plain text transcript directly on the device without speaker diarization.

**Why this priority**: Offline transcription enables study and review in classrooms or transit without internet access.

**Independent Test**: Disable internet connectivity and process a 15-second voice clip. Verify that the system generates a readable text transcript locally.

**Acceptance Scenarios**:

1. **Given** an offline device, **When** a user triggers speech recognition, **Then** the local speech engine converts speech to text without network requests and labels it under a single speaker.
2. **Given** background ambient classroom noise, **When** local transcription processes, **Then** the output remains intelligible and formatted.

---

### User Story 3 - Audio Conditioning and Voice Enhancement (Priority: P3)

Audio recorded from a phone on a lecture table often suffers from low volume or HVAC hum. The system applies noise reduction, dynamic gain control, and silence trimming before speech recognition.

**Why this priority**: Clean input audio increases speech recognition accuracy and eliminates spurious background noise words.

**Independent Test**: Provide an audio clip with background fan noise. Verify that speech recognition accuracy improves compared to raw unprocessed audio.

**Acceptance Scenarios**:

1. **Given** raw audio with low amplitude, **When** pre-processing completes, **Then** speech signal volume normalizes without digital clipping.
2. **Given** background steady noise, **When** filtering applies, **Then** non-speech frequencies attenuate before transcription.

---

### Edge Cases

- What happens when two speakers talk simultaneously? The diarization engine assigns text to the primary dominant speaker and marks the overlap region.
- What happens when the audio contains only ambient silence or room noise? The system detects absence of voice activity and flags the clip as having no intelligible speech.
- What happens when a speaker has a heavy accent or speaks technical jargon? The system uses confidence scores to preserve best phonetic matches and flags low-confidence phrases.
- What happens when an online transcription request fails or times out? The system switches immediately to the on-device Whisper engine to produce a plain text transcript without user-facing failure.

## Requirements

### Functional Requirements

- **FR-001**: System MUST normalize audio gain and attenuate background noise before speech recognition.
- **FR-002**: System MUST segment continuous audio into discrete speech chunks based on voice activity detection.
- **FR-003**: System MUST route speech recognition to Firebase AI (Gemini 1.5 Flash) when network access is present, providing automatic language detection (including Taglish) and speaker diarization, and MUST switch immediately to the on-device engine upon cloud error or timeout.
- **FR-004**: System MUST route speech recognition to the local on-device speech engine when network access is absent, producing plain text transcripts without speaker separation.
- **FR-005**: System MUST assign distinct speaker labels to different speaking voices during online processing; offline transcripts MUST use a single generic speaker label.
- **FR-006**: System MUST associate millisecond start and end timestamps with each transcribed sentence.
- **FR-007**: System MUST expose a clean transcription interface that accepts audio buffers and returns formatted transcript data for downstream pipeline integration.

### Key Entities

- **Audio Segment**: Processed audio buffer containing normalized PCM samples, sample rate, duration, and noise reduction status.
- **Speech Chunk**: Identified voice activity slice with start time offset, duration, and voice activity confidence.
- **Transcript Sentence**: Text representation of an utterance containing speaker label, start timestamp, end timestamp, and confidence rating.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Cloud transcription completes in under 0.5x recording duration (e.g., 30-second audio finishes within 15 seconds).
- **SC-002**: Word Error Rate (WER) remains below 12% on clear speech in quiet or moderate lecture room environments.
- **SC-003**: Speaker diarization correctly differentiates at least 2 distinct voices in over 85% of online multi-speaker tests.
- **SC-004**: On-device transcription operates without causing dropped audio frames or interface lag.

## Assumptions

- Microphones provide single-channel or stereo PCM audio at 16 kHz or higher sample rates.
- The cloud transcription service uses Firebase AI (Gemini 1.5 Flash) with support for audio payloads, multilingual auto-detection, and speaker diarization.
- The on-device engine uses a compact quantized speech recognition model (such as Whisper base or tiny) that produces single-stream transcripts without speaker diarization.
- End-to-end trigger orchestration (such as auto-triggering on knock save) is managed by pipeline integration and is decoupled from the Unit 3 transcription engine.
