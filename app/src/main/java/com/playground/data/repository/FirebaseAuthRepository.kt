package com.playground.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.playground.data.remote.FirestoreCollections
import com.playground.data.remote.dto.UserDto
import com.playground.domain.model.User
import com.playground.domain.repository.AuthRepository
import com.playground.utils.OpResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementacja [AuthRepository] oparta o Firebase Authentication + Firestore.
 *
 * Większość metod jest tu zostawiona jako TODO – będą uzupełniane w kolejnych
 * iteracjach (po skonfigurowaniu prawdziwego projektu Firebase).
 */
@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    override val currentUser: Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val firebaseUser = auth.currentUser
            trySend(
                firebaseUser?.let {
                    User(
                        id = it.uid,
                        name = it.displayName.orEmpty(),
                        email = it.email.orEmpty(),
                        avatarUrl = it.photoUrl?.toString()
                    )
                }
            )
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override suspend fun signInWithEmail(email: String, password: String): OpResult<User> {
        // TODO: firebaseAuth.signInWithEmailAndPassword(email, password).await(); fetch profile from Firestore
        return OpResult.failure(NotImplementedError("signInWithEmail – do uzupełnienia"))
    }

    override suspend fun registerWithEmail(
        name: String,
        email: String,
        password: String
    ): OpResult<User> {
        // TODO: createUserWithEmailAndPassword + zapis dokumentu w kolekcji users
        return OpResult.failure(NotImplementedError("registerWithEmail – do uzupełnienia"))
    }

    override suspend fun signInWithGoogle(idToken: String): OpResult<User> {
        // TODO: firebaseAuth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
        return OpResult.failure(NotImplementedError("signInWithGoogle – do uzupełnienia"))
    }

    override suspend fun sendPasswordResetEmail(email: String): OpResult<Unit> {
        // TODO: firebaseAuth.sendPasswordResetEmail(email).await()
        return OpResult.failure(NotImplementedError("sendPasswordResetEmail – do uzupełnienia"))
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
    }

    @Suppress("unused")
    private fun usersCollection() = firestore.collection(FirestoreCollections.USERS)

    @Suppress("unused")
    private fun UserDto.dummyReference(): UserDto = this // marker, by import nie został wycięty
}
