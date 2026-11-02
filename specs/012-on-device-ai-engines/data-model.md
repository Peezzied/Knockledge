# Data Model: On-Device AI Engines

No Room, Firestore or settings schema changes. The feature changes catalog data and adds in-memory result types.

## ModelFile (existing, data changes only)
| Field | Rule |
|-------|------|
| id | Unchanged for existing entries: `whisper`, `whisper_small`, `whisper_base`, `whisper_tiny`, `vad`, `qwen2b`, `qwen08b` |
| fileName / url / approxBytes | Qwen entries change from `.gguf` to `.litertlm` (see research R3). Whisper and VAD entries unchanged |
| Validation | The catalog may only contain formats an engine can load (FR-008). A unit test asserts every catalog file extension matches its engine |

## SummaryModelIds (existing)
`qwen2b` and `qwen08b` keep their ids so saved choices stay valid. `apple` is unchanged.

## EngineError (new, in-memory)
Failure types the engines throw and the pipeline maps to messages.

| Type | Meaning | User outcome |
|------|---------|--------------|
| ModelMissing | File absent, empty or truncated | Message naming the model to download. Recording kept |
| ModelLoadFailed | File exists but the engine rejects it | Message to re-download. Recording kept |
| UnsupportedDevice | Not arm64, or native library missing | "Offline AI not supported on this device." Cloud used in Auto mode |
| OutOfMemory | Allocation failed during load or run | Suggest a lighter model. Recording kept |

## ProcessingResult (existing note fields, no new storage)
- Transcript text, or "nothing heard".
- Recap text, or "nothing heard".
- A failure never writes placeholder text into the note.

## State transitions
`Idle -> LoadingSpeechModel -> Transcribing -> SpeechModelReleased -> LoadingSummaryModel -> Summarizing -> SummaryModelReleased -> Done`. Any step can move to `Failed(EngineError)`. Speech and summary states never overlap (FR-007).
