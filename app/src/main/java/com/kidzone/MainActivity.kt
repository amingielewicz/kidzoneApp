package com.kidzone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.kidzone.navigation.KidZoneNavGraph
import com.kidzone.ui.theme.KidZoneTheme
import dagger.hilt.android.AndroidEntryPoint

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
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KidZoneTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    KidZoneNavGraph()
                }
            }
        }
    }
}
