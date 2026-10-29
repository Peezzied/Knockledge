package group.two.knockledge

import group.two.knockledge.ai.AiControls
import group.two.knockledge.ai.OnDeviceSummarizer
import group.two.knockledge.ai.OnDeviceTranscriber
import group.two.knockledge.ai.SpeechEngine
import group.two.knockledge.ai.SystemModelEngine
import group.two.knockledge.ai.TextEngine
import group.two.knockledge.ai.inferenceThreads
import group.two.knockledge.audio.AudioSource
import group.two.knockledge.audio.MixedAudioSource
import group.two.knockledge.audio.SessionHooks
import group.two.knockledge.data.InMemoryRecentsRepository
import group.two.knockledge.data.RecentsRepository
import group.two.knockledge.data.StoredRecentsRepository
import group.two.knockledge.models.ModelCatalog
import group.two.knockledge.models.ModelDownloader
import group.two.knockledge.models.ModelProvider
import group.two.knockledge.models.ModelStorage
import group.two.knockledge.models.ModelStore
import group.two.knockledge.pipeline.PlaceholderSummarizer
import group.two.knockledge.pipeline.PlaceholderTranscriber
import group.two.knockledge.pipeline.RegexSensitiveFilter
import group.two.knockledge.pipeline.RewindPipeline
import group.two.knockledge.pipeline.SensitiveFilter
import group.two.knockledge.pipeline.Summarizer
import group.two.knockledge.pipeline.Transcriber
import group.two.knockledge.settings.AppSettings
import group.two.knockledge.settings.InputSources
import group.two.knockledge.settings.KeyValueStore

/**
 * Manual dependency wiring. Each platform builds exactly one of these per process
 * and passes in its own microphone and "in progress" surface.
 */
class RewinderGraph(
    audioSource: AudioSource,
    hooks: SessionHooks,
    transcriber: Transcriber = PlaceholderTranscriber(),
    filter: SensitiveFilter = RegexSensitiveFilter(),
    summarizer: Summarizer = PlaceholderSummarizer(),
    val recents: RecentsRepository = InMemoryRecentsRepository(),
    /** On-device AI model store; null where the platform has none yet. */
    val models: ModelProvider? = null,
    /** Settings and model picker; null in previews. */
    val ai: AiControls? = null,
) {
    val controller = RewinderController(
        audioSource = audioSource,
        hooks = hooks,
        pipeline = RewindPipeline(transcriber, filter, summarizer),
        recents = recents,
    )
}

/**
 * The one wiring both platforms use: same models, same settings, same pipeline, same UI.
 * A platform supplies only its microphone, "in progress" surface, AI engines, file/HTTP access
 * and a key-value store (plus Apple's built-in model on iOS).
 */
fun onDeviceGraph(
    audioSource: AudioSource,
    hooks: SessionHooks,
    speech: SpeechEngine,
    text: TextEngine,
    storage: ModelStorage,
    downloader: ModelDownloader,
    settingsStore: KeyValueStore,
    totalRamBytes: Long,
    cpuCores: Int,
    systemModel: SystemModelEngine? = null,
    /** Sound playing on the phone (Android playback capture, iOS screen broadcast), if supported. */
    deviceAudio: AudioSource? = null,
): RewinderGraph {
    // Qwen is the default on both platforms (sized to the phone's RAM). Apple's model stays
    // available in Settings on iOS 26.
    val defaultSummary = ModelCatalog.summarizerFor(totalRamBytes).id
    val settings = AppSettings(settingsStore, defaultSummary)
    val models = ModelStore(storage, downloader) { settings.summaryModel.value }
    val threads = inferenceThreads(cpuCores)
    val input = MixedAudioSource(audioSource, deviceAudio) {
        settings.inputSource.value == InputSources.MIC_AND_DEVICE
    }
    return RewinderGraph(
        audioSource = input,
        hooks = hooks,
        transcriber = OnDeviceTranscriber(speech, models, threads) { settings.language.value },
        summarizer = OnDeviceSummarizer(
            engine = text,
            files = models,
            threads = threads,
            system = systemModel,
            selected = { settings.summaryModel.value },
            outputLanguage = { settings.summaryLanguage.value },
        ),
        recents = StoredRecentsRepository(settingsStore),
        models = models,
        ai = AiControls(settings, models, systemModel, totalRamBytes, supportsDeviceAudio = deviceAudio != null),
    )
}

