package com.kidzone.data.repository

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
import com.kidzone.data.remote.FirestoreCollections
import com.kidzone.data.remote.dto.UserDto
import com.kidzone.domain.model.User
import com.kidzone.domain.repository.AuthRepository
import com.kidzone.utils.AuthException
import com.kidzone.utils.OpResult
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
            // Self-heal: jeśli ten user nie ma jeszcze doca w `users` (np. konto
            // utworzone zanim ten kod istniał, albo rejestracja zakończyła się
            // częściowym błędem), dotworzymy go teraz na podstawie FirebaseUser.
            ensureUserDoc(firebaseUser)
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

        // Self-heal: niezależnie czy to nowy user (isNewUser==true) czy istniejący,
        // upewnij się, że jest dla niego doc w `users`. Ta gałąź zastępuje wcześniejszą
        // logikę "twórz tylko gdy isNewUser" – była zawodna dla legacy userów, którzy
        // logowali się Google'em zanim tworzyliśmy doc.
        ensureUserDoc(firebaseUser)

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

    override suspend fun getTopUsers(limit: Int): OpResult<List<User>> = try {
        require(limit > 0) { "limit musi być > 0" }
        // Sortowanie po `placesAddedCount` desc – „kto dodał najwięcej miejsc”.
        // Drugorzędny sort po `reviewsCount` w kliencie poniżej (Firestore
        // wymagałby kompozytowego indeksu).
        val snapshot = firestore.collection(FirestoreCollections.USERS)
            .orderBy("placesAddedCount", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()
        val users = snapshot.documents
            .mapNotNull { it.toObject<UserDto>()?.toDomain() }
            .sortedWith(
                compareByDescending<User> { it.placesAddedCount }
                    .thenByDescending { it.reviewsCount }
            )
        OpResult.success(users)
    } catch (e: Exception) {
        OpResult.failure(e)
    }

    // --- helpers ---

    /**
     * Upewnia się, że istnieje dokument w `users/{uid}` dla zalogowanego usera.
     *
     * Jeśli doc już istnieje – nic nie robi (żadnego zapisu, żeby nie nadpisać
     * np. zmienionego przez usera display name w Firestore).
     * Jeśli go brak – tworzy go z danych z [FirebaseUser]. Kontuery
     * (`placesAddedCount`, `reviewsCount`) zostawiamy domyślne (0) z [UserDto].
     *
     * Ta metoda jest najtańszą formą self-healingu po stronie klienta:
     * jeden read + warunkowy write. Wywoływana po pomyślnym `signInWith…`.
     * Błędy zapisu są logowane, ale nie blokują logowania.
     */
    private suspend fun ensureUserDoc(firebaseUser: FirebaseUser) {
        val docRef = firestore.collection(FirestoreCollections.USERS)
            .document(firebaseUser.uid)
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val userDto = UserDto(
                    id = firebaseUser.uid,
                    name = firebaseUser.displayName.orEmpty(),
                    email = firebaseUser.email.orEmpty(),
                    avatarUrl = firebaseUser.photoUrl?.toString(),
                    createdAtMillis = System.currentTimeMillis()
                )
                docRef.set(userDto).await()
            }
        } catch (_: Exception) {
            // Świadomie tłumimy: brak doca w users to *nie* powód, by uniemożliwić
            // logowanie. Liczniki w rankingu zadziałają i tak (set+merge w
            // FirestorePlaceRepository / FirestoreReviewRepository tworzy
            // minimalny doc), a przy najbliższej okazji zalogowania spróbujemy
            // uzupełnić ponownie.
        }
    }

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
