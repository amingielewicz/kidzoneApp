package com.kidzone.utils

/**
 * Stałe konfiguracyjne aplikacji – placeholder-y, które właściciel projektu
 * powinien zaktualizować przed publikacją w Google Play.
 *
 * Trzymamy je w jednym pliku zamiast w `strings.xml`, bo:
 *  - są używane głównie z Kotlina (PrivacyPolicyDialog, ewentualne mailto: intent),
 *  - nie wymagają tłumaczenia (e-mail i nazwa administratora są takie same w
 *    każdym lokale),
 *  - łatwiej je odróżnić od stringów lokalizowanych przy zmianach,
 *  - kompilator znajdzie literówkę w referencji od razu (vs runtime miss
 *    w R.string).
 *
 * Gdyby kiedyś trafiły do Remote Config / BuildConfig – ten plik staje się
 * fallbackiem; wystarczy zmienić wartości na `BuildConfig.PRIVACY_CONTACT_EMAIL`
 * i podpiąć Gradle.
 */
object AppConfig {

    // ========== Networking & Timeouts ==========

    /**
     * Maksymalny czas oczekiwania na zapis do Firestore (w ms).
     *
     * Bez timeoutu Firebase retryuje w nieskończoność przy problemach
     * z Google Play Services. Po tym czasie zwracamy TimeoutException,
     * żeby UI mogło pokazać sensowny komunikat zamiast wieczystego spinnera.
     */
    const val WRITE_TIMEOUT_MS: Long = 30_000L

    // ========== Review limits ==========

    /**
     * Maksymalna długość komentarza w opinii.
     *
     * Egzekwowana w UI (AddReviewSheet counter) i warstwie data
     * (FirestoreReviewRepository). Zsynchronizowana z Remote Config
     * default `max_review_length`.
     */
    const val REVIEW_COMMENT_MAX_LENGTH: Int = 500

    // ========== Admin & Privacy ==========

    /**
     * Pełna nazwa administratora danych osobowych w rozumieniu RODO.
     *
     * Dla klauzuli w polityce prywatności. Ustaw przed wdrożeniem na
     * imię i nazwisko / nazwę firmy / pseudonim deweloperski – cokolwiek,
     * pod czym konto deweloperskie figuruje w Google Play.
     */
    const val ADMINISTRATOR_NAME: String = "Adam Mingielewicz (kidZone)"

    /**
     * E-mail kontaktowy do spraw RODO i wsparcia użytkownika.
     *
     * Powinien być czytany regularnie – Google Play wymaga, by user mógł
     * tym kanałem zażądać usunięcia konta nawet gdyby in-app delete
     * przestał działać.
     */
    const val PRIVACY_CONTACT_EMAIL: String = "bonoboczemunie@gmail.com"

    /**
     * Data wejścia w życie aktualnej wersji polityki prywatności.
     *
     * Format `dd.MM.yyyy` (czytelny dla polskiego usera). Ręcznie aktualizuj
     * przy każdej istotnej zmianie tekstu w [com.kidzone.presentation.profile.PrivacyPolicyDialog].
     */
    const val PRIVACY_POLICY_EFFECTIVE_DATE: String = "29.05.2026"
}
