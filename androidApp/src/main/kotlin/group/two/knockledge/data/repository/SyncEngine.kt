package group.two.knockledge.data.repository

import group.two.knockledge.data.local.NoteDao
import group.two.knockledge.data.local.SyncQueueDao
import group.two.knockledge.data.local.SyncQueueEntity
import group.two.knockledge.data.local.toNote
import group.two.knockledge.data.model.QueueStatus
import group.two.knockledge.data.model.SyncStatus
import group.two.knockledge.data.model.SyncTaskType
import group.two.knockledge.data.remote.AudioUploads
import group.two.knockledge.data.remote.NoteSyncSource
import group.two.knockledge.data.remote.toFirestoreDocument
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

class SyncEngine(
    private val notes: NoteDao,
    private val queue: SyncQueueDao,
    private val storage: AudioUploads,
    private val remote: NoteSyncSource,
    private val now: () -> Long = System::currentTimeMillis,
    private val sleep: suspend (Long) -> Unit = { delay(it) },
    private val maxAttempts: Int = 4,
    private val backoffBaseMs: Long = 1_000L,
) {
    suspend fun syncUser(userId: String) {
        queue.pending(userId).forEach { process(it) }
    }

    private suspend fun process(item: SyncQueueEntity) {
        queue.insert(item.copy(status = QueueStatus.PROCESSING, updatedAtEpochMs = now()))
        var attempts = item.retryCount
        while (true) {
            try {
                execute(item)
                queue.delete(item.queueId)
                return
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                attempts++
                val failed = attempts >= maxAttempts
                queue.insert(
                    item.copy(
                        retryCount = attempts,
                        status = if (failed) QueueStatus.FAILED else QueueStatus.QUEUED,
                        lastError = e.message,
                        updatedAtEpochMs = now(),
                    ),
                )
                if (failed) {
                    markFailed(item)
                    return
                }
                sleep(backoffBaseMs shl (attempts - 1))
            }
        }
    }

    private suspend fun execute(item: SyncQueueEntity) {
        val entity = notes.getNote(item.noteId, item.userId) ?: return
        when (item.taskType) {
            SyncTaskType.UPLOAD_AUDIO -> {
                if (entity.audioRemoteUrl != null) return
                val url = storage.uploadAudio(item.userId, item.noteId, File(entity.audioLocalPath))
                notes.upsert(
                    entity.copy(
                        audioRemoteUrl = url,
                        syncStatus = SyncStatus.SYNCING,
                        updatedAtEpochMs = now(),
                    ),
                )
            }
            SyncTaskType.SYNC_FIRESTORE -> {
                remote.upsertNote(item.userId, item.noteId, entity.toNote().toFirestoreDocument())
                val status = if (entity.audioRemoteUrl != null) SyncStatus.SYNCED else SyncStatus.SYNC_FAILED
                notes.upsert(entity.copy(syncStatus = status, updatedAtEpochMs = now()))
            }
        }
    }

    private suspend fun markFailed(item: SyncQueueEntity) {
        notes.getNote(item.noteId, item.userId)?.let {
            notes.upsert(it.copy(syncStatus = SyncStatus.SYNC_FAILED, updatedAtEpochMs = now()))
        }
    }
}
