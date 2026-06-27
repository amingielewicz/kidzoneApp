package com.kidzone

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.kidzone.analytics.ColdStartTrace
import com.kidzone.i18n.LanguagePreferences
import com.kidzone.i18n.LocaleApplier
import com.kidzone.navigation.KidZoneNavGraph
import com.kidzone.ui.theme.KidZoneTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * Jedyna aktywność aplikacji – host dla całej hierarchii Compose.
 *
 * Adnotacja [AndroidEntryPoint] włącza wstrzykiwanie zależności przez Hilt
 * w aktywności i zagnieżdżonych w niej @HiltViewModel-ach.
 *
 * `installSplashScreen()` przed `super.onCreate()` aktywuje Splash Screen API
 * (Android 12+, backportowane na starsze przez core-splashscreen). System
 * uzywa wtedy `Theme.KidZone.Starting` z themes.xml: tlo `splash_background`
 * (#F5F8FB, jak Compose SplashScreen) i dedykowana mala ikona w srodku
 * (mieszczaca sie w okraglej masce systemu, bez przycinania). Po zaladowaniu
 * Compose theme przelacza sie na `Theme.KidZone` przez `postSplashScreenTheme`.
 *
 * **In-App Update:** Przy każdym starcie sprawdza czy w Google Play jest
 * nowsza wersja. Jeśli tak — uruchamia IMMEDIATE update flow (blokujący,
 * user musi zaakceptować). Gwarantuje że użytkownicy zawsze mają najnowszą
 * wersję. Jeśli update się nie powiedzie / user anuluje — apka działa dalej
 * normalnie, sprawdzenie powtórzy się przy następnym uruchomieniu.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var coldStartTrace: ColdStartTrace

    @Inject
    lateinit var languagePreferences: LanguagePreferences

    private val appUpdateManager by lazy { AppUpdateManagerFactory.create(this) }

    /**
     * Launcher dla update flow – wynik RESULT_OK = update zainstalowany,
     * RESULT_CANCELED = user odrzucił (apka działa dalej).
     */
    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) {
            Timber.w("In-app update cancelled or failed (code=${result.resultCode})")
        }
    }

    /**
     * Listener na stan pobierania – dla IMMEDIATE update nie jest potrzebny
     * (Google sam pokazuje full-screen progress), ale trzymamy na wypadek
     * przyszłego przejścia na FLEXIBLE.
     */
    private val installStateListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            // Dla FLEXIBLE flow: po pobraniu wymusić restart.
            // Przy IMMEDIATE to się nie wykonuje – update jest atomowy.
            appUpdateManager.completeUpdate()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        LocaleApplier.apply(this, languagePreferences.getLanguage())
        enableEdgeToEdge()

        appUpdateManager.registerListener(installStateListener)
        checkForAppUpdate()

        setContent {
            LaunchedEffect(Unit) {
                coldStartTrace.stopAtFirstContent()
            }
            KidZoneTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    KidZoneNavGraph(
                        intent = intent,
                        onLocaleChanged = ::recreate
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Jeśli update był w trakcie (np. user wyszedł z apki podczas
        // pobierania), wznów sprawdzenie – IMMEDIATE update wymaga żeby
        // apka wróciła do flow.
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                appUpdateManager.startUpdateFlowForResult(
                    info,
                    updateLauncher,
                    AppUpdateOptions.defaultOptions(AppUpdateType.IMMEDIATE)
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        appUpdateManager.unregisterListener(installStateListener)
    }

    /**
     * Sprawdza dostępność aktualizacji w Play Store.
     *
     * IMMEDIATE flow = full-screen dialog Google'a, user nie może go ominąć
     * (ale może anulować – apka wtedy działa dalej na starej wersji).
     *
     * Jeśli `isImmediateUpdateAllowed` = false (np. zbyt stara wersja Play
     * Store lub device policy), po prostu nic nie robimy – apka startuje
     * normalnie.
     */
    private fun checkForAppUpdate() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            val isUpdateAvailable =
                info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
            val isAllowed = info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)

            if (isUpdateAvailable && isAllowed) {
                appUpdateManager.startUpdateFlowForResult(
                    info,
                    updateLauncher,
                    AppUpdateOptions.defaultOptions(AppUpdateType.IMMEDIATE)
                )
            }
        }.addOnFailureListener { e ->
            // Brak Play Store / brak sieci / emulator – nie blokujemy startu.
            Timber.w(e, "In-app update check failed")
        }
    }

}
