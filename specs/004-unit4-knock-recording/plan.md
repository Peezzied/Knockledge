# Implementation Plan: Unit 4 — Knock Gesture & Recording

**Branch**: `004-unit4-knock-recording` | **Date**: 2026-10-29 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from [specs/004-unit4-knock-recording/spec.md](spec.md)

## Summary

Unit 4 implements continuous rolling audio recording cached in local device storage and double-knock gesture detection using native Android linear acceleration sensors. Users configure the cache buffer duration up to 5 minutes maximum. When the user double-knocks on the phone or surface, the sensor triggers immediate snapshotting of the preceding audio buffer to an audio file for downstream transcription, confirmed by haptic vibration feedback.

## Technical Context

**Language/Version**: Kotlin 2.0 / Android SDK 34 (minSdk 26)

**Primary Dependencies**: Android Jetpack, Kotlin Coroutines, Dagger DI

**Storage**: App-specific cache storage (`cache/audio_buffer/`) for rolling 5-second PCM audio chunks

**Testing**: JUnit 4 JVM unit tests via `./gradlew test`

**Target Platform**: Android 8.0+ (API 26+) physical devices and emulators

**Project Type**: Android Native Mobile Application

**Performance Goals**: < 150 ms gesture trigger latency, constant disk usage ($\le 9.6\text{ MB}$ for 5 minutes of 16 kHz 16-bit mono audio)

**Constraints**: Zero heap memory bloat for audio cache, zero third-party ML model dependencies, offline-capable

**Scale/Scope**: 1 sensor detector, 1 file-backed rolling buffer, 1 foreground service integration, 1 settings parameter

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Principle I: Minimum Viable Hackathon Build (3-Day Scope)**: PASS. Uses minimal heuristic peak detection and chunked local storage files. Rejects complex TensorFlow Lite or Columbus dependencies.
- **Principle II: ADHD-Driven Execution**: PASS. Clear sequential verification steps and bounded tasks.
- **Principle III: Karpathy Discipline**: PASS. Explicit interfaces and reproducing unit tests before service wiring.
- **Principle IV: Ponytail Simplicity**: PASS. Uses native platform `Sensor.TYPE_LINEAR_ACCELERATION`, standard Java/Kotlin file APIs, and standard Android `Vibrator`. Requires zero new Gradle dependencies.

## Project Structure

### Documentation (this feature)

```text
specs/004-unit4-knock-recording/
├── spec.md              # Feature specification
├── plan.md              # Implementation plan
├── research.md          # Architectural decisions & research
├── data-model.md        # Entities, validation, and state machines
├── quickstart.md        # Runnable verification guide
├── contracts/           # Component interfaces
│   ├── gesture-detector-contract.md
│   ├── audio-buffer-contract.md
│   └── service-ui-contract.md
└── tasks.md             # Generated in Phase 2 via /speckit-tasks
```

### Source Code (repository root)

```text
androidApp/src/main/kotlin/group/two/knockledge/
├── sensor/
│   ├── KnockGestureDetector.kt          # Native linear acceleration peak detector
│   └── AndroidKnockGestureDetector.kt   # SensorManager implementation with haptic feedback
├── core/
│   ├── LocalAudioRingBuffer.kt         # Disk-backed segmented circular audio buffer
│   └── RewindPolicy.kt                  # Configurable buffer duration parameters
├── di/
│   └── KnockRecordingModule.kt          # Dagger DI module with @Binds bindings
├── settings/
│   └── AppSettings.kt                   # User preference for buffer duration (60s to 300s)
└── KnockledgeService.kt                 # Foreground service coordinating sensors and audio cache

androidApp/src/test/kotlin/two/knockledge/
├── KnockGestureDetectorTest.kt          # JVM tests for peak timing, debounce, and noise rejection
└── LocalAudioRingBufferTest.kt          # JVM tests for chunk eviction, snapshotting, and cleanup
```

**Structure Decision**: Place the sensor detector in `sensor/`, disk buffer in `core/`, integrate preference into `settings/AppSettings.kt`, and coordinate via the existing `KnockledgeService.kt` foreground service.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| None | N/A | N/A |
