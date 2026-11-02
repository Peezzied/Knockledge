#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>

#include "whisper.h"

#define LOG_TAG "knockledge_whisper"
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

extern "C" {

JNIEXPORT jlong JNICALL
Java_group_two_knockledge_ai_WhisperNative_initContext(JNIEnv *env, jobject, jstring modelPath) {
    const char *path = env->GetStringUTFChars(modelPath, nullptr);
    whisper_context_params cparams = whisper_context_default_params();
    cparams.use_gpu = false;
    whisper_context *ctx = whisper_init_from_file_with_params(path, cparams);
    env->ReleaseStringUTFChars(modelPath, path);
    return reinterpret_cast<jlong>(ctx);
}

JNIEXPORT jstring JNICALL
Java_group_two_knockledge_ai_WhisperNative_transcribe(
    JNIEnv *env, jobject, jlong ctxPtr, jfloatArray pcm,
    jstring language, jstring prompt, jstring vadModelPath, jint threads
) {
    whisper_context *ctx = reinterpret_cast<whisper_context *>(ctxPtr);
    if (ctx == nullptr) return env->NewStringUTF("");

    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.n_threads = threads;
    params.no_timestamps = true;
    params.single_segment = false;
    params.print_realtime = false;
    params.print_progress = false;
    params.print_timestamps = false;

    const char *lang = env->GetStringUTFChars(language, nullptr);
    params.language = (lang != nullptr && lang[0] != '\0' &&
                       !(lang[0] == 'a' && strcmp(lang, "auto") == 0)) ? lang : nullptr;
    params.detect_language = params.language == nullptr;

    const char *pr = env->GetStringUTFChars(prompt, nullptr);
    params.initial_prompt = (pr != nullptr && pr[0] != '\0') ? pr : nullptr;

    const char *vadPath = nullptr;
    if (vadModelPath != nullptr) {
        vadPath = env->GetStringUTFChars(vadModelPath, nullptr);
    }
    if (vadPath != nullptr && vadPath[0] != '\0') {
        params.vad = true;
        params.vad_model_path = vadPath;
        params.vad_params = whisper_vad_default_params();
    }

    jsize n = env->GetArrayLength(pcm);
    std::vector<float> samples(static_cast<size_t>(n));
    env->GetFloatArrayRegion(pcm, 0, n, samples.data());

    std::string text;
    if (n > 0 && whisper_full(ctx, params, samples.data(), n) == 0) {
        const int nSegments = whisper_full_n_segments(ctx);
        for (int i = 0; i < nSegments; ++i) {
            const char *segment = whisper_full_get_segment_text(ctx, i);
            if (segment != nullptr) text += segment;
        }
    } else {
        LOGW("whisper_full failed or empty input (%d samples)", static_cast<int>(n));
    }

    if (vadPath != nullptr) env->ReleaseStringUTFChars(vadModelPath, vadPath);
    env->ReleaseStringUTFChars(prompt, pr);
    env->ReleaseStringUTFChars(language, lang);

    return env->NewStringUTF(text.c_str());
}

JNIEXPORT void JNICALL
Java_group_two_knockledge_ai_WhisperNative_freeContext(JNIEnv *, jobject, jlong ctxPtr) {
    whisper_free(reinterpret_cast<whisper_context *>(ctxPtr));
}

}
