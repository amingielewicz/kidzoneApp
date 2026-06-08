package com.kidzone

import android.app.Application
import com.kidzone.data.local.PlaceDao
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * TTL cache'u miejsca – wpisy starsze niż 7 dni są usuwane przy starcie apki.
 *
 * 7 dni to kompromis: wystarczająco długo żeby offline działało kilka dni
 * bez sieci, ale krótko na tyle żeby nie trzymać mocno stale danych
 * (zamknięte miejsca, zmienione opisy itp.).
 */
private const val CACHE_TTL_MS = 7L * 24 * 60 * 60 * 1000 // 7 dni

/**
 * Klasa [Application] uruchamiająca Hilt jako kontener DI dla całej aplikacji.
 *
 * Przy starcie wykonuje garbage collection na Room cache – usuwa wpisy
 * starsze niż [CACHE_TTL_MS]. Operacja jest lekka (jedno DELETE WHERE)
 * i nie blokuje UI (Dispatchers.IO).
 */
@HiltAndroidApp
class KidZoneApplication : Application() {

    @Inject
    lateinit var placeDao: PlaceDao

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        cleanStaleCache()
        com.kidzone.messaging.KidZoneMessagingService.registerCurrentToken(this)
    }

    private fun cleanStaleCache() {
        appScope.launch {
            val threshold = System.currentTimeMillis() - CACHE_TTL_MS
            placeDao.deleteStale(threshold)
        }
    }
}
