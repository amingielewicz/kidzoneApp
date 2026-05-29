package com.kidzone.data.repository

import android.net.Uri
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.storage.FirebaseStorage
import com.kidzone.data.remote.FirestoreCollections
import com.kidzone.data.remote.dto.UserDto
import com.kidzone.domain.model.User
import com.kidzone.domain.repository.AuthRepository
import com.kidzone.domain.repository.SignInProvider
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
    private val firestore: FirebaseFirestore,
    private val firebaseStorage: FirebaseStorage
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

    override fun observeUser(userId: String): Flow<User?> = callbackFlow {
        if (userId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val docRef = firestore.collection(FirestoreCollections.USERS).document(userId)
        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val user = snapshot?.toObject<UserDto>()?.toDomain()
            trySend(user)
        }
        awaitClose { registration.remove() }
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

    override suspend fun updateUserProfile(
        displayName: String,
        firstName: String,
        lastName: String,
        avatarUrl: String?
    ): OpResult<User> = try {
        val firebaseUser = firebaseAuth.currentUser
            ?: throw IllegalStateException("Brak zalogowanego użytkownika")

        // 1) Zapis do Firestore – merge, żeby nie nadpisać `placesAddedCount`,
        //    `reviewsCount`, `createdAtMillis` ani `email` (które tu nie są
        //    edytowane przez usera). Mapa zamiast pełnego DTO, bo merge na
        //    DTO też by działał, ale wprost mapa lepiej dokumentuje, co
        //    faktycznie zmieniamy.
        val updates = mapOf(
            "name" to displayName,
            "firstName" to firstName,
            "lastName" to lastName,
            "avatarUrl" to avatarUrl
        )
        firestore.collection(FirestoreCollections.USERS)
            .document(firebaseUser.uid)
            .set(updates, SetOptions.merge())
            .await()

        // 2) Aktualizacja FirebaseAuth – żeby strumień [currentUser] (oparty
        //    o FirebaseUser) zobaczył nowy nick i avatar od razu, zanim
        //    obserwator Firestore wyemituje pełny dokument.
        //    Uwaga: setPhotoUri(null) NIE czyści photoUrl – Firebase Auth
        //    interpretuje null jako "nie zmieniaj". Żeby usunąć avatar
        //    musielibyśmy użyć `userProfileChangeRequest.setPhotoUri(Uri.EMPTY)`,
        //    co dla naszego MVP nie jest potrzebne (brak guzika "Usuń avatar").
        val request = userProfileChangeRequest {
            this.displayName = displayName
            avatarUrl?.let { this.photoUri = Uri.parse(it) }
        }
        firebaseUser.updateProfile(request).await()

        // 3) Zwracamy "świeżego" usera – ProfileViewModel używa głównie
        //    observeUser, ale ten return type jest przydatny w testach
        //    i ewentualnych one-shot wywołaniach.
        OpResult.success(
            firebaseUser.toDomain().copy(
                name = displayName,
                firstName = firstName,
                lastName = lastName,
                avatarUrl = avatarUrl
            )
        )
    } catch (e: Exception) {
        OpResult.failure(e)
    }

    override suspend fun uploadAvatar(localUri: Uri): OpResult<String> = try {
        val firebaseUser = firebaseAuth.currentUser
            ?: throw IllegalStateException("Brak zalogowanego użytkownika")

        // Stała ścieżka – nadpisywanie istniejącego avatara zamiast tworzenia
        // nowego pliku przy każdym uploadzie. Plus: nie generujemy "śmieci"
        // w bucketcie ani nie musimy ich kasować po edycji.
        // Minus: stary URL z download tokenem przestaje działać dla
        // userów, którzy mieli go zacache'owanego (Coil to zauważa, bo URL
        // ma świeży `?alt=media&token=...`).
        val storageRef = firebaseStorage.reference
            .child("avatars/${firebaseUser.uid}/avatar.jpg")

        storageRef.putFile(localUri).await()
        val downloadUrl = storageRef.downloadUrl.await().toString()
        OpResult.success(downloadUrl)
    } catch (e: Exception) {
        OpResult.failure(e)
    }

    // ============================================================
    // === Account management
    // ============================================================

    override suspend fun getCurrentSignInProvider(): SignInProvider {
        val user = firebaseAuth.currentUser ?: return SignInProvider.UNKNOWN
        // FirebaseUser.providerData zawiera firebase (firebaseProviderId="firebase")
        // PLUS faktyczny provider (password, google.com, ...). Sprawdzamy oba
        // możliwe znaczniki – jak user się logował obu sposobami (linkowane konto),
        // priorytetyzujemy email/password, bo wtedy zmiana hasła ma sens.
        val providerIds = user.providerData.map { it.providerId }
        return when {
            EmailAuthProvider.PROVIDER_ID in providerIds -> SignInProvider.EMAIL_PASSWORD
            GoogleAuthProvider.PROVIDER_ID in providerIds -> SignInProvider.GOOGLE
            else -> SignInProvider.UNKNOWN
        }
    }

    override suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ): OpResult<Unit> = try {
        val user = firebaseAuth.currentUser
            ?: throw IllegalStateException("Brak zalogowanego użytkownika")
        val email = user.email
            ?: throw IllegalStateException("Konto bez e-maila – nie można zmienić hasła")

        // Reauth – Firebase wymaga "fresh" credentialu do zmiany hasła.
        // EmailAuthProvider.getCredential(email, password) działa tylko dla
        // kont z password providerem. Dla Google by się sypnęło na samym
        // reauthenticate – zostawiamy ten naturalny błąd zamiast
        // pre-emptywnej walidacji, żeby nie duplikować logiki z
        // [getCurrentSignInProvider] (UI i tak ukrywa akcję).
        val credential = EmailAuthProvider.getCredential(email, currentPassword)
        user.reauthenticate(credential).await()

        user.updatePassword(newPassword).await()
        OpResult.success(Unit)
    } catch (e: FirebaseAuthInvalidCredentialsException) {
        // Niepoprawne aktualne hasło (reauth padł).
        OpResult.failure(AuthException.InvalidCredentials)
    } catch (e: FirebaseAuthWeakPasswordException) {
        OpResult.failure(AuthException.WeakPassword)
    } catch (e: Exception) {
        OpResult.failure(e)
    }

    override suspend fun changeEmail(
        currentPassword: String,
        newEmail: String
    ): OpResult<Unit> = try {
        val user = firebaseAuth.currentUser
            ?: throw IllegalStateException("Brak zalogowanego użytkownika")
        val email = user.email
            ?: throw IllegalStateException("Konto bez e-maila – nie można zmienić e-maila")

        val credential = EmailAuthProvider.getCredential(email, currentPassword)
        user.reauthenticate(credential).await()

        // verifyBeforeUpdateEmail (zamiast deprecated updateEmail):
        //  - wysyła link weryfikacyjny na NOWY adres,
        //  - zmiana w Auth zachodzi dopiero po kliknięciu linku przez usera,
        //  - działa nawet z włączoną "Email enumeration protection".
        // UI musi wprost zakomunikować, że jeszcze NIE jest zmienione.
        user.verifyBeforeUpdateEmail(newEmail).await()
        OpResult.success(Unit)
    } catch (e: FirebaseAuthInvalidCredentialsException) {
        // Może być: zły aktualny password (reauth) albo niepoprawny format newEmail.
        // Firebase nie rozróżnia w typie – sprawdzamy message, jak w runFirebase.
        val msg = e.message.orEmpty().lowercase()
        if (msg.contains("email")) {
            OpResult.failure(AuthException.InvalidEmail)
        } else {
            OpResult.failure(AuthException.InvalidCredentials)
        }
    } catch (e: FirebaseAuthUserCollisionException) {
        OpResult.failure(AuthException.EmailAlreadyInUse)
    } catch (e: Exception) {
        OpResult.failure(e)
    }

    override suspend fun deleteAccount(currentPassword: String): OpResult<Unit> = try {
        val user = firebaseAuth.currentUser
            ?: throw IllegalStateException("Brak zalogowanego użytkownika")

        // MVP: tylko email/password. Dla Google reauth musiałby przejść
        // przez UI launcher – wymaga większej zmiany VM/UI niż mamy czas
        // dziś, dorobimy w następnym PR.
        val email = user.email
        val isPasswordUser = user.providerData.any { it.providerId == EmailAuthProvider.PROVIDER_ID }
        if (email == null || !isPasswordUser) {
            throw IllegalStateException(
                "Usuwanie konta jest dostępne tylko dla logowania e-mail/hasłem. " +
                    "Dla logowania przez Google – usuń konto z poziomu konta Google " +
                    "lub napisz do nas na e-mail z prośbą o usunięcie."
            )
        }

        val credential = EmailAuthProvider.getCredential(email, currentPassword)
        user.reauthenticate(credential).await()

        val uid = user.uid

        // 2) Usuń wszystkie opinie usera.
        // Reguły Firestore allow delete: userId == auth.uid – pasuje 1:1.
        val reviewsSnap = firestore.collection(FirestoreCollections.REVIEWS)
            .whereEqualTo("userId", uid)
            .get()
            .await()
        reviewsSnap.documents.forEach { it.reference.delete().await() }

        // 3) Usuń wszystkie miejsca usera. Świadomie nie kasujemy cudzych
        //    opinii na tych miejscach – reguły by tego nie pozwoliły, a
        //    Cloud Function admin SDK wymaga Blaze. Zostają jako orphans
        //    (placeId wskazujący na nieistniejący doc); UI listy opinii
        //    użytkowników już dziś tego nie pokazuje, bo z poziomu
        //    PlaceDetailsScreen nie wejdzie się na nieistniejące miejsce.
        val placesSnap = firestore.collection(FirestoreCollections.PLACES)
            .whereEqualTo("ownerUserId", uid)
            .get()
            .await()
        placesSnap.documents.forEach { it.reference.delete().await() }

        // 4) Doc /users/{uid}. Wymaga, żeby firestore.rules pozwalały
        //    na `delete: if request.auth.uid == userId` – patrz fix w
        //    firestore.rules (PR #profile-account-management).
        firestore.collection(FirestoreCollections.USERS)
            .document(uid)
            .delete()
            .await()

        // 5) Avatar w Storage – best effort. Brak pliku == sukces (404 i
        //    tak wolimy zignorować). Inne błędy też tłumimy: priorytetem
        //    jest dotrzeć do kroku 6.
        runCatching {
            firebaseStorage.reference
                .child("avatars/$uid/avatar.jpg")
                .delete()
                .await()
        }

        // 6) Konto Auth – ostatnie, bo po nim user nie ma uprawnień do
        //    żadnego z poprzednich kroków. Po tym strumień [currentUser]
        //    wyemituje null i NavGraph przejdzie na Login.
        user.delete().await()

        OpResult.success(Unit)
    } catch (e: FirebaseAuthInvalidCredentialsException) {
        OpResult.failure(AuthException.InvalidCredentials)
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
