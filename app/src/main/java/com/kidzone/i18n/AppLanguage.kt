package com.kidzone.i18n

import androidx.annotation.StringRes
import com.kidzone.R

enum class AppLanguage(
    val storageValue: String,
    val languageTag: String?,
    @StringRes val labelRes: Int
) {
    SYSTEM("system", null, R.string.language_system_default),
    POLISH("pl", "pl", R.string.language_polish),
    ENGLISH("en", "en", R.string.language_english);

    companion object {
        fun fromStorageValue(value: String?): AppLanguage =
            entries.firstOrNull { it.storageValue == value } ?: SYSTEM
    }
}
