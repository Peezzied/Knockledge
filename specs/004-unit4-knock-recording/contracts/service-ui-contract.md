# Contract: UI, Service, and Dagger DI Integration

This contract specifies the interfaces for Jetpack Compose UI, the foreground service, and Dagger dependency injection.

## 1. UI & ViewModel Contract

The UI consumes state through an immutable Kotlin `StateFlow` following Unidirectional Data Flow (UDF).

```kotlin
package group.two.knockledge.ui

import kotlinx.coroutines.flow.StateFlow

data class KnockRecordingUiState(
    val isRecording: Boolean = false,
    val bufferedSeconds: Int = 0,
    val maxBufferSeconds: Int = 300,
    val isGestureActive: Boolean = false,
    val isSavingSnapshot: Boolean = false,
    val lastSaveSuccess: Boolean? = null
)

interface KnockRecordingController {
    val uiState: StateFlow<KnockRecordingUiState>
    fun startSession()
    fun stopSession()
    fun triggerManualSave()
    fun setBufferDurationSeconds(seconds: Int)
}
```

## 2. Service Integration Contract

The Android foreground service coordinates audio capture and the knock gesture sensor.

```kotlin
package group.two.knockledge.service

interface KnockServiceCoordinator {
    fun attachSensors()
    fun detachSensors()
    fun onTriggerCaptured()
}
```

## 3. Dagger DI Module Specification

All implementations provide `@Inject` constructors and bind to clean interfaces through a standard Dagger `@Module`.

```kotlin
package group.two.knockledge.di

import dagger.Binds
import dagger.Module
import group.two.knockledge.core.LocalAudioRingBuffer
import group.two.knockledge.core.FileLocalAudioRingBuffer
import group.two.knockledge.sensor.KnockGestureDetector
import group.two.knockledge.sensor.AndroidKnockGestureDetector
import javax.inject.Singleton

@Module
abstract class KnockRecordingModule {

    @Binds
    @Singleton
    abstract fun bindGestureDetector(
        impl: AndroidKnockGestureDetector
    ): KnockGestureDetector

    @Binds
    @Singleton
    abstract fun bindAudioRingBuffer(
        impl: FileLocalAudioRingBuffer
    ): LocalAudioRingBuffer
}
```
