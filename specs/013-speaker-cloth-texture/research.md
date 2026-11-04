# Technical Research: Acoustic Speaker Cloth Surface Texture

## Overview
This document records technical decisions, evaluations, and trade-offs for rendering a seamless repeating acoustic speaker cloth texture across the Knockledge native Android application.

---

### Research Item 1: Texture Asset Format & Storage

- **Decision**: Use a 16×16 pixel seamless repeating 32-bit RGBA PNG bitmap asset stored in `androidApp/src/main/res/drawable/texture_speaker_grill.png`.
- **Rationale**:
  - Memory footprint is negligible (137 bytes on disk, <1 KB decoded memory).
  - A 16×16dp tile repeats seamlessly without visible tiling boundaries or grid line seams.
  - Native Android `BitmapShader` and Compose `ImageShader` are optimized for power-of-two repeating bitmaps.
  - Unlike vector drawables or procedural SVG filters, bitmap shaders execute on the GPU with zero CPU math during render passes.
- **Alternatives Considered**:
  - *Vector Drawable XML*: Android vector drawables do not natively support hardware-accelerated 2D tiling without manual canvas loop clipping.
  - *Runtime AGSL Procedural Shader*: Requires Android 13+ (API 33), breaking compatibility with Android 8.0–12 devices.
  - *SVG with `feTurbulence` / CSS filters*: Causes high battery drain and has browser/rendering engine security and performance limitations.

---

### Research Item 2: Compose Rendering Architecture (`Modifier.drawWithCache`)

- **Decision**: Implement a custom Jetpack Compose modifier extension:
  ```kotlin
  @Composable
  fun Modifier.speakerClothTexture(
      alpha: Float = 0.18f,
      @DrawableRes drawableRes: Int = R.drawable.texture_speaker_grill
  ): Modifier
  ```
  Using `drawWithCache` with `ShaderBrush(ImageShader(imageBitmap, TileMode.Repeated, TileMode.Repeated))`.
- **Rationale**:
  - `drawWithCache` caches the `ShaderBrush` and allocation between draw passes, executing zero object allocations during recomposition.
  - `TileMode.Repeated` delegates the repeating pattern directly to hardware GPU rasterizers.
  - Calling `drawRect(brush, alpha)` before `drawContent()` ensures the texture acts as a substrate, preserving 100% vector sharpness on foreground text, buttons, and note cards.
- **Alternatives Considered**:
  - *`Modifier.paint(painterResource(...))`*: Does not support 2D repeat tiling without wrapping in custom Painter implementations.
  - *`Canvas` draw loop inside composable*: Introduces CPU iteration overhead on every scroll frame.

---

### Research Item 3: Surface Hierarchy & Visual Accessibility

- **Decision**:
  - Background canvas (`ObsidianMidnight`): 18% texture opacity.
  - Acoustic sensor bay (`#1A1832` top buffer header): 24% texture opacity.
  - Note cards (`ParchmentCream`), controls, and text fields: 0% texture (smooth vector surfaces).
- **Rationale**:
  - Enforces **The Acoustic Cloth Rule** and **The Ink on Parchment Rule** in `DESIGN.md`.
  - Guarantees body text contrast ratios exceed 4.5:1 (Parchment Cream text on Obsidian Midnight reaches >10:1).
  - Creates tangible contrast between the textured hardware deck and smooth archival paper notes.
- **Alternatives Considered**:
  - *Uniform texture across all surfaces*: Reduces text legibility and destroys visual hierarchy.
  - *Background-only texture*: Misses the functional affordance of highlighting the top microphone buffer area.
