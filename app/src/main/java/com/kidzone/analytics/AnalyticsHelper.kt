package com.kidzone.analytics

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralna klasa do logowania eventow Firebase Analytics w aplikacji kidZone.
 *
 * Kazdy ekran i interakcja usera przechodzi przez ten helper – dzieki temu:
 *  - unikamy rozrzuconych FirebaseAnalytics.getInstance() po calym kodzie,
 *  - event names i param keys sa w jednym miejscu (latwo audytowac),
 *  - Timber loguje lokalnie kazdy event (widoczny w Logcat podczas dev).
 */
@Singleton
class AnalyticsHelper @Inject constructor(
    private val analytics: FirebaseAnalytics
) {
    // ─── Screen views ───────────────────────────────────────────────────

    fun logScreenView(screenName: String) {
        Timber.d("Analytics: screen_view → $screenName")
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
        }
    }

    // ─── Auth ───────────────────────────────────────────────────────────

    fun logLogin(method: String) {
        Timber.d("Analytics: login → method=$method")
        analytics.logEvent(FirebaseAnalytics.Event.LOGIN) {
            param(FirebaseAnalytics.Param.METHOD, method)
        }
    }

    fun logSignUp(method: String) {
        Timber.d("Analytics: sign_up → method=$method")
        analytics.logEvent(FirebaseAnalytics.Event.SIGN_UP) {
            param(FirebaseAnalytics.Param.METHOD, method)
        }
    }

    // ─── Places ─────────────────────────────────────────────────────────

    fun logAddPlace(placeId: String, category: String) {
        Timber.d("Analytics: add_place → id=$placeId, category=$category")
        analytics.logEvent("add_place") {
            param("place_id", placeId)
            param("category", category)
        }
    }

    fun logViewPlace(placeId: String, placeName: String) {
        Timber.d("Analytics: view_place → id=$placeId, name=$placeName")
        analytics.logEvent("view_place") {
            param("place_id", placeId)
            param("place_name", placeName.take(100))
        }
    }

    fun logDeletePlace(placeId: String) {
        Timber.d("Analytics: delete_place → id=$placeId")
        analytics.logEvent("delete_place") {
            param("place_id", placeId)
        }
    }

    fun logReportPlace(placeId: String, reason: String) {
        Timber.d("Analytics: report_place → id=$placeId, reason=$reason")
        analytics.logEvent("report_place") {
            param("place_id", placeId)
            param("reason", reason)
        }
    }

    // ─── Reviews ────────────────────────────────────────────────────────

    fun logAddReview(placeId: String, rating: Float) {
        Timber.d("Analytics: add_review → placeId=$placeId, rating=$rating")
        analytics.logEvent("add_review") {
            param("place_id", placeId)
            param("rating", rating.toDouble())
        }
    }

    fun logDeleteReview(reviewId: String) {
        Timber.d("Analytics: delete_review → id=$reviewId")
        analytics.logEvent("delete_review") {
            param("review_id", reviewId)
        }
    }

    // ─── Photos ─────────────────────────────────────────────────────────

    fun logAddPhoto(source: String) {
        Timber.d("Analytics: add_photo → source=$source")
        analytics.logEvent("add_photo") {
            param("source", source)
        }
    }

    // ─── Onboarding ─────────────────────────────────────────────────────

    fun logOnboardingStarted() {
        Timber.d("Analytics: onboarding_started")
        analytics.logEvent("onboarding_started") {}
    }

    fun logOnboardingCompleted() {
        Timber.d("Analytics: onboarding_completed")
        analytics.logEvent("onboarding_completed") {}
    }

    fun logOnboardingSkipped(lastPage: Int) {
        Timber.d("Analytics: onboarding_skipped → page=$lastPage")
        analytics.logEvent("onboarding_skipped") {
            param("last_page", lastPage.toLong())
        }
    }

    // ─── Map interactions ───────────────────────────────────────────────

    fun logMapInteraction(action: String) {
        Timber.d("Analytics: map_interaction → action=$action")
        analytics.logEvent("map_interaction") {
            param("action", action)
        }
    }

    // ─── Search ─────────────────────────────────────────────────────────

    fun logSearch(query: String, resultsCount: Int) {
        Timber.d("Analytics: search → query=$query, results=$resultsCount")
        analytics.logEvent(FirebaseAnalytics.Event.SEARCH) {
            param(FirebaseAnalytics.Param.SEARCH_TERM, query.take(100))
            param("results_count", resultsCount.toLong())
        }
    }

    // ─── User properties ────────────────────────────────────────────────

    fun setUserProperty(key: String, value: String?) {
        Timber.d("Analytics: user_property → $key=$value")
        analytics.setUserProperty(key, value)
    }

    fun setUserId(uid: String?) {
        analytics.setUserId(uid)
    }
}
