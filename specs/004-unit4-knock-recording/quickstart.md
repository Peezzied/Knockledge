# Quickstart: Unit 4 — Knock Gesture & Recording

This guide describes how to validate the knock gesture detector and the local storage rolling audio buffer.

## Prerequisites

- Android SDK installed with platform tools (`adb`).
- Physical Android test device with accelerometer and microphone connected via USB debugging (or emulator for unit tests).
- Gradle build system.

---

## Scenario 1: Validate Heuristic Knock Detection via Unit Test

Run JVM unit tests that simulate linear acceleration time-series events to prove peak interval detection and debounce logic.

### Test Command
```bash
./gradlew :androidApp:testDebugUnitTest --tests "two.knockledge.KnockGestureDetectorTest"
```

### Expected Outcome
- Double impact peaks within 100 ms to 400 ms fire `onKnockDetected()`.
- Single peaks or peaks separated by more than 400 ms do not fire.
- Rapid successive impacts within the 2000 ms debounce window trigger only once.

---

## Scenario 2: Validate Rolling Audio Cache Eviction

Prove that the disk-backed circular buffer preserves only the configured window duration without unbounded disk consumption.

### Test Command
```bash
./gradlew :androidApp:testDebugUnitTest --tests "two.knockledge.LocalAudioRingBufferTest"
```

### Expected Outcome
- When configuring a 60-second window and streaming 180 seconds of PCM audio, stored chunks do not exceed 60 seconds (12 chunks of 5 seconds each).
- Older chunks are deleted from disk automatically.
- Calling `snapshot()` outputs a valid audio file matching the active window duration.

---

## Scenario 3: Physical Device Knock Trigger End-to-End

Deploy the app to a physical test device to verify real knock capture and haptic feedback.

### Execution Steps
1. Install and launch the application:
   ```bash
   ./gradlew :androidApp:installDebug
   ```
2. Start the recording session from the UI.
3. Wait 60 seconds for the buffer to fill.
4. Knock twice firmly on the desk or phone body.
5. Verify haptic vibration pulse occurs within 150 ms.
6. Verify an exported audio clip appears in recent recordings containing the preceding audio.
