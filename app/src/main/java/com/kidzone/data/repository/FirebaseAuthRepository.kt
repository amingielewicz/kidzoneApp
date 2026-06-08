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
import java.util.Locale
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

            // Blokada logowania bez potwierdzonego emaila.
            // Google Sign-In jest zwolniony (email zweryfikowany z natury).
            // W debug buildach pomijamy weryfikację (ułatwia testowanie).
            if (!com.kidzone.BuildConfig.DEBUG && !firebaseUser.isEmailVerified) {
                // Wyślij ponownie link weryfikacyjny (na wypadek gdyby stary wygasł)
                runCatching { firebaseUser.sendEmailVerification().await() }
                // Wyloguj – nie pozwól na dostęp do apki
                firebaseAuth.signOut()
                throw AuthException.EmailNotVerified
            }

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

        // Z perspektywy Firebase Auth user jest już utworzony i zalogowany -
        // dlatego dopiero tutaj możemy odpytać Firestore o unikalność loginu
        // (reguły wymagają isSignedIn). Jeśli login okaże się zajęty, robimy
        // rollback przez `firebaseUser.delete()`, żeby nie zostawiać konta
        // Auth-only wiszącego bez doca w `users`.
        try {
            val nameLowercase = name.toUserNameLowercase()
            if (isUsernameTaken(nameLowercase, excludeUid = firebaseUser.uid)) {
                runCatching { firebaseUser.delete().await() }
                throw AuthException.UsernameAlreadyTaken
            }

            // Ustaw display name na FirebaseUser, zeby byl dostepny od razu w UI.
            firebaseUser.updateProfile(
                userProfileChangeRequest { displayName = name }
            ).await()

            // Zapisz pelen profil do kolekcji `users` (zrodlo prawdy o statystykach itp.).
            val userDto = UserDto(
                id = firebaseUser.uid,
                name = name,
                nameLowercase = nameLowercase,
                email = email,
                avatarUrl = firebaseUser.photoUrl?.toString(),
                createdAtMillis = System.currentTimeMillis(),
                badgeEarnedAt = emptyMap()
            )
            firestore.collection(FirestoreCollections.USERS)
                .document(firebaseUser.uid)
                .set(userDto)
                .await()

            // Wyślij email weryfikacyjny – link do potwierdzenia konta.
            // Nie blokujemy rejestracji jeśli się nie uda (best-effort).
            runCatching { firebaseUser.sendEmailVerification().await() }

            userDto.toDomain()
        } catch (e: Throwable) {
            // Awaria po createUser - sprzątamy konto Auth, by user mógł
            // spróbować ponownie z innymi danymi bez "duchów" w Auth.
            // `runCatching` żeby błąd cleanupu nie zasłonił oryginalnego.
            if (e !is AuthException.UsernameAlreadyTaken) {
                runCatching { firebaseUser.delete().await() }
            }
            throw e
        }
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

    override suspend fun resendVerificationEmail(email: String, password: String): OpResult<Unit> = runFirebase {
        // Logujemy tymczasowo żeby mieć dostęp do FirebaseUser (sendEmailVerification wymaga zalogowania)
        val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
        val user = result.user
            ?: throw IllegalStateException("Nie udało się zalogować w celu wysłania weryfikacji")
        user.sendEmailVerification().await()
        // Wyloguj z powrotem – user nie powinien mieć sesji bez weryfikacji
        firebaseAuth.signOut()
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

        // Sprawdź unikalność loginu (case-insensitive). Jeśli user zostawił
        // ten sam display name co poprzednio, query znajdzie tylko jego
        // własny dokument - excludeUid go odfiltrowuje.
        //
        // Świadomie throw zamiast `return OpResult.failure(...)`: cała funkcja
        // ma expression body (`= try { ... } catch ...`), w którym `return`
        // jest niedozwolony. AuthException łapie się niżej w `catch (e: AuthException)`
        // i mapuje na OpResult.failure(e) bez tracenia typu błędu.
        val nameLowercase = displayName.toUserNameLowercase()
        if (isUsernameTaken(nameLowercase, excludeUid = firebaseUser.uid)) {
            throw AuthException.UsernameAlreadyTaken
        }

        // 1) Zapis do Firestore – merge, żeby nie nadpisać `placesAddedCount`,
        //    `reviewsCount`, `createdAtMillis` ani `email` (które tu nie są
        //    edytowane przez usera). Mapa zamiast pełnego DTO, bo merge na
        //    DTO też by działał, ale wprost mapa lepiej dokumentuje, co
        //    faktycznie zmieniamy.
        val updates = mapOf(
            "name" to displayName,
            "nameLowercase" to nameLowercase,
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
                avatarUrl = avatarUrl,
                nameLowercase = nameLowercase
            )
        )
    } catch (e: AuthException) {
        OpResult.failure(e)
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

    override suspend fun deleteAccountWithGoogle(idToken: String): OpResult<Unit> = try {
        val user = firebaseAuth.currentUser
            ?: throw IllegalStateException("Brak zalogowanego użytkownika")

        // Reauth przez Google credential
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        user.reauthenticate(credential).await()

        val uid = user.uid

        // Kaskada: opinie → miejsca → doc usera → avatar → Auth (identycznie
        // jak w deleteAccount dla email/password).
        val reviewsSnap = firestore.collection(FirestoreCollections.REVIEWS)
            .whereEqualTo("userId", uid)
            .get()
            .await()
        reviewsSnap.documents.forEach { it.reference.delete().await() }

        val placesSnap = firestore.collection(FirestoreCollections.PLACES)
            .whereEqualTo("ownerUserId", uid)
            .get()
            .await()
        placesSnap.documents.forEach { it.reference.delete().await() }

        firestore.collection(FirestoreCollections.USERS)
            .document(uid)
            .delete()
            .await()

        runCatching {
            firebaseStorage.reference
                .child("avatars/$uid/avatar.jpg")
                .delete()
                .await()
        }

        user.delete().await()

        OpResult.success(Unit)
    } catch (e: FirebaseAuthInvalidCredentialsException) {
        OpResult.failure(AuthException.InvalidCredentials)
    } catch (e: Exception) {
        OpResult.failure(e)
    }

    // --- helpers ---

    override suspend fun recordBadgesEarned(
        badgeNames: List<String>
    ): OpResult<Unit> = try {
        if (badgeNames.isEmpty()) {
            OpResult.success(Unit)
        } else {
            val firebaseUser = firebaseAuth.currentUser
                ?: throw IllegalStateException("Brak zalogowanego użytkownika")

            // First-write-wins: czytamy istniejące timestampy i zapisujemy
            // pole `badgeEarnedAt.NAME` TYLKO dla odznak, których nie ma
            // jeszcze w mapie. Bez tego dwóch klientów (np. dwa urządzenia
            // tego samego usera) nadpisałoby chronologię, gdyby uruchomili
            // detekcję w różnych momentach.
            //
            // Uwaga: dot-notation w SetOptions.merge() pozwala aktualizować
            // pojedyncze klucze mapy bez nadpisania całej mapy. Klucz
            // `badgeEarnedAt.FIRST_PLACE` to standardowy zapis Firestore
            // dla "podpole o tej nazwie".
            val docRef = firestore.collection(FirestoreCollections.USERS)
                .document(firebaseUser.uid)
            val snap = docRef.get().await()
            @Suppress("UNCHECKED_CAST")
            val existing = (snap.get("badgeEarnedAt") as? Map<String, Long>).orEmpty()

            val now = System.currentTimeMillis()
            val updates = badgeNames
                .filter { it !in existing }
                .associate { name -> "badgeEarnedAt.$name" to now }

            if (updates.isEmpty()) {
                OpResult.success(Unit)
            } else {
                docRef.set(updates, SetOptions.merge()).await()
                OpResult.success(Unit)
            }
        }
    } catch (e: Exception) {
        OpResult.failure(e)
    }

    /**
     * Lowercase nazwy użytkownika dla case-insensitive zapytań w Firestore.
     *
     * Trim + locale `pl_PL`, żeby polskie znaki (Ą/Ć/Ę/Ł/Ń/Ó/Ś/Ź/Ż) zostały
     * znormalizowane spójnie z [com.kidzone.utils.TextNormalization]. Dla
     * pustego inputa zwraca pusty string - nie zapisujemy w bazie "ducha"
     * (puste `nameLowercase` w Firestore nie matchuje żadnemu zapytaniu
     * `whereEqualTo("nameLowercase", X)`).
     */
    private fun String.toUserNameLowercase(): String =
        trim().lowercase(Locale("pl", "PL"))

    /**
     * True gdy istnieje inny użytkownik z taką samą lowercase nazwą.
     *
     * Wymaga, by aktualnie wołający był zalogowany - reguły Firestore
     * dla `users` mają `read: if isSignedIn()`. W praktyce wołamy z
     * - register (po `createUserWithEmailAndPassword` user już jest signed-in),
     * - updateUserProfile (zalogowany z definicji).
     *
     * @param excludeUid pominąć dokument o tym uid - używane przy edycji
     *   profilu, żeby user mógł zachować tę samą nazwę.
     *
     * Strategia dwustopniowa:
     *  1. **Fast path** - `whereEqualTo("nameLowercase", X)`. Działa dla
     *     wszystkich userów zarejestrowanych po wprowadzeniu pola
     *     [UserDto.nameLowercase] oraz dla legacy userów po backfillu
     *     w `ensureUserDoc` (przy ich pierwszym kolejnym sign-inie).
     *  2. **Legacy fallback** - skan do [LEGACY_SCAN_LIMIT] doców bez
     *     wypełnionego `nameLowercase` i porównanie ich `name`
     *     case-insensitive po stronie klienta. Bez tego user "Adam"
     *     z legacy bazy mógłby zostać "zduplikowany" przez nową
     *     rejestrację jako "adam" / "ADAM" - dokładnie ten bug, który
     *     zgłaszamy w Issue #username-uniqueness.
     *
     *     Skan jest cap-owany, bo przy dużej bazie userów nie chcemy
     *     ciągnąć wszystkich na klient - legacy doców z czasem ubywa
     *     (każdy login to backfill), więc fallback w naturalny sposób
     *     staje się "tańszy".
     *
     * Best-effort: błąd Firestore (np. brak sieci) traktujemy jako
     * "nie wiemy, więc puszczamy" zamiast blokować całą rejestrację -
     * alternatywnie każdy timeout sieci skutkowałby "nazwa zajęta",
     * co mylące. Race condition (dwóch userów rejestrujących to samo
     * imię równocześnie) świadomie ignorujemy dla MVP - przy małej
     * skali ryzyko znikome, a alternatywą jest osobna kolekcja
     * `usernames/{lowercaseName}` z transakcyjnym zapisem.
     */
    private suspend fun isUsernameTaken(
        nameLowercase: String,
        excludeUid: String?
    ): Boolean {
        if (nameLowercase.isBlank()) return false
        return try {
            // 1) Fast path - po backfillu wszyscy userzy mają to pole.
            //    Świadomie nie używamy `return@try` (Kotlin nie zna labela
            //    `try`) - zamiast tego if/else z fallbackiem w gałęzi else.
            val byField = firestore.collection(FirestoreCollections.USERS)
                .whereEqualTo("nameLowercase", nameLowercase)
                .limit(2)
                .get()
                .await()
            val fastPathHit = byField.documents.any { it.id != excludeUid }

            if (fastPathHit) {
                true
            } else {
                // 2) Legacy fallback - dokumenty bez wypełnionego nameLowercase
                //    (zarejestrowane przed wprowadzeniem tego pola). Bierzemy
                //    z cap-em, porównujemy lowercase'y `name` po stronie klienta.
                //    Jeśli match, zwracamy true.
                val legacyScan = firestore.collection(FirestoreCollections.USERS)
                    .limit(LEGACY_SCAN_LIMIT.toLong())
                    .get()
                    .await()
                legacyScan.documents.any { doc ->
                    if (doc.id == excludeUid) return@any false
                    val storedNameLc = doc.getString("nameLowercase").orEmpty()
                    if (storedNameLc.isNotBlank()) {
                        // Już objęty fast path-em (1) - tu liczy się tylko legacy.
                        return@any false
                    }
                    val storedName = doc.getString("name").orEmpty()
                    storedName.toUserNameLowercase() == nameLowercase
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    private companion object {
        /**
         * Maks. liczba dokumentów `users` skanowanych przez legacy fallback
         * w [isUsernameTaken]. Gdy baza userów dramatycznie urośnie a
         * legacy doców nadal będzie wiele, lepiej jednorazowo zmigrować
         * (skrypt admin SDK setujący `nameLowercase` dla wszystkich) -
         * wtedy ten cap można obniżyć / usunąć.
         */
        const val LEGACY_SCAN_LIMIT = 500
    }

    /**
     * Upewnia się, że istnieje dokument w `users/{uid}` dla zalogowanego usera.
     *
     * Jeśli doc już istnieje – sprawdzamy też, czy ma uzupełnione pole
     * [UserDto.nameLowercase] i ewentualnie je dorzucamy (backfill dla
     * legacy doców sprzed wprowadzenia tego pola). Inne pola zostają
     * nietknięte, żeby nie nadpisać zmienionego przez usera display name.
     *
     * Jeśli go brak – tworzy go z danych z [FirebaseUser]. Liczniki
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
                val displayName = firebaseUser.displayName.orEmpty()
                val userDto = UserDto(
                    id = firebaseUser.uid,
                    name = displayName,
                    nameLowercase = displayName.toUserNameLowercase(),
                    email = firebaseUser.email.orEmpty(),
                    avatarUrl = firebaseUser.photoUrl?.toString(),
                    createdAtMillis = System.currentTimeMillis()
                )
                docRef.set(userDto).await()
            } else {
                // Backfill `nameLowercase` jeśli stary doc go nie ma (a jest
                // niepuste `name`). Idempotentne - jak już jest wypełnione,
                // nie generuje write'a.
                val existingNameLc = snap.getString("nameLowercase").orEmpty()
                val existingName = snap.getString("name").orEmpty()
                if (existingNameLc.isBlank() && existingName.isNotBlank()) {
                    docRef.set(
                        mapOf("nameLowercase" to existingName.toUserNameLowercase()),
                        SetOptions.merge()
                    ).await()
                }
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
    } catch (e: AuthException) {
        // Domain error rzucony przez nas (np. UsernameAlreadyTaken z
        // registerWithEmail). Przepuszczamy bez wrapowania w Network,
        // żeby UI dostał typowany błąd.
        OpResult.failure(e)
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
