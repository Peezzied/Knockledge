package group.two.knockledge.core

import android.app.Application
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject

/** Not thread safe on purpose: KnockledgeController confines every call to its audio dispatcher. */
class FileLocalAudioRingBuffer(
    private val dir: File,
    windowSeconds: Int = RewindPolicy.WINDOW_SECONDS,
    private val sampleRate: Int = RewindPolicy.SAMPLE_RATE,
) : LocalAudioRingBuffer {

    @Inject
    constructor(app: Application) : this(audioBufferDir(app.cacheDir))

    private val chunkSamples = sampleRate * RewindPolicy.CHUNK_SECONDS
    private var maxChunks = chunksFor(windowSeconds)
    private val files = ArrayDeque<File>()
    private var active: FileOutputStream? = null
    private var activeSamples = 0
    private var nextIndex = 0L
    private var totalWritten = 0L
    private var cutSamples = 0L

    init {
        wipeDir()
    }

    /** Appends PCM16 to the newest chunk file, starting a new one at the chunk size and deleting the oldest past the window. */
    override fun write(chunk: ShortArray, length: Int) {
        var offset = 0
        while (offset < length) {
            if (active == null || activeSamples == chunkSamples) roll()
            val n = minOf(length - offset, chunkSamples - activeSamples)
            val bytes = ByteBuffer.allocate(n * 2).order(ByteOrder.LITTLE_ENDIAN)
            for (i in offset until offset + n) bytes.putShort(chunk[i])
            active!!.write(bytes.array())
            activeSamples += n
            offset += n
        }
        totalWritten += length
    }

    /** Resizes the window, clamping it to the policy range and deleting the chunks that no longer fit. */
    override fun setWindowSeconds(seconds: Int) {
        maxChunks = chunksFor(seconds)
        prune()
    }

    /** Audio recorded since the last snapshot, clamped to what is still on disk. */
    override fun bufferedMillis(): Long {
        if (files.isEmpty()) return 0L
        return (totalWritten - maxOf(cutSamples, firstChunkStart())) * 1000L / sampleRate
    }

    /** Writes the audio recorded since the previous snapshot into a new 16 kHz mono WAV, then marks it saved. */
    override fun snapshot(): File {
        val out = File(dir, "snapshot_${System.nanoTime()}.wav")
        val start = maxOf(cutSamples, firstChunkStart())
        var skipBytes = (start - firstChunkStart()) * 2
        val dataBytes = (totalWritten - start) * 2
        out.outputStream().buffered().use { o ->
            o.write(wavHeader(dataBytes.toInt()))
            files.forEach { f ->
                f.inputStream().use { ins ->
                    skipBytes -= ins.skipFully(skipBytes)
                    ins.copyTo(o)
                }
            }
        }
        cutSamples = totalWritten
        return out
    }

    /** Closes the active chunk and deletes every buffered file, including earlier snapshots. */
    override fun clear() {
        active?.close()
        active = null
        files.clear()
        activeSamples = 0
        nextIndex = 0L
        totalWritten = 0L
        cutSamples = 0L
        wipeDir()
    }

    /** Absolute sample position of the first byte of the oldest remaining chunk. */
    private fun firstChunkStart(): Long =
        if (files.isEmpty()) totalWritten else (nextIndex - files.size) * chunkSamples

    private fun roll() {
        active?.close()
        val file = File(dir, "chunk_%05d.pcm".format(nextIndex++))
        files.addLast(file)
        active = FileOutputStream(file)
        activeSamples = 0
        prune()
    }

    private fun prune() {
        while (files.size > maxChunks) files.removeFirst().delete()
    }

    private fun wipeDir() {
        dir.mkdirs()
        dir.listFiles()?.forEach { it.delete() }
    }

    private fun chunksFor(seconds: Int) =
        (RewindPolicy.clampWindowSeconds(seconds) + RewindPolicy.CHUNK_SECONDS - 1) / RewindPolicy.CHUNK_SECONDS

    /** Discards up to [bytes] by reading; FileInputStream.skip() may jump past EOF and overspend the budget. */
    private fun java.io.InputStream.skipFully(bytes: Long): Long {
        var left = bytes
        val scratch = ByteArray(8 * 1024)
        while (left > 0) {
            val n = read(scratch, 0, minOf(scratch.size.toLong(), left).toInt())
            if (n < 0) break
            left -= n
        }
        return bytes - left
    }

    private fun wavHeader(dataBytes: Int): ByteArray =
        ByteBuffer.allocate(WAV_HEADER_BYTES).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + dataBytes); put("WAVE".toByteArray())
            put("fmt ".toByteArray()); putInt(16); putShort(1); putShort(1)
            putInt(sampleRate); putInt(sampleRate * 2); putShort(2); putShort(16)
            put("data".toByteArray()); putInt(dataBytes)
        }.array()

    companion object {
        const val WAV_HEADER_BYTES = 44
    }
}
