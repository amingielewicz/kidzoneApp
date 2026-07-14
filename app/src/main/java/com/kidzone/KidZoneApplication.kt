package com.kidzone

import android.app.Application
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import com.google.firebase.storage.FirebaseStorage
import com.kidzone.analytics.ColdStartTrace
import com.kidzone.data.local.PlaceDao
import com.kidzone.data.remote.RemoteConfigService
import com.kidzone.experiment.ExperimentManager
import com.kidzone.logging.CrashlyticsTree
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * TTL cache'u miejsca – wpisy starsze niż 7 dni są usuwane przy starcie apki.
 *
 * 7 dni to kompromis: wystarczająco długo żeby offline działało kilka dni
 * bez sieci, ale krótko na tyle żeby nie trzymać mocno stale danych
 * (zamknięte miejsca, zmienione opisy itp.).
 */
private const val CACHE_TTL_MS = 7L * 24 * 60 * 60 * 1000
private const val FIREBASE_EMULATOR_HOST = "10.0.2.2"
private const val AUTH_EMULATOR_PORT = 9099
private const val FIRESTORE_EMULATOR_PORT = 8080
private const val STORAGE_EMULATOR_PORT = 9199

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

    @Inject
    lateinit var remoteConfigService: RemoteConfigService

    @Inject
    lateinit var experimentManager: ExperimentManager

    @Inject
    lateinit var coldStartTrace: ColdStartTrace

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        initTimber()
        configureFirebaseEmulators()
        coldStartTrace.start()
        initDebugTools()
        initAppCheck()
        initRemoteConfig()
        cleanStaleCache()
        com.kidzone.messaging.KidZoneMessagingService.registerCurrentToken(this)
    }

    private fun configureFirebaseEmulators() {
        if (!BuildConfig.DEBUG) return

        FirebaseAuth.getInstance()
            .useEmulator(FIREBASE_EMULATOR_HOST, AUTH_EMULATOR_PORT)

        FirebaseFirestore.getInstance().apply {
            useEmulator(FIREBASE_EMULATOR_HOST, FIRESTORE_EMULATOR_PORT)

            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(
                    MemoryCacheSettings.newBuilder().build()
                )
                .build()
        }

        FirebaseStorage.getInstance()
            .useEmulator(FIREBASE_EMULATOR_HOST, STORAGE_EMULATOR_PORT)

        Timber.d(
            "Firebase emulators configured: " +
                    "Auth=$FIREBASE_EMULATOR_HOST:$AUTH_EMULATOR_PORT, " +
                    "Firestore=$FIREBASE_EMULATOR_HOST:$FIRESTORE_EMULATOR_PORT, " +
                    "Storage=$FIREBASE_EMULATOR_HOST:$STORAGE_EMULATOR_PORT"
        )
    }

    /**
     * Inicjalizacja Firebase App Check:
     *  - Debug: DebugAppCheckProviderFactory (pozwala na testowanie w emulatorze)
     *  - Release: PlayIntegrityAppCheckProviderFactory (produkcyjna weryfikacja)
     *
     * Debug provider wypisuje token w Logcat. Token trzeba dodać w Firebase
     * Console przed włączeniem App Check enforcement dla debug buildów.
     * Szczegóły: docs/app-check.md.
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

    private fun initDebugTools() {
        if (!BuildConfig.DEBUG) return
        runCatching {
            Class.forName("com.kidzone.DebugTools")
                .getMethod("install")
                .invoke(null)
        }.onFailure { error ->
            Timber.d(error, "Debug tools not installed")
        }
    }

    private fun cleanStaleCache() {
        appScope.launch {
            val threshold = System.currentTimeMillis() - CACHE_TTL_MS
            placeDao.deleteStale(threshold)
        }
    }

    /**
     * Delegate Remote Config fetch to [RemoteConfigService] which already
     * handles defaults, settings, and fetchAndActivate in its init/method.
     * Previously this method duplicated the fetch call – now it's a single
     * fire-and-forget delegation.
     */
    private fun initRemoteConfig() {
        appScope.launch {
            remoteConfigService.fetchAndActivate()
            experimentManager.syncAssignments()
        }
    }
}
