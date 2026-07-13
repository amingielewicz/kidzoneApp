package com.kidzone.logging

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

/**
 * Timber Tree dla buildow release – przekazuje logi WARN+ do Crashlytics.
 *
 * W release nie chcemy Logcat output (dlatego nie uzywamy DebugTree),
 * ale chcemy zeby zredagowane ostrzezenia i bledy trafialy jako breadcrumbs
 * do Crashlytics – ulatwiaja debugowanie crash-reportsow.
 *
 * Logika:
 *  - priority >= WARN  → Crashlytics.log() (breadcrumb)
 *  - throwable         → celowo ignorowany, aby nie wysylac surowych danych z wyjatku
 *  - komunikaty sa redagowane z podstawowych danych wrazliwych
 *  - priority < WARN   → ignorowane (nie zasmiecamy Crashlytics)
 *
 * Wyjatki wymagajace raportowania jako non-fatal powinny byc wysylane jawnie
 * w miejscu, w ktorym mozna zagwarantowac, ze nie zawieraja danych wrazliwych.
 */
class CrashlyticsTree(
    private val crashlyticsSink: CrashlyticsSink = FirebaseCrashlyticsSink()
) : Timber.Tree() {

    override fun isLoggable(tag: String?, priority: Int): Boolean {
        return priority >= Log.WARN
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (!isLoggable(tag, priority)) return

        val redactedMessage = message.redactSensitiveValues().take(MAX_BREADCRUMB_LENGTH)
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
