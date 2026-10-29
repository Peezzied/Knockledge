package group.two.knockledge.sensor

interface KnockGestureDetector {
    fun startListening(onKnockDetected: () -> Unit)

    fun stopListening()

    val isListening: Boolean
}

class KnockPeakMatcher(
    private val threshold: Float = 12.0f,
    private val minGapMs: Long = 100L,
    private val maxGapMs: Long = 400L,
    private val debounceMs: Long = 2000L,
) {
    private var hasFirst = false
    private var firstPeakMs = 0L
    private var ignoreUntilMs = Long.MIN_VALUE

    fun onSample(magnitude: Float, timeMs: Long): Boolean {
        if (timeMs < ignoreUntilMs || magnitude < threshold) return false
        val gap = timeMs - firstPeakMs
        if (hasFirst && gap < minGapMs) return false
        if (hasFirst && gap <= maxGapMs) {
            hasFirst = false
            ignoreUntilMs = timeMs + debounceMs
            return true
        }
        hasFirst = true
        firstPeakMs = timeMs
        return false
    }

    fun reset() {
        hasFirst = false
        ignoreUntilMs = Long.MIN_VALUE
    }
}
