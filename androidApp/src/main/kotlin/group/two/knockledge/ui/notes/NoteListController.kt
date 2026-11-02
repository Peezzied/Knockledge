package group.two.knockledge.ui.notes

import group.two.knockledge.data.model.Note
import kotlinx.coroutines.flow.StateFlow

/**
 * Contract for observing and mutating the signed-in user's note list.
 */
interface NoteListController {
    /** Notes owned by the active user, newest first as emitted by the repository. */
    val notes: StateFlow<List<Note>>

    /** Deletes the note with [id] for the active user. */
    fun deleteNote(id: String)

    /** Renames the note with [id] to [title] for the active user. */
    fun renameNote(id: String, title: String)
}
