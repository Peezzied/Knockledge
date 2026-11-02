package group.two.knockledge.core

import java.io.File

/** Rolling window of recent audio behind Rewind: samples are appended continuously and the oldest are dropped. */
interface LocalAudioRingBuffer {
    /** Appends the first [length] samples of [chunk] to the end of the window. */
    fun write(chunk: ShortArray, length: Int = chunk.size)

    /** Retargets the window to [seconds]; audio that no longer fits is dropped at once. */
    fun setWindowSeconds(seconds: Int)

    /** Buffered audio not yet saved by a snapshot, in milliseconds. */
    fun bufferedMillis(): Long

    /** Returns the audio recorded since the previous snapshot as a WAV file, then marks it saved. */
    fun snapshot(): File

    /** Drops all buffered audio. */
    fun clear()
}
