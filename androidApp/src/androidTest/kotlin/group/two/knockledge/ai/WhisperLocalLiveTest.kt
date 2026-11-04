package group.two.knockledge.ai

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import group.two.knockledge.pipeline.AudioPreprocessor
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Live instrumented test executing local whisper.cpp on a physical Android device.
 */
@RunWith(AndroidJUnit4::class)
class WhisperLocalLiveTest {

    @Test
    fun liveWhisperTranscribesRealAudioOnDevice() {
        val modelFile = File("/data/local/tmp/ggml-tiny-q5_1.bin")
        assertTrue("Model file missing at ${modelFile.absolutePath}", modelFile.exists() && modelFile.length() > 0)

        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        val wavBytes = assets.open("sample.wav").use { it.readBytes() }
        val rawPcm = decodeWavRaw(wavBytes)

        assertTrue("PCM samples empty", rawPcm.pcm.isNotEmpty())

        // Use universal AudioPreprocessor normalization for 16 kHz mono PCM
        val normalizedPcm = AudioPreprocessor.normalizeTo16kMono(
            pcm = rawPcm.pcm,
            sampleRate = rawPcm.sampleRate,
            channels = rawPcm.channels,
        )

        val transcript = AndroidSpeechEngine.transcribe(
            modelPath = modelFile.absolutePath,
            pcm = normalizedPcm,
            language = "en",
            prompt = "",
            vadModelPath = null,
            threads = 4,
        )

        println("WHISPER_LIVE_OUTPUT: '$transcript'")
        assertNotNull("Transcript should not be null", transcript)
        assertTrue("Transcript should not be blank, got: '$transcript'", transcript.isNotBlank())
    }

    private data class RawAudio(val pcm: ShortArray, val sampleRate: Int, val channels: Int)

    private fun decodeWavRaw(bytes: ByteArray): RawAudio {
        val b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        require(String(bytes, 0, 4) == "RIFF" && String(bytes, 8, 4) == "WAVE") { "Not a WAV file" }
        var pos = 12
        var rate = 0
        var channels = 0
        var bits = 0
        var pcm: ByteArray? = null
        while (pos + 8 <= bytes.size) {
            val id = String(bytes, pos, 4)
            val size = b.getInt(pos + 4)
            when (id) {
                "fmt " -> {
                    channels = b.getShort(pos + 10).toInt()
                    rate = b.getInt(pos + 12)
                    bits = b.getShort(pos + 22).toInt()
                }
                "data" -> pcm = bytes.copyOfRange(pos + 8, pos + 8 + size)
            }
            pos += 8 + size + (size and 1)
        }
        require(pcm != null && rate > 0) { "Missing data or format chunk" }
        require(bits == 16) { "Only 16-bit PCM supported, got $bits" }

        val samples = ShortArray(pcm.size / 2)
        for (i in samples.indices) {
            val low = pcm[i * 2].toInt() and 0xFF
            val high = pcm[i * 2 + 1].toInt() shl 8
            samples[i] = (high or low).toShort()
        }
        return RawAudio(samples, rate, channels)
    }
}
