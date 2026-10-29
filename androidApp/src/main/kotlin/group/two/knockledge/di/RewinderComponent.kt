package group.two.knockledge.di

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import group.two.knockledge.RewinderController
import group.two.knockledge.RewinderGraph
import group.two.knockledge.RewinderService
import group.two.knockledge.ai.AiControls
import group.two.knockledge.ai.AndroidModelStorage
import group.two.knockledge.ai.AndroidSpeechEngine
import group.two.knockledge.ai.AndroidTextEngine
import group.two.knockledge.ai.FirebaseAiTranscriber
import group.two.knockledge.ai.FirebaseGeminiClient
import group.two.knockledge.ai.HttpModelDownloader
import group.two.knockledge.ai.OnDeviceSummarizer
import group.two.knockledge.ai.OnDeviceTranscriber
import group.two.knockledge.ai.PrefsStore
import group.two.knockledge.ai.SpeechEngine
import group.two.knockledge.ai.SystemModelEngine
import group.two.knockledge.ai.TextEngine
import group.two.knockledge.ai.WhisperTranscriberAdapter
import group.two.knockledge.ai.inferenceThreads
import group.two.knockledge.audio.AndroidAudioSource
import group.two.knockledge.audio.AndroidDeviceAudioSource
import group.two.knockledge.audio.AudioSource
import group.two.knockledge.audio.MixedAudioSource
import group.two.knockledge.audio.SessionHooks
import group.two.knockledge.core.LocalAudioRingBuffer
import group.two.knockledge.data.RecentsRepository
import group.two.knockledge.data.StoredRecentsRepository
import group.two.knockledge.models.ModelCatalog
import group.two.knockledge.models.ModelDownloader
import group.two.knockledge.models.ModelFiles
import group.two.knockledge.models.ModelProvider
import group.two.knockledge.models.ModelStorage
import group.two.knockledge.models.ModelStore
import group.two.knockledge.pipeline.CompositeTranscriber
import group.two.knockledge.pipeline.RegexSensitiveFilter
import group.two.knockledge.pipeline.RewindPipeline
import group.two.knockledge.pipeline.SensitiveFilter
import group.two.knockledge.pipeline.Summarizer
import group.two.knockledge.pipeline.Transcriber
import group.two.knockledge.settings.AppSettings
import group.two.knockledge.settings.InputSources
import group.two.knockledge.settings.KeyValueStore
import javax.inject.Named
import javax.inject.Singleton
import org.jetbrains.annotations.Nullable

@Singleton
@Component(modules = [RewinderModule::class, DataModule::class, KnockRecordingModule::class])
interface RewinderComponent {
    fun data(): DataGraph

    fun graph(): RewinderGraph

    fun inject(service: RewinderService)

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance app: Application): RewinderComponent
    }
}

/**
 * The wiring onDeviceGraph used to do by hand: same models, same settings, same pipeline.
 * Android supplies its microphone, "in progress" surface, AI engines, file/HTTP access and a
 * key-value store here; everything else is shared code.
 */
@Module
object RewinderModule {

    @Provides
    @Singleton
    fun keyValueStore(app: Application): KeyValueStore = PrefsStore(app)

    @Provides
    @Named("totalRamBytes")
    fun totalRamBytes(app: Application): Long {
        val am = app.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }.totalMem
    }

    @Provides
    @Named("inferenceThreads")
    fun threads(): Int = inferenceThreads(Runtime.getRuntime().availableProcessors())

    @Provides
    @Singleton
    fun settings(store: KeyValueStore, @Named("totalRamBytes") totalRamBytes: Long): AppSettings =
        AppSettings(store, ModelCatalog.summarizerFor(totalRamBytes).id)

    @Provides
    @Singleton
    fun modelStorage(app: Application): ModelStorage = AndroidModelStorage(app)

    @Provides
    @Singleton
    fun modelDownloader(): ModelDownloader = HttpModelDownloader()

    @Provides
    @Singleton
    fun modelStore(storage: ModelStorage, downloader: ModelDownloader, settings: AppSettings): ModelStore =
        ModelStore(storage, downloader) { settings.summaryModel.value }

    @Provides
    fun modelProvider(store: ModelStore): ModelProvider = store

    @Provides
    fun modelFiles(store: ModelStore): ModelFiles = store

    @Provides
    fun speechEngine(): SpeechEngine = AndroidSpeechEngine

    @Provides
    fun textEngine(): TextEngine = AndroidTextEngine

    /** Null on Android: only iOS 26 ships a built-in model. */
    @Provides
    @Nullable
    fun systemModel(): SystemModelEngine? = null

    @Provides
    @Singleton
    fun onDeviceTranscriber(
        engine: SpeechEngine,
        files: ModelFiles,
        @Named("inferenceThreads") threads: Int,
        settings: AppSettings,
    ): OnDeviceTranscriber = OnDeviceTranscriber(engine, files, threads) { settings.language.value }

    @Provides
    @Singleton
    fun transcriber(onDevice: OnDeviceTranscriber, app: Application): Transcriber {
        val cm = app.getSystemService(ConnectivityManager::class.java)
        return CompositeTranscriber(
            online = FirebaseAiTranscriber(FirebaseGeminiClient()),
            offline = WhisperTranscriberAdapter { onDevice.transcribe(it) },
            isOnline = { cm.getNetworkCapabilities(cm.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true },
        )
    }

    @Provides
    @Singleton
    fun summarizer(
        engine: TextEngine,
        files: ModelFiles,
        @Named("inferenceThreads") threads: Int,
        system: SystemModelEngine?,
        settings: AppSettings,
    ): Summarizer = OnDeviceSummarizer(
        engine = engine,
        files = files,
        threads = threads,
        system = system,
        selected = { settings.summaryModel.value },
        outputLanguage = { settings.summaryLanguage.value },
    )

    @Provides
    fun sensitiveFilter(): SensitiveFilter = RegexSensitiveFilter()

    @Provides
    @Singleton
    fun recents(store: KeyValueStore): RecentsRepository = StoredRecentsRepository(store)

    @Provides
    @Singleton
    fun micSource(app: Application): AndroidAudioSource = AndroidAudioSource(app)

    /** Sound playing on the phone (playback capture), if this Android version supports it. */
    @Provides
    @Singleton
    @Nullable
    fun deviceSource(app: Application): AndroidDeviceAudioSource? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) AndroidDeviceAudioSource(app) else null

    @Provides
    @Singleton
    fun audioSource(
        mic: AndroidAudioSource,
        device: AndroidDeviceAudioSource?,
        settings: AppSettings,
    ): AudioSource = MixedAudioSource(mic, device) {
        settings.inputSource.value == InputSources.MIC_AND_DEVICE
    }

    @Provides
    @Singleton
    fun sessionHooks(app: Application): SessionHooks = ForegroundServiceHooks(app)

    @Provides
    @Singleton
    fun pipeline(transcriber: Transcriber, filter: SensitiveFilter, summarizer: Summarizer): RewindPipeline =
        RewindPipeline(transcriber, filter, summarizer)

    @Provides
    @Singleton
    fun controller(
        audioSource: AudioSource,
        hooks: SessionHooks,
        pipeline: RewindPipeline,
        recents: RecentsRepository,
        buffer: LocalAudioRingBuffer,
        settings: AppSettings,
    ): RewinderController =
        RewinderController(audioSource, hooks, pipeline, recents, buffer, settings.bufferDurationSeconds)

    @Provides
    @Singleton
    fun aiControls(
        settings: AppSettings,
        store: ModelStore,
        system: SystemModelEngine?,
        @Named("totalRamBytes") totalRamBytes: Long,
        device: AndroidDeviceAudioSource?,
    ): AiControls = AiControls(settings, store, system, totalRamBytes, supportsDeviceAudio = device != null)

    @Provides
    @Singleton
    fun graph(
        controller: RewinderController,
        recents: RecentsRepository,
        models: ModelProvider,
        ai: AiControls,
    ): RewinderGraph = RewinderGraph(controller, recents, models, ai)
}

/** Keeps the mic alive in the background and puts Rewinder on the lock screen. */
private class ForegroundServiceHooks(private val context: Context) : SessionHooks {
    override fun onSessionStarted() {
        context.startForegroundService(Intent(context, RewinderService::class.java))
    }

    override fun onSessionStopped() {
        context.stopService(Intent(context, RewinderService::class.java))
    }
}
