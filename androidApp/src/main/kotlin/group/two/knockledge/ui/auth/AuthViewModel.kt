package group.two.knockledge.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import group.two.knockledge.data.AuthCollisionException
import group.two.knockledge.data.AuthRepository
import group.two.knockledge.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthState(
    val user: UserProfile? = null,
    val busy: Boolean = false,
    val error: String? = null,
    val collisionNotice: String? = null,
)

class AuthViewModel(
    private val repo: AuthRepository,
    private val googleToken: suspend () -> String,
) : ViewModel() {
    private val _state = MutableStateFlow(AuthState(user = repo.currentUser.value))
    val state: StateFlow<AuthState> = _state.asStateFlow()

    init {
        repo.currentUser.onEach { u -> _state.update { it.copy(user = u) } }.launchIn(viewModelScope)
    }

    fun signInWithGoogle() = run { runCatching { googleToken() }.fold({ repo.signInWithGoogle(it) }, { Result.failure(it) }) }

    fun signInWithEmail(email: String, password: String) = run { repo.signInWithEmail(email, password) }

    fun register(name: String, email: String, password: String) = run { repo.registerWithEmail(name, email, password) }

    fun signOut() {
        viewModelScope.launch { repo.signOut() }
    }

    private fun run(block: suspend () -> Result<UserProfile>) {
        _state.update { it.copy(busy = true, error = null, collisionNotice = null) }
        viewModelScope.launch {
            val e = block().exceptionOrNull()
            _state.update {
                it.copy(
                    busy = false,
                    error = e?.takeIf { x -> x !is AuthCollisionException }?.let { x -> x.message ?: "Sign-in failed" },
                    collisionNotice = (e as? AuthCollisionException)?.message,
                )
            }
        }
    }
}
