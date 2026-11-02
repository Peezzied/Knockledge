---

description: "Task list for On-Device AI Engines"
---

# Tasks: On-Device AI Engines

**Input**: Design documents from `/specs/012-on-device-ai-engines/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/engines.md, quickstart.md

**Tests**: Included. The constitution requires fast JVM unit tests for business logic, written first. Native and LiteRT-LM paths are verified on a device through quickstart.md.

**Organization**: Tasks are grouped by user story. Paths are relative to the repo root. `androidApp/src/main/kotlin/group/two/knockledge` is abbreviated as `KT/`. `androidApp/src/test/kotlin/two/knockledge` is abbreviated as `TEST/`.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to
- Run Gradle only through PowerShell: `.\gradlew.bat ...`. Never run bulk integration tests.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Approvals recorded, dependency pinned, native build wired.

- [x] T001 Amend `.specify/memory/constitution.md`: add on-device inference (whisper.cpp via JNI, LiteRT-LM) to Technology Stack item 7, bump version to 1.4.0 (MINOR), update the Sync Impact Report and Last Amended date. The user approved this amendment. The bridge guard denies constitution edits while a handoff is `executing`, so set the 011 handoff to `blocked` first, edit, then restore it
- [x] T002 [P] Pin LiteRT-LM in `gradle/libs.versions.toml`: replace `version = "latest.release"` on line 58 with the specific version that resolves today. It must be 0.18.0 or newer. Record the resolved version in a code comment-free way (version string only)
- [x] T003 [P] Add the native build to `androidApp/build.gradle.kts`: `ndkVersion = "28.2.13676358"`, `externalNativeBuild { cmake { path = file("src/main/cpp/CMakeLists.txt"); version = "3.22.1" } }`, and `defaultConfig { ndk { abiFilters += "arm64-v8a" } }`. Do not remove ONNX yet
- [x] T004 Create `androidApp/src/main/cpp/CMakeLists.txt`: C++17, `FetchContent` for whisper.cpp pinned to a release tag at least 7 days old, build a shared library from `whisper_jni.cpp` linked to whisper, add the linker flag `-Wl,-z,max-page-size=16384`. Keep the library name in one CMake variable. Depends on T003

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared error types, interface change, catalog data. MUST complete before any user story.

**CRITICAL**: No user story work can begin until this phase is complete.

- [x] T005 [P] Write failing test `TEST/models/ModelCatalogFormatTest.kt`: every `ModelCatalog.qwenModels` entry has `fileName` ending in `.litertlm` and a URL starting with `https://huggingface.co/litert-community/`; every `ModelCatalog.whisperModels` entry ends in `.bin`; the catalog has no `.gguf` file
- [x] T006 Update `KT/models/ModelCatalog.kt` Qwen entries (ids stay `qwen2b` and `qwen08b`): `qwen2b` file `Qwen3.5-2B_int8.litertlm` from `https://huggingface.co/litert-community/Qwen3.5-2B/resolve/main/Qwen3.5-2B_int8.litertlm`, `approxBytes = 2_120_000_000L`; `qwen08b` file `Qwen3.5-0.8B_int8.litertlm` from `https://huggingface.co/litert-community/Qwen3.5-0.8B/resolve/main/Qwen3.5-0.8B_int8.litertlm`, `approxBytes = 965_000_000L`. Update the KDoc. Verify both URLs resolve before finishing. T005 must pass
- [x] T007 [P] Create `KT/ai/EngineError.kt`: a sealed class with `ModelMissing(modelId: String)`, `ModelLoadFailed(modelId: String, cause: Throwable?)`, `UnsupportedDevice`, and `OutOfMemory`. Add KDoc. Types and meanings come from `data-model.md` EngineError table
- [x] T008 [P] Add string resources to `androidApp/src/main/res/values/strings.xml`: `engine_error_model_missing` (with a `%1$s` model label), `engine_error_load_failed`, `engine_error_unsupported_device` with the text "Offline AI not supported on this device.", `engine_error_out_of_memory`, `engine_nothing_heard`
- [x] T009 Change `TextEngine` in `KT/ai/OnDeviceAi.kt` to the contract in `contracts/engines.md`: `complete(modelPath, systemPrompt, userPrompt, maxTokens, temperature, topP, topK)`. Update the stale KDoc on `SpeechEngine` and `TextEngine` (remove llama.cpp and ONNX mentions). Update every implementer and fake that no longer compiles, including `TEST/` fakes. Depends on T007

**Checkpoint**: `.\gradlew.bat :androidApp:compileDebugKotlin` passes. User story work can begin.

---

## Phase 3: User Story 1 - Offline Transcript of a Saved Clip (Priority: P1) MVP

**Goal**: Offline mode returns a real transcript from the selected Whisper model using the Silero VAD file.

**Independent Test**: Airplane mode, Offline, save a 30-second Filipino clip. Note shows a real transcript. Quickstart scenarios 1 and 5.

### Tests for User Story 1

> Write these first and confirm they FAIL.

- [x] T010 [P] [US1] Test `TEST/ai/OnDeviceTranscriberTest.kt`: with a fake `SpeechEngine`, `OnDeviceTranscriber.transcribe` returns the cleaned engine text, passes `files.transcriberModel` path, the language, the Taglish prompt only for Filipino, and the VAD path
- [x] T011 [P] [US1] Test in the same file: when `pathOf(transcriberModel)` is null, the result is an empty string and the engine is never called
- [x] T012 [P] [US1] Test in the same file: an empty engine result yields an empty transcript, with no placeholder text. Assert the result never contains "Transcription complete" or "Transcribed audio segment"

### Implementation for User Story 1

- [x] T013 [P] [US1] Create `androidApp/src/main/cpp/whisper_jni.cpp` following `contracts/engines.md` JNI surface: `initContext(modelPath): jlong` (0 on failure), `transcribe(ctx, pcm: FloatArray, language, prompt, vadModelPath, threads): String`, `freeContext(ctx)`. Use whisper.cpp built-in VAD with the Silero file when `vadModelPath` is non-null. If the pinned tag has no VAD params, transcribe without VAD and log it. Do not reintroduce an energy gate. Depends on T004
- [x] T014 [P] [US1] Create `KT/ai/WhisperNative.kt`: `external fun` declarations matching T013 and a `System.loadLibrary` call whose library name comes from a constant. A load failure maps to `EngineError.UnsupportedDevice`. KDoc on the class and each function
- [x] T015 [US1] Rewrite `AndroidSpeechEngine` in `KT/ai/AndroidEngines.kt` as a whisper.cpp engine using `WhisperNative`: convert PCM16 to floats, init, transcribe, always free in `finally`, throw `EngineError.ModelLoadFailed` when init returns 0 and `EngineError.OutOfMemory` on `OutOfMemoryError`. Delete `filterWithVad`, the energy check, and every placeholder string. Depends on T013, T014
- [x] T016 [US1] Bind the engine in `KT/di/KnockledgeComponent.kt` for the speech engine provider, keeping the existing provider structure. Depends on T015
- [x] T017 [US1] Confirm `OnDeviceTranscriber` in `KT/ai/OnDeviceAi.kt` meets T010 to T012 without behavior change. Fix only what the tests show. Depends on T010 to T012

**Checkpoint**: T010 to T012 pass. Device: quickstart scenarios 1 and 5 pass.

---

## Phase 4: User Story 2 - Offline Recap of a Transcript (Priority: P1)

**Goal**: Offline mode returns a real recap from a LiteRT-LM Qwen3.5 model in the selected recap language.

**Independent Test**: Airplane mode with a non-empty transcript. Recap appears in Filipino, English and match-speakers. Quickstart scenario 3.

### Tests for User Story 2

- [x] T018 [P] [US2] Test `TEST/ai/OnDeviceSummarizerTest.kt`: with a fake `TextEngine`, `OnDeviceSummarizer.summarize` passes the system and user prompt separately for each of `SummaryLanguages.AUTO`, `FILIPINO`, `ENGLISH` and parses the returned text
- [x] T019 [P] [US2] Test in the same file: a blank `filteredTranscript` returns `Summaries.nothingHeard` and the engine is never called
- [x] T020 [P] [US2] Test in the same file: when `files.summarizerModel` is null or its file is missing, the existing fallback path runs and no engine call happens
- [x] T021 [P] [US2] Test `TEST/pipeline/SummaryParserThoughtTest.kt`: `SummaryParser.parse` ignores a leading thought block if the model emits one, and still returns the structured summary

### Implementation for User Story 2

- [x] T022 [US2] Rewrite `AndroidTextEngine` in `KT/ai/AndroidEngines.kt` with LiteRT-LM: create one fresh `Engine` and `Conversation` per call, send `systemPrompt` and `userPrompt`, collect the generated text, close both in `finally`. Use CPU backend. Apply `maxTokens`, `temperature`, `topP`, `topK` where the pinned version supports them, and drop unsupported parameters. Delete the fixed status strings and the "Error:" string returns. Throw `EngineError.ModelLoadFailed` and `EngineError.OutOfMemory`. Use the `litert-lm` skill for the Kotlin API. Depends on T002, T009
- [x] T023 [US2] Update `OnDeviceSummarizer.runQwen` in `KT/ai/OnDeviceAi.kt` to call the new `TextEngine.complete` with `promptProvider.summarizerSystem(language)` and `promptProvider.summarizerUser(...)`. Stop calling `QwenChatFormat.build`. Depends on T009, T022
- [x] T024 [US2] If T021 fails, extend `SummaryParser` in `KT/pipeline/` to strip a leading thought block. Make only that change
- [x] T025 [US2] Bind the text engine in `KT/di/KnockledgeComponent.kt`, keeping the existing provider structure. Depends on T022

**Checkpoint**: T018 to T021 pass. Device: quickstart scenario 3 passes.

---

## Phase 5: User Story 3 - Choose and Use the Selected Whisper Model (Priority: P2)

**Goal**: The Whisper model selected in Settings is the one that runs.

**Independent Test**: Install two Whisper models, run the same clip with each selected, and see each transcript come from the selected model. Quickstart scenario 2.

### Tests for User Story 3

- [x] T026 [P] [US3] Test in `TEST/ai/OnDeviceTranscriberTest.kt`: with a `ModelFiles` fake whose `transcriberModel` is `ModelCatalog.whisperSmall`, the engine receives the path of that file and not the default
- [x] T027 [P] [US3] Test in the same file: when the selected model is missing but another Whisper model is installed, the result is empty and the engine is not called with the other model's path

### Implementation for User Story 3

- [x] T028 [US3] Fix any gap T026 and T027 expose in `KT/ai/OnDeviceAi.kt` or `KT/models/ModelStore.kt`. The expected result is no code change, because model selection was wired earlier. Document nothing beyond the passing tests
- [x] T029 [US3] Confirm the Filipino weakness hint string for small, base and tiny exists in `androidApp/src/main/res/values/strings.xml` and is shown in the transcription sheet in `KT/ui/settings/SettingsScreen.kt`. If missing, add it

**Checkpoint**: T026 and T027 pass. Device: quickstart scenario 2 passes.

---

## Phase 6: User Story 4 - Safe Failure and Fallback (Priority: P2)

**Goal**: Model and engine failures keep the recording, show a clear message, and fall back to the cloud in Auto mode.

**Independent Test**: Delete or truncate the selected model and process a clip offline. Recording kept, message shown, no crash. Quickstart scenario 6.

### Tests for User Story 4

- [x] T030 [P] [US4] Test `TEST/pipeline/EngineErrorMappingTest.kt`: each `EngineError` type maps to its string resource id and a message that names the model for `ModelMissing`
- [x] T031 [P] [US4] Test `TEST/pipeline/CompositeTranscriberFallbackTest.kt`: in Auto mode with network, an `EngineError` from the offline engine falls back to the cloud engine; in Local mode the error surfaces and no cloud call happens; in Cloud mode behavior is unchanged
- [x] T032 [P] [US4] Test `TEST/pipeline/CompositeSummarizerFallbackTest.kt`: the same three rules for the summarizer

### Implementation for User Story 4

- [x] T033 [US4] Create the `EngineError` to message mapping next to the pipeline controller that surfaces failures (find the call site that catches processing errors in `KT/ui/processing/NoteProcessingViewModel.kt`), using the string resources from T008. Never write placeholder text into the note. Depends on T007, T008
- [x] T034 [US4] Update `KT/pipeline/CompositeTranscriber.kt` so `EngineError` in Auto mode with network falls back to the cloud engine and in Local mode rethrows. Depends on T031
- [x] T035 [US4] Update `KT/pipeline/CompositeSummarizer.kt` with the same rule. Depends on T032
- [x] T036 [US4] Make sure a failed processing run leaves the saved recording and note intact. Add the assertion to `TEST/ui/NoteProcessingViewModelTest.kt` if that file exists; otherwise add it to the test from T030

**Checkpoint**: T030 to T032 pass. Device: quickstart scenario 6 passes.

---

## Phase 7: Polish & Cross-Cutting Concerns

- [x] T037 [P] Remove ONNX: grep `androidApp` and `sensor` for `ai.onnxruntime` and `onnx`. If nothing else uses them, delete `onnxruntime-android` and `onnxruntime-desktop` from `androidApp/build.gradle.kts` and `gradle/libs.versions.toml`, and delete `noCompress += "onnx"` in `androidApp/build.gradle.kts`
- [x] T038 [P] Delete `QwenChatFormat` in `KT/pipeline/` and its tests if nothing else calls it
- [x] T039 [P] Update comments in `KT/models/ModelCatalog.kt` and `KT/ai/OnDeviceAi.kt` that still claim llama.cpp or GGUF. Remove the stale `scripts/models.sh` mention in `ModelCatalog` KDoc since that file does not exist
- [ ] T040 Optional cleanup: when the catalog loads, delete orphaned `.gguf` model files from the models directory in `KT/ai/AndroidModelIo.kt`. Skip if time is short
- [x] T041 Run `.\gradlew.bat :androidApp:testDebugUnitTest --tests "two.knockledge.ai.*" --tests "two.knockledge.models.*" --tests "two.knockledge.pipeline.*" --tests "two.knockledge.ui.settings.*"` and fix failures
- [ ] T042 Run `.\gradlew.bat :androidApp:assembleDebug`, install on an arm64 phone, and run every scenario in `quickstart.md`. Record results next to the checklist

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies. T004 depends on T003. T001 should happen first because it unblocks the constitution gate
- **Foundational (Phase 2)**: Depends on Setup. Blocks all stories
- **US1 (Phase 3)** and **US2 (Phase 4)**: Both depend on Phase 2 and can run in parallel. They touch `AndroidEngines.kt` in different objects, so serialize edits to that file (T015 before T022 or the reverse)
- **US3 (Phase 5)**: Depends on US1 because it tests the same transcriber
- **US4 (Phase 6)**: Depends on US1 and US2 because it handles their errors
- **Polish (Phase 7)**: Depends on every story you intend to ship

### Within Each Story

- Tests first and failing, then implementation
- JNI and native tasks before the Kotlin engine
- Engine before DI binding

### Parallel Opportunities

- T002 and T003 in Setup
- T005, T007 and T008 in Phase 2
- T010 to T012 together, T013 and T014 together
- T018 to T021 together
- T026 and T027, T030 to T032 together
- T037 to T039 in Polish

## Parallel Example: User Story 1

```text
Task: "Test TEST/ai/OnDeviceTranscriberTest.kt (T010 to T012)"
Task: "Create androidApp/src/main/cpp/whisper_jni.cpp (T013)"
Task: "Create KT/ai/WhisperNative.kt (T014)"
```

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1 and Phase 2
2. Complete Phase 3 (real offline transcript)
3. **STOP and VALIDATE**: quickstart scenarios 1 and 5 on a phone
4. Demo transcription before touching the summarizer

### Incremental Delivery

1. Setup + Foundational
2. US1: transcript (MVP)
3. US2: recap
4. US3: model selection checks
5. US4: failure handling
6. Polish and the full quickstart

### Risks to watch

- First native build downloads whisper.cpp and takes several minutes. If the build must run offline, vendor the source instead of `FetchContent` and change T004
- Qwen3.5 needs a fresh LiteRT-LM engine per conversation (T022)
- Pinned whisper.cpp tag may lack VAD params (T013 fallback)

## Notes

- [P] tasks = different files, no dependencies
- Commit after each task or logical group
- Stop at any checkpoint to validate the story independently
