# Data Model: Unit 3 — Transcription & Diarization

## Entities

### 1. `AudioSegment`
Represents preprocessed audio data ready for transcription.

| Field | Type | Description | Validation |
|-------|------|-------------|------------|
| `pcm` | `ShortArray` | 16-bit PCM audio samples | Not empty, length matches duration |
| `sampleRate` | `Int` | Sampling rate in Hertz | Must equal `16000` |
| `durationMillis` | `Long` | Audio duration in milliseconds | `> 0` and `<= 300000` (5 min max) |
| `isGainNormalized` | `Boolean` | Flag indicating whether gain conditioning ran | Default `true` after preprocessing |

---

### 2. `SpeechChunk`
Represents an active voice activity segment identified by silence trimming.

| Field | Type | Description | Validation |
|-------|------|-------------|------------|
| `offsetMillis` | `Long` | Offset from the beginning of the audio segment | `>= 0` |
| `durationMillis` | `Long` | Duration of the speech chunk | `> 0` |
| `pcm` | `ShortArray` | Audio samples within this speech chunk | Size equals `(durationMillis * sampleRate) / 1000` |
| `energyScore` | `Float` | Voice energy indicator | Range `[0.0, 1.0]` |

---

### 3. `TranscriptSentence`
Represents an individual sentence or utterance with speaker identification and timestamps.

| Field | Type | Description | Validation |
|-------|------|-------------|------------|
| `speaker` | `String` | Speaker turn label (e.g., `"Speaker 1"`, `"Speaker 2"`, or generic `"Speaker"`) | Non-blank |
| `startTimeMs` | `Long` | Millisecond offset where the sentence starts | `>= 0` |
| `endTimeMs` | `Long` | Millisecond offset where the sentence ends | `> startTimeMs` |
| `text` | `String` | Formatted transcription text | Non-blank |
| `confidence` | `Float` | Confidence rating of the recognition | Range `[0.0, 1.0]` |

---

### 4. `TranscriptResult`
The consolidated output returned by the `Transcriber` interface.

| Field | Type | Description | Validation |
|-------|------|-------------|------------|
| `sentences` | `List<TranscriptSentence>` | Ordered list of timestamped sentences | Non-empty on successful recognition |
| `fullText` | `String` | Combined plain text transcript | Non-blank |
| `isDiarized` | `Boolean` | True if distinct speaker labels exist; false if single generic speaker | True for online; false for offline fallback |
| `detectedLanguage` | `String` | Primary detected language code (e.g., `"en"`, `"fil"`, `"taglish"`) | Non-blank |
| `engineType` | `TranscriptionEngineType` | Engine used to produce the transcript | `CLOUD_FIREBASE_AI` or `LOCAL_WHISPER` |

---

## Enumerations

### `TranscriptionEngineType`
- `CLOUD_FIREBASE_AI`: Transcribed and diarized using Firebase AI (Gemini 1.5 Flash).
- `LOCAL_WHISPER`: Transcribed locally using on-device Whisper ONNX engine.

---

## State Transitions & Data Flow

```text
Audio Buffer (PCM16, 16kHz)
      │
      ▼
Audio Preprocessing (Gain Normalization via AudioGain + Silence Trimming)
      │
      ├──────────────────────────────┐
      ▼ (Online: Network Available)  ▼ (Offline or Cloud Timeout / Error)
Firebase AI (Gemini 1.5 Flash)    On-Device Whisper (AndroidSpeechEngine)
      │                              │
      ▼ (Structured Diarization)     ▼ (Plain Text)
TranscriptResult (isDiarized=true) TranscriptResult (isDiarized=false, speaker="Speaker")
      │                              │
      └──────────────┬───────────────┘
                     ▼
           Downstream Pipeline
```
