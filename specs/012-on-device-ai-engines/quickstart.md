# Quickstart: On-Device AI Engines

Validation guide. It proves the feature works end to end on a real arm64 phone.

## Prerequisites
- Constitution amended for on-device inference, and whisper.cpp dependency approved.
- Phone with arm64, about 4 GB RAM or more, USB debugging on.
- NDK 28.2.13676358 and CMake 3.22.1 installed (already present on the dev machine).
- Models installed through Settings: default Whisper, `vad`, and the summary model for your device.

## Build
```powershell
.\gradlew.bat :androidApp:assembleDebug
```
Expected: the native library builds for arm64-v8a and the APK installs.

## Fast checks (no device)
```powershell
.\gradlew.bat :androidApp:testDebugUnitTest --tests "two.knockledge.ai.*" --tests "two.knockledge.models.*"
```
Expected: engine fakes, model selection, missing-file and catalog-format tests pass.

## Device scenarios
1. **Offline transcript (SC-001)**: Airplane mode on. Settings -> Offline. Save a 30-second clear Filipino clip. Expect a real transcript matching the speech. Repeat 5 times.
2. **Model selection (SC-002)**: Install small, base and tiny. Select each in turn and process the same clip. Expect a transcript from each, and a different model label in logs.
3. **Offline recap (SC-003)**: With a transcript, check the recap appears for recap language Filipino, English and "match speakers".
4. **Auto mode offline (SC-004)**: Airplane mode on, Settings -> Auto. Save a clip. Expect transcript and recap. Repeat 5 times.
5. **Silence (SC-005)**: Save a clip with no speech. Expect "nothing heard" and no invented text.
6. **Bad model (SC-006)**: Delete or truncate the selected model file and process a clip. Expect a clear message, the recording kept, and no crash.
7. **Memory (SC-008)**: Watch logs during a run. Expect the speech model freed before the summary model loads.

## Device-only caveats
- No on-device result can be checked in JVM tests. Scenarios 1 to 7 need a phone.
- The first native build downloads whisper.cpp and takes several minutes.
