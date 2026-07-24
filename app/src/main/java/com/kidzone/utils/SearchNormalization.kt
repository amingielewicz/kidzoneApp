package com.kidzone.utils

import java.text.Normalizer
import java.util.Locale

/**
 * 🎯 Odpowiedzialności:
 * - Ujednolicanie tekstu na potrzeby wyszukiwania (usuwanie znaków diakrytycznych, małe litery).
 */
object SearchNormalization {

    private val PL_LOCALE = Locale("pl", "PL")

    /**
     * Normalizuje tekst do wyszukiwania:
     * - usuwa polskie ogonki i inne znaki diakrytyczne (np. ą -> a),
     * - zamienia na małe litery zgodnie z polską lokalizacją,
     * - zamienia specyficzne znaki (np. ł -> l).
     */
    fun normalize(input: String): String =
        Normalizer.normalize(input, Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .replace("ł", "l", ignoreCase = true)
            .replace("Ł", "L", ignoreCase = true)
            .lowercase(PL_LOCALE)
}
