# Implementation Plan: On-Device AI Engines

**Branch**: `012-on-device-ai-engines` | **Date**: 2026-11-02 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/012-on-device-ai-engines/spec.md`

## Summary

Offline mode must produce real output. Today `AndroidSpeechEngine` loads ggml Whisper files through ONNX Runtime (impossible) and returns placeholder text, and `AndroidTextEngine` opens a model and returns a fixed string. The plan replaces both engines behind the existing `SpeechEngine` and `TextEngine` interfaces:

- **Transcription**: whisper.cpp built with the NDK and called through a small JNI binding. whisper.cpp's built-in Silero VAD support uses the already-downloaded `ggml-silero` file, which replaces the energy gate.
- **Summarization**: LiteRT-LM (already a dependency) running `.litertlm` Qwen3.5 models from `litert-community`. The catalog's GGUF Qwen entries are replaced.
- The ONNX Runtime dependency is removed once nothing uses it.
- Failures become typed errors the pipeline already knows how to surface. No placeholder text remains anywhere.

## Technical Context

**Language/Version**: Kotlin (existing), C/C++17 for the JNI layer (new, about one file)

**Primary Dependencies**: whisper.cpp (new, native, pinned release tag), LiteRT-LM `litertlm-android` (existing; pin a version of 0.18.0 or newer instead of `latest.release`), Dagger, Room (existing). `onnxruntime-android` and `onnxruntime-desktop` are removed if no remaining use.

**Storage**: Model files in `noBackupFilesDir/models` (existing `ModelStorage`). No Room or Firestore change.

**Testing**: JUnit4/Robolectric JVM tests behind the `SpeechEngine` and `TextEngine` interfaces. Native and LiteRT paths are verified manually on a device through `quickstart.md`. No bulk integration test runs.

**Target Platform**: Android, arm64-v8a only, minSdk 26, compileSdk 37. NDK 28.2.13676358 and CMake 3.22.1 are already installed on the dev machine.

**Project Type**: mobile-app (single `androidApp` module plus `sensor`)

**Performance Goals**: UI stays responsive (no main-thread stall over 1 second). A 30-second clip transcribes in a reasonable time on the default model. No numeric latency target in the spec.

**Constraints**: Speech and summary models never loaded together. 4 GB RAM phones run the 2B summary model. Native libraries must be 16 KB page aligned (Android 15+ requirement). Cloud behavior unchanged.

**Scale/Scope**: One new native module (CMake + one JNI file), two engine rewrites, catalog entries for the summary models, and tests. No UI redesign.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Note |
|-----------|--------|------|
| I. Hackathon MVP | Pass | Smallest path to real offline output. No speculative features. Stops when the spec's success criteria pass. |
| II. ADHD execution | Pass | Applies to agent communication, not to this feature. |
| III. Karpathy discipline | Pass | Touches only the two engines, catalog entries, and DI wiring. Tests first. |
| IV. Ponytail simplicity | **Needs approval** | whisper.cpp is a new external dependency. Approval is required before implementation. Rationale in Complexity Tracking. |
| V. Android best practices | Pass | Off-main-thread inference, lifecycle-safe release of models, 16 KB alignment. |
| VI. No hard-coding | Pass | Thread counts, context size, token limits and model entries go in constants or catalog data. User messages go in string resources. |
| VII. Domain layer | Pass | No interactor or use case added. The pipeline keeps its current structure. |
| Tech stack item 7 (AI inference = Firebase AI) | **Needs amendment** | On-device inference is not in the stack. The constitution says amendments need rationale, impact analysis and explicit approval. |

**Result**: no unjustified violation. Two items are justified in Complexity Tracking and block implementation until you approve them.

**Post-design re-check**: unchanged. Phase 1 added no new dependency, interactor, or layer beyond the above.

## Project Structure

### Documentation (this feature)

```text
specs/012-on-device-ai-engines/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/
│   └── engines.md       # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit-tasks, not created here)
```

### Source Code (repository root)

```text
androidApp/
├── build.gradle.kts                         # add externalNativeBuild (CMake, arm64-v8a, NDK 28.2), pin litertlm, drop onnxruntime
├── src/main/
│   ├── cpp/
│   │   ├── CMakeLists.txt                   # builds whisper.cpp at a pinned tag + jni
│   │   └── whisper_jni.cpp                  # init/transcribe/free bridge
│   ├── kotlin/group/two/knockledge/
│   │   ├── ai/
│   │   │   ├── AndroidEngines.kt            # rewritten: WhisperCppEngine, LiteRtTextEngine
│   │   │   ├── WhisperNative.kt             # external funs + System.loadLibrary
│   │   │   └── OnDeviceAi.kt                # error mapping only, no flow change
│   │   ├── models/ModelCatalog.kt           # summary entries become .litertlm
│   │   └── di/KnockledgeComponent.kt        # bind the new engines
│   └── res/values/strings.xml               # engine error messages
└── src/test/kotlin/two/knockledge/ai/       # fakes behind SpeechEngine/TextEngine
```

**Structure Decision**: Keep the single `androidApp` module. The native build lives under `androidApp/src/main/cpp` so no new Gradle module or abstraction layer is needed.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| New dependency: whisper.cpp (Principle IV) | Only way to read the ggml Whisper files the catalog already ships, including q5 quantization and built-in Silero VAD | ONNX needs re-exported models plus a hand-written decoder; the platform has no speech API that runs offline Whisper |
| Constitution amendment: add on-device inference to the stack (item 7) | Spec requires local transcription and summarization; the constitution lists only cloud inference | None. The constitution already lists Whisper, so the amendment documents what the stack needs |
| 16 KB page alignment flags | Required for Android 15+ devices | Skipping them fails to load on newer phones |
