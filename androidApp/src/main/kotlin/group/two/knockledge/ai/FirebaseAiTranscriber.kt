package group.two.knockledge.ai

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import group.two.knockledge.pipeline.AudioSegment
import group.two.knockledge.pipeline.TranscriptResult
import group.two.knockledge.pipeline.TranscriptSentence
import group.two.knockledge.pipeline.TranscriptionEngine
import group.two.knockledge.pipeline.TranscriptionEngineType
import java.nio.ByteBuffer
import java.nio.ByteOrder

interface GeminiClient {
    suspend fun generate(wav: ByteArray, prompt: String): String
}

class FirebaseGeminiClient : GeminiClient {
    private val model by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = MODEL_ID,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = Schema.obj(
                    mapOf(
                        "detectedLanguage" to Schema.string(),
                        "sentences" to Schema.array(
                            Schema.obj(
                                mapOf(
                                    "speaker" to Schema.string(),
                                    "startTimeMs" to Schema.long(),
                                    "endTimeMs" to Schema.long(),
                                    "text" to Schema.string(),
                                    "confidence" to Schema.float(),
                                ),
                                optionalProperties = listOf("confidence"),
                            )
                        ),
                    )
                )
            },
        )
    }

    override suspend fun generate(wav: ByteArray, prompt: String): String =
        model.generateContent(content { inlineData(wav, "audio/wav"); text(prompt) }).text
            ?: error("empty Gemini response")

    companion object {
        const val MODEL_ID = "gemini-3.5-flash"
    }
}

class FirebaseAiTranscriber(private val client: GeminiClient) : TranscriptionEngine {
    override val engineType = TranscriptionEngineType.CLOUD_FIREBASE_AI

    override suspend fun process(segment: AudioSegment): TranscriptResult =
        parse(client.generate(wav(segment.pcm, segment.sampleRate), PROMPT))

    private fun parse(json: String): TranscriptResult {
        val root = MiniJson(json).value() as Map<*, *>
        val sentences = (root["sentences"] as List<*>).map {
            it as Map<*, *>
            TranscriptSentence(
                speaker = it["speaker"] as String,
                startTimeMs = (it["startTimeMs"] as Number).toLong(),
                endTimeMs = (it["endTimeMs"] as Number).toLong(),
                text = it["text"] as String,
                confidence = (it["confidence"] as Number?)?.toFloat() ?: 1.0f,
            )
        }
        return TranscriptResult(
            sentences = sentences,
            fullText = sentences.joinToString(" ") { it.text },
            isDiarized = sentences.map { it.speaker }.distinct().size > 1,
            detectedLanguage = root["detectedLanguage"] as String,
            engineType = engineType,
        )
    }

    private fun wav(pcm: ShortArray, rate: Int): ByteArray {
        val data = pcm.size * 2
        val b = ByteBuffer.allocate(44 + data).order(ByteOrder.LITTLE_ENDIAN)
        b.put("RIFF".toByteArray()).putInt(36 + data).put("WAVEfmt ".toByteArray())
        b.putInt(16).putShort(1).putShort(1).putInt(rate).putInt(rate * 2).putShort(2).putShort(16)
        b.put("data".toByteArray()).putInt(data)
        for (s in pcm) b.putShort(s)
        return b.array()
    }

    private companion object {
        const val PROMPT = "Transcribe this audio verbatim. The language may be English, Filipino or Taglish. " +
            "Separate speaker turns as Speaker 1, Speaker 2, and so on, with millisecond start and end times."
    }
}

private class MiniJson(private val s: String) {
    private var i = 0

    fun value(): Any? {
        ws()
        return when (val c = s[i]) {
            '{' -> {
                i++
                val m = LinkedHashMap<String, Any?>()
                ws()
                if (s[i] == '}') i++ else do {
                    ws()
                    val k = string()
                    ws(); expect(':')
                    m[k] = value()
                    ws()
                } while (s[i++] == ',')
                m
            }
            '[' -> {
                i++
                val l = ArrayList<Any?>()
                ws()
                if (s[i] == ']') i++ else do {
                    l += value()
                    ws()
                } while (s[i++] == ',')
                l
            }
            '"' -> string()
            't' -> lit("true", true)
            'f' -> lit("false", false)
            'n' -> lit("null", null)
            else -> {
                require(c == '-' || c.isDigit()) { "bad json at $i" }
                val st = i
                while (i < s.length && (s[i].isDigit() || s[i] in "+-.eE")) i++
                s.substring(st, i).toDouble()
            }
        }
    }

    private fun string(): String {
        expect('"')
        val sb = StringBuilder()
        while (s[i] != '"') {
            var c = s[i++]
            if (c == '\\') {
                c = s[i++]
                when (c) {
                    'n' -> sb.append('\n')
                    't' -> sb.append('\t')
                    'r' -> sb.append('\r')
                    'b' -> sb.append('\b')
                    'f' -> sb.append('\u000C')
                    'u' -> { sb.append(s.substring(i, i + 4).toInt(16).toChar()); i += 4 }
                    else -> sb.append(c)
                }
            } else sb.append(c)
        }
        i++
        return sb.toString()
    }

    private fun lit(word: String, v: Any?): Any? {
        require(s.startsWith(word, i)) { "bad json at $i" }
        i += word.length
        return v
    }

    private fun expect(c: Char) = require(s[i++] == c) { "bad json at ${i - 1}" }

    private fun ws() { while (i < s.length && s[i].isWhitespace()) i++ }
}
