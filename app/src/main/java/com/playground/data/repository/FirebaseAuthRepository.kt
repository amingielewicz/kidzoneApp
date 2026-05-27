package com.playground.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.playground.data.remote.FirestoreCollections
import com.playground.data.remote.dto.UserDto
import com.playground.domain.model.User
import com.playground.domain.repository.AuthRepository
import com.playground.utils.AuthException
import com.playground.utils.OpResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementacja [AuthRepository] oparta o Firebase Authentication + Firestore.
 *
 *  - logowanie i rejestracja e-mail/haslo,
 *  - logowanie Google przez Google Sign-In (token przekazywany z UI),
 *  - reset hasla e-mailem,
 *  - obserwacja aktualnie zalogowanego uzytkownika,
 *  - odczyt publicznych danych innych uzytkownikow (autor miejsca itp.).
 *
 * Po pomyslnej rejestracji tworzymy dokument w kolekcji `users`
 * (zob. [FirestoreCollections.USERS]), zeby reszta aplikacji mogla go
 * bogato odczytywac (avatar, statystyki) bez polegania wylacznie na
 * FirebaseUser.
 */
@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    override val currentUser: Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.toDomain())
        }
        firebaseAuth.addAuthStateListener(listener)
        // Wyemituj aktualna wartosc natychmiast (listener emituje dopiero przy zmianach).
        trySend(firebaseAuth.currentUser?.toDomain())
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override suspend fun signInWithEmail(email: String, password: String): OpResult<User> =
        runFirebase {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
                ?: throw IllegalStateException("Logowanie się powiodło, ale Firebase nie zwrócił użytkownika")
            firebaseUser.toDomain()
        }

    override suspend fun registerWithEmail(
        name: String,
        email: String,
        password: String
    ): OpResult<User> = runFirebase {
        val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        val firebaseUser = result.user
            ?: throw IllegalStateException("Rejestracja się powiodła, ale Firebase nie zwrócił użytkownika")

        // Ustaw display name na FirebaseUser, zeby byl dostepny od razu w UI.
        firebaseUser.updateProfile(
            userProfileChangeRequest { displayName = name }
        ).await()

        // Zapisz pelen profil do kolekcji `users` (zrodlo prawdy o statystykach itp.).
        val userDto = UserDto(
            id = firebaseUser.uid,
            name = name,
            email = email,
            avatarUrl = firebaseUser.photoUrl?.toString(),
            createdAtMillis = System.currentTimeMillis()
        )
        firestore.collection(FirestoreCollections.USERS)
            .document(firebaseUser.uid)
            .set(userDto)
            .await()

        userDto.toDomain()
    }

    override suspend fun signInWithGoogle(idToken: String): OpResult<User> = runFirebase {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = firebaseAuth.signInWithCredential(credential).await()
        val firebaseUser = result.user
            ?: throw IllegalStateException("Logowanie Google się powiodło, ale Firebase nie zwrócił użytkownika")

        // Jesli to pierwsze logowanie tego uzytkownika - stworz mu dokument w `users`.
        val isNewUser = result.additionalUserInfo?.isNewUser == true
        if (isNewUser) {
            val userDto = UserDto(
                id = firebaseUser.uid,
                name = firebaseUser.displayName.orEmpty(),
                email = firebaseUser.email.orEmpty(),
                avatarUrl = firebaseUser.photoUrl?.toString(),
                createdAtMillis = System.currentTimeMillis()
            )
            firestore.collection(FirestoreCollections.USERS)
                .document(firebaseUser.uid)
                .set(userDto)
                .await()
        }

        firebaseUser.toDomain()
    }

    override suspend fun sendPasswordResetEmail(email: String): OpResult<Unit> = runFirebase {
        firebaseAuth.sendPasswordResetEmail(email).await()
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
    }

    override suspend fun getUserById(userId: String): OpResult<User> = try {
        require(userId.isNotBlank()) { "userId nie może być puste" }
        val snapshot = firestore.collection(FirestoreCollections.USERS)
            .document(userId)
            .get()
            .await()
        val dto = snapshot.toObject<UserDto>()
        if (dto != null) {
            OpResult.success(dto.toDomain())
        } else {
            OpResult.failure(NoSuchElementException("Brak użytkownika o id=$userId"))
        }
    } catch (e: Exception) {
        OpResult.failure(e)
    }

    // --- helpers ---

    private fun FirebaseUser.toDomain(): User = User(
        id = uid,
        name = displayName.orEmpty(),
        email = email.orEmpty(),
        avatarUrl = photoUrl?.toString()
    )

    /**
     * Uruchamia [block] wewnatrz try/catch i mapuje wyjatki Firebase na
     * dziedzinowe [AuthException]. Pozwala miec czyste `runFirebase { ... }`
     * w kazdej metodzie repo.
     */
    private inline fun <T> runFirebase(block: () -> T): OpResult<T> = try {
        OpResult.success(block())
    } catch (e: FirebaseAuthInvalidUserException) {
        OpResult.failure(AuthException.UserNotFound)
    } catch (e: FirebaseAuthInvalidCredentialsException) {
        // Firebase rzuca to dla zlego hasla ORAZ dla niepoprawnego formatu e-maila.
        val message = e.message.orEmpty().lowercase()
        if (message.contains("email")) {
            OpResult.failure(AuthException.InvalidEmail)
        } else {
            OpResult.failure(AuthException.InvalidCredentials)
        }
    } catch (e: FirebaseAuthUserCollisionException) {
        OpResult.failure(AuthException.EmailAlreadyInUse)
    } catch (e: FirebaseAuthWeakPasswordException) {
        OpResult.failure(AuthException.WeakPassword)
    } catch (e: Exception) {
        OpResult.failure(AuthException.Network(e))
    }
}
