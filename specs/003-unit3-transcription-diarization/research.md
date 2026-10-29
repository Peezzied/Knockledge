# Phase 0 Research: Unit 3 — Transcription & Diarization

## Decision 1: Online Speech Transcription & Speaker Diarization Engine

- **Decision**: Use Firebase AI Logic (Gemini 1.5 Flash) with audio blob ingestion to transcribe and diarize speech in a single operation.
- **Rationale**: 
  - Gemini 1.5 Flash natively processes PCM/WAV audio buffers without requiring live microphone streaming.
  - Automatically identifies languages (English, Tagalog, and mixed Taglish) without rigid language constraints.
  - Generates structured JSON containing speaker turn labels (`Speaker 1`, `Speaker 2`) and millisecond start/end timestamps.
  - Adheres to the Knockledge Constitution (Principle 7: Firebase AI for cloud-assisted processing).
- **Alternatives Considered**:
  - *Google Cloud Speech-to-Text v2 (Chirp)*: Rejected due to extra GCP service credentials and API keys configuration when Firebase AI is already available in the project stack.
  - *Android SpeechRecognizer Platform API*: Rejected because it only records live microphone streams, cannot ingest pre-recorded buffer chunks, and lacks speaker diarization.
  - *Hosted Whisper API*: Rejected because raw Whisper output lacks speaker diarization, requiring an additional complex clustering pass.

---

## Decision 2: Audio Conditioning & Signal Normalization

- **Decision**: Reuse the existing `AudioGain` module in `group.two.knockledge.core` along with amplitude filtering to normalize speech gain and eliminate steady-state silence.
- **Rationale**:
  - `AudioGain.kt` is already implemented and unit-tested in the repository (`AudioGainTest.kt`).
  - Implements Ponytail Simplicity (Constitution Principle IV) by reusing standard library Kotlin code instead of introducing external digital signal processing libraries.
- **Alternatives Considered**:
  - *External DSP / NoiseSuppression libraries*: Rejected to avoid adding unapproved third-party dependencies and build overhead.

---

## Decision 3: Offline Transcription & Speaker Separation Boundary

- **Decision**: On-device Whisper engine (`AndroidSpeechEngine`) generates plain text transcripts without speaker separation. When offline, all sentences default to a single generic `"Speaker"` label.
- **Rationale**:
  - Running on-device speaker diarization models (e.g., PyAnnote ONNX) requires high memory (>500MB) and causes frame drops on mobile chipsets.
  - Adheres strictly to the Minimum Viable Hackathon Build (Constitution Principle I: 3-day limit) by delivering immediate readable text offline.
- **Alternatives Considered**:
  - *On-device acoustic clustering*: Rejected due to high implementation complexity and memory risk within the hackathon timeline.
  - *Queuing offline audio for cloud diarization*: Rejected as deferred post-MVP scope; offline transcripts should remain immediately usable.

---

## Decision 4: Online Error & Timeout Fallback Policy

- **Decision**: Implement a fail-fast timeout (10 seconds) on the Firebase AI cloud call. Upon network error or timeout, switch immediately to the local on-device Whisper engine.
- **Rationale**:
  - In intermittent classroom or lecture hall connectivity, retry loops stall user interaction.
  - Switching immediately guarantees transcript availability without user-facing failure.
- **Alternatives Considered**:
  - *Exponential backoff retry*: Rejected because waiting 30+ seconds for retries causes application stalls.
  - *User-facing error dialog*: Rejected because automatic fallback delivers the transcript with zero user friction.
