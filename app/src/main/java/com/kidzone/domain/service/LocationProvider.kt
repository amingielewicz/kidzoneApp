package com.kidzone.domain.service

/**
 * Abstrakcja dostępu do lokalizacji urządzenia.
 *
 * Oddziela ViewModele od Android `Context`, `LocationManager` i
 * `FusedLocationProviderClient`, dzięki czemu logika korzystająca z lokalizacji może być testowana
 * bez prawdziwego urządzenia.
 *
 * Implementacja nie powinna samodzielnie wyświetlać dialogów uprawnień ani otwierać ustawień.
 * Odpowiada wyłącznie za odczyt rzeczywistego stanu systemu i pobranie lokalizacji.
 */
interface LocationProvider {

    /**
     * Sprawdza, czy aplikacja ma co najmniej jedno uprawnienie lokalizacji używane podczas pracy
     * aplikacji.
     *
     * @return `true`, gdy przyznano lokalizację dokładną lub przybliżoną.
     */
    fun hasPermission(): Boolean

    /**
     * Sprawdza, czy systemowa usługa lokalizacji jest włączona.
     *
     * @return `true`, gdy co najmniej jeden obsługiwany provider może dostarczyć lokalizację.
     */
    fun isServiceEnabled(): Boolean

    /**
     * Pobiera bieżącą lokalizację urządzenia.
     *
     * Metoda nie może zakładać, że uprawnienie nadal jest przyznane. Implementacja powinna
     * bezpiecznie obsłużyć timeout, wyłączenie usługi oraz cofnięcie zgody podczas operacji.
     *
     * @return para `(latitude, longitude)` albo `null`, gdy lokalizacja jest niedostępna.
     */
    suspend fun getCurrentLocation(): Pair<Double, Double>?

    /**
     * Zwraca ostatnią lokalizację skutecznie pobraną przez aplikację.
     *
     * @return zapamiętana para współrzędnych albo `null`, gdy brak danych.
     */
    fun getLastKnownLocation(): Pair<Double, Double>?

    /**
     * Zwraca wiek ostatniej zapamiętanej lokalizacji.
     *
     * @return liczba pełnych minut od zapisu albo `null`, gdy brak lokalizacji.
     */
    fun getLastKnownLocationAgeMinutes(): Int?
}
