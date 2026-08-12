package com.kidzone.data.service

import android.content.Context
import com.kidzone.domain.service.BadgePreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SharedPreferences-backed implementation of [BadgePreferences].
 *
 * Key format: `seen_badges_{uid}` — isolates badge state per user
 * (multiple users on the same device).
 */
@Singleton
class AndroidBadgePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) : BadgePreferences {

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun getSeenBadges(uid: String): Set<String> {
        // Return a copy to avoid modification issues with SharedPreferences internal state
        return prefs.getStringSet("$KEY_PREFIX$uid", emptySet())?.toSet().orEmpty()
    }

    override fun setSeenBadges(uid: String, badges: Set<String>) {
        // Use commit() instead of apply() to ensure synchronous write before next possible read
        prefs.edit().putStringSet("$KEY_PREFIX$uid", badges).commit()
    }

    private companion object {
        const val PREFS_NAME = "badge_notifications"
        const val KEY_PREFIX = "seen_badges_"
    }
}
