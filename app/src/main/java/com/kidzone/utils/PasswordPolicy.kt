package com.kidzone.utils

/**
 * Polityka siły hasła egzekwowana po stronie klienta.
 *
 * Wymagania (wszystkie muszą być spełnione, by hasło było prawidłowe):
 *  - minimum [MIN_LENGTH] znaków
 *  - co najmniej jedna mała litera
 *  - co najmniej jedna duża litera
 *  - co najmniej jeden znak specjalny (znak nie będący literą ani cyfrą)
 *
 * Te same reguły obowiązują w 3 miejscach:
 *  - rejestracja (RegisterScreen / RegisterViewModel)
 *  - zmiana hasła (ChangePasswordDialog / ProfileViewModel)
 *  - ekran logowania (komunikat dla "weak password" z Firebase)
 *
 * Firebase Auth dodatkowo wymaga min. 6 znaków server-side. Nasza polityka
 * jest mocniejsza, więc Firebase nigdy nie odrzuci hasła, które przeszło
 * lokalną walidację - chyba że projekt Firebase ma własną politykę
 * silniejszą niż ta tutaj. Na MVP nie konfigurujemy server-side policy,
 * bo wymagałoby to Identity Platform (płatne).
 *
 * Mała litera: testowana po Locale-independent toLowerCase, żeby polskie
 * znaki diakrytyczne (ą, ć, ę, ...) liczyły się tak samo jak ASCII.
 */
object PasswordPolicy {

    const val MIN_LENGTH = 8

    /**
     * Pojedyncza zasada walidacji hasła. Używana przez UI do pokazania
     * checklisty wymagań ze stanem spełnione/niespełnione.
     *
     * @property label tekst do pokazania użytkownikowi (po polsku)
     * @property predicate sprawdza, czy podane hasło spełnia tę regułę
     */
    data class Rule(
        val label: String,
        val predicate: (String) -> Boolean
    )

    val rules: List<Rule> = listOf(
        Rule("Minimum $MIN_LENGTH znaków") { it.length >= MIN_LENGTH },
        Rule("Co najmniej jedna mała litera") { pwd -> pwd.any { it.isLowerCase() } },
        Rule("Co najmniej jedna duża litera") { pwd -> pwd.any { it.isUpperCase() } },
        Rule("Co najmniej jeden znak specjalny") { pwd ->
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
        RuleStatus(rule.label, rule.predicate(password))
    }

    data class RuleStatus(val label: String, val isSatisfied: Boolean)

    /**
     * Krótki komunikat dla snackbara / pola "errorMessage" w VM, gdy user
     * próbuje submitować zbyt słabe hasło. Treść spójna z [rules].
     */
    const val DEFAULT_ERROR_MESSAGE: String =
        "Hasło musi mieć min. $MIN_LENGTH znaków, w tym małą i dużą literę oraz znak specjalny"
}
