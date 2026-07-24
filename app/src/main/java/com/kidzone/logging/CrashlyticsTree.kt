package com.kidzone.logging

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

/**
 * 🎯 Odpowiedzialności:
 * - Przekazywanie istotnych logów (WARN+) do Firebase Crashlytics jako "breadcrumbs".
 * - Redagowanie (redakcja) danych wrażliwych (E-maile, Tokeny) przed wysyłką do chmury.
 *
 * 🛡️ Bezpieczeństwo i Prywatność:
 * - Automatycznie usuwa wzorce e-maili i długich tokenów z treści logów.
 * - Celowo nie wysyła pełnych obiektów [Throwable], aby uniknąć wycieku danych z komunikatów systemowych.
 *
 * ✅ Gwarancje:
 * - Nie generuje wyjścia do standardowego Logcata (bezpieczne dla buildów produkcyjnych).
 */
class CrashlyticsTree(
    private val crashlyticsSink: CrashlyticsSink = FirebaseCrashlyticsSink()
) : Timber.Tree() {

    override fun isLoggable(tag: String?, priority: Int): Boolean {
        return priority >= Log.WARN
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (!isLoggable(tag, priority)) return

        // Timber dokleja stack trace do message przed wywolaniem Tree.log().
        // Przy obecnym kontrakcie komunikaty aplikacji sa jednoliniowe, wiec dla logow
        // z wyjatkiem zachowujemy tylko jawny komunikat sprzed pierwszego znaku nowej linii.
        val explicitMessage = if (t != null) message.substringBefore('\n') else message
        val redactedMessage = explicitMessage.redactSensitiveValues().take(MAX_BREADCRUMB_LENGTH)
        if (redactedMessage.isNotBlank()) {
            crashlyticsSink.log("${priorityLabel(priority)}/${tag.orEmpty()}: $redactedMessage")
        }

        // Deliberately do not forward `t` to FirebaseCrashlytics.recordException().
        // Exception messages and nested causes may contain tokens, e-mail addresses,
        // request URLs, identifiers or server responses that bypass breadcrumb redaction.
    }

    private fun priorityLabel(priority: Int): String = when (priority) {
        Log.WARN -> "W"
        Log.ERROR -> "E"
        Log.ASSERT -> "A"
        else -> "?"
    }

    private fun String.redactSensitiveValues(): String =
        replace(EMAIL_REGEX, "[redacted-email]")
            .replace(TOKEN_REGEX, "[redacted-token]")

    companion object {
        private const val MAX_BREADCRUMB_LENGTH = 512
        private val EMAIL_REGEX = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
        private val TOKEN_REGEX = Regex("\\b[A-Za-z0-9_-]{80,}\\b")
    }
}

interface CrashlyticsSink {
    fun log(message: String)
}

private class FirebaseCrashlyticsSink : CrashlyticsSink {
    private val crashlytics: FirebaseCrashlytics = FirebaseCrashlytics.getInstance()

    override fun log(message: String) {
        crashlytics.log(message)
    }
}
