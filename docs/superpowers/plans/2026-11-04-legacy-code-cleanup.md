# Legacy Code and GoGitIt-Rewinder Traces Cleanup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Eliminate all orphaned files, legacy iOS stubs, obsolete SharedPreferences `Recents` persistence, and phantom documentation links inherited from antecedent `GoGitIt-Rewinder`.

**Architecture:** Remove dead files and dependencies while preserving Knockledge's active Room and Firestore storage pipeline (`NoteRepository`, `KnockledgeDatabase`). Refactor `KnockledgeController` to remove vestigial `RecentsRepository` parameters, clean up iOS `SystemModelEngine` stubs from AI engine contracts, and rename legacy `Rewinder*` test suites to `Knockledge*`.

**Tech Stack:** Kotlin 2.4.10, Android SDK 37, Dagger 2.59.2, Jetpack Compose, Room 2.8.5.

**Spec:** [legacy_audit.md](file:///C:/Users/karol/.gemini/antigravity-cli/brain/8afe9150-d3db-4453-b6c6-77e2d425f8c6/legacy_audit.md)

## Global Constraints

- **Platform Scope**: Android only (minSdk 26, compileSdk 37, targetSdk 37). Zero iOS dependencies, mocks, or platform branching.
- **Git Safety**: Do not rewrite shared git history (`git rebase -i` or `git filter-repo`) on published branches without explicit authorization. Do not run `git commit`, `git push`, or `git tag` automatically. Keep all changes in the working tree for explicit user confirmation.
- **Persistence Authority**: Room and Firestore via `NoteRepository` are the exclusive persistence mechanism for saved notes and audio.
- **Test Integrity**: Ensure `./gradlew testDebugUnitTest` compiles and passes after each task.

---

### Task 1: Delete Dead Pipeline and Core Buffering Code

**Files:**
- Delete: `androidApp/src/main/kotlin/group/two/knockledge/pipeline/SensitiveFilter.kt`
- Delete: `androidApp/src/test/kotlin/two/knockledge/SensitiveFilterTest.kt`
- Delete: `androidApp/src/main/kotlin/group/two/knockledge/core/AudioRingBuffer.kt`
- Delete: `androidApp/src/test/kotlin/two/knockledge/AudioRingBufferTest.kt`
- Modify: `androidApp/src/main/kotlin/group/two/knockledge/audio/AudioSource.kt:25-42`

**Interfaces:**
- Consumes: None (these classes are unused in production).
- Produces: None.

- [ ] **Step 1: Verify dead code status**

Verify with `git grep` that no production classes in `androidApp/src/main` reference `SensitiveFilter`, `RegexSensitiveFilter`, or `AudioRingBuffer`:
Run: `git grep "SensitiveFilter" androidApp/src/main/`
Run: `git grep "AudioRingBuffer" androidApp/src/main/`

- [ ] **Step 2: Delete orphaned files**

Remove the unused files:
```powershell
Remove-Item "androidApp/src/main/kotlin/group/two/knockledge/pipeline/SensitiveFilter.kt"
Remove-Item "androidApp/src/test/kotlin/two/knockledge/SensitiveFilterTest.kt"
Remove-Item "androidApp/src/main/kotlin/group/two/knockledge/core/AudioRingBuffer.kt"
Remove-Item "androidApp/src/test/kotlin/two/knockledge/AudioRingBufferTest.kt"
```

- [ ] **Step 3: Remove `SimulatedAudioSource` from `AudioSource.kt`**

Edit `androidApp/src/main/kotlin/group/two/knockledge/audio/AudioSource.kt` to delete lines 25-42 (`SimulatedAudioSource`).

- [ ] **Step 4: Verify test suite execution**

Run: `.\gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL (all remaining unit tests pass without compilation error).

---

### Task 2: Remove Apple Foundation Model & iOS Residual Code

**Files:**
- Modify: `androidApp/src/main/kotlin/group/two/knockledge/ai/OnDeviceAi.kt`
- Modify: `androidApp/src/main/kotlin/group/two/knockledge/ai/AiControls.kt`
- Modify: `androidApp/src/main/kotlin/group/two/knockledge/models/ModelCatalog.kt`
- Modify: `androidApp/src/main/kotlin/group/two/knockledge/pipeline/Pipeline.kt`
- Modify: `androidApp/src/main/kotlin/group/two/knockledge/settings/AppSettings.kt`
- Modify: `androidApp/src/main/kotlin/group/two/knockledge/di/KnockledgeComponent.kt`
- Modify: `androidApp/src/test/kotlin/two/knockledge/TapesAndMixingTest.kt`

**Interfaces:**
- Consumes: `TextEngine`, `SpeechEngine`, `ModelFiles`.
- Produces: `OnDeviceSummarizer` without unused iOS system engine parameter.

- [ ] **Step 1: Remove `SystemModelEngine` and iOS fallback from `OnDeviceAi.kt`**

1. Delete `interface SystemModelEngine` (lines 62-75).
2. In `class OnDeviceSummarizer`:
   - Remove `system: SystemModelEngine? = null` parameter from constructor.
   - Remove the `if (selected() == SummaryModelIds.APPLE && system != null)` block (lines 136-159).

- [ ] **Step 2: Clean up Apple references in `ModelCatalog.kt`, `Pipeline.kt`, and `AiControls.kt`**

1. In `androidApp/src/main/kotlin/group/two/knockledge/models/ModelCatalog.kt`:
   - Remove `const val APPLE = "apple"` from `SummaryModelIds`.
   - Update `qwenById` KDoc to remove reference to Apple's model.
2. In `androidApp/src/main/kotlin/group/two/knockledge/pipeline/Pipeline.kt`:
   - Remove `const val APPLE_MAX_TRANSCRIPT_CHARS: Int = 5_000` and its KDoc.
3. In `androidApp/src/main/kotlin/group/two/knockledge/ai/AiControls.kt`:
   - Remove `system: SystemModelEngine?` parameter and any Apple system branches.

- [ ] **Step 3: Remove legacy iOS setting migration in `AppSettings.kt`**

In `androidApp/src/main/kotlin/group/two/knockledge/settings/AppSettings.kt`:
Replace the `init` block:
```kotlin
    init {
        val version = store.getString(KEY_VERSION)?.toIntOrNull() ?: 1
        if (version < 2) {
            store.putString(KEY_VERSION, "2")
        }
    }
```
(Removes `if (store.getString(KEY_SUMMARY) == "apple") store.putString(KEY_SUMMARY, null)`).

- [ ] **Step 4: Update Dagger component and remove test in `TapesAndMixingTest.kt`**

1. In `androidApp/src/main/kotlin/group/two/knockledge/di/KnockledgeComponent.kt`:
   - Delete `fun systemModel(): SystemModelEngine? = null`.
   - Remove `systemModel` parameters passed to `aiControls` and `onDeviceSummarizer`.
2. In `androidApp/src/test/kotlin/two/knockledge/TapesAndMixingTest.kt`:
   - Delete `@Test fun qwenBecomesDefaultOnceForOldAppleChoice()`.

- [ ] **Step 5: Verify AI test execution**

Run: `.\gradlew testDebugUnitTest --tests "two.knockledge.ai.*" --tests "two.knockledge.pipeline.*"`
Expected: BUILD SUCCESSFUL.

---

### Task 3: Decouple & Remove Legacy SharedPreferences Recents Storage

**Files:**
- Modify: `androidApp/src/main/kotlin/group/two/knockledge/KnockledgeController.kt`
- Modify: `androidApp/src/main/kotlin/group/two/knockledge/KnockledgeGraph.kt`
- Modify: `androidApp/src/main/kotlin/group/two/knockledge/di/KnockledgeComponent.kt`
- Delete: `androidApp/src/main/kotlin/group/two/knockledge/data/Recents.kt`
- Modify: `androidApp/src/test/kotlin/two/knockledge/KnockRecordingViewModelTest.kt`
- Modify: `androidApp/src/test/kotlin/two/knockledge/KnockledgeControllerAutoAiTest.kt`
- Modify: `androidApp/src/test/kotlin/two/knockledge/KnockledgeControllerSessionTest.kt`
- Modify: `androidApp/src/test/kotlin/two/knockledge/KnockledgeNoteSinkTest.kt`
- Modify: `androidApp/src/test/kotlin/two/knockledge/RewinderEmptyAudioTest.kt`
- Modify: `androidApp/src/test/kotlin/two/knockledge/RewinderSaveThresholdTest.kt`
- Modify: `androidApp/src/test/kotlin/two/knockledge/data/OriginalAudioPreservationTest.kt`
- Modify: `androidApp/src/test/kotlin/two/knockledge/TapesAndMixingTest.kt`

**Interfaces:**
- Consumes: `NoteSink` for persisting rewind notes.
- Produces: `KnockledgeController` without redundant `RecentsRepository`.

- [ ] **Step 1: Remove `RecentsRepository` from `KnockledgeController.kt`**

1. In `KnockledgeController.kt`:
   - Remove `recents: RecentsRepository` parameter from constructor.
   - Remove `_created` / `createdRecents: SharedFlow<Long>`.
   - Remove `val recent = recents.add(...)` call in `rewind()`.
   - Remove `delete(id: Long)` method.

- [ ] **Step 2: Remove `recents` from `KnockledgeGraph.kt` and `KnockledgeComponent.kt`**

1. In `KnockledgeGraph.kt`:
   - Remove `recents: RecentsRepository = InMemoryRecentsRepository()` parameter and field.
2. In `KnockledgeComponent.kt`:
   - Delete `@Provides @Singleton fun recents(store: KeyValueStore): RecentsRepository`.
   - Remove `recents: RecentsRepository` parameter from `controller(...)` and `knockledgeGraph(...)`.

- [ ] **Step 3: Delete `Recents.kt`**

Run: `Remove-Item "androidApp/src/main/kotlin/group/two/knockledge/data/Recents.kt"`

- [ ] **Step 4: Update test callers and remove tests in `TapesAndMixingTest.kt`**

1. Remove `recents = ...` arguments from test instantiations of `KnockledgeController` and `KnockRecordingViewModel` in:
   - `KnockRecordingViewModelTest.kt`
   - `KnockledgeControllerAutoAiTest.kt`
   - `KnockledgeControllerSessionTest.kt`
   - `KnockledgeNoteSinkTest.kt`
   - `RewinderEmptyAudioTest.kt`
   - `RewinderSaveThresholdTest.kt`
   - `OriginalAudioPreservationTest.kt`
2. In `TapesAndMixingTest.kt`:
   - Delete tests `tapesRoundTrip()` and `separatorCharactersCannotBreakTheFormat()`.

- [ ] **Step 5: Verify test suite execution**

Run: `.\gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL (all remaining unit tests pass).

---

### Task 4: Rename Legacy Test Files and Classes

**Files:**
- Rename/Move:
  - `androidApp/src/test/kotlin/two/knockledge/RewinderEmptyAudioTest.kt` -> `KnockledgeEmptyAudioTest.kt`
  - `androidApp/src/test/kotlin/two/knockledge/RewinderSaveThresholdTest.kt` -> `KnockledgeSaveThresholdTest.kt`
  - `androidApp/src/test/kotlin/two/knockledge/TapesAndMixingTest.kt` -> `AudioMixingTest.kt`

**Interfaces:**
- Consumes: `KnockledgeController`, `MixedAudioSource`.
- Produces: Correctly named JVM test classes.

- [ ] **Step 1: Rename `RewinderEmptyAudioTest.kt`**

1. Rename file to `androidApp/src/test/kotlin/two/knockledge/KnockledgeEmptyAudioTest.kt`.
2. Update class declaration to `class KnockledgeEmptyAudioTest`.

- [ ] **Step 2: Rename `RewinderSaveThresholdTest.kt`**

1. Rename file to `androidApp/src/test/kotlin/two/knockledge/KnockledgeSaveThresholdTest.kt`.
2. Update class declaration to `class KnockledgeSaveThresholdTest`.

- [ ] **Step 3: Rename `TapesAndMixingTest.kt`**

1. Rename file to `androidApp/src/test/kotlin/two/knockledge/AudioMixingTest.kt`.
2. Update class declaration to `class AudioMixingTest`.

- [ ] **Step 4: Verify test suite execution**

Run: `.\gradlew testDebugUnitTest --tests "two.knockledge.KnockledgeEmptyAudioTest" --tests "two.knockledge.KnockledgeSaveThresholdTest" --tests "two.knockledge.AudioMixingTest"`
Expected: BUILD SUCCESSFUL with 3 passed test suites.

---

### Task 5: Sanitize Comments and Phantom Documentation Links

**Files:**
- Modify: `androidApp/src/main/kotlin/group/two/knockledge/core/RewindPolicy.kt`
- Modify: `androidApp/src/main/kotlin/group/two/knockledge/pipeline/Placeholders.kt`
- Modify: `androidApp/src/main/kotlin/group/two/knockledge/audio/AudioSource.kt`
- Modify: `androidApp/src/main/kotlin/group/two/knockledge/audio/SessionHooks.kt`
- Modify: `androidApp/src/main/kotlin/group/two/knockledge/models/ModelCatalog.kt`
- Modify: `androidApp/src/main/kotlin/group/two/knockledge/models/ModelProvider.kt`
- Modify: `androidApp/src/main/kotlin/group/two/knockledge/models/ModelStore.kt`
- Modify: `androidApp/src/main/kotlin/group/two/knockledge/settings/AppSettings.kt`

**Interfaces:**
- Consumes: None.
- Produces: Accurate KDoc documentation pointing to Android-only SpecKit specifications.

- [ ] **Step 1: Fix phantom doc links in `RewindPolicy.kt` and `Placeholders.kt`**

1. In `RewindPolicy.kt`:
   - Replace `* Product rules from docs/SPEC.md, in one place.` with `* Product rules for rolling buffer thresholds from specs/004-unit4-knock-recording.`
2. In `Placeholders.kt`:
   - Replace KDoc referencing `docs/ARCHITECTURE.md` and "both platforms" with:
     ```kotlin
     /**
      * Offline stubs for testing and Compose previews when AI models are not bound.
      */
     ```

- [ ] **Step 2: Remove obsolete iOS and KMP commentary**

Update comments mentioning `AVAudioEngine`, `NSUserDefaults`, `Swift Live Activities`, and `whisper.xcframework` in:
- `AudioSource.kt`: Remove `iOS: implement with AVAudioEngine.`
- `SessionHooks.kt`: Remove `iOS will start a Live Activity (implement this interface in Swift).`
- `ModelCatalog.kt`: Remove `Shared by Android and iOS.`
- `ModelProvider.kt`: Remove `(iOS for now).`
- `ModelStore.kt`: Remove `Shared model manager for Android and iOS.`
- `AppSettings.kt`: Remove `iOS: NSUserDefaults.` and `shared by both apps`.

- [ ] **Step 3: Verification with grep and test build**

Run: `git grep -i "docs/SPEC.md" androidApp/`
Run: `git grep -i "docs/ARCHITECTURE.md" androidApp/`
Run: `.\gradlew testDebugUnitTest`
Expected: 0 matches for phantom docs, all tests pass.

---

### Task 6: Git Repository Cleanup (History & Dangling Objects)

**Context & Safety Rules:**
- The antecedent repo `GoGitIt-Rewinder` commits (from remote `https://github.com/pinakanov/GoGitIt-Rewinder`) were fetched into `FETCH_HEAD` for comparison.
- Knockledge's active branch `main` has 69 commits starting at root `7d310d3`.
- **Git History Rewriting Warning**: Rewriting past commits on `main` (via `git filter-repo` or `git rebase -i`) will change the SHA commit hash of all 69 commits. This requires `--force` pushing, which destroys git history parity for other collaborators.
- **Recommended Cleanup Procedure**: Clean up working tree files first, prune unreferenced fetched objects, and commit the cleanup directly.

- [ ] **Step 1: Check git status and prune unreachable objects**

Run: `git status`
Run: `git gc --prune=now`
Run: `git status`
Expected: Working tree reflects deleted and modified files cleanly; dangling objects from remote fetch are pruned.

- [ ] **Step 2: Present final verification report**

Run: `.\gradlew testDebugUnitTest`
Expected: Full test suite passes.
```powershell
.\gradlew testDebugUnitTest
```
