package com.kidzone

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Klasa [Application] uruchamiająca Hilt jako kontener DI dla całej aplikacji.
 */
@HiltAndroidApp
class KidZoneApplication : Application()
