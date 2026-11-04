package group.two.knockledge.ai

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import java.io.File
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking

/**
 * Offline Whisper speech engine using whisper.cpp through [WhisperNative].
 * The Silero VAD file is handed to whisper.cpp so silence never becomes text.
 */
object AndroidSpeechEngine : SpeechEngine {

    /**
     * Loads the model at [modelPath], transcribes [pcm] and frees the model before returning,
     * even on failure. Throws [EngineError] on load, memory or device problems.
     */
    override fun transcribe(
        modelPath: String,
        pcm: ShortArray,
        language: String,
        prompt: String,
        vadModelPath: String?,
        threads: Int,
    ): String {
        if (pcm.isEmpty()) return ""

        WhisperNative.ensureLoaded()

        val floatAudio = FloatArray(pcm.size) { i -> pcm[i] / 32768.0f }

        val ctx = try {
            WhisperNative.initContext(modelPath)
        } catch (e: OutOfMemoryError) {
            throw EngineError.OutOfMemory
        }
        if (ctx == 0L) {
            throw EngineError.ModelLoadFailed(modelIdFromPath(modelPath))
        }

        return try {
            WhisperNative.transcribe(ctx, floatAudio, language, prompt, vadModelPath, threads)
        } catch (e: OutOfMemoryError) {
            throw EngineError.OutOfMemory
        } finally {
            WhisperNative.freeContext(ctx)
        }
    }
}

/**
 * Offline text summarization engine using LiteRT-LM on the CPU backend.
 * Qwen3.5 needs a fresh Engine and Conversation per call, so each [complete] opens the
 * model, generates, and closes both before returning.
 */
object AndroidTextEngine : TextEngine {
    /**
     * Returns the generated text for [userPrompt] under [systemPrompt].
     * Throws [EngineError.ModelLoadFailed] when the model cannot be opened and
     * [EngineError.OutOfMemory] when loading or generating runs out of memory.
     */
    override fun complete(
        modelPath: String,
        systemPrompt: String,
        userPrompt: String,
        maxTokens: Int,
        temperature: Float,
        topP: Float,
        topK: Int,
    ): String {
        val modelFile = File(modelPath)
        if (!modelFile.exists() || modelFile.length() == 0L) {
            throw EngineError.ModelMissing(modelFile.nameWithoutExtension)
        }

        val engine = try {
            Engine(
                EngineConfig(
                    modelPath = modelFile.absolutePath,
                    backend = Backend.CPU(),
                    cacheDir = listOfNotNull(
                        modelFile.parentFile?.takeIf { it.canWrite() },
                        File(modelFile.parentFile, "litert_cache").takeIf { it.exists() && it.canWrite() },
                        File(System.getProperty("java.io.tmpdir") ?: "").takeIf { it.exists() && it.canWrite() },
                        File("/data/data/group.two.knockledge/cache").takeIf { it.exists() && it.canWrite() },
                        File("/data/user/0/group.two.knockledge/cache").takeIf { it.exists() && it.canWrite() },
                    ).firstOrNull()?.absolutePath ?: modelFile.parentFile?.absolutePath,
                ),
            ).also { it.initialize() }
        } catch (e: OutOfMemoryError) {
            throw EngineError.OutOfMemory
        } catch (e: Exception) {
            throw EngineError.ModelLoadFailed(modelFile.nameWithoutExtension, e)
        }

        return try {
            val conversation = engine.createConversation(
                ConversationConfig(
                    systemInstruction = Contents.of(systemPrompt),
                    samplerConfig = SamplerConfig(
                        topK = topK,
                        topP = topP.toDouble(),
                        temperature = temperature.toDouble(),
                    ),
                ),
            )
            try {
                // shortcut: maxTokens is not exposed by LiteRT-LM 0.18; the prompt asks for a
                // short answer instead. Revisit when the runtime supports a token cap.
                runBlocking {
                    conversation.sendMessageAsync(userPrompt)
                        .toList()
                        .joinToString("") { it.toString() }
                }
            } finally {
                conversation.close()
            }
        } catch (e: OutOfMemoryError) {
            throw EngineError.OutOfMemory
        } finally {
            engine.close()
        }
    }
}

private fun modelIdFromPath(modelPath: String): String =
    File(modelPath).nameWithoutExtension
