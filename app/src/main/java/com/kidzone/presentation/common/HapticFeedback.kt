package com.kidzone.presentation.common

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView

/**
 * 🎯 Odpowiedzialności:
 * - Dostarczanie ustandaryzowanych wzorców wibracji (haptic feedback) dla kluczowych akcji UX.
 * - Mapowanie scenariuszy (sukces, błąd, nagroda) na natywne stałe [HapticFeedbackConstants].
 *
 * ⚙️ Techniczne:
 * - Obsługuje fallbacki dla starszych wersji systemu Android (pre-API 30).
 * - Działa w oparciu o bieżący [View] z kompozycji Jetpack Compose.
 *
 * ✅ Gwarancje:
 * - Brak opóźnień (wibracje wyzwalane natychmiastowo).
 * - Spójność haptyczna w całej aplikacji (te same wibracje dla tych samych typów zdarzeń).
 */
class KidZoneHaptic(private val view: View) {

    /** Krótka wibracja sukcesu (dodanie miejsca, zdobycie odznaki) */
    fun success() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    /** Delikatne kliknięcie (reakcja na tap) */
    fun click() {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    /** Ostrzegawcza wibracja (błąd, odrzucenie) */
    fun error() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    /** Nagroda / achievement (odznaka, TOP ranking) */
    fun reward() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }
}

/**
 * Remember a [KidZoneHaptic] instance scoped to the current composable.
 *
 * Usage examples:
 *
 * 1. AddPlaceScreen – after successful place submission:
 *    ```
 *    val haptic = rememberHapticFeedback()
 *    LaunchedEffect(placeAdded) { if (placeAdded) haptic.success() }
 *    ```
 *
 * 2. ProfileScreen – when new badge earned:
 *    ```
 *    val haptic = rememberHapticFeedback()
 *    LaunchedEffect(newBadges) { if (newBadges.isNotEmpty()) haptic.reward() }
 *    ```
 *
 * 3. AddReviewSheet – after successful review:
 *    ```
 *    val haptic = rememberHapticFeedback()
 *    LaunchedEffect(reviewSubmitted) { if (reviewSubmitted) haptic.success() }
 *    ```
 *
 * 4. Any button with tactile feedback:
 *    ```
 *    val haptic = rememberHapticFeedback()
 *    Button(onClick = { haptic.click(); navigateNext() }) { Text("Dalej") }
 *    ```
 */
@Composable
fun rememberHapticFeedback(): KidZoneHaptic {
    val view = LocalView.current
    return KidZoneHaptic(view)
}
