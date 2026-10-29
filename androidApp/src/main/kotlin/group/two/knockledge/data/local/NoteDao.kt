package group.two.knockledge.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE user_id = :userId ORDER BY created_at_epoch_ms DESC")
    fun observeNotes(userId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id AND user_id = :userId")
    suspend fun getNote(id: String, userId: String): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id AND user_id = :userId")
    suspend fun delete(id: String, userId: String)
}

@Dao
interface SyncQueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SyncQueueEntity)

    @Query("SELECT * FROM sync_queue WHERE user_id = :userId AND status != 'PROCESSING' ORDER BY updated_at_epoch_ms")
    suspend fun pending(userId: String): List<SyncQueueEntity>
}
