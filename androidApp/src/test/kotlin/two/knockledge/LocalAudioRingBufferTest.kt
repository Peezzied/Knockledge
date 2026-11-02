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
        assertEquals(1_000L, b.bufferedMillis())
    }

    @Test
    fun secondSnapshotContainsOnlyAudioRecordedAfterFirst() {
        val b = buffer()
        b.write(ShortArray(rate * 10) { it.toShort() })
        b.snapshot()
        assertEquals(0L, b.bufferedMillis())

        val second = ShortArray(rate * 5) { (1_000 + it).toShort() }
        b.write(second)
        assertEquals(5_000L, b.bufferedMillis())

        val pcm = pcmOf(b.snapshot())
        assertEquals(second.size, pcm.size)
        second.forEachIndexed { i, s -> assertEquals(s, pcm[i]) }
    }

    @Test
    fun snapshotCutCanFallMidChunk() {
        val b = buffer()
        b.write(ShortArray(rate * 7) { it.toShort() })
        b.snapshot()

        val second = ShortArray(rate * 7) { (700 + it).toShort() }
        b.write(second)

        val pcm = pcmOf(b.snapshot())
        assertEquals(second.size, pcm.size)
        second.forEachIndexed { i, s -> assertEquals(s, pcm[i]) }
    }

    @Test
    fun snapshotStartsAtBufferStartWhenWindowPrunesPastCut() {
        val b = buffer(60)
        b.write(ShortArray(rate * 60) { it.toShort() })
        b.snapshot()

        val extra = ShortArray(rate * 70) { (6_000 + it).toShort() }
        b.write(extra)
        assertEquals(60_000L, b.bufferedMillis())

        val pcm = pcmOf(b.snapshot())
        assertEquals(rate * 60, pcm.size)
        extra.takeLast(rate * 60).forEachIndexed { i, s -> assertEquals(s, pcm[i]) }
    }

    @Test
    fun clearResetsSnapshotCut() {
        val b = buffer()
        b.write(ShortArray(rate * 10))
        b.snapshot()
        b.clear()

        val fresh = ShortArray(rate * 5) { it.toShort() }
        b.write(fresh)
        val pcm = pcmOf(b.snapshot())
        assertEquals(fresh.size, pcm.size)
        fresh.forEachIndexed { i, s -> assertEquals(s, pcm[i]) }
    }

    private fun pcmOf(wav: File): ShortArray {
        val bytes = wav.readBytes()
        return ShortArray((bytes.size - FileLocalAudioRingBuffer.WAV_HEADER_BYTES) / 2).also {
            java.nio.ByteBuffer.wrap(bytes, FileLocalAudioRingBuffer.WAV_HEADER_BYTES, it.size * 2)
                .order(java.nio.ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(it)
        }
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
