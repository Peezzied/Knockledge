package group.two.knockledge

import android.content.Context
import group.two.knockledge.data.AudioSnapshotInput
import group.two.knockledge.data.ChapterMarkerInput
import group.two.knockledge.data.SentenceInput
import group.two.knockledge.data.SummaryInput
import group.two.knockledge.data.TranscriptInput
import group.two.knockledge.data.model.AiEngineType
import group.two.knockledge.data.model.TranscriptionEngineType
import group.two.knockledge.di.DataGraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

/** Debug-only mock data: seeds `sample.wav` with a timestamped transcript once per signed-in user. */
object DebugSeeder {
    private const val ASSET = "sample.wav"
    private const val PREFS = "debug_seed"
    private const val DURATION_MS = 10_000L
    private const val SENTENCE_MS = 2_500L
    private val SENTENCES = listOf("Mock sentence one.", "Mock sentence two.", "Mock sentence three.", "Mock sentence four.")

    /** Waits for a signed-in user, then seeds once; later launches skip via a per-uid flag. */
    fun start(context: Context, data: DataGraph, scope: CoroutineScope) {
        scope.launch {
            val uid = data.auth.currentUser.filterNotNull().first().uid
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            if (prefs.getBoolean(uid, false)) return@launch
            val file = File(context.cacheDir, ASSET)
            context.assets.open(ASSET).use { input -> file.outputStream().use(input::copyTo) }
            val now = System.currentTimeMillis()
            val note = data.notes.createFromSnapshot(
                AudioSnapshotInput(file, now - DURATION_MS, now, DURATION_MS),
                uid,
            )
            val sentences = SENTENCES.mapIndexed { i, text ->
                SentenceInput("Speaker 1", i * SENTENCE_MS, (i + 1) * SENTENCE_MS, text)
            }
            data.notes.attachTranscript(
                note.id, uid,
                TranscriptInput(SENTENCES.joinToString(" "), sentences, false, "en", TranscriptionEngineType.CLOUD_FIREBASE_AI),
            )
            data.notes.attachSummary(
                note.id, uid,
                SummaryInput(
                    "Mock sample note", "Mock summary.", listOf("Mock point."),
                    listOf(ChapterMarkerInput(0L, "Mock chapter", emptyList())),
                    AiEngineType.CLOUD_FIREBASE_AI,
                ),
            )
            prefs.edit().putBoolean(uid, true).apply()
        }
    }
}
