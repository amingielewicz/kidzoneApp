package com.kidzone.utils

/**
 * 🎯 Odpowiedzialności:
 * - Definiowanie i egzekwowanie polityki siły haseł po stronie klienta.
 * - Udostępnianie reguł walidacji dla komponentów UI (np. checklisty wymagań).
 *
 * ✅ Gwarancje:
 * - Deterministyczna walidacja haseł przed wysyłką do Firebase Auth.
 * - Spójność reguł między ekranami rejestracji i zmiany hasła.
 */
object PasswordPolicy {

    const val MIN_LENGTH = 8

    /**
     * Pojedyncza zasada walidacji hasła. Używana przez UI do pokazania
     * checklisty wymagań ze stanem spełnione/niespełnione.
     *
     * @property labelKey stabilny klucz etykiety do zmapowania na zasoby UI
     * @property predicate sprawdza, czy podane hasło spełnia tę regułę
     */
    data class Rule(
        val labelKey: LabelKey,
        val predicate: (String) -> Boolean
    )

    enum class LabelKey {
        MinLength,
        Lowercase,
        Uppercase,
        SpecialCharacter
    }

    val rules: List<Rule> = listOf(
        Rule(LabelKey.MinLength) { it.length >= MIN_LENGTH },
        Rule(LabelKey.Lowercase) { pwd -> pwd.any { it.isLowerCase() } },
        Rule(LabelKey.Uppercase) { pwd -> pwd.any { it.isUpperCase() } },
        Rule(LabelKey.SpecialCharacter) { pwd ->
            pwd.any { ch -> !ch.isLetterOrDigit() && !ch.isWhitespace() }
        }
    )

    /** True gdy hasło spełnia wszystkie [rules]. */
    fun isValid(password: String): Boolean = rules.all { it.predicate(password) }

    /**
     * Zwraca listę reguł wraz z informacją, czy aktualnie podane hasło
     * je spełnia. Wykorzystywane przez UI do renderowania checklisty
     * z zielonymi / szarymi ikonami.
     */
    fun evaluate(password: String): List<RuleStatus> = rules.map { rule ->
        RuleStatus(rule.labelKey, rule.predicate(password))
    }

    data class RuleStatus(val labelKey: LabelKey, val isSatisfied: Boolean)

    /**
     * Krótki komunikat błędu, gdy hasło nie spełnia wymagań.
     */
    const val DEFAULT_ERROR_MESSAGE: String =
        "Hasło musi mieć co najmniej $MIN_LENGTH znaków, zawierać małe i duże litery oraz znak specjalny."
}
