# Contract: On-Device Engines

The app's own internal interfaces. The pipeline calls these and never calls native code directly.

## SpeechEngine (existing signature, unchanged)

```kotlin
interface SpeechEngine {
    fun transcribe(
        modelPath: String,
        pcm: ShortArray,        // 16 kHz mono PCM16
        language: String,       // TranscriptionLanguages.*
        prompt: String,         // spelling bias, empty for non-Filipino
        vadModelPath: String?,  // Silero file, null when not installed
        threads: Int,
    ): String
}
```

Behavior the implementation MUST keep:
- Loads the model, transcribes, then frees the model before returning, even on failure.
- Returns text only from the model. Never returns a status or placeholder string.
- Returns an empty string when the clip has no speech after VAD.
- Throws `EngineError` for load, memory and device problems (see data-model.md).
- Runs only off the main thread (the caller wraps it in `Dispatchers.Default`).

## TextEngine (signature changes)

LiteRT-LM applies the model's own chat template, so the engine receives system and user text separately.

```kotlin
interface TextEngine {
    fun complete(
        modelPath: String,
        systemPrompt: String,
        userPrompt: String,
        maxTokens: Int,
        temperature: Float,
        topP: Float,
        topK: Int,
    ): String
}
```

Notes:
- `nCtx`, `threads` and `presencePenalty` leave the interface unless the pinned LiteRT-LM version exposes them. Confirm before removing (research R4).
- Uses one fresh Engine and Conversation per call and closes both before returning.
- Returns generated text only. Throws `EngineError` on failure.
- Caller: `OnDeviceSummarizer` only. `QwenChatFormat` is no longer called for the Qwen path and is deleted if nothing else uses it.

## JNI surface (internal, `WhisperNative.kt`)

| Function | Purpose |
|----------|---------|
| `initContext(modelPath: String): Long` | Loads the ggml file; returns 0 on failure |
| `transcribe(ctx: Long, pcm: FloatArray, language: String, prompt: String, vadModelPath: String?, threads: Int): String` | Runs the model and returns text |
| `freeContext(ctx: Long)` | Releases the model |

Rules: the library name and exported function names live in constants and one place only. A failed `System.loadLibrary` maps to `UnsupportedDevice`.

## Test seam

Unit tests use fake `SpeechEngine` and `TextEngine` implementations. They cover model selection, missing file, load failure, empty transcript, and the Auto-mode cloud fallback.
