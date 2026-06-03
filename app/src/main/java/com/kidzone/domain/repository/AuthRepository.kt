package com.kidzone.domain.repository

import android.net.Uri
import com.kidzone.domain.model.User
import com.kidzone.utils.OpResult
import kotlinx.coroutines.flow.Flow

/**
 * Sposób, w jaki aktualnie zalogowany użytkownik się uwierzytelnił.
 *
 * Wpływa na to, jakie operacje zarządzania kontem są dla niego dostępne:
 *  - [EMAIL_PASSWORD] – pełen zakres (zmiana hasła, zmiana e-maila, usunięcie
 *    konta z hasłem jako reauth credential),
 *  - [GOOGLE] – zmiana hasła nie ma sensu (Google nim zarządza), zmiana
 *    e-maila wymaga zmiany konta Google. Usunięcie konta wymagałoby reauth
 *    przez ponowny Google Sign-In (nieobsługiwane w MVP),
 *  - [UNKNOWN] – fallback dla nieoczekiwanego providera lub gdy user jest
 *    wylogowany. UI powinno wtedy schować całą sekcję "Konto i bezpieczeństwo".
 */
enum class SignInProvider { EMAIL_PASSWORD, GOOGLE, UNKNOWN }

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

    /** Wysyła ponownie email weryfikacyjny do aktualnie zalogowanego usera. */
    suspend fun resendVerificationEmail(email: String, password: String): OpResult<Unit>

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

    // ============================================================
    // === Account management (zmiana hasła / e-maila / usunięcie) ===
    // ============================================================

    /**
     * Zwraca, w jaki sposób aktualnie zalogowany user był uwierzytelniony.
     *
     * UI używa wartości do pokazania / ukrycia akcji w sekcji
     * "Konto i bezpieczeństwo". Zwraca [SignInProvider.UNKNOWN] gdy nikt nie
     * jest zalogowany albo provider nie jest obsługiwany.
     *
     * Świadomie suspend, mimo że pod spodem to synchroniczny odczyt
     * [com.google.firebase.auth.FirebaseAuth.currentUser] – zostawiamy sobie
     * możliwość, by w przyszłości pójść po providerData asynchronicznie
     * (np. po refresh tokenu) bez breaking change.
     */
    suspend fun getCurrentSignInProvider(): SignInProvider

    /**
     * Zmienia hasło zalogowanego użytkownika.
     *
     * Wymaga uprzedniej re-authentication: Firebase odrzuca [FirebaseUser.updatePassword]
     * jeśli ostatni login był "stary" (zwykle > 5 min). Reauth jest wykonywany
     * wewnątrz tej metody – wywołujący nie musi się o to martwić.
     *
     * Działa tylko dla kont [SignInProvider.EMAIL_PASSWORD]. Dla Google
     * zwraca [OpResult.Failure] z [IllegalStateException].
     *
     * @param currentPassword aktualne hasło – służy zarówno jako reauth
     *   credential, jak i jako "ludzkie" potwierdzenie ("wiesz co robisz?").
     * @param newPassword nowe hasło zgodne z [com.kidzone.utils.PasswordPolicy]
     *   (min. 8 znaków, mała + duża litera, znak specjalny). Walidację po stronie
     *   klienta wykonują ChangePasswordDialog i RegisterViewModel - tutaj
     *   pozostaje fallback Firebase server-side ([com.google.firebase.auth.FirebaseAuthWeakPasswordException]).
     */
    suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ): OpResult<Unit>

    /**
     * Inicjuje zmianę adresu e-mail zalogowanego użytkownika.
     *
     * **Nowy e-mail nie zostanie aktywny od razu.** Firebase wysyła link
     * weryfikacyjny na `newEmail`; dopiero kliknięcie linku finalizuje zmianę.
     * UI powinno o tym poinformować ("Sprawdź skrzynkę: $newEmail").
     *
     * Pod spodem używamy [FirebaseUser.verifyBeforeUpdateEmail] zamiast
     * deprecated [FirebaseUser.updateEmail] – ta druga nie współpracuje
     * z włączoną w projekcie ochroną "Email enumeration protection" (włączoną
     * domyślnie w nowych projektach Firebase).
     *
     * Wymaga reauth – analogicznie jak [changePassword]. Działa tylko dla
     * kont [SignInProvider.EMAIL_PASSWORD].
     */
    suspend fun changeEmail(
        currentPassword: String,
        newEmail: String
    ): OpResult<Unit>

    /**
     * Trwałe usunięcie konta wraz z danymi użytkownika z aplikacji.
     *
     * Wykonuje (w tej kolejności):
     *  1. **Reauth** – wymagany przez Firebase do `firebaseUser.delete()`.
     *  2. Usunięcie wszystkich opinii usera (`reviews.userId == uid`).
     *  3. Usunięcie wszystkich miejsc usera (`places.ownerUserId == uid`).
     *     Świadomie **nie kasujemy** opinii innych userów na tych miejscach –
     *     reguły Firestore na to nie pozwalają (kasować można tylko swoje
     *     opinie). Te opinie zostają jako "orphans". Pełną kaskadę dałaby
     *     dopiero Cloud Function z Admin SDK (Blaze plan), do dorobienia
     *     w przyszłości.
     *  4. Usunięcie dokumentu `users/{uid}`.
     *  5. Usunięcie avatara w Storage (`avatars/{uid}/avatar.jpg`) – best
     *     effort, błąd nie zatrzymuje procesu.
     *  6. `firebaseUser.delete()` – ostatecznie odbiera użytkownikowi tożsamość.
     *
     * Po sukcesie strumień [currentUser] wyemituje `null`, więc UI naturalnie
     * wyląduje na ekranie logowania (NavGraph reaguje na auth state).
     *
     * Operacja **nie jest atomowa**. Jeśli przerwie się w połowie (np. brak
     * Internetu między krokami 3 a 4), część danych może już zniknąć, a
     * konto Auth dalej istnieje – kolejna próba usunięcia dokończy resztę.
     *
     * @param currentPassword aktualne hasło dla reauth (wymagane dla email/password
     *   user). Dla Google user MVP nie obsługuje – zwraca błąd.
     */
    suspend fun deleteAccount(currentPassword: String): OpResult<Unit>

    /**
     * Zapisuje na dokumencie `users/{uid}` znaczniki czasu zdobycia podanych
     * odznak.
     *
     * Wywoływane przez [com.kidzone.presentation.profile.ProfileViewModel]
     * w momencie, gdy lokalny diff (`current - seen` w SharedPreferences)
     * wykryje, że użytkownik właśnie wbił nowy próg. Zapis jest **idempotent
     * "first-write-wins"** - jeśli dane pole `badgeEarnedAt.NAME` już istnieje
     * w Firestore (bo inny klient już je zapisał), nie nadpisujemy go.
     *
     * Po co to jest: chcemy mieć **chronologiczny porządek odznak** widoczny
     * we wszystkich klientach (na karcie usera w rankingu user widzi
     * "od najstarszej do najnowszej"). SharedPreferences trzymane lokalnie
     * nie wystarczą, bo karta usera A jest renderowana na urządzeniu usera B.
     *
     * @param badgeNames lista [com.kidzone.presentation.common.UserBadge.name]
     *   nowo zdobytych odznak. Pusta lista = no-op (zwraca Success(Unit)).
     */
    suspend fun recordBadgesEarned(badgeNames: List<String>): OpResult<Unit>
}
