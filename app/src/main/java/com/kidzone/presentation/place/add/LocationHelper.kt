package com.kidzone.presentation.place.add

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

        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    cont.resume(location.latitude to location.longitude)
                } else {
                    // Jesli getCurrentLocation zwrocilo null, probujemy pobrac ostatnia znana lokalizacje
                    client.lastLocation.addOnSuccessListener { lastLoc ->
                        cont.resume(lastLoc?.let { it.latitude to it.longitude })
                    }.addOnFailureListener {
                        cont.resume(null)
                    }
                }
            }
            .addOnFailureListener { e ->
                // W razie bledu getCurrentLocation rowniez probujemy lastLocation jako fallback
                client.lastLocation.addOnSuccessListener { lastLoc ->
                    cont.resume(lastLoc?.let { it.latitude to it.longitude })
                }.addOnFailureListener {
                    cont.resumeWithException(e)
                }
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
 * Składa [Address] do czytelnego formatu adresu.
 *
 * Strategia:
 *  1. Próbujemy `getAddressLine(0)` – Android sam składa to najlepiej,
 *     z lokalizacją po polsku (np. "Aleje Ujazdowskie 4, 00-478 Warszawa, Polska").
 *  2. Jeśli `addressLine(0)` jest puste lub zawiera tylko kod pocztowy
 *     (np. "00-478") – odrzucamy je i składamy ręcznie z części.
 *  3. Ręczne składanie też nie wstawia samego kodu pocztowego bez miasta –
 *     bo "00-478" w polu adresu to bezużyteczne UX.
 *  4. Jak nic sensownego nie da się złożyć – zwracamy `null` i ekran
 *     zachowuje to, co user wpisał ręcznie.
 */
private fun Address.toFormattedString(): String? {
    val fromLine = getAddressLine(0)?.takeIf { line ->
        line.isNotBlank() && !line.trim().matches(Regex("^\\d{2}-\\d{3}$"))
    }
    if (fromLine != null) return fromLine

    val street = listOfNotNull(thoroughfare, subThoroughfare)
        .joinToString(" ")
        .ifBlank { null }
    val city = locality ?: subAdminArea
    val cityPart = when {
        city != null && postalCode != null -> "$postalCode $city"
        city != null -> city
        else -> null // sam kod pocztowy bez miasta – pomijamy
    }
    return listOfNotNull(street, cityPart)
        .joinToString(", ")
        .ifBlank { null }
}
