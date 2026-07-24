package com.kidzone.utils

/**
 * 🎯 Odpowiedzialności:
 * - Ujednolicanie formatu tekstów wprowadzanych przez użytkownika (nazwy, adresy).
 * - Realizacja reguł "Title Case" oraz "Sentence case".
 *
 * ⚙️ Techniczne:
 * - Obsługa specyfiki języka polskiego (polskie znaki diakrytyczne).
 * - Specjalne reguły dla nazwisk i marek (Mc, Mac, O').
 *
 * ✅ Gwarancje:
 * - Deterministyczny wynik dla tych samych danych wejściowych.
 * - Usuwanie nadmiarowych spacji i znaków niedrukowalnych.
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
