package group.two.knockledge.data.repository

import group.two.knockledge.data.AuthRepository
import group.two.knockledge.data.model.UserProfile
import group.two.knockledge.data.remote.AuthSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthRepositoryImpl(private val source: AuthSource) : AuthRepository {
    private val user = MutableStateFlow(source.restoreSession())

    override val currentUser: StateFlow<UserProfile?> = user.asStateFlow()

    override suspend fun signInWithGoogle(idToken: String): Result<UserProfile> = attempt {
        require(idToken.isNotBlank()) { "Missing Google ID token" }
        source.signInWithGoogle(idToken)
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<UserProfile> = attempt {
        requireEmail(email)
        require(password.isNotEmpty()) { "Password is required" }
        source.signInWithEmail(email.trim(), password)
    }

    override suspend fun registerWithEmail(displayName: String, email: String, password: String): Result<UserProfile> =
        attempt {
            require(displayName.trim().length in 1..50) { "Name must be 1 to 50 characters" }
            requireEmail(email)
            require(password.length >= 6) { "Password must be at least 6 characters" }
            source.register(displayName.trim(), email.trim(), password)
        }

    override suspend fun signOut() {
        source.signOut()
        user.value = null
    }

    private fun requireEmail(email: String) =
        require(EMAIL.matches(email.trim())) { "Enter a valid email address" }

    private suspend fun attempt(block: suspend () -> UserProfile): Result<UserProfile> =
        try {
            Result.success(block().also { user.value = it })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    private companion object {
        val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}
