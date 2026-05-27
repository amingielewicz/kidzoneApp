package com.playground.presentation.place.add

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Sprawdza, czy uzytkownik nadal aplikacji uprawnienie do lokalizacji
 * (`ACCESS_FINE_LOCATION` lub `ACCESS_COARSE_LOCATION`).
 */
fun hasLocationPermission(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
}

/**
 * Pobiera aktualna lokalizacje uzytkownika przez FusedLocationProviderClient.
 *
 * Zaklada, ze uprawnienie [Manifest.permission.ACCESS_FINE_LOCATION] zostalo
 * juz przyznane (callsite musi to zweryfikowac przez [hasLocationPermission]).
 *
 * Zwraca pare (latitude, longitude) lub null, jesli urzadzenie nie ma fixu
 * (np. emulator bez ustawionej lokalizacji).
 */
@SuppressLint("MissingPermission")
suspend fun fetchCurrentLocation(context: Context): Pair<Double, Double>? =
    suspendCancellableCoroutine { cont ->
        val client = LocationServices.getFusedLocationProviderClient(context)
        val cts = CancellationTokenSource()

        client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
            .addOnSuccessListener { location ->
                cont.resume(location?.let { it.latitude to it.longitude })
            }
            .addOnFailureListener { e ->
                cont.resumeWithException(e)
            }

        cont.invokeOnCancellation { cts.cancel() }
    }
