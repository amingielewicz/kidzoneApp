package com.kidzone

import android.app.Application
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.kidzone.data.local.PlaceDao
import com.kidzone.logging.CrashlyticsTree
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
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
        initTimber()
        initAppCheck()
        initRemoteConfig()
        cleanStaleCache()
        com.kidzone.messaging.KidZoneMessagingService.registerCurrentToken(this)
    }

    /**
     * Inicjalizacja Firebase App Check:
     *  - Debug: DebugAppCheckProviderFactory (pozwala na testowanie w emulatorze)
     *  - Release: PlayIntegrityAppCheckProviderFactory (produkcyjna weryfikacja)
     */
    private fun initAppCheck() {
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        if (BuildConfig.DEBUG) {
            try {
                val clazz = Class.forName("com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory")
                val factory = clazz.getMethod("getInstance").invoke(null)
                firebaseAppCheck.installAppCheckProviderFactory(
                    factory as com.google.firebase.appcheck.AppCheckProviderFactory
                )
            } catch (_: Exception) {
                firebaseAppCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                )
            }
        } else {
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }
        Timber.d("Firebase App Check initialized (debug=${BuildConfig.DEBUG})")
    }


    /**
     * Inicjalizacja Timber:
     *  - Debug: DebugTree (pelen Logcat output z tagiem = nazwa klasy)
     *  - Release: CrashlyticsTree (WARN+ → Crashlytics breadcrumbs)
     */
    private fun initTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(CrashlyticsTree())
        }
        Timber.d("Timber initialized (debug=${BuildConfig.DEBUG})")
    }

    private fun cleanStaleCache() {
        appScope.launch {
            val threshold = System.currentTimeMillis() - CACHE_TTL_MS
            placeDao.deleteStale(threshold)
        }
    }

    /**
     * Fire-and-forget fetch Remote Config przy starcie aplikacji.
     * Jeśli fetch się nie uda – używamy cached/default values.
     */
    private fun initRemoteConfig() {
        appScope.launch {
            try {
                FirebaseRemoteConfig.getInstance().fetchAndActivate().await()
                Timber.d("Remote Config activated")
            } catch (e: Exception) {
                Timber.w(e, "Remote Config fetch failed")
            }
        }
    }
}
