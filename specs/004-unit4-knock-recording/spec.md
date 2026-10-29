# Feature Specification: Unit 4 — Knock Gesture & Recording

**Feature Branch**: `004-unit4-knock-recording`

**Created**: 2026-10-29

**Status**: Draft

**Input**: User description: "write /speckit-specify for each units: Unit 4 Knock Gesture and Recording"

## User Scenarios & Testing

### User Story 1 - Physical Knock Gesture Capture (Priority: P1)

A student listens to an important lecture point. The student knocks twice on the desk or phone body. The system detects the double-knock through linear acceleration sensor peaks and saves the preceding audio buffer to local storage.

**Why this priority**: Gesture capture allows frictionless retroactive note saving without looking at or unlocking the phone.

**Independent Test**: Simulate or perform a double-knock vibration on the device while the audio buffer runs. Verify that the system freezes the preceding buffer and writes an audio file.

**Acceptance Scenarios**:

1. **Given** an active rolling audio buffer, **When** the linear acceleration sensor detects a double-knock gesture, **Then** the system captures the preceding audio buffer within 150 milliseconds.
2. **Given** a successful knock capture, **When** the buffer saves, **Then** the system provides brief haptic feedback confirming capture.

---

### User Story 2 - User-Configurable Rolling Audio Cache Buffer in Local Storage (Priority: P1)

The application records microphone audio continuously into a circular buffer stored in local storage. The user configures the buffer duration up to a maximum of 5 minutes. The system deletes audio frames that exceed the configured window to conserve local storage space.

**Why this priority**: Continuous caching provides the retroactive audio window while bounding local storage consumption and saving RAM.

**Independent Test**: Set the cache buffer duration to 5 minutes in settings. Let the recording run for 10 minutes. Verify that local storage maintains only the latest 5 minutes of audio.

**Acceptance Scenarios**:

1. **Given** an active recording session, **When** audio data exceeds the configured duration, **Then** the system removes older audio frames from local storage.
2. **Given** application settings, **When** the user selects a buffer duration up to 5 minutes, **Then** the system adjusts the cache size.
3. **Given** 30 minutes of continuous operation, **When** storage is inspected, **Then** disk space usage remains bounded by the configured limit.

---

### User Story 3 - Manual Save Fallback Trigger (Priority: P2)

When the user cannot perform a physical knock gesture or prefers on-screen controls, the user taps the manual save button on the recording screen. The system saves the preceding buffer identically to a gesture trigger.

**Why this priority**: Manual trigger provides accessibility and a reliable fallback when the phone is held in hand or in motion.

**Independent Test**: Tap the manual save button on the recording screen. Verify that the rolling buffer snapshot saves to a valid audio clip.

**Acceptance Scenarios**:

1. **Given** active audio caching, **When** the user taps the manual save button, **Then** the system exports the current buffer window to local storage.

---

### Edge Cases

- What happens when the phone drops or undergoes vigorous shaking? The heuristic filter evaluates spike duration, amplitude threshold, and inter-knock intervals (100 ms to 400 ms) to reject non-knock movement.
- What happens when an incoming phone call interrupts audio capture? The system pauses recording gracefully, commits any pending trigger, and resumes buffer capture after call conclusion.
- What happens when the user knocks multiple times in rapid succession? The system implements a 2-second debounce window to prevent creating redundant duplicate audio files.

## Requirements

### Functional Requirements

- **FR-001**: System MUST record microphone audio continuously into a rolling circular buffer in local storage.
- **FR-002**: System MUST allow the user to configure the buffer duration up to a maximum limit of 5 minutes.
- **FR-003**: System MUST automatically evict audio frames from local storage that exceed the configured duration.
- **FR-004**: System MUST execute a persistent foreground service with an ongoing notification to maintain background recording.
- **FR-005**: System MUST monitor `Sensor.TYPE_LINEAR_ACCELERATION` readings to detect sharp impact spikes without gravity interference.
- **FR-006**: System MUST enforce an inter-knock interval between 100 milliseconds and 400 milliseconds to validate double-knock gestures.
- **FR-007**: System MUST reject false positive movement patterns from walking, running, or tilt via high-pass amplitude filtering.
- **FR-008**: System MUST snapshot the cached audio file immediately upon gesture trigger or manual UI trigger.
- **FR-009**: System MUST write the captured buffer snapshot to a standard audio file for downstream transcription and storage.
- **FR-010**: System MUST expose recording state to UI via immutable `StateFlow` and support Dagger dependency injection.

### Key Entities

- **Rolling Buffer**: Circular audio cache file in local storage with raw PCM samples, sample rate, and a configurable duration (up to 5 minutes).
- **Gesture Event**: Sensor event record containing peak linear acceleration magnitude, inter-knock interval timing (100 ms to 400 ms), and timestamp.
- **Audio Snapshot**: Exported audio clip containing extracted audio data, start timestamp, recording duration, and trigger type.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Buffer snapshot preserves the configured duration (up to 5 minutes) of audio immediately preceding the trigger event.
- **SC-002**: Gesture detection latency from physical knock to save trigger is under 150 milliseconds.
- **SC-003**: False trigger rate remains below 1 false capture per 4 hours of standard physical device handling.
- **SC-004**: Background recording service maintains bounded local storage footprint and operates without memory leaks.

## Assumptions

- The Android device supports `Sensor.TYPE_LINEAR_ACCELERATION` or provides standard accelerometer readings with gravity filtering.
- The user grants microphone and foreground service permissions during initial setup.
- Sensor sampling rate is sufficient (at least 50 Hz) to detect sharp knock transients.
