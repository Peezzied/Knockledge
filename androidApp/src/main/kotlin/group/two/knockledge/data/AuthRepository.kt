package group.two.knockledge.data

import group.two.knockledge.data.model.AuthProvider
import group.two.knockledge.data.model.UserProfile
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val currentUser: StateFlow<UserProfile?>

    suspend fun signInWithGoogle(idToken: String): Result<UserProfile>

    suspend fun signInWithEmail(email: String, password: String): Result<UserProfile>

    suspend fun registerWithEmail(displayName: String, email: String, password: String): Result<UserProfile>

    suspend fun signOut()
}

class AuthCollisionException(
    val email: String,
    val existingProvider: AuthProvider,
    message: String = "An account with email $email already exists using $existingProvider. Please sign in with that provider.",
) : Exception(message)
