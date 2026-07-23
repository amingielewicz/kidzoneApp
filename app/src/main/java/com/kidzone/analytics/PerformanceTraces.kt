package com.kidzone.analytics

import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import com.kidzone.utils.OpResult
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🎯 Odpowiedzialności:
 * - Centralizacja śledzenia opóźnień (latency) w kluczowych ścieżkach użytkownika.
 * - Monitorowanie wydajności operacji sieciowych i obliczeniowych (GPS, obrazki, Firestore).
 *
 * ⚡ Wydajność i Zasoby:
 * - Minimalny narzut dzięki wykorzystaniu natywnego SDK Firebase Performance.
 * - Przesyła dane analityczne w zoptymalizowanych paczkach w tle.
 *
 * ✅ Gwarancje:
 * - Brak wpływu na logikę biznesową (automatyczna obsługa błędów w blokach pomiarowych).
 * - Spójność nazewnictwa śladów (traces) w całym systemie.
 */
@Singleton
class PerformanceTraces @Inject constructor() {

    private val perf: FirebasePerformance = FirebasePerformance.getInstance()

    /**
     * Rozpoczyna nazwany ślad wydajności.
     *
     * @param name Nazwa śladu (używaj stałych z companion object).
     * @return Uruchomiony [Trace] — wywołujący musi zawołać [stopTrace] po zakończeniu.
     */
    fun startTrace(name: String): Trace {
        Timber.d("Perf trace started: $name")
        return perf.newTrace(name).also { it.start() }
    }

    /**
     * Zatrzymuje działający ślad. Bezpieczne do wielokrotnego wywołania.
     */
    fun stopTrace(trace: Trace) {
        trace.stop()
        Timber.d("Perf trace stopped: ${trace.name}")
    }

    /**
     * Pomocnik inline — mierzy blok typu suspend i zwraca jego wynik.
     *
     * Automatycznie startuje/zatrzymuje ślad i dodaje atrybut sukcesu/błędu.
     *
     * ```kotlin
     * val places = performanceTraces.measure(NEARBY_PLACES_LOAD) {
     *     placeRepository.getPlacesNear(lat, lng, radius)
     * }
     * ```
     */
    suspend fun <T> measure(traceName: String, block: suspend () -> T): T {
        val trace = startTrace(traceName)
        return try {
            val result = block()
            trace.putAttribute("status", "success")
            result
        } catch (e: Exception) {
            trace.putAttribute("status", "error")
            trace.putAttribute("error_type", e.javaClass.simpleName)
            throw e
        } finally {
            stopTrace(trace)
        }
    }

    /**
     * Wariant dla metod repozytorium, które zwracają [OpResult] zamiast rzucać wyjątki.
     */
    suspend fun <T> measureResult(traceName: String, block: suspend () -> OpResult<T>): OpResult<T> {
        val trace = startTrace(traceName)
        return try {
            when (val result = block()) {
                is OpResult.Success -> {
                    trace.putAttribute("status", "success")
                    result
                }
                is OpResult.Failure -> {
                    trace.putAttribute("status", "error")
                    trace.putAttribute("error_type", result.error.javaClass.simpleName)
                    result
                }
            }
        } catch (e: Exception) {
            trace.putAttribute("status", "error")
            trace.putAttribute("error_type", e.javaClass.simpleName)
            throw e
        } finally {
            stopTrace(trace)
        }
    }

    /**
     * Wersja nie-suspend metody [measure] dla operacji synchronicznych.
     */
    fun <T> measureSync(traceName: String, block: () -> T): T {
        val trace = startTrace(traceName)
        return try {
            val result = block()
            trace.putAttribute("status", "success")
            result
        } catch (e: Exception) {
            trace.putAttribute("status", "error")
            trace.putAttribute("error_type", e.javaClass.simpleName)
            throw e
        } finally {
            stopTrace(trace)
        }
    }

    companion object {
        // ─── Nazwy śladów (Traces) ───────────────────────────────────────

        /** Pobieranie lokalizacji GPS/fused (od żądania do współrzędnych). */
        const val LOCATION_FETCH = "location_fetch"

        /** Kompresja obrazu (decode → rotate → resize → WebP). */
        const val IMAGE_COMPRESS = "image_compress"

        /** Wysyłanie zdjęcia do Firebase Storage (bajty → URL). */
        const val PHOTO_UPLOAD = "photo_upload"

        /** Ładowanie pojedynczego miejsca z Firestore. */
        const val PLACE_LOAD = "place_load"

        /** Zapytanie o miejsca w pobliżu (geohash + haversine). */
        const val NEARBY_PLACES_LOAD = "nearby_places_load"

        /** Ładowanie miejsc widocznych w viewporcie mapy. */
        const val MAP_PLACES_LOAD = "map_places_load"

        /** Zapytanie o ranking miejsc. */
        const val TOP_PLACES_LOAD = "top_places_load"

        /** Stronicowane zapytanie listy miejsc (bez wyszukiwania tekstowego). */
        const val PLACES_PAGE_LOAD = "places_page_load"

        /** Zapytanie wyszukiwania miejsc. */
        const val PLACE_SEARCH_LOAD = "place_search_load"

        /** Wysyłanie opinii (walidacja + zapis Firestore). */
        const val REVIEW_SUBMIT = "review_submit"

        /** Proces dodawania miejsca (zapis + upload zdjęć). */
        const val ADD_PLACE = "add_place"

        /** "Zimny start" aplikacji do pierwszego wyrenderowania treści. */
        const val COLD_START = "cold_start"

        /** Pobieranie i aktywacja Firebase Remote Config. */
        const val REMOTE_CONFIG_FETCH = "remote_config_fetch"

        /** Obliczanie odznak i kontekstu rankingu. */
        const val BADGE_COMPUTATION = "badge_computation"
    }
}
