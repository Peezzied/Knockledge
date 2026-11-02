# Research: On-Device AI Engines

## R1 - Transcription runtime
- **Decision**: whisper.cpp built with the NDK, called through a JNI binding. Reuse the structure of whisper.cpp's own `examples/whisper.android` (`CMakeLists.txt` plus `jni.c`) as the reference.
- **Rationale**: It reads the ggml files in the catalog directly, supports q5_0 and q5_1 quantization, and has a maintained Android example. The team chose it.
- **Alternatives considered**: ONNX Runtime (needs re-exported models and a custom decoder loop); Google SpeechRecognizer (not offline Whisper, no model choice).
- **Open for implementation**: pin a release tag at least 7 days old; confirm `whisper_full` parameters in that tag.

## R2 - Voice activity
- **Decision**: Use whisper.cpp's built-in VAD with the existing `ggml-silero-v6.2.0.bin`. Replace the energy check in `filterWithVad`.
- **Rationale**: `ggml-org/whisper-vad` is the model whisper.cpp's VAD expects, and the catalog already downloads it. This removes a separate preprocessing step.
- **Alternatives considered**: Keep the energy gate (does not use Silero, already judged inadequate); run Silero through ONNX (drags ONNX back in).
- **Open for implementation**: confirm the VAD parameters exist in the pinned tag. If not, fall back to transcribing without VAD and report it, rather than reintroducing the energy gate silently.

## R3 - Summarization runtime and models
- **Decision**: LiteRT-LM with `.litertlm` models from `litert-community`:
  - 2B class: `litert-community/Qwen3.5-2B`, file `Qwen3.5-2B_int8.litertlm`, about 2.12 GB (2,119,744,416 B per the model card).
  - Lightweight: `litert-community/Qwen3.5-0.8B`, file `Qwen3.5-0.8B_int8.litertlm`, about 965 MB.
  - Catalog ids stay `qwen2b` and `qwen08b` so saved settings keep working. File names, URLs and sizes change.
- **Rationale**: Same model family and sizes as today, text-only builds, Apache-2.0, and the model cards state compatibility with LiteRT-LM 0.15 or newer (checked by the author on 0.18.0).
- **Alternatives considered**: Qwen3-0.6B or Qwen3-1.7B (older family, different quality); Qwen3.5-4B (about 2.75 GB mixed int4, too large for 4 GB phones); llama.cpp for GGUF (second native build, rejected by the team).
- **Caveats to carry into tasks**:
  - Model cards warn that Qwen3.5 recurrent state needs **one Engine per conversation**. Use a fresh Engine and Conversation per rewind and close it afterwards. This matches the "free after each run" requirement.
  - The cards say "thinking is off by default" for Qwen3.5. Verify the summary prompt does not need thinking tokens and that output parsing ignores any thought channel.
  - The exact URLs follow the usual Hugging Face `resolve/main/<file>` pattern. Verify each resolves before committing the catalog.

## R4 - LiteRT-LM API and library version
- **Decision**: Pin `litertlm-android` to a specific version of 0.18.0 or newer instead of `latest.release` (a floating version breaks reproducible builds and the supply-chain guideline).
- **Rationale**: The model cards were verified on 0.18.0, and the newest cards mention 0.19.0 for GPU paths. Whatever version the Gradle build resolves today must be recorded, then pinned.
- **Open for implementation**: use the `litert-lm` skill for the Kotlin `Engine`, `EngineConfig`, `Conversation` and `sendMessageAsync` usage. Choose CPU backend first for predictability; GPU is optional.
- **Interface impact**: LiteRT-LM applies the model's own chat template. `TextEngine.complete` therefore takes the system and user text separately instead of a prebuilt Qwen prompt. This is a signature change confined to `TextEngine`, `OnDeviceSummarizer` and the one implementation. Sampling parameters the runtime does not support (for example presence penalty) are dropped. Verify which sampler settings the pinned version exposes before removing any.

## R5 - Native build setup
- **Decision**: `externalNativeBuild` with CMake 3.22.1, NDK 28.2.13676358 (both already installed), `abiFilters = arm64-v8a`. whisper.cpp is fetched by CMake at the pinned tag.
- **Rationale**: arm64 only keeps the APK small and matches the spec's target devices. NDK r28 produces 16 KB aligned libraries by default, which Android 15+ requires. Add the explicit linker flag if the toolchain does not.
- **Alternatives considered**: Git submodule (changes repo layout and needs team awareness); copying source into the repo (large diff, harder to update).
- **Risk**: first native build needs network and takes several minutes. `FetchContent` at build time breaks offline builds, so decide at implementation time whether to vendor the source instead.

## R6 - Error handling contract
- **Decision**: Engines throw typed exceptions for: model file missing or unreadable, model load failed, unsupported device (not arm64), and out of memory. `OnDeviceTranscriber` and `OnDeviceSummarizer` already return a fallback or an empty result for a missing file, and the controller layer maps exceptions to a user message. Messages go in string resources.
- **Rationale**: Satisfies FR-005 and FR-006: no placeholder text, recording kept.
- **Alternatives considered**: Return status strings (what the stubs do today, and why they produce fake transcripts).

## R7 - Removing ONNX
- **Decision**: Remove `onnxruntime-android` and `onnxruntime-desktop` and the `noCompress += "onnx"` line after confirming no other code or test uses them. Note that `testImplementation(libs.onnxruntime.desktop)` exists today.
- **Rationale**: Principle IV favors deletion, and ONNX is dead weight in the APK.
- **Open for implementation**: grep for any other ONNX usage before deleting.

## R8 - Existing user files
- **Decision**: Old GGUF Qwen files stay on disk as orphans. Optionally delete them when the new catalog loads.
- **Rationale**: They can be about 1.2 GB, which is notable on a phone, but cleanup is not in the spec.
- **Alternatives considered**: Leave them and mention in release notes.
