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
     * Obsługuje specjalne prefiksy:
     *  - **Mc** / **Mac** → "McDonald" (litera po prefixie = uppercase)
     *  - **O'** → "O'Brien" (litera po apostrofie = uppercase)
     *
     * Słowa rozdzielamy przez whitespace (spacje, taby) - znaki interpunkcyjne
     * NIE są separatorami, żeby "Bla-bla" zostało "Bla-bla", a nie "Bla-Bla".
     *
     * Przykłady:
     *  - "plac zabaw kasztanowa" -> "Plac Zabaw Kasztanowa"
     *  - "RESTAURACJA NIEDŹWIEDŹ" -> "Restauracja Niedźwiedź"
     *  - "mcdonald's" -> "McDonald's"
     *  - "o'connor" -> "O'Connor"
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
                    capitalizeWord(word)
                }
            }
    }

    /**
     * Kapitalizuje pojedyncze słowo z uwzględnieniem Mc/Mac/O' prefixów.
     */
    private fun capitalizeWord(word: String): String {
        val lower = word.lowercase(PL_LOCALE)

        // O'xxx → O'Xxx
        val apoIdx = lower.indexOf('\'')
        if (apoIdx == 1 && lower.length > 2) {
            val before = lower.substring(0, 1).uppercase(PL_LOCALE)
            val afterApo = lower.substring(2, 3).uppercase(PL_LOCALE)
            val rest = lower.substring(3)
            return "$before'$afterApo$rest"
        }

        // McXxx → McXxx
        if (lower.startsWith("mc") && lower.length > 2 && lower[2].isLetter()) {
            val afterMc = lower.substring(2, 3).uppercase(PL_LOCALE)
            val rest = lower.substring(3)
            return "Mc$afterMc$rest"
        }

        // MacXxx → MacXxx (but not "Maciej" — only if >5 chars to reduce false positives)
        if (lower.startsWith("mac") && lower.length > 5 && lower[3].isLetter()) {
            val afterMac = lower.substring(3, 4).uppercase(PL_LOCALE)
            val rest = lower.substring(4)
            return "Mac$afterMac$rest"
        }

        // Default: First uppercase, rest lowercase
        val first = lower.substring(0, 1).uppercase(PL_LOCALE)
        val rest = lower.substring(1)
        return first + rest
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
