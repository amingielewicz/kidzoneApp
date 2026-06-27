package com.kidzone.i18n

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidLanguagePreferences @Inject constructor(
    @ApplicationContext context: Context
) : LanguagePreferences {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getLanguage(): AppLanguage =
        AppLanguage.fromStorageValue(prefs.getString(KEY_LANGUAGE, AppLanguage.SYSTEM.storageValue))

    override fun setLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, language.storageValue).apply()
    }

    private companion object {
        const val PREFS_NAME = "kidzone_language"
        const val KEY_LANGUAGE = "language"
    }
}
