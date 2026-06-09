package com.kidzone.logging

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

/**
 * Timber Tree dla buildow release – przekazuje logi WARN+ do Crashlytics.
 *
 * W release nie chcemy Logcat output (dlatego nie uzywamy DebugTree),
 * ale chcemy zeby ostrzezenia i bledy trafialy jako breadcrumbs do
 * Crashlytics – ulatwiaja debugowanie crash-reportsow.
 *
 * Logika:
 *  - priority >= WARN  → Crashlytics.log() (breadcrumb)
 *  - throwable != null → Crashlytics.recordException() (non-fatal)
 *  - priority < WARN   → ignorowane (nie zasmiecamy Crashlytics)
 */
class CrashlyticsTree : Timber.Tree() {

    override fun isLoggable(tag: String?, priority: Int): Boolean {
        return priority >= Log.WARN
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val crashlytics = FirebaseCrashlytics.getInstance()

        // Breadcrumb – widoczny w Crashlytics timeline przed crashem
        crashlytics.log("${priorityLabel(priority)}/$tag: $message")

        // Non-fatal exception – pojawi sie jako osobny issue w konsoli
        if (t != null) {
            crashlytics.recordException(t)
        }
    }

    private fun priorityLabel(priority: Int): String = when (priority) {
        Log.WARN -> "W"
        Log.ERROR -> "E"
        Log.ASSERT -> "A"
        else -> "?"
    }
}
