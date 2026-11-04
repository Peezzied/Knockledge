# Interface Contract: Speaker Cloth Texture Modifier API

## Scope
Defines the public Kotlin Jetpack Compose modifier interface for rendering acoustic speaker grill textures across the Knockledge UI.

---

### Function Signature

```kotlin
package group.two.knockledge.ui.theme

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Applies a seamless repeating acoustic speaker grill cloth texture over the composable surface.
 *
 * Employs [Modifier.drawWithCache] and [ShaderBrush] with [TileMode.Repeated] to guarantee
 * zero memory allocations during recomposition and zero main-thread layout overhead.
 * The texture draws before child content to preserve vector sharpness on typography and controls.
 *
 * @param alpha The blending opacity of the cloth pattern (default 0.18f).
 * @param drawableRes The drawable resource representing the repeating acoustic tile.
 */
@Composable
fun Modifier.speakerClothTexture(
    alpha: Float = 0.18f,
    @DrawableRes drawableRes: Int = R.drawable.texture_speaker_grill,
): Modifier
```

---

### Invariants & Guarantees

1. **Draw Order Invariant**:
   - The texture shader MUST draw before `drawContent()`.
   - Any child composable (text, buttons, icons, note cards) MUST render above the texture layer with zero blur or grain distortion.

2. **Memory Allocation Invariant**:
   - The `ImageBitmap` asset and `ShaderBrush` MUST be cached inside `drawWithCache`.
   - Recomposition of the parent composable MUST NOT trigger bitmap decoding or re-allocation.

3. **Platform Compatibility Invariant**:
   - Must operate on all supported Android versions (Android 8.0 / API 26+) without requiring AGSL (Android 13+) runtime shader compilation.

4. **Opacity Boundaries**:
   - Default background opacity: `0.18f` (18%).
   - Sensor bay header opacity: `0.24f` (24%).
   - Text contrast ratio above textured surfaces MUST remain `≥ 4.5:1`.
