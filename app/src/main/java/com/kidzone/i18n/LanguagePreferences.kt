package com.kidzone.i18n

/**
 * 🎯 Odpowiedzialności:
 * - Trwałe przechowywanie (Persistence) wybranego języka aplikacji.
 */
interface LanguagePreferences {
    fun getLanguage(): AppLanguage
    fun setLanguage(language: AppLanguage)
}
