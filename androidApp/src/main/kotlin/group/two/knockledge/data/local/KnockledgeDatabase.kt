package group.two.knockledge.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [NoteEntity::class, SyncQueueEntity::class], version = 1, exportSchema = false)
@TypeConverters(NoteTypeConverters::class)
abstract class KnockledgeDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun syncQueueDao(): SyncQueueDao
}
