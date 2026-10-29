package group.two.knockledge.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.google.android.gms.tasks.Task
import group.two.knockledge.data.AuthCollisionException
import group.two.knockledge.data.model.AuthProvider
import group.two.knockledge.data.model.UserProfile
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class FirebaseAuthSource(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) : AuthSource {
    override fun restoreSession(): UserProfile? = auth.currentUser?.toProfile()

    override suspend fun signInWithGoogle(idToken: String): UserProfile {
        val user = auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await().user!!
        if (user.providerData.any { it.providerId == "password" }) {
            auth.signOut()
            throw AuthCollisionException(user.email.orEmpty(), AuthProvider.EMAIL_PASSWORD)
        }
        return user.toProfile()
    }

    override suspend fun signInWithEmail(email: String, password: String): UserProfile =
        auth.signInWithEmailAndPassword(email, password).await().user!!.toProfile()

    override suspend fun register(displayName: String, email: String, password: String): UserProfile {
        val user = try {
            auth.createUserWithEmailAndPassword(email, password).await().user!!
        } catch (e: FirebaseAuthUserCollisionException) {
            throw AuthCollisionException(email, AuthProvider.GOOGLE)
        }
        user.updateProfile(userProfileChangeRequest { this.displayName = displayName }).await()
        return user.toProfile()
    }

    override fun signOut() = auth.signOut()

    private fun FirebaseUser.toProfile() = UserProfile(
        uid = uid,
        email = email.orEmpty(),
        displayName = displayName.orEmpty(),
        photoUrl = photoUrl?.toString(),
        provider = if (providerData.any { it.providerId == "google.com" }) AuthProvider.GOOGLE else AuthProvider.EMAIL_PASSWORD,
        createdAtEpochMs = metadata?.creationTimestamp ?: 1L,
    )
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { c ->
    addOnCompleteListener {
        if (it.isSuccessful) c.resume(it.result) else c.resumeWithException(it.exception ?: IllegalStateException("Task failed"))
    }
}
