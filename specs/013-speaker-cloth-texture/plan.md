# Implementation Plan: Acoustic Speaker Cloth Surface Texture

**Branch**: `013-speaker-cloth-texture` | **Date**: 2026-11-04 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/013-speaker-cloth-texture/spec.md`

## Summary

Implement a tactile acoustic speaker grill cloth surface texture across the Knockledge native Android application. The technical approach uses a seamless 16×16 repeating PNG bitmap asset (`R.drawable.texture_speaker_grill`) paired with a custom Jetpack Compose modifier (`Modifier.speakerClothTexture`) powered by `Modifier.drawWithCache` and `ShaderBrush(ImageShader(..., TileMode.Repeated))`. This renders the texture on the root background canvas at 18% opacity and on the top microphone buffer header at 24% opacity, while strictly preserving smooth vector surfaces on note cards, buttons, and telemetry typography.

## Technical Context

**Language/Version**: Kotlin 2.0.21 / Java 17  
**Primary Dependencies**: Android Jetpack Compose (Material 3), Compose Foundation, Compose UI Graphics  
**Storage**: Static PNG asset in `androidApp/src/main/res/drawable/` (<1 KB)  
**Testing**: JVM Unit Tests (`./gradlew :androidApp:testDebugUnitTest`), Compose previews (`@Preview`)  
**Target Platform**: Android API 26+ (Android 8.0 Oreo through Android 15)  
**Project Type**: Native Android Mobile Application  
**Performance Goals**: 60fps fluid list scrolling, 0ms main-thread texture compilation, zero memory re-allocation on recomposition  
**Constraints**: Zero external libraries, text contrast ratio ≥ 4.5:1, universal API 26+ compatibility without requiring AGSL  
**Scale/Scope**: 1 drawable asset, 1 Compose modifier helper, applied to 2 primary screens (`HomeScreen`, `RecordScreen`)  

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

1. **Gate 1: Minimum Viable Hackathon Build (3-Day Scope)**: PASS. Delivers the tactile audio hardware identity for the working app without speculative scope.
2. **Gate 2: ADHD-Driven Execution**: PASS. Discrete, testable tasks with concrete acceptance criteria and immediate verification.
3. **Gate 3: Karpathy Discipline**: PASS. Surgical code additions touching only `Texture.kt` and designated surface roots.
4. **Gate 4: Ponytail Simplicity**: PASS. Built entirely with Android platform APIs (`ImageShader`, `BitmapShader`) and existing Compose tools. Zero new Gradle dependencies.

## Project Structure

### Documentation (this feature)

```text
specs/013-speaker-cloth-texture/
├── plan.md              # This implementation plan
├── research.md          # Phase 0 technical research & evaluations
├── data-model.md        # Phase 1 surface entities & hierarchy
├── quickstart.md        # Phase 1 verification & run guide
├── contracts/           # Phase 1 interface specifications
│   └── modifier-api.md  # Compose modifier API contract
└── checklists/
    └── requirements.md  # Spec quality validation checklist
```

### Source Code Layout

```text
androidApp/
├── src/main/
│   ├── kotlin/group/two/knockledge/ui/
│   │   ├── HomeScreen.kt          # Scaffold & topBar modifier applications
│   │   ├── RecordScreen.kt        # Canvas modifier application
│   │   └── theme/
│   │       ├── Color.kt           # Color tokens (ObsidianMidnight, SurfaceIndigo)
│   │       ├── Texture.kt         # Modifier.speakerClothTexture definition
│   │       └── Theme.kt           # Material 3 theme configuration
│   └── res/
│       └── drawable/
│           └── texture_speaker_grill.png  # Seamless 16x16 acoustic tile
docs/
└── design/
    └── texture_designs.html       # Interactive browser texture inspector
```

**Structure Decision**: The texture logic lives entirely in the presentation layer under `ui/theme/` as a general-purpose Compose modifier, maintaining standard Android architectural separation.

## Complexity Tracking

*No constitutional violations exist. No justifications required.*
