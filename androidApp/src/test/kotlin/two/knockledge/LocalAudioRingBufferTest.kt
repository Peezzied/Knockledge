package group.two.knockledge

import group.two.knockledge.core.FileLocalAudioRingBuffer
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocalAudioRingBufferTest {
    private val rate = 100
    private val dir: File = Files.createTempDirectory("ring").toFile()

    @AfterTest
    fun cleanup() {
        dir.deleteRecursively()
    }

    private fun chunkFiles() = dir.listFiles { f -> f.name.endsWith(".pcm") }!!.sortedBy { it.name }

    private fun buffer(window: Int = 60) = FileLocalAudioRingBuffer(dir, window, rate)

    @Test
    fun writesFiveSecondChunks() {
        val b = buffer()
        b.write(ShortArray(rate * 12))
        val files = chunkFiles()
        assertEquals(3, files.size)
        assertEquals(listOf(1000L, 1000L, 400L), files.map { it.length() })
        assertEquals(12_000L, b.bufferedMillis())
    }

    @Test
    fun evictsOldestBeyondWindow() {
        val b = buffer(60)
        repeat(180) { b.write(ShortArray(rate) { 1 }) }
        assertEquals(12, chunkFiles().size)
        assertEquals(60_000L, b.bufferedMillis())
    }

    @Test
    fun shrinkingWindowPrunesImmediately() {
        val b = buffer(300)
        repeat(200) { b.write(ShortArray(rate)) }
        assertEquals(40, chunkFiles().size)
        b.setWindowSeconds(60)
        assertEquals(12, chunkFiles().size)
        b.setWindowSeconds(10)
        assertEquals(12, chunkFiles().size)
    }

    @Test
    fun snapshotConcatenatesInOrderAsWav() {
        val b = buffer()
        val samples = ShortArray(rate * 7) { it.toShort() }
        b.write(samples, samples.size)
        val bytes = b.snapshot().readBytes()
        assertEquals("RIFF", String(bytes, 0, 4))
        assertEquals("WAVE", String(bytes, 8, 4))
        assertEquals(44 + samples.size * 2, bytes.size)
        val bb = java.nio.ByteBuffer.wrap(bytes, 44, samples.size * 2).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        samples.forEach { assertEquals(it, bb.short) }
        b.write(ShortArray(rate))
        assertEquals(8_000L, b.bufferedMillis())
    }

    @Test
    fun clearDeletesEverything() {
        val b = buffer()
        b.write(ShortArray(rate * 12))
        b.snapshot()
        b.clear()
        assertTrue(dir.listFiles()!!.isEmpty())
        assertEquals(0L, b.bufferedMillis())
    }
}
