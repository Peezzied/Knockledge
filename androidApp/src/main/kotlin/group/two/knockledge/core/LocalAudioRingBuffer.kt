package group.two.knockledge.core

import java.io.File

interface LocalAudioRingBuffer {
    fun write(chunk: ShortArray, length: Int = chunk.size)

    fun setWindowSeconds(seconds: Int)

    fun bufferedMillis(): Long

    fun snapshot(): File

    fun clear()
}
