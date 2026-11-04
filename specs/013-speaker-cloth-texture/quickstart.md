# Quickstart Validation Guide: Acoustic Speaker Cloth Texture

## Purpose
This document provides rapid verification scenarios to validate that the acoustic speaker cloth texture renders correctly, maintains high contrast, and introduces zero performance regressions.

---

### Scenario 1: Asset & Build Verification

**Goal**: Verify that the texture asset is present, valid, and compiles into the Android APK.

1. Verify the PNG asset exists:
   ```powershell
   Test-Path "androidApp/src/main/res/drawable/texture_speaker_grill.png"
   # Expected Output: True
   ```

2. Run Kotlin debug compilation:
   ```powershell
   .\gradlew.bat :androidApp:compileDebugKotlin
   # Expected Output: BUILD SUCCESSFUL
   ```

3. Run the unit test suite:
   ```powershell
   .\gradlew.bat :androidApp:testDebugUnitTest
   # Expected Output: BUILD SUCCESSFUL (all unit tests pass)
   ```

---

### Scenario 2: Interactive Browser Swatch Verification

**Goal**: Inspect texture rendering, contrast ratios, and pattern scaling in the design draft.

1. Open the interactive design artifact:
   ```text
   docs/design/texture_designs.html
   ```
2. Verify the following:
   - Click **TEXTURE: ON / OFF** to confirm instant visibility change.
   - Observe the 10× Swatch Loupe to confirm the interlocking acoustic warp and weft fibers with subtle brass flecks.
   - Confirm that Parchment note cards remain smooth and text stays high-contrast.

---

### Scenario 3: Android Device / Emulator Visual Verification

**Goal**: Inspect the texture running on native Android hardware.

1. Launch the app on an Android device or emulator:
   ```powershell
   .\gradlew.bat :androidApp:installDebug
   ```
2. Inspect the **HomeScreen**:
   - The dark background displays a subtle woven cloth texture.
   - The top header display ("Recordings") carries an acoustic sensor bay grill.
   - The note cards (`ParchmentCream`) float on top with solid borders and sharp dark ink text.
3. Scroll through the recordings list:
   - Verify smooth 60fps scrolling without stutter or texture tearing.
