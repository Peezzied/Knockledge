package group.two.knockledge.pipeline

import android.util.Log
import group.two.knockledge.ai.EngineError
import group.two.knockledge.settings.AppSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout

/**
 * Routes summarization requests between [online] (Gemini cloud) and [offline] (on-device)
 * based on [mode] ("auto", "online", "local") and network status.
 */
class CompositeSummarizer(
    private val online: Summarizer,
    private val offline: Summarizer,
    private val isOnline: () -> Boolean,
    private val mode: () -> String = { AppSettings.MODE_AUTO },
    private val timeoutMillis: Long = 120_000,
) : Summarizer {
    /** Summarizes with the engine [mode] selects, falling back to [offline] on timeout or failure. */
    override suspend fun summarize(request: SummaryRequest): Summary {
        val currentMode = mode()
        if (currentMode == AppSettings.MODE_LOCAL) {
            return run(offline, "local", request)
        }
        if (currentMode == AppSettings.MODE_ONLINE || (currentMode == AppSettings.MODE_AUTO && isOnline())) {
            try {
                return withTimeout(timeoutMillis) { run(online, "cloud", request) }
            } catch (e: CancellationException) {
                if (e !is kotlinx.coroutines.TimeoutCancellationException) throw e
            } catch (e: Exception) {
                if (currentMode == AppSettings.MODE_ONLINE) throw e
            }
        }
        try {
            return run(offline, "local", request)
        } catch (e: EngineError) {
            // A broken local engine in Auto mode still has the cloud to fall back on.
            if (currentMode == AppSettings.MODE_AUTO && isOnline()) {
                return run(online, "cloud", request)
            }
            throw e
        }
    }

    /** Runs [engine] bracketed by logcat lines naming it, for AI-processing audits. [kind] is "cloud" or "local". */
    private suspend fun run(engine: Summarizer, kind: String, request: SummaryRequest): Summary {
        val name = "$kind/${engine.javaClass.simpleName}"
        audit("summary start: engine=$name")
        return try {
            engine.summarize(request).also { audit("summary end: engine=$name") }
        } catch (e: Exception) {
            audit("summary failed: engine=$name, ${e.javaClass.simpleName}")
            throw e
        }
    }

    /** android.util.Log is stubbed on the JVM unit test classpath; ignore failures there. */
    private fun audit(message: String) = runCatching { Log.i(TAG, message) }

    private companion object {
        const val TAG = "AiAudit"
    }
}
