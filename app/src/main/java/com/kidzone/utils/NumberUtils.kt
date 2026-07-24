package com.kidzone.utils

/**
 * 🎯 Odpowiedzialności:
 * - Ujednolicone formatowanie liczb w aplikacji.
 */
object NumberUtils {

    /** Formatowanie oceny (np. 4.5). */
    private const val RATING_FORMAT = "%.1f"

    /**
     * Formatuje średnią ocenę do jednego miejsca po przecinku.
     */
    fun formatRating(rating: Double): String = RATING_FORMAT.format(rating)
}
