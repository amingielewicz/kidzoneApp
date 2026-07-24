package com.kidzone.analytics

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Zamknięty zestaw powodów zgłoszenia wysyłanych do Firebase Analytics.
 *
 * Do Analytics trafia wyłącznie stabilny kod techniczny, nigdy tekst wpisany
 * przez użytkownika. Szczegółowy opis zgłoszenia może być przechowywany w
 * Firestore na potrzeby moderacji, ale nie powinien być parametrem Analytics.
 */
enum class PlaceReportReason(val analyticsCode: String) {
    INCORRECT_INFORMATION("incorrect_information"),
    CLOSED_OR_MISSING("closed_or_missing"),
    DUPLICATE("duplicate"),
    INAPPROPRIATE_CONTENT("inappropriate_content"),
    OTHER("other")
}

enum class ReviewReportReason(val analyticsCode: String) {
    SPAM("spam"),
    OFFENSIVE("offensive"),
    FALSE_INFO("false_info"),
    NOT_RELEVANT("not_relevant"),
    OTHER("other")
}

enum class PhotoReportReason(val analyticsCode: String) {
    INAPPROPRIATE("inappropriate"),
    NOT_RELEVANT("not_relevant"),
    COPYRIGHT("copyright"),
    OFFENSIVE("offensive"),
    OTHER("other")
}

/**
 * 🎯 Odpowiedzialności:
 * - Centralizacja logowania zdarzeń Firebase Analytics.
 * - Ujednolicanie nazw zdarzeń i kluczy parametrów (latwy audyt).
 * - Lokalny debug log zdarzeń do konsoli Timber.
 *
 * 🛡️ Bezpieczeństwo i Prywatność:
 * - Zabrania się przesyłania danych PII (email, nazwisko, komentarze) jako parametrów.
 * - Przesyła wyłącznie techniczne kody zdarzeń i anonimowe identyfikatory zasobów.
 *
 * ⚡ Wydajność i Zasoby:
 * - Korzysta z natywnego mechanizmu Firebase Analytics (batching zdarzeń w tle).
 * - Minimalny wpływ na wydajność wątku głównego.
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
        Timber.d("Analytics: add_place → category=$category")
        analytics.logEvent("add_place") {
            param("place_id", placeId)
            param("category", category)
        }
    }

    fun logViewPlace(placeId: String, placeName: String) {
        Timber.d("Analytics: view_place")
        analytics.logEvent("view_place") {
            param("place_id", placeId)
            param("place_name_length", placeName.length.coerceAtMost(MAX_TEXT_LENGTH_METRIC).toLong())
        }
    }

    fun logDeletePlace(placeId: String) {
        Timber.d("Analytics: delete_place")
        analytics.logEvent("delete_place") {
            param("place_id", placeId)
        }
    }

    fun logReportPlace(reason: PlaceReportReason) {
        Timber.d(
            "Analytics: report_place → reason=${reason.analyticsCode}"
        )

        analytics.logEvent("report_place") {
            param("reason", reason.analyticsCode)
        }
    }

    // ─── Reviews ────────────────────────────────────────────────────────

    fun logAddReview(placeId: String, rating: Float) {
        Timber.d("Analytics: add_review → rating=$rating")
        analytics.logEvent("add_review") {
            param("place_id", placeId)
            param("rating", rating.toDouble())
        }
    }

    fun logDeleteReview(reviewId: String) {
        Timber.d("Analytics: delete_review")
        analytics.logEvent("delete_review") {
            param("review_id", reviewId)
        }
    }

    fun logReportReview(reason: ReviewReportReason) {
        analytics.logEvent("report_review") {
            param("reason", reason.analyticsCode)
        }
    }

    // ─── Photos ─────────────────────────────────────────────────────────

    fun logAddPhoto(source: String) {
        Timber.d("Analytics: add_photo → source=$source")
        analytics.logEvent("add_photo") {
            param("source", source)
        }
    }

    fun logReportPhoto(reason: PhotoReportReason) {
        Timber.d(
            "Analytics: report_photo → reason=${reason.analyticsCode}"
        )

        analytics.logEvent("report_photo") {
            param("reason", reason.analyticsCode)
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
        Timber.d("Analytics: search → queryLength=${query.length}, results=$resultsCount")
        analytics.logEvent(FirebaseAnalytics.Event.SEARCH) {
            param("query_length", query.length.coerceAtMost(MAX_TEXT_LENGTH_METRIC).toLong())
            param("results_count", resultsCount.toLong())
        }
    }

    // ─── User properties ────────────────────────────────────────────────

    fun setUserProperty(key: String, value: String?) {
        val loggedValue = if (value == null) "null" else "[set]"
        Timber.d("Analytics: user_property → $key=$loggedValue")
        analytics.setUserProperty(key, value)
    }

    private companion object {
        const val MAX_TEXT_LENGTH_METRIC = 100
    }
}
