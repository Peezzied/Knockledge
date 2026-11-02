package group.two.knockledge.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import group.two.knockledge.data.NoteRepository
import group.two.knockledge.data.model.Note
import group.two.knockledge.data.model.SyncStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel exposing the active user's notes and their delete/rename mutations.
 */
class NoteListViewModel(
    private val repo: NoteRepository,
    private val userId: String,
    scope: CoroutineScope? = null,
) : ViewModel(), NoteListController {

    private val coroutineScope = scope ?: viewModelScope

    private val manualSyncing = MutableStateFlow(false)

    /** Stream of notes for [userId], eagerly collected. */
    override val notes: StateFlow<List<Note>> = repo.observeNotes(userId)
        .stateIn(coroutineScope, SharingStarted.Eagerly, emptyList())

    /** True when pulling or syncing Firestore data for notes. */
    override val isSyncing: StateFlow<Boolean> = combine(manualSyncing, repo.observeNotes(userId)) { syncing, list ->
        syncing || list.any { it.syncStatus == SyncStatus.SYNCING }
    }.stateIn(coroutineScope, SharingStarted.Eagerly, false)

    /** Triggers a cloud sync/pull for pending notes. */
    override fun syncPending() {
        coroutineScope.launch {
            manualSyncing.value = true
            try {
                repo.syncPending(userId)
            } finally {
                manualSyncing.value = false
            }
        }
    }

    /** Deletes the note with [id] via the repository. */
    override fun deleteNote(id: String) {
        coroutineScope.launch { repo.deleteNote(id, userId) }
    }

    /** Renames the note with [id] to [title] via the repository. */
    override fun renameNote(id: String, title: String) {
        coroutineScope.launch { repo.renameNote(id, userId, title) }
    }
}
