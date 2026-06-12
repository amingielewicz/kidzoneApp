package com.kidzone.review

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import com.google.android.play.core.review.ReviewManagerFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages in-app review prompts via Google Play Review API.
 *
 * Prompt logic:
 *  - Triggered after the user's 3rd added place OR 3rd submitted review.
 *  - Each trigger type fires at most once (won't nag on 4th, 5th, etc.).
 *  - Google Play controls actual display frequency – we only request;
 *    the API may silently no-op if quota is exceeded.
 *
 * Counts are persisted in SharedPreferences so they survive process death.
 */
@Singleton
class InAppReviewManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /** Increment place-added counter and return true if review should be triggered. */
    fun onPlaceAdded(): Boolean {
        val count = prefs.getInt(KEY_PLACES_ADDED, 0) + 1
        prefs.edit().putInt(KEY_PLACES_ADDED, count).apply()
        return count == TRIGGER_THRESHOLD && !prefs.getBoolean(KEY_REVIEW_SHOWN_PLACE, false)
    }

    /** Increment review-submitted counter and return true if review should be triggered. */
    fun onReviewSubmitted(): Boolean {
        val count = prefs.getInt(KEY_REVIEWS_SUBMITTED, 0) + 1
        prefs.edit().putInt(KEY_REVIEWS_SUBMITTED, count).apply()
        return count == TRIGGER_THRESHOLD && !prefs.getBoolean(KEY_REVIEW_SHOWN_REVIEW, false)
    }

    /**
     * Launches the in-app review flow.
     *
     * Must be called from an Activity context. Best-effort – failures are
     * silently logged (review prompts are non-critical UX).
     */
    suspend fun launchReviewFlow(activity: Activity) {
        try {
            val manager = ReviewManagerFactory.create(context)
            val reviewInfo = manager.requestReviewFlow().await()
            manager.launchReviewFlow(activity, reviewInfo).await()
            // Mark as shown for both triggers to avoid double-prompting
            prefs.edit()
                .putBoolean(KEY_REVIEW_SHOWN_PLACE, true)
                .putBoolean(KEY_REVIEW_SHOWN_REVIEW, true)
                .apply()
            Timber.d("InAppReviewManager: review flow launched successfully")
        } catch (e: Exception) {
            Timber.w(e, "InAppReviewManager: failed to launch review flow")
        }
    }

    companion object {
        private const val PREFS_NAME = "kidzone_in_app_review"
        private const val KEY_PLACES_ADDED = "places_added_count"
        private const val KEY_REVIEWS_SUBMITTED = "reviews_submitted_count"
        private const val KEY_REVIEW_SHOWN_PLACE = "review_shown_for_place"
        private const val KEY_REVIEW_SHOWN_REVIEW = "review_shown_for_review"
        /** Number of actions before triggering review prompt. */
        const val TRIGGER_THRESHOLD = 3
    }
}
