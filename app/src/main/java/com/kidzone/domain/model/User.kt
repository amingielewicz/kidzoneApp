package com.kidzone.domain.model

/**
 * Użytkownik aplikacji kidZone.
 *
 * Odpowiada tabeli USERS z dokumentu projektu.
 *
 * Konwencja pól:
 *  - [name] – publiczny "login"/nick widoczny w rankingach, autorach miejsc
 *    i opinii. Ustawiany przy rejestracji jako "Nazwa", edytowalny w profilu.
 *  - [firstName] / [lastName] – dane osobowe, opcjonalne, wpisywane na ekranie
 *    profilu. Nie wymagane do działania reszty aplikacji – istniejący userzy
 *    będą je mieli puste, dopóki sami ich nie uzupełnią.
 *  - [avatarUrl] – pełny URL do avatara (Firebase Storage downloadUrl albo
 *    photoUrl z Google Sign-In). Null = brak avatara, UI pokazuje placeholder
 *    z ikoną osoby.
 */
data class User(
    val id: String,
    val name: String,
    val email: String,
    val firstName: String = "",
    val lastName: String = "",
    val avatarUrl: String? = null,
    val placesAddedCount: Int = 0,
    val reviewsCount: Int = 0,
    val createdAtMillis: Long = 0L,
    /**
     * Lowercase wersja [name] (locale pl_PL). Zapisywana w Firestore obok
     * [name] po to, by można było robić zapytanie `whereEqualTo("nameLowercase", ...)`
     * i sprawdzać unikalność loginu case-insensitive (Firestore nie ma natywnego
     * collation). Pole jest zarządzane przez warstwę data; UI nie powinien go
     * modyfikować bezpośrednio.
     *
     * Pusty string = legacy doc bez tego pola; warstwa data uzupełnia
     * automatycznie przy najbliższym save / sign-in (zob. `ensureUserDoc`
     * i `backfillNameLowercase` w FirebaseAuthRepository).
     */
    val nameLowercase: String = "",
    /**
     * Mapa: nazwa odznaki ([com.kidzone.presentation.common.UserBadge.name]) ->
     * timestamp zdobycia w millis (System.currentTimeMillis na kliencie,
     * który wykrył nową odznakę po raz pierwszy).
     *
     * Wykorzystywane do sortowania chronologicznego ikon w
     * [com.kidzone.presentation.common.BadgesIconRow] na karcie usera w
     * rankingu - wszyscy klienci widzą ten sam porządek "kto co kiedy
     * zdobył", niezależnie od tego, na którym urządzeniu user przekroczył
     * próg.
     *
     * Brak wpisu = odznaka jeszcze nie zdobyta lub legacy user, którego
     * profilu nigdy nie otwarto w wersji z tym polem. Sort fallback w
     * UI ustawia takich userów na koniec (sortedBy z Long.MAX_VALUE).
     */
    val badgeEarnedAt: Map<String, Long> = emptyMap(),
    /**
     * Timestamp do kiedy konto jest zablokowane (millis).
     * -1 = blokada bezpowrotna (permanentna).
     * 0 lub brak = brak blokady.
     */
    val bannedUntilMillis: Long = 0L,
    /** Powód blokady ustawiony przez admina. */
    val banReason: String = ""
) {
    /** Czy konto jest aktualnie zablokowane. */
    val isBanned: Boolean
        get() = bannedUntilMillis == -1L || (bannedUntilMillis > 0L && bannedUntilMillis > System.currentTimeMillis())

    /** Czytelny komunikat o blokadzie. */
    val banMessage: String
        get() = when {
            bannedUntilMillis == -1L -> "Twoje konto zostało zablokowane bezpowrotnie."
            bannedUntilMillis > System.currentTimeMillis() -> {
                val date = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale("pl")).format(java.util.Date(bannedUntilMillis))
                "Twoje konto jest zablokowane do $date."
            }
            else -> ""
        }
}
