package two.knockledge.ui.notes

import group.two.knockledge.data.AudioSnapshotInput
import group.two.knockledge.data.NoteRepository
import group.two.knockledge.data.SummaryInput
import group.two.knockledge.data.TranscriptInput
import group.two.knockledge.data.model.AudioMetadata
import group.two.knockledge.data.model.Note
import group.two.knockledge.data.model.TriggerType
import group.two.knockledge.ui.notes.NoteListViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlin.test.Test
import kotlin.test.assertEquals

class NoteListViewModelTest {

    private class FakeNotes(private val initial: List<Note>) : NoteRepository {
        var deleted: Pair<String, String>? = null
        var renamed: Triple<String, String, String>? = null

        override fun observeNotes(userId: String): Flow<List<Note>> = flowOf(initial)
        override suspend fun getNote(id: String, userId: String): Note? = initial.firstOrNull { it.id == id }
        override suspend fun createFromSnapshot(snapshot: AudioSnapshotInput, userId: String): Note = TODO()

        override suspend fun attachTranscript(noteId: String, userId: String, transcript: TranscriptInput): Note =
            initial.first { it.id == noteId }

        override suspend fun attachSummary(noteId: String, userId: String, summary: SummaryInput): Note =
            initial.first { it.id == noteId }

        override suspend fun renameNote(id: String, userId: String, title: String): Note {
            renamed = Triple(id, userId, title)
            return initial.first { it.id == id }.copy(title = title)
        }

        override suspend fun deleteNote(id: String, userId: String) {
            deleted = id to userId
        }

        override suspend fun syncPending(userId: String) = Unit
    }

    private fun note(id: String, userId: String) = Note(
        id = id,
        userId = userId,
        title = "Title $id",
        createdAtEpochMs = 1_000L,
        updatedAtEpochMs = 1_000L,
        audio = AudioMetadata("/tmp/$id.wav", null, 1_000L, 16_000, TriggerType.GESTURE),
    )

    private val userId = "u1"
    private val scope = CoroutineScope(Dispatchers.Unconfined)

    @Test
    fun notesFlowEmitsNotesForUserId() {
        val notes = listOf(note("n1", userId), note("n2", userId))
        val vm = NoteListViewModel(FakeNotes(notes), userId, scope)

        assertEquals(notes, vm.notes.value)
    }

    @Test
    fun deleteNoteDelegatesToRepositoryWithUserId() {
        val notes = listOf(note("n1", userId))
        val repo = FakeNotes(notes)
        val vm = NoteListViewModel(repo, userId, scope)

        vm.deleteNote("n1")

        assertEquals("n1" to userId, repo.deleted)
    }

    @Test
    fun renameNoteDelegatesToRepositoryWithUserId() {
        val notes = listOf(note("n1", userId))
        val repo = FakeNotes(notes)
        val vm = NoteListViewModel(repo, userId, scope)

        vm.renameNote("n1", "Renamed")

        assertEquals(Triple("n1", userId, "Renamed"), repo.renamed)
    }
}
