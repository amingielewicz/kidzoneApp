package com.kidzone.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 🎯 Odpowiedzialności:
 * - Ujednolicone formatowanie dat w całej aplikacji.
 */
object DateUtils {

    private const val DATE_FORMAT = "dd.MM.yyyy"

    /**
     * Formatuje timestamp na czytelny tekst (np. 29.05.2026).
     */
    fun formatDate(millis: Long): String {
        if (millis <= 0) return ""
        val formatter = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
        return formatter.format(Date(millis))
    }

    /**
     * Formatuje datę z godziną.
     */
    fun formatDateWithTime(millis: Long): String {
        if (millis <= 0) return ""
        val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return formatter.format(Date(millis))
    }
}
