# Quickstart Validation Guide: Unit 3 — Transcription & Diarization

This document provides runnable verification procedures to validate Unit 3 components against acceptance criteria.

## Prerequisites

1. Android SDK installed with Java 17 / JDK 21.
2. Device or emulator running Android 8.0+ (API 26+) for instrumented checks, or JVM for unit test suites.
3. Firebase project configuration (`google-services.json`) present in `androidApp/` with Firebase AI enabled.

---

## Validation Scenario 1: Audio Conditioning & Gain Normalization

Verify that raw PCM audio with low amplitude normalizes properly before speech recognition.

- **Test Command**:
  ```bash
  ./gradlew test --tests "two.knockledge.AudioGainTest"
  ```
- **Expected Outcome**:
  - Gain multiplier amplifies low-level speech without clipping maximum 16-bit short boundaries.
  - Tests pass in under 5 seconds.

---

## Validation Scenario 2: Online Transcription & Speaker Diarization (Firebase AI)

Verify that online transcription processes multi-speaker audio clips, detects Taglish/English, and returns speaker turn tags.

- **Verification Steps**:
  1. Initialize `FirebaseAiTranscriber` with valid Firebase AI context.
  2. Ingest a 30-second multi-speaker PCM sample (16 kHz mono).
  3. Inspect returned [TranscriptResult](data-model.md#4-transcriptresult):
     - `isDiarized` must equal `true`.
     - `sentences` list contains at least 2 distinct speaker labels (e.g., `"Speaker 1"`, `"Speaker 2"`).
     - Execution time finishes within 15 seconds (SC-001).

---

## Validation Scenario 3: Offline Transcription Fallback (Whisper)

Verify that offline transcription functions without internet access and produces single-stream text.

- **Verification Steps**:
  1. Disable network connectivity on device/test environment.
  2. Ingest a 15-second voice PCM sample.
  3. Inspect returned [TranscriptResult](data-model.md#4-transcriptresult):
     - `isDiarized` equals `false`.
     - `engineType` equals `LOCAL_WHISPER`.
     - `sentences` contains formatted text with a generic `"Speaker"` label.

---

## Validation Scenario 4: Fail-Fast Fallback on Cloud Error

Verify that cloud timeouts switch immediately to on-device Whisper.

- **Verification Steps**:
  1. Mock or induce a timeout / network failure on Firebase AI client.
  2. Invoke `Transcriber.transcribe(pcm)`.
  3. Verify that the call does not throw a user-facing exception.
  4. Verify that `engineType` resolves to `LOCAL_WHISPER` and returns valid plain text without hanging.
