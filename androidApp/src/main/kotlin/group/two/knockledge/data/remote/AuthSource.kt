package group.two.knockledge.data.remote

import group.two.knockledge.data.model.UserProfile

interface AuthSource {
    fun restoreSession(): UserProfile?

    suspend fun signInWithGoogle(idToken: String): UserProfile

    suspend fun signInWithEmail(email: String, password: String): UserProfile

    suspend fun register(displayName: String, email: String, password: String): UserProfile

    fun signOut()
}
