---
description: "Task list for Acoustic Speaker Cloth Surface Texture implementation"
---

# Tasks: Acoustic Speaker Cloth Surface Texture

**Input**: Design documents from `specs/013-speaker-cloth-texture/`  
**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/modifier-api.md](contracts/modifier-api.md), [quickstart.md](quickstart.md)  
**Tests**: Automated JVM unit tests and Compose preview verification  
**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`
- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Explicit file paths included in all task descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Asset creation and interactive documentation setup

- [X] T001 Generate seamless 16×16 repeating acoustic weave PNG asset in `androidApp/src/main/res/drawable/texture_speaker_grill.png`
- [X] T002 [P] Create interactive browser preview and magnification loupe in `docs/design/texture_designs.html`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core Compose texture modifier infrastructure that MUST be complete before user story integration

**⚠️ CRITICAL**: Foundational tasks block user story implementation

- [X] T003 Implement `Modifier.speakerClothTexture(alpha, drawableRes)` in `androidApp/src/main/kotlin/group/two/knockledge/ui/theme/Texture.kt` using `drawWithCache` and `ShaderBrush` with `TileMode.Repeated`
- [X] T004 Document The Acoustic Cloth Rule in `DESIGN.md` defining background (18%) and header bay (24%) opacity limits

**Checkpoint**: Foundation ready - user story integration can now proceed

---

## Phase 3: User Story 1 - Tactile Acoustic Ambience on App Canvas (Priority: P1) 🎯 MVP

**Goal**: Render the acoustic speaker grill cloth across the main application background canvas at 18% opacity.

**Independent Test**: Launch the application; the root background displays the woven cloth pattern while typography and controls remain readable.

### Implementation for User Story 1

- [X] T005 [US1] Apply `background(ObsidianMidnight).speakerClothTexture(alpha = 0.18f)` and `containerColor = Color.Transparent` to `Scaffold` in `androidApp/src/main/kotlin/group/two/knockledge/ui/HomeScreen.kt`
- [X] T006 [P] [US1] Apply `background(ObsidianMidnight).speakerClothTexture(alpha = 0.18f)` to root canvas `Box` in `androidApp/src/main/kotlin/group/two/knockledge/ui/RecordScreen.kt`
- [X] T007 [P] [US1] Apply `background(ObsidianMidnight).speakerClothTexture(alpha = 0.18f)` to root shell `Box` in `LiveTestAppShell` in `androidApp/src/main/kotlin/group/two/knockledge/ui/HomeScreen.kt`
- [X] T008 [US1] Apply `speakerClothTexture(alpha = 0.18f)` to `HomeScreenPreview` root container in `androidApp/src/main/kotlin/group/two/knockledge/ui/HomeScreen.kt`

**Checkpoint**: User Story 1 is complete and independently testable as an MVP.

---

## Phase 4: User Story 2 - Dedicated Acoustic Microphone Bay (Priority: P2)

**Goal**: Render the top microphone buffer header as an acoustic sensor bay with 24% opacity cloth and rounded lower corners.

**Independent Test**: Inspect the collapsible top header; it displays the woven cloth grill texture with rounded corners and high-contrast telemetry.

### Implementation for User Story 2

- [X] T009 [US2] Style top bar `Box` with `.clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)).background(SurfaceIndigo.copy(alpha = 0.85f)).speakerClothTexture(alpha = 0.24f)` in `androidApp/src/main/kotlin/group/two/knockledge/ui/HomeScreen.kt`
- [X] T010 [US2] Verify header collapse scaling during scroll in `androidApp/src/main/kotlin/group/two/knockledge/ui/HomeScreen.kt`

**Checkpoint**: User Story 2 is functional and integrates seamlessly with User Story 1.

---

## Phase 5: User Story 3 - High-Contrast Note Cards and Interactive Elements (Priority: P3)

**Goal**: Guarantee that saved audio note cards, waveforms, and controls remain untextured vector surfaces to protect legibility.

**Independent Test**: Inspect `AnchorCard` and playback visualizer; verify 0% texture opacity and ≥4.5:1 text contrast.

### Implementation for User Story 3

- [X] T011 [US3] Verify `HomeRecordingsContent` and `LazyColumn` maintain smooth untextured note card surfaces in `androidApp/src/main/kotlin/group/two/knockledge/ui/HomeScreen.kt`
- [X] T012 [P] [US3] Verify waveform frequency bars and interactive buttons render with vector-sharp edges in `androidApp/src/main/kotlin/group/two/knockledge/ui/HomeScreen.kt`

**Checkpoint**: All user stories are complete and validated against accessibility standards.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Verification and validation across the entire application

- [X] T013 [P] Execute compilation verification via `./gradlew.bat :androidApp:compileDebugKotlin`
- [X] T014 [P] Execute unit test suite via `./gradlew.bat :androidApp:testDebugUnitTest`
- [ ] T015 Run quickstart validation scenarios defined in `specs/013-speaker-cloth-texture/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies
- **Phase 1 (Setup)**: No dependencies.
- **Phase 2 (Foundational)**: Depends on Phase 1 asset creation (T001). Blocks all user stories.
- **Phase 3 (User Story 1 - MVP)**: Depends on Phase 2 completion (T003).
- **Phase 4 (User Story 2)**: Depends on Phase 2 and integrates with Phase 3.
- **Phase 5 (User Story 3)**: Depends on Phase 3 and Phase 4.
- **Phase 6 (Polish)**: Depends on all user story phases.

### User Story Dependencies
- **US1 (P1)**: Independent of other user stories.
- **US2 (P2)**: Independent implementation on top header; layers above US1 background.
- **US3 (P3)**: Validates non-interference on note cards and child composables.

### Parallel Opportunities
- T001 and T002 can run in parallel.
- T006 and T007 can run in parallel with T005.
- T013 and T014 can run in parallel during the polish phase.

---

## Implementation Strategy

### MVP First (User Story 1 Only)
1. Complete Phase 1 (Asset generation).
2. Complete Phase 2 (Compose modifier foundation).
3. Complete Phase 3 (Apply to canvas background).
4. Validate MVP rendering on device or preview.

### Incremental Delivery
1. Foundation: Modifier + Asset.
2. Increment 1: Root background cloth (US1).
3. Increment 2: Top microphone sensor bay (US2).
4. Increment 3: Contrast and card smoothness verification (US3).
5. Validation: Automated tests and quickstart run.
