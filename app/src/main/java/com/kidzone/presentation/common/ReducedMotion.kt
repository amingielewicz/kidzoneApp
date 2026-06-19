package com.kidzone.presentation.common

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Uses Android's global animator scale as the app-level reduced-motion signal.
 * When the user disables system animations, reward-only effects such as
 * confetti are skipped while the success message remains visible.
 */
@Composable
fun rememberReducedMotionEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) { isReducedMotionEnabled(context) }
}

private fun isReducedMotionEnabled(context: Context): Boolean =
    runCatching {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    }.getOrDefault(false)
