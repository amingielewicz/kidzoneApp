package com.playground.presentation.place.add

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Sprawdza, czy użytkownik nadał aplikacji uprawnienie do lokalizacji
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
 * Pobiera aktualną lokalizację użytkownika przez FusedLocationProviderClient.
 *
 * Zakłada, że uprawnienie [Manifest.permission.ACCESS_FINE_LOCATION] zostało
 * już przyznane (callsite musi to zweryfikować przez [hasLocationPermission]).
 *
 * Zwraca parę (latitude, longitude) lub null, jeśli urządzenie nie ma fixu
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

/**
 * Reverse geocoding – z pary (lat, lng) zwraca adres jako string.
 *
 * Używa wbudowanego [android.location.Geocoder]. Na API 33+ używamy nowego
 * asynchronicznego API z listenerem; na starszych wersjach – zsynchronizowanej
 * (deprecated, ale działa) wersji w wątku I/O.
 *
 * Best-effort – zwraca null gdy:
 *  - urządzenie nie ma usługi geocodera (np. wybrane emulatory bez Google Play)
 *  - brak Internetu (geocoder potrzebuje sieci)
 *  - dla danych współrzędnych nie ma adresu
 */
suspend fun reverseGeocode(
    context: Context,
    latitude: Double,
    longitude: Double
): String? = withContext(Dispatchers.IO) {
    if (!Geocoder.isPresent()) return@withContext null

    val geocoder = Geocoder(context, Locale("pl", "PL"))

    val address: Address? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // Android 13+ – API z listenerem (oficjalne, nieblokujące).
        runCatching {
            suspendCancellableCoroutine<Address?> { cont ->
                geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                    cont.resume(addresses.firstOrNull())
                }
            }
        }.getOrNull()
    } else {
        // Pre-Android 13 – stary, synchroniczny API. Deprecated, ale wciąż działa.
        @Suppress("DEPRECATION")
        runCatching {
            geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
        }.getOrNull()
    }

    address?.toFormattedString()
}

/**
 * Składa [Address] do czytelnego formatu w stylu polskim:
 * "Ulica Numer, KOD-POCZTOWY Miasto". Jeśli z jakiegoś powodu te pola są
 * puste, używamy wbudowanego `getAddressLine(0)` jako fallback.
 */
private fun Address.toFormattedString(): String? {
    val streetPart = listOfNotNull(thoroughfare, subThoroughfare)
        .joinToString(" ")
        .ifBlank { null }
    val cityPart = listOfNotNull(postalCode, locality ?: subAdminArea)
        .joinToString(" ")
        .ifBlank { null }
    val combined = listOfNotNull(streetPart, cityPart)
        .joinToString(", ")
        .ifBlank { null }
    return combined ?: getAddressLine(0)?.takeIf { it.isNotBlank() }
}
