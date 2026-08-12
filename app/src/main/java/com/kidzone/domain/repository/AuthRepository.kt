package com.kidzone.domain.repository

import android.net.Uri
import com.kidzone.domain.model.User
import com.kidzone.utils.OpResult
import kotlinx.coroutines.flow.Flow

/**
 * Sposób uwierzytelnienia aktualnego użytkownika.
 *
 * Wartość określa dostępne operacje zarządzania kontem:
 * - [EMAIL_PASSWORD] obsługuje zmianę hasła, zmianę e-maila i reautoryzację hasłem,
 * - [GOOGLE] korzysta z reautoryzacji tokenem Google,
 * - [UNKNOWN] oznacza brak sesji albo nieobsługiwanego providera.
 */
enum class SignInProvider { EMAIL_PASSWORD, GOOGLE, UNKNOWN }

/**
 * 🎯 Odpowiedzialności:
 * - Zarządzanie cyklem życia sesji użytkownika (Login, Register, Logout, Delete).
 * - Synchronizacja danych profilowych między Firebase Auth a dokumentem Firestore.
 * - Zarządzanie odznakami i uprawnieniami (rolami) użytkownika.
 *
 * 🔌 Strategia Cache:
 * - Sesja uwierzytelniania zarządzana przez Firebase SDK (trwała między restartami).
 * - Dokument profilu użytkownika (stats, badges) cache'owany lokalnie w Room.
 *
 * 🛡️ Autoryzacja i Bezpieczeństwo:
 * - Metody zarządzania kontem (zmiana email/hasła, usunięcie) wymagają świeżej reautoryzacji.
 * - Brak logowania haseł, tokenów i danych PII w dziennikach systemowych.
 *
 * ✅ Gwarancje spójności:
 * - Idempotentność zapisu odznak (first-write-wins).
 * - Atomowość wylogowania: jednoczesne czyszczenie lokalnego cache, sesji i tokenów FCM.
 *
 * 📤 Mapowanie błędów:
 * - Kody błędów Firebase (np. `weak-password`, `invalid-credential`) mapowane na [AuthException] i [OpResult].
 *
 * 🧵 Threading:
 * - Bezpieczne wywołania z dowolnego wątku. Strumień [currentUser] emituje zmiany natychmiast po zmianie stanu w SDK.
 */
interface AuthRepository {

    /**
     * Strumień lekkiego modelu aktualnie zalogowanego użytkownika.
     *
     * Model zawiera dane dostępne bezpośrednio z providera uwierzytelniania. Pełny dokument
     * Firestore udostępnia [observeUser].
     *
     * @return strumień użytkownika albo `null` po wylogowaniu, banie lub usunięciu konta.
     */
    val currentUser: Flow<User?>

    /**
     * Obserwuje pełny dokument użytkownika z kolekcji `users`.
     *
     * Emituje `null`, gdy [userId] jest pusty albo dokument nie istnieje. Błędy dostępu lub sieci
     * powinny zostać zmapowane na poziomie implementacji lub warstwy wyższej.
     *
     * @param userId identyfikator użytkownika.
     * @return strumień aktualnego dokumentu profilu.
     */
    fun observeUser(userId: String): Flow<User?>

    /**
     * Loguje użytkownika adresem e-mail i hasłem.
     *
     * @param email adres przypisany do konta.
     * @param password hasło użytkownika; nie może być logowane ani przechowywane.
     * @return zalogowany użytkownik albo zmapowany błąd uwierzytelniania.
     */
    suspend fun signInWithEmail(email: String, password: String): OpResult<User>

    /**
     * Tworzy konto e-mail/hasło i inicjalizuje powiązany profil użytkownika.
     *
     * Implementacja powinna obsłużyć częściowy błąd między utworzeniem konta Auth a zapisem profilu
     * i nie zgłaszać pełnego sukcesu, dopóki wymagane dane konta nie są gotowe.
     *
     * @param name publiczna nazwa użytkownika.
     * @param email adres nowego konta.
     * @param password hasło zgodne z polityką aplikacji.
     * @return utworzony użytkownik albo zmapowany błąd.
     */
    suspend fun registerWithEmail(name: String, email: String, password: String): OpResult<User>

    /**
     * Loguje lub rejestruje użytkownika poświadczeniem Google.
     *
     * @param idToken krótkotrwały token ID uzyskany na urządzeniu; nie może być logowany.
     * @return zalogowany użytkownik albo zmapowany błąd providera.
     */
    suspend fun signInWithGoogle(idToken: String): OpResult<User>

    /**
     * Wysyła wiadomość umożliwiającą reset hasła.
     *
     * Odpowiedź użytkownika nie powinna ujawniać, czy podany adres istnieje w systemie.
     *
     * @param email adres, na który ma zostać wysłany link resetujący.
     */
    suspend fun sendPasswordResetEmail(email: String): OpResult<Unit>

    /**
     * Ponownie wysyła wiadomość weryfikacyjną dla konta e-mail/hasło.
     *
     * @param email adres aktualnego konta.
     * @param password hasło używane do wymaganej reautoryzacji.
     */
    suspend fun resendVerificationEmail(email: String, password: String): OpResult<Unit>

    /**
     * Kończy lokalną i zdalną sesję użytkownika.
     *
     * Wywołujący lub implementacja muszą również wyczyścić prywatny cache, token FCM, lokalizację
     * widgetu oraz operacje oczekujące powiązane z zakończoną sesją.
     */
    suspend fun signOut()

    /**
     * Odświeża dane providera aktualnie zalogowanego użytkownika, np. status weryfikacji e-mail.
     */
    suspend fun refreshUser(): OpResult<Unit>

    /**
     * Pobiera publiczny dokument użytkownika.
     *
     * Metoda może służyć do wyświetlenia autora miejsca lub opinii i nie wymaga, aby [userId]
     * należał do aktualnie zalogowanej osoby.
     *
     * @param userId identyfikator publicznego profilu.
     */
    suspend fun getUserById(userId: String): OpResult<User>

    /**
     * Pobiera ranking użytkowników według aktualnej reguły domenowej.
     *
     * @param limit maksymalna liczba wyników.
     */
    suspend fun getTopUsers(limit: Int = 10): OpResult<List<User>>

    /**
     * Aktualizuje edytowalne pola profilu zalogowanego użytkownika.
     *
     * Zapisuje dokument Firestore oraz dane prezentacyjne Firebase Auth. Pola prywatne i publiczne
     * powinny być przechowywane zgodnie z aktualnym schematem bezpieczeństwa.
     *
     * @param displayName publiczny nick użytkownika.
     * @param firstName opcjonalne imię.
     * @param lastName opcjonalne nazwisko.
     * @param avatarUrl URL avatara albo `null`, aby go wyczyścić.
     * @return zaktualizowany model użytkownika albo błąd.
     */
    suspend fun updateUserProfile(
        displayName: String,
        firstName: String,
        lastName: String,
        avatarUrl: String?
    ): OpResult<User>

    /**
     * Wgrywa lokalny obraz avatara do Storage.
     *
     * Upload nie aktualizuje automatycznie profilu; zwrócony URL należy przekazać do
     * [updateUserProfile]. Implementacja powinna walidować MIME, rozmiar i ownership ścieżki.
     *
     * @param localUri URI obrazu z aparatu lub Android Photo Picker.
     * @return download URL albo zmapowany błąd uploadu.
     */
    suspend fun uploadAvatar(localUri: Uri): OpResult<String>

    /**
     * Zwraca provider aktualnej sesji.
     *
     * @return [SignInProvider.UNKNOWN], gdy brak sesji lub provider nie jest obsługiwany.
     */
    suspend fun getCurrentSignInProvider(): SignInProvider

    /**
     * Zmienia hasło użytkownika konta e-mail/hasło po reautoryzacji.
     *
     * @param currentPassword aktualne hasło używane wyłącznie jako credential reauth.
     * @param newPassword nowe hasło zgodne z polityką aplikacji.
     */
    suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ): OpResult<Unit>

    /**
     * Inicjuje zmianę adresu e-mail konta e-mail/hasło.
     *
     * Nowy adres staje się aktywny dopiero po wykonaniu procesu weryfikacyjnego Firebase.
     *
     * @param currentPassword hasło używane do reautoryzacji.
     * @param newEmail nowy adres e-mail.
     */
    suspend fun changeEmail(
        currentPassword: String,
        newEmail: String
    ): OpResult<Unit>

    /**
     * Trwale usuwa konto e-mail/hasło i powiązane dane.
     *
     * Operacja obejmuje reautoryzację, cleanup Firestore, Storage, FCM, lokalnych danych, widgetu i
     * tożsamości Auth. Proces nie jest atomowy, dlatego częściowy błąd musi pozostać widoczny, a
     * ponowienie powinno być idempotentne.
     *
     * @param currentPassword hasło używane do reautoryzacji.
     */
    suspend fun deleteAccount(currentPassword: String): OpResult<Unit>

    /**
     * Trwale usuwa konto zalogowane przez Google.
     *
     * @param idToken token ID używany do ponownej autoryzacji; nie może być logowany.
     */
    suspend fun deleteAccountWithGoogle(idToken: String): OpResult<Unit>

    /**
     * Zapisuje czas zdobycia nowych odznak w sposób idempotentny `first-write-wins`.
     *
     * @param badgeNames nazwy nowo zdobytych odznak; pusta lista jest operacją no-op.
     */
    suspend fun recordBadgesEarned(badgeNames: List<String>): OpResult<Unit>

    /**
     * Rejestruje fakt akceptacji Regulaminu przez użytkownika.
     */
    suspend fun acceptTos(): OpResult<Unit>

    /**
     * Usuwa zapisane odznaki, których warunki nie są już spełnione.
     *
     * @param badgeNames nazwy odznak do cofnięcia; pusta lista jest operacją no-op.
     */
    suspend fun revokeBadges(badgeNames: List<String>): OpResult<Unit>
}
