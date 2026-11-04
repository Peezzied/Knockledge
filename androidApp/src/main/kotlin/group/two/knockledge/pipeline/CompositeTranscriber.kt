package group.two.knockledge.pipeline

import android.util.Log
import group.two.knockledge.ai.EngineError
import group.two.knockledge.audio.AudioConditioner
import group.two.knockledge.settings.AppSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout

/**
 * Routes transcription between the [online] and [offline] engines by the user's
 * [mode]: "local" always uses [offline], "online" always uses [online] and
 * "auto" tries [online] when a connection is available and falls back to
 * [offline] on timeout or failure. Audio is conditioned before either sees it.
 */
class CompositeTranscriber(
    private val online: TranscriptionEngine,
    private val offline: TranscriptionEngine,
    private val isOnline: () -> Boolean,
    private val mode: () -> String = { AppSettings.MODE_AUTO },
    private val timeoutMillis: Long = 120_000,
) : Transcriber {
    /** Conditions [pcm], then transcribes online if possible and offline otherwise. */
    override suspend fun transcribe(pcm: ShortArray, sampleRate: Int): TranscriptResult {
        val conditioned = AudioConditioner.condition(pcm, sampleRate)
        if (conditioned.isEmpty()) {
            return TranscriptResult(emptyList(), "", false, "unknown", TranscriptionEngineType.LOCAL_WHISPER)
        }
        val segment = AudioSegment(conditioned, sampleRate, conditioned.size * 1000L / sampleRate, isGainNormalized = true)
        val currentMode = mode()

        if (currentMode == AppSettings.MODE_LOCAL) {
            return run(offline, segment)
        }

        if (currentMode == AppSettings.MODE_ONLINE || (currentMode == AppSettings.MODE_AUTO && isOnline())) {
            try {
                return withTimeout(timeoutMillis) { run(online, segment) }
            } catch (e: CancellationException) {
                if (e !is kotlinx.coroutines.TimeoutCancellationException) throw e
            } catch (e: Exception) {
                if (currentMode == AppSettings.MODE_ONLINE) throw e
            }
        }
        try {
            return run(offline, segment)
        } catch (e: EngineError) {
            // A broken local engine in Auto mode still has the cloud to fall back on.
            if (currentMode == AppSettings.MODE_AUTO && isOnline()) {
                return run(online, segment)
            }
            throw e
        }
    }

    /** Runs [engine] bracketed by logcat lines naming the engine, for AI-processing audits. */
    private suspend fun run(engine: TranscriptionEngine, segment: AudioSegment): TranscriptResult {
        audit("transcription start: engine=${engine.engineType}")
        return try {
            engine.process(segment).also { audit("transcription end: engine=${it.engineType}") }
        } catch (e: Exception) {
            audit("transcription failed: engine=${engine.engineType}, ${e.javaClass.simpleName}")
            throw e
        }
    }

    /** android.util.Log is stubbed on the JVM unit test classpath; ignore failures there. */
    private fun audit(message: String) = runCatching { Log.i(TAG, message) }

    private companion object {
        const val TAG = "AiAudit"
    }
}
