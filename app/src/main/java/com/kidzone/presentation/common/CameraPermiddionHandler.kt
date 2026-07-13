package com.kidzone.presentation.common

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.core.app.ActivityCompat

private const val CAMERA_PERMISSION_PREFS =
    "camera_permission_preferences"

private const val CAMERA_PERMISSION_REQUESTED_KEY =
    "camera_permission_requested"

fun requestCameraPermissionOrOpenSettings(
    context: Context,
    requestPermission: () -> Unit
) {
    val preferences = context.getSharedPreferences(
        CAMERA_PERMISSION_PREFS,
        Context.MODE_PRIVATE
    )

    val wasRequestedBefore = preferences.getBoolean(
        CAMERA_PERMISSION_REQUESTED_KEY,
        false
    )

    val activity = context.findActivityForCameraPermission()

    val permanentlyDenied =
        wasRequestedBefore &&
                activity != null &&
                !ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.CAMERA
                )

    if (permanentlyDenied) {
        openApplicationSettings(context)
    } else {
        preferences.edit()
            .putBoolean(
                CAMERA_PERMISSION_REQUESTED_KEY,
                true
            )
            .apply()

        requestPermission()
    }
}

private tailrec fun Context.findActivityForCameraPermission(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivityForCameraPermission()
        else -> null
    }