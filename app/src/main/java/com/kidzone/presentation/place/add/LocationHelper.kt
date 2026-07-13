package com.kidzone.presentation.place.add

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Maks. czas oczekiwania na fix GPS / fused location (ms).
 *
 * 15 sekund to świadomy kompromis: krótszy niż wewnętrzne timeouty Google
 * Play Services (ok. 30 s) i wystarczająco długi, żeby normalny user na
 * świeżym uruchomieniu apki nadążył (FusedLocationProviderClient zwykle
 * dostarcza pierwszy fix w 1-5 s, gorzej tylko gdy GPS jest "zimny").
 *
 * Po przekroczeniu [fetchCurrentLocation] zwraca null - callsite tłumaczy
 * to na user-friendly komunikat "Problem z ustaleniem lokalizacji.
 * Spróbuj później." (zob. AddPlaceScreen.fetchAndSetLocation).
 */
private const val LOCATION_TIMEOUT_MS = 15_000L

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
 * Sprawdza, czy usługa lokalizacji (GPS / Network) jest włączona w systemie.
 *
 * Odróżnia to od [hasLocationPermission]:
 *  - `hasLocationPermission` = czy apka ma uprawnienie do używania lokalizacji
 *  - `isLocationServiceEnabled` = czy telefon w ogóle ma włączony GPS/lokalizację
 *
 * Użycie: przed próbą pobrania GPS sprawdź obie funkcje. Jeśli usługa
 * wyłączona — pokaż dialog zachęcający do włączenia GPS.
 */
fun isLocationServiceEnabled(context: Context): Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
}

/**
 * Pobiera aktualną lokalizację użytkownika przez FusedLocationProviderClient.
 *
 * Zakłada, że uprawnienie [Manifest.permission.ACCESS_FINE_LOCATION] zostało
 * już przyznane (callsite musi to zweryfikować przez [hasLocationPermission]).
 *
 * Zwraca parę (latitude, longitude) lub `null`, jeśli:
 *  - urządzenie nie ma fixu (np. emulator bez ustawionej lokalizacji),
 *  - operacja przekroczyła [LOCATION_TIMEOUT_MS] - GPS w "zimnym" stanie
 *    bywa wolny, a my nie chcemy zostawiać usera na bezterminowym spinnerze.
 */
@SuppressLint("MissingPermission")
suspend fun fetchCurrentLocation(context: Context): Pair<Double, Double>? {
    // Sprawdzenie czy usługa lokalizacji jest włączona — bez tego
    // FusedLocationClient i tak zwróci timeout, ale komunikat będzie
    // jaśniejszy ("Lokalizacja wyłączona" zamiast "Problem z ustaleniem").
    if (!hasLocationPermission(context) || !isLocationServiceEnabled(context)) return null

    return withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
        suspendCancellableCoroutine<Pair<Double, Double>?> { cont ->
            val client = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()

            fun resumeLocation(location: Pair<Double, Double>?) {
                if (cont.isActive) {
                    cont.resume(location)
                }
            }

            fun fetchLastLocation() {
                runCatching {
                    client.lastLocation
                        .addOnSuccessListener { lastLoc ->
                            resumeLocation(lastLoc?.let { it.latitude to it.longitude })
                        }
                        .addOnFailureListener {
                            resumeLocation(null)
                        }
                }.onFailure {
                    resumeLocation(null)
                }
            }

            runCatching {
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            resumeLocation(location.latitude to location.longitude)
                        } else {
                            fetchLastLocation()
                        }
                    }
                    .addOnFailureListener {
                        fetchLastLocation()
                    }
            }.onFailure {
                resumeLocation(null)
            }

            cont.invokeOnCancellation { cts.cancel() }
        }
    }
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

    address?.let { com.kidzone.utils.AddressUtils.fromAndroidAddress(it) }
}
