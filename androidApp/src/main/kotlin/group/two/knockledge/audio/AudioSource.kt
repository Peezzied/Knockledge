package group.two.knockledge.audio

import kotlinx.coroutines.flow.Flow

/**
 * Platform microphone. Emits mono PCM16 chunks (about 100 ms each) while collected,
 * and releases the mic as soon as collection is cancelled.
 */
interface AudioSource {
    /** Emits mono 16-bit chunks at [sampleRate]; collecting holds the source open. */
    fun frames(sampleRate: Int): Flow<ShortArray>

    /** Which input is live and any problem with it, for the home screen. Null if unknown. */
    fun status(): InputStatus? = null
}

/** For example label "Built-in mic · MIC 16 kHz", warning "Android is muting this app's mic". */
data class InputStatus(val label: String, val warning: String?)


