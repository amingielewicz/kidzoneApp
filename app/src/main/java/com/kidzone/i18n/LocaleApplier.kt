package com.kidzone.i18n

import android.content.Context
import android.content.res.Resources
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

/**
 * 🎯 Odpowiedzialności:
 * - Dynamiczna zmiana ustawień regionalnych (Locale) aplikacji bez restartu urządzenia.
 * - Synchronizacja ustawień systemowych z preferencjami użytkownika.
 */
object LocaleApplier {
    fun apply(context: Context, language: AppLanguage) {
        val locale = language.languageTag?.let(Locale::forLanguageTag)
        val config = Configuration(context.resources.configuration)
        if (locale == null) {
            val systemLocales = Resources.getSystem().configuration.locales
            Locale.setDefault(systemLocales[0])
            config.setLocales(systemLocales)
        } else {
            Locale.setDefault(locale)
            config.setLocale(locale)
            config.setLocales(LocaleList(locale))
        }
        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }
}
