package com.kidzone.i18n

interface LanguagePreferences {
    fun getLanguage(): AppLanguage
    fun setLanguage(language: AppLanguage)
}
