package com.kidzone.domain.service

/**
 * Abstrakcja lokalnego zapisu informacji o odznakach już pokazanych użytkownikowi.
 *
 * Umożliwia `ProfileViewModel` wykrywanie nowych odznak bez zależności od Android `Context` i
 * `SharedPreferences`. Dane są lokalnym stanem prezentacyjnym, a nie źródłem prawdy o przyznanych
 * odznakach.
 */
interface BadgePreferences {

    /**
     * Pobiera nazwy odznak, które zostały już pokazane wskazanemu użytkownikowi.
     *
     * @param uid identyfikator użytkownika, rozdzielający stan między kontami na urządzeniu.
     * @return zapisany zbiór nazw albo pusty zbiór, gdy użytkownik nie widział jeszcze odznak.
     */
    fun getSeenBadges(uid: String): Set<String>

    /**
     * Zastępuje lokalny zbiór odznak oznaczonych jako pokazane.
     *
     * @param uid identyfikator użytkownika.
     * @param badges pełny zbiór nazw odznak, który ma zostać zapamiętany.
     */
    fun setSeenBadges(uid: String, badges: Set<String>)
}
