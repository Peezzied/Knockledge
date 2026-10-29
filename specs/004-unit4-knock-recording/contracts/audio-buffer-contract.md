# Contract: Local Storage Audio Ring Buffer

This contract defines the interface and behaviors for managing rolling audio cached in device local storage.

## Interface Definition

```kotlin
package group.two.knockledge.core

import java.io.File

interface LocalAudioRingBuffer {
    /**
     * Appends raw PCM16 audio samples to the active storage chunk.
     */
    fun write(chunk: ShortArray, length: Int = chunk.size)

    /**
     * Updates the maximum rolling window duration.
     * Older chunks beyond this duration are pruned immediately.
     */
    fun setWindowSeconds(seconds: Int)

    /**
     * Returns total milliseconds of audio available in the buffer.
     */
    fun bufferedMillis(): Long

    /**
     * Exports all audio in the rolling window to a single PCM/WAV file.
     * Returns the output file.
     */
    fun snapshot(): File

    /**
     * Deletes all cached buffer files and resets buffer state.
     */
    fun clear()
}
```

## Behavioral Specifications

1. **Storage Location**:
   - Chunks are stored in the application cache directory under `cache/audio_buffer/`.
   - Each chunk stores exactly 5 seconds of 16 kHz mono 16-bit PCM audio.

2. **Automatic Eviction**:
   - The buffer computes maximum allowed chunk count: $\text{ceil}(\text{durationSeconds} / 5)$.
   - When a new chunk finishes writing, the oldest chunk exceeding the limit is deleted immediately.

3. **Snapshot Isolation**:
   - Taking a snapshot locks the manifest briefly or snapshots completed chunks, concatenating them into an exported audio file.
   - Recording into a new chunk continues uninterrupted during snapshot export.

4. **Resource Cleanup**:
   - Calling `clear()` or terminating the session deletes all active temporary chunk files from storage.
