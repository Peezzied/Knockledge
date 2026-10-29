# Research: Unit 4 — Knock Gesture & Recording

This document records architectural decisions for the knock gesture detector and the local storage rolling buffer.

## Decision 1: Local Storage Circular Audio Buffer

### Decision
Store rolling audio cache in app-specific cache directory as a set of fixed 5-second PCM segment files with a rolling manifest, or a fixed-capacity circular binary file. We choose a segmented chunk file approach (e.g. `cache/buffer/chunk_XXXXX.pcm`) with a manifest or timestamp-based index.

### Rationale
- Standard Android storage allows writing small sequential PCM files without heavy random disk seeking.
- Older files beyond the configured duration (up to 5 minutes) delete cleanly using standard `File.delete()`.
- Prevents out-of-memory errors by keeping PCM sample buffers off the JVM heap.
- Snapshot operations concatenate the active segment files into a single WAV or PCM audio file for downstream transcription.

### Alternatives Considered
- **In-Memory Ring Buffer (`ShortArray`)**: Rejected by user requirement. Consumes up to 10 MB heap memory for 5 minutes of 16 kHz audio.
- **Single RandomAccessFile Ring**: High complexity with wrapping file pointers. Corrupted pointers can break recording during unexpected crashes.
- **Room Database BLOBs**: High SQLite overhead and garbage collection pressure for streaming high-frequency raw audio.

---

## Decision 2: Heuristic Linear Acceleration Knock Detection

### Decision
Use Android `Sensor.TYPE_LINEAR_ACCELERATION` with a heuristic peak detector and timing window:
- Sampling rate: `SensorManager.SENSOR_DELAY_GAME` (50 Hz to 100 Hz).
- Impact threshold: Acceleration magnitude $\sqrt{x^2 + y^2 + z^2} \ge 12.0\text{ m/s}^2$.
- Inter-knock timing window: Second peak must occur between 100 ms and 400 ms after the first peak.
- Cooldown debounce: 2000 ms silence after a confirmed trigger to prevent duplicate files.
- Sensor fallback: If `Sensor.TYPE_LINEAR_ACCELERATION` is unavailable, use `Sensor.TYPE_ACCELEROMETER` with a high-pass filter ($y[i] = \alpha (y[i-1] + x[i] - x[i-1])$) to remove gravity.

### Rationale
- Zero third-party dependencies and zero machine learning binary models.
- Minimal CPU overhead and fast trigger latency (< 15 ms).
- Fits within the strict 3-day hackathon timeline (Ponytail simplicity).

### Alternatives Considered
- **TapTap / Google Columbus with TensorFlow Lite**: Rejected due to binary `.tflite` model complexity, licensing, and heavy native runtime footprint.
- **Audio Microphone Knock Detection**: Vulnerable to ambient room noise, loud classroom voices, and echo feedback.

---

## Decision 3: User-Configurable Buffer Duration

### Decision
Store the user-selected buffer duration (60 seconds to 300 seconds, default 300 seconds) in `AppSettings` via `KeyValueStore`. Pass the setting directly to the rolling buffer controller.

### Rationale
- Reuses existing `AppSettings` and `KeyValueStore` infrastructure.
- Allows user adjustment in settings UI between 1 and 5 minutes.
- Enforces an upper limit of 5 minutes (300 seconds) to satisfy specification constraints.

### Alternatives Considered
- **Hardcoded Policy Constants**: Rejected because user explicitly required configurable buffer length.
- **Unbounded Recording Duration**: Rejected because unbounded audio buffers cause storage bloat and violate privacy.
