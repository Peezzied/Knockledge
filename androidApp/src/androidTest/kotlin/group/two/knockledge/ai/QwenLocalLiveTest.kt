package group.two.knockledge.ai

import androidx.test.ext.junit.runners.AndroidJUnit4
import group.two.knockledge.pipeline.SummaryParser
import group.two.knockledge.pipeline.SummaryRequest
import group.two.knockledge.pipeline.SummarizerPrompt
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Live instrumented test executing local LiteRT-LM Qwen3.5 summarization on device CPU.
 */
@RunWith(AndroidJUnit4::class)
class QwenLocalLiveTest {

    @Test
    fun liveQwenSummarizesTranscriptOnDevice() {
        val modelFile = File("/data/local/tmp/Qwen3.5-0.8B_int8.litertlm")
        assertTrue("Model file missing at ${modelFile.absolutePath}", modelFile.exists() && modelFile.length() > 0)

        val transcript = "Speaker 1: Okay, tara na. Let us start the meeting now.\n" +
            "Speaker 2: Sige. I will share my screen in five seconds."

        val systemPrompt = SummarizerPrompt.summarizerSystem("English")
        val userPrompt = SummarizerPrompt.summarizerUser(
            request = SummaryRequest(
                filteredTranscript = transcript,
                looksLikeConversation = true,
                redactedCount = 0,
                durationMillis = 10_000L,
            ),
            outputLanguage = "English",
        )

        val raw = AndroidTextEngine.complete(
            modelPath = modelFile.absolutePath,
            systemPrompt = systemPrompt,
            userPrompt = userPrompt,
            maxTokens = 320,
            temperature = 0.7f,
            topP = 0.8f,
            topK = 20,
        )

        println("QWEN_RAW_OUTPUT:\n$raw")
        assertNotNull("Raw completion should not be null", raw)
        assertTrue("Raw completion should not be blank", raw.isNotBlank())

        val summary = SummaryParser.parse(raw, 0)
        println("PARSED_SUMMARY: title='${summary.title}' gist='${summary.gist}' points=${summary.points}")

        assertTrue("Summary title should not be blank", summary.title.isNotBlank())
        assertTrue("Summary gist should not be blank", summary.gist.isNotBlank())
    }
}
