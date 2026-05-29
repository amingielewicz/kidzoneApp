package com.kidzone.domain.repository

import android.net.Uri
import com.kidzone.domain.model.User
import com.kidzone.utils.OpResult
import kotlinx.coroutines.flow.Flow

/**
 * Operacje uwierzytelniania (e-mail/hasło + Google), obserwacja
 * aktualnie zalogowanego użytkownika oraz odczyt publicznych danych
 * innych użytkowników (np. autora miejsca).
 */
interface AuthRepository {

    /**
     * Strumień aktualnie zalogowanego użytkownika lub null gdy wylogowany.
     *
     * Uwaga: ten strumień wystawia "lekkiego" usera zbudowanego z [FirebaseUser]
     * (pola: id, name, email, avatarUrl). Pełnego dokumentu z Firestore – w tym
     * `firstName`, `lastName`, `placesAddedCount`, `reviewsCount` – dostarcza
     * [observeUser]. Większości ekranów wystarcza ten lekki stream
     * (potrzebują tylko zalogowanego uid + nick), profil korzysta z [observeUser].
     */
    val currentUser: Flow<User?>

    /**
     * Strumień **pełnego** dokumentu użytkownika z kolekcji `users` (snapshot
     * listener). Emituje:
     *  - aktualny stan po pierwszym fetchu,
     *  - kolejne emisje przy każdej zmianie dokumentu (np. po
     *    [updateUserProfile] albo gdy inkrementuje się licznik miejsc).
     *
     * Emituje `null` gdy [userId] jest pusty albo dokument nie istnieje.
     * Strumień zamyka się błędem przy błędach Firestore (np. brak uprawnień).
     */
    fun observeUser(userId: String): Flow<User?>

    suspend fun signInWithEmail(email: String, password: String): OpResult<User>

    suspend fun registerWithEmail(name: String, email: String, password: String): OpResult<User>

    /** [idToken] pochodzi z Google Sign-In na urządzeniu. */
    suspend fun signInWithGoogle(idToken: String): OpResult<User>

    suspend fun sendPasswordResetEmail(email: String): OpResult<Unit>

    suspend fun signOut()

    /**
     * Pobiera dokument użytkownika z kolekcji `users`.
     *
     * Używane np. na ekranie szczegółów miejsca, żeby pokazać
     * "Dodano przez: {nick}". Nie wymaga, by [userId] był aktualnie
     * zalogowanym użytkownikiem.
     */
    suspend fun getUserById(userId: String): OpResult<User>

    /**
     * Top użytkowników wg [User.placesAddedCount] (sort malejąco).
     *
     * Używane przez ekran Ranking. Drugorzędne sortowanie
     * (np. po `reviewsCount`) wykonuje strona klienta, bo composite index
     * wymagałby ręcznej konfiguracji w konsoli Firebase.
     */
    suspend fun getTopUsers(limit: Int = 10): OpResult<List<User>>

    /**
     * Aktualizuje edytowalne pola profilu zalogowanego użytkownika.
     *
     * Zapisuje:
     *  - `users/{uid}` w Firestore (merge: zmienia tylko podane pola, nie nadpisuje
     *    liczników i daty utworzenia),
     *  - `displayName` i `photoUrl` w Firebase Auth (żeby strumień [currentUser]
     *    od razu pokazał nowy nick / avatar bez czekania na refresh tokena).
     *
     * @param displayName nowy publiczny nick (login). Powinien być niepusty –
     *   jest używany w rankingu i przy autorze opinii / miejsca.
     * @param firstName imię (może być puste, jeśli user nie chce go podawać).
     * @param lastName nazwisko (może być puste).
     * @param avatarUrl URL avatara z Firebase Storage albo z Google Sign-In.
     *   Null = "wyczyść avatar".
     *
     * Zwraca zaktualizowanego [User] (po stronie aplikacji można na niego
     * reagować, mimo że [observeUser] i tak za chwilę wyemituje ten sam stan).
     * Jeśli user nie jest zalogowany, zwraca [OpResult.Failure] z
     * [IllegalStateException].
     */
    suspend fun updateUserProfile(
        displayName: String,
        firstName: String,
        lastName: String,
        avatarUrl: String?
    ): OpResult<User>

    /**
     * Wgrywa wybrany lokalnie obrazek do Firebase Storage pod
     * `avatars/{uid}/avatar.jpg` i zwraca publiczny [String] download URL.
     *
     * Wywołujący (zwykle ProfileViewModel) jest odpowiedzialny za
     * przekazanie tego URL-a do [updateUserProfile] – upload sam w sobie
     * **nie aktualizuje** dokumentu usera, żeby UI mogło pokazać preview
     * przed zapisem ("Zapisz" / "Anuluj" w sheecie edycji).
     *
     * Limity (egzekwowane też w `storage.rules`): tylko obrazki (MIME image/...),
     * maks. 5 MB. Większe pliki dostaną błąd z Firebase.
     */
    suspend fun uploadAvatar(localUri: Uri): OpResult<String>
}
