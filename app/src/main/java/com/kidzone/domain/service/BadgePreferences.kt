package com.kidzone.domain.service

/**
 * Abstraction over badge notification persistence (SharedPreferences).
 *
 * Allows ProfileViewModel to track "seen badges" without direct dependency
 * on Android Context / SharedPreferences. Easily mockable in unit tests.
 */
interface BadgePreferences {
    /** Get set of badge names that were already shown to this user. */
    fun getSeenBadges(uid: String): Set<String>

    /** Persist the current set of badges as "seen" for this user. */
    fun setSeenBadges(uid: String, badges: Set<String>)
}
