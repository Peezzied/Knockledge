# Contract: Knock Gesture Detector

This contract defines the interface and behaviors for detecting physical knock gestures using device sensors.

## Interface Definition

```kotlin
package group.two.knockledge.sensor

interface KnockGestureDetector {
    /**
     * Starts listening for knock motion events on the sensor thread.
     */
    fun startListening(onKnockDetected: () -> Unit)

    /**
     * Stops sensor listening and releases hardware resources.
     */
    fun stopListening()

    /**
     * Returns true if the detector is active.
     */
    val isListening: Boolean
}
```

## Behavioral Specifications

1. **Sensor Registration**:
   - The detector requests `Sensor.TYPE_LINEAR_ACCELERATION` from `SensorManager`.
   - If linear acceleration is unavailable, the detector registers `Sensor.TYPE_ACCELEROMETER` and applies high-pass gravity filtering.
   - Sensor events are processed on a dedicated background handler thread to avoid blocking the main thread.

2. **Knock Validation Rule**:
   - A single knock candidate occurs when sensor acceleration magnitude exceeds $12.0\text{ m/s}^2$.
   - A valid double knock requires a second peak occurring within $100\text{ ms}$ to $400\text{ ms}$ of the first peak.
   - Any peak occurring less than $100\text{ ms}$ is discarded as sensor bounce.
   - If no second peak occurs within $400\text{ ms}$, the state resets to listening.

3. **Debounce Period**:
   - After a valid knock triggers `onKnockDetected()`, the detector ignores all sensor readings for $2000\text{ ms}$.

4. **Haptic Feedback**:
   - Upon confirming a double knock, the system triggers a brief haptic vibration pulse ($50\text{ ms}$) using `Vibrator` or `VibratorManager`.
