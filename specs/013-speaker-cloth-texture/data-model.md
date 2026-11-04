# Data Model: Acoustic Surface Texture Entities

## Overview
This document models the visual surface entities, hierarchy, and token relationships for the acoustic speaker cloth texture in the Knockledge user interface.

---

### Entity 1: ChassisSurface
Represents the baseline application substrate corresponding to the physical hardware enclosure.

| Attribute | Type | Default Value | Description |
|-----------|------|---------------|-------------|
| `backgroundColor` | `Color` | `ObsidianMidnight` (`#14122B`) | Deep midnight foundation color |
| `textureAsset` | `Int` | `R.drawable.texture_speaker_grill` | 16×16 repeating acoustic weave PNG |
| `textureAlpha` | `Float` | `0.18f` (18%) | Subdued background blending opacity |
| `tileMode` | `TileMode` | `TileMode.Repeated` | Continuous 2D GPU pattern wrap |
| `targetScreens` | `List<String>` | `["HomeScreen", "RecordScreen", "LiveTestAppShell"]` | Screens adopting the acoustic chassis |

---

### Entity 2: AcousticSensorBay
Represents the top telemetry and microphone buffer header panel.

| Attribute | Type | Default Value | Description |
|-----------|------|---------------|-------------|
| `containerColor` | `Color` | `#1A1832` (Surface Indigo variant) | Dedicated sensor dock container |
| `textureAlpha` | `Float` | `0.24f` (24%) | Higher presence to emphasize the acoustic input area |
| `cornerRadii` | `Dp` | `bottomStart = 16.dp, bottomEnd = 16.dp` | Rounded bottom hardware edge |
| `telemetryTint` | `Color` | `WaveformTeal` (`#2DE2D0`) | Accent color for rolling capture status |
| `contentSharpening` | `Boolean` | `true` | Ensures header labels and gear icons remain vector-sharp |

---

### Entity 3: AnchorNoteCard
Represents individual saved audio recordings floating above the textured chassis.

| Attribute | Type | Default Value | Description |
|-----------|------|---------------|-------------|
| `containerColor` | `Color` | `ParchmentCream` (`#F2E8D0`) | Vintage archival parchment paper |
| `contentInkColor` | `Color` | `TextInk` (`#1B1833`) | High-contrast dark ink for transcripts and titles |
| `textureAlpha` | `Float` | `0.0f` (0%) | Strictly untextured to guarantee 100% legibility |
| `borderStroke` | `BorderStroke` | `1.dp, AntiqueBrass` (`#B8913F`) | Hardware metallic edge definition |
| `elevation` | `Dp` | `8.dp` | Physical lift above the acoustic chassis |

---

### Entity 4: SpeakerClothConfig
Configuration parameters governing the texture modifier execution.

| Attribute | Type | Constraints | Description |
|-----------|------|-------------|-------------|
| `alpha` | `Float` | `0.05f .. 0.40f` | Permitted range for texture blending |
| `drawableRes` | `Int` | Valid drawable resource | Seamless power-of-two PNG drawable |
| `drawOrder` | `Enum` | `BehindContent` | Enforces `drawRect` before `drawContent` |
