package group.two.knockledge.di

import android.app.Application
import androidx.room.Room
import dagger.Module
import dagger.Provides
import group.two.knockledge.data.AuthRepository
import group.two.knockledge.data.NoteRepository
import group.two.knockledge.data.local.AudioFileStorage
import group.two.knockledge.data.local.KnockledgeDatabase
import group.two.knockledge.data.local.NoteDao
import group.two.knockledge.data.local.SyncQueueDao
import group.two.knockledge.data.remote.AudioUploads
import group.two.knockledge.data.remote.AuthSource
import group.two.knockledge.data.remote.FirebaseAuthSource
import group.two.knockledge.data.remote.FirebaseStorageSource
import group.two.knockledge.data.remote.FirestoreDataSource
import group.two.knockledge.data.remote.NoteSyncSource
import group.two.knockledge.data.repository.AuthRepositoryImpl
import group.two.knockledge.data.repository.NoteRepositoryImpl
import group.two.knockledge.data.repository.SyncEngine
import javax.inject.Inject
import javax.inject.Singleton

class DataGraph @Inject constructor(val auth: AuthRepository, val notes: NoteRepository)

@Module
object DataModule {
    @Provides
    @Singleton
    fun database(app: Application): KnockledgeDatabase =
        Room.databaseBuilder(app, KnockledgeDatabase::class.java, "knockledge.db").build()

    @Provides
    fun noteDao(db: KnockledgeDatabase): NoteDao = db.noteDao()

    @Provides
    fun syncQueueDao(db: KnockledgeDatabase): SyncQueueDao = db.syncQueueDao()

    @Provides
    @Singleton
    fun authSource(): AuthSource = FirebaseAuthSource()

    @Provides
    @Singleton
    fun authRepository(source: AuthSource): AuthRepository = AuthRepositoryImpl(source)

    @Provides
    @Singleton
    fun audioUploads(): AudioUploads = FirebaseStorageSource()

    @Provides
    @Singleton
    fun noteSyncSource(): NoteSyncSource = FirestoreDataSource()

    @Provides
    @Singleton
    fun syncEngine(
        notes: NoteDao,
        queue: SyncQueueDao,
        uploads: AudioUploads,
        remote: NoteSyncSource,
    ): SyncEngine = SyncEngine(notes, queue, uploads, remote)

    @Provides
    @Singleton
    fun noteRepository(
        app: Application,
        notes: NoteDao,
        queue: SyncQueueDao,
        engine: SyncEngine,
    ): NoteRepository =
        NoteRepositoryImpl(notes, queue, AudioFileStorage.audioDir(app), onSync = engine::syncUser)
}
