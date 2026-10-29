package group.two.knockledge.data

import kotlinx.coroutines.flow.StateFlow

/**
 * Authentication Contract exposed by Unit 2.
 * Supports Google Sign-In and Email/Password with mandatory login gate.
 */
interface AuthRepository {
    /**
     * Observes the currently authenticated user profile, or null if logged out.
     */
    val currentUser: StateFlow<UserProfile?>

    /**
     * Authenticates an existing user via Google Sign-In using an ID token.
     * @throws AuthCollisionException if the email already belongs to an email/password account.
     */
    suspend fun signInWithGoogle(idToken: String): Result<UserProfile>

    /**
     * Authenticates an existing user using email and password.
     */
    suspend fun signInWithEmail(email: String, password: String): Result<UserProfile>

    /**
     * Registers a new account with display name, email, and password.
     */
    suspend fun registerWithEmail(displayName: String, email: String, password: String): Result<UserProfile>

    /**
     * Signs out the active user session.
     * Local cached data is retained partitioned by user ID in SQLite.
     */
    suspend fun signOut()
}

data class UserProfile(
    val uid: String,
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
    val provider: AuthProvider,
    val createdAtEpochMs: Long,
)

class AuthCollisionException(
    val email: String,
    val existingProvider: AuthProvider,
    message: String = "An account with email $email already exists using $existingProvider. Please sign in with that provider.",
) : Exception(message)
