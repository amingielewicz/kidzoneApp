package com.kidzone.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.kidzone.utils.DistanceUtils

/**
 * 🎯 Odpowiedzialności:
 * - Standaryzacja wyświetlania dystansów w interfejsie użytkownika Compose.
 */
@Composable
fun formatDistance(
    km: Double,
    staleLocationAgeMinutes: Int? = null
): String {
    val context = LocalContext.current
    val distance = DistanceUtils.formatDistanceMeters(context, (km * METERS_IN_KILOMETER).toInt())
    return staleLocationAgeMinutes?.let { "$distance (${staleAgeLabel(it)})" } ?: distance
}

private const val METERS_IN_KILOMETER = 1000

@Composable
fun staleAgeLabel(ageMinutes: Int): String {
    val context = LocalContext.current
    return DistanceUtils.formatStaleAge(context, ageMinutes)
}
