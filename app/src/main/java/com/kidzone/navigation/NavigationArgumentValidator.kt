package com.kidzone.navigation

/**
 * Central validation for route arguments coming from deep links, push payloads,
 * saved state, or manual route creation.
 */
object NavigationArgumentValidator {

    private const val MAX_PLACE_ID_LENGTH = 128
    private val safePlaceIdPattern = Regex("^[A-Za-z0-9_-]+$")

    /**
     * Returns true only for place identifiers that are safe to use inside
     * navigation routes and repository calls.
     */
    fun isValidPlaceId(placeId: String?): Boolean {
        if (placeId.isNullOrBlank()) return false
        if (placeId.length > MAX_PLACE_ID_LENGTH) return false
        return safePlaceIdPattern.matches(placeId)
    }

    /**
     * Returns a normalized safe place id or null when the input is invalid.
     */
    fun sanitizePlaceId(placeId: String?): String? {
        val trimmed = placeId?.trim()
        return if (isValidPlaceId(trimmed)) trimmed else null
    }
}
