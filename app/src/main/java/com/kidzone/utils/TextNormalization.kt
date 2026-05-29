package com.kidzone.utils

/**
 * Normalizacja tekstów wprowadzanych przez użytkownika - wspólne reguły dla
 * wszystkich miejsc w apce, gdzie zapisujemy do Firestore "ładny" tekst
 * (nazwa miejsca, adres itp.).
 *
 * Po co to jest:
 *  - klawiatura Androida włącza Title Case przez [KeyboardCapitalization.Words],
 *    ale autocorrect/swipe potrafią to zepsuć (np. "plac Zabaw Kasztanowa"),
 *  - z legacy danych w bazie też mogą wpadać złe stringi,
 *  - chcemy mieć JEDNO miejsce, w którym widać "to są nasze reguły kapitalizacji",
 *    a nie ścigać wszystkich `.replaceFirstChar` w ekranach.
 *
 * Świadomie nie używamy `kotlin.text.capitalize` (deprecated) ani `Locale.ROOT`
 * - polskie znaki (ą, ć, ę, ł, ń, ó, ś, ź, ż) wymagają polskiej Locale, żeby
 * `toUpperCase`/`toLowerCase` dawały spodziewany wynik.
 */
object TextNormalization {

    private val PL_LOCALE = java.util.Locale("pl", "PL")

    /**
     * Zamienia tekst na "Title Case" - każde słowo zaczyna się dużą literą,
     * reszta liter jest mała. Łańcuchy cyfr i znaki specjalne zachowują się
     * jak są (np. "30A" zostaje "30A", "ul. Krótka" -> "Ul. Krótka").
     *
     * Słowa rozdzielamy przez whitespace (spacje, taby) - znaki interpunkcyjne
     * NIE są separatorami, żeby "Bla-bla" zostało "Bla-bla", a nie "Bla-Bla".
     *
     * Przykłady:
     *  - "plac zabaw kasztanowa" -> "Plac Zabaw Kasztanowa"
     *  - "RESTAURACJA NIEDŹWIEDŹ" -> "Restauracja Niedźwiedź"
     *  - "  ulica  Marii   Skłodowskiej  " -> "Ulica Marii Skłodowskiej"
     *    (collapsujemy też whitespace - bezpieczne dla nazwy / adresu).
     */
    fun toTitleCase(input: String): String {
        if (input.isBlank()) return ""
        return input
            .trim()
            .split(Regex("\\s+"))
            .joinToString(" ") { word ->
                if (word.isEmpty()) {
                    word
                } else {
                    val first = word.substring(0, 1).uppercase(PL_LOCALE)
                    val rest = word.substring(1).lowercase(PL_LOCALE)
                    first + rest
                }
            }
    }

    /**
     * Zamienia tekst na "Sentence case" - duża litera tylko na początku
     * pierwszego niepustego znaku, reszta zachowuje się tak jak ją wpisał
     * user (czyli np. nazwy własne w środku zdania pozostają nietknięte).
     *
     * Trim + collapse whitespace jak w [toTitleCase].
     */
    fun toSentenceCase(input: String): String {
        if (input.isBlank()) return ""
        val collapsed = input.trim().replace(Regex("\\s+"), " ")
        if (collapsed.isEmpty()) return ""
        val first = collapsed.substring(0, 1).uppercase(PL_LOCALE)
        return first + collapsed.substring(1)
    }
}
