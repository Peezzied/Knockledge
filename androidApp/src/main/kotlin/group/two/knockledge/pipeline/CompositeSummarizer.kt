package group.two.knockledge.pipeline

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
    private val timeoutMillis: Long = 15_000,
) : Summarizer {
    /** Summarizes with the engine [mode] selects, falling back to [offline] on timeout or failure. */
    override suspend fun summarize(request: SummaryRequest): Summary {
        val currentMode = mode()
        if (currentMode == AppSettings.MODE_LOCAL) {
            return offline.summarize(request)
        }
        if (currentMode == AppSettings.MODE_ONLINE || (currentMode == AppSettings.MODE_AUTO && isOnline())) {
            try {
                return withTimeout(timeoutMillis) { online.summarize(request) }
            } catch (e: CancellationException) {
                if (e !is kotlinx.coroutines.TimeoutCancellationException) throw e
            } catch (e: Exception) {
                if (currentMode == AppSettings.MODE_ONLINE) throw e
            }
        }
        try {
            return offline.summarize(request)
        } catch (e: EngineError) {
            // A broken local engine in Auto mode still has the cloud to fall back on.
            if (currentMode == AppSettings.MODE_AUTO && isOnline()) {
                return online.summarize(request)
            }
            throw e
        }
    }
}
