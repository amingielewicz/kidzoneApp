package com.kidzone.presentation.common

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat

private const val LOCATION_PERMISSION_PREFS =
    "location_permission_preferences"

private const val LOCATION_PERMISSION_REQUESTED_KEY =
    "fine_location_requested"

/**
 * 🎯 Odpowiedzialności:
 * - Inteligentne zarządzanie prośbami o uprawnienia lokalizacji.
 * - Wykrywanie stanu "trwałej odmowy" (permanently denied).
 * - Przekierowywanie użytkownika do ustawień systemowych aplikacji, gdy dialogi systemowe są nieskuteczne.
 *
 * ⚙️ Techniczne:
 * - Śledzi historię zapytań w SharedPreferences (`location_permission_preferences`).
 * - Wykorzystuje `shouldShowRequestPermissionRationale` do detekcji intencji użytkownika.
 *
 * ✅ Gwarancje:
 * - Brak "martwych kliknięć": jeśli systemowy dialog się nie pojawi, otwierane są ustawienia.
 * - Spójna obsługa lokalizacji w całej aplikacji.
 */
fun requestLocationPermissionOrOpenSettings(
    context: Context,
    requestPermission: () -> Unit
) {
    val preferences = context.getSharedPreferences(
        LOCATION_PERMISSION_PREFS,
        Context.MODE_PRIVATE
    )

    val wasRequestedBefore = preferences.getBoolean(
        LOCATION_PERMISSION_REQUESTED_KEY,
        false
    )

    val activity = context.findActivity()

    val permanentlyDenied =
        wasRequestedBefore &&
                activity != null &&
                !ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )

    if (permanentlyDenied) {
        openApplicationSettings(context)
    } else {
        preferences.edit()
            .putBoolean(
                LOCATION_PERMISSION_REQUESTED_KEY,
                true
            )
            .apply()

        requestPermission()
    }
}

fun openApplicationSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts(
            "package",
            context.packageName,
            null
        )
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    context.startActivity(intent)
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
