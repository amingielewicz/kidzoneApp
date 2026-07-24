package com.kidzone.presentation.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.kidzone.R
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Composable helper zwracający lambda do uruchomienia Google Sign-In
 * w celu reautentykacji (np. przed usunięciem konta).
 *
 * Używa tej samej logiki co LoginScreen: Credential Manager → legacy
 * fallback. Po uzyskaniu tokena wywołuje [onTokenReceived].
 *
 * @param onTokenReceived callback z idToken po pomyślnym Google Sign-In
 * @param onError callback z komunikatem błędu
 * @return lambda do wywołania w onClick / w dialogu
 */
@Composable
fun rememberGoogleSignInLauncher(
    onTokenReceived: (idToken: String) -> Unit,
    onError: (message: String) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val activity = context.findActivity()
    val scope = rememberCoroutineScope()

    val webClientId = try {
        val resId = context.resources.getIdentifier(
            WEB_CLIENT_ID_RES_NAME, "string", context.packageName
        )
        if (resId != 0) context.getString(resId) else ""
    } catch (_: Exception) { "" }

    // Legacy fallback launcher
    val legacyLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            when (val result = parseLegacyGoogleSignInResult(activityResult.data)) {
                is GoogleSignInResult.Success -> onTokenReceived(result.idToken)
                is GoogleSignInResult.Cancelled -> { /* user cancelled */ }
                is GoogleSignInResult.Error -> {
                    Timber.w("Google Reauth Error (legacy): $result")
                    onError(context.getString(R.string.google_sign_in_unavailable))
                }
                else -> onError(context.getString(R.string.error_unknown))
            }
        }
    }

    return {
        val currentActivity = activity
        if (currentActivity == null) {
            onError(context.getString(R.string.google_sign_in_missing_activity))
        } else {
            scope.launch {
                when (val result = launchGoogleSignIn(currentActivity, webClientId)) {
                    is GoogleSignInResult.Success -> onTokenReceived(result.idToken)
                    is GoogleSignInResult.Cancelled -> { /* user cancelled */ }
                    is GoogleSignInResult.FallbackToLegacy -> {
                        val intent = buildLegacyGoogleSignInIntent(context, webClientId)
                        legacyLauncher.launch(intent)
                    }
                    is GoogleSignInResult.Error -> {
                        Timber.w("Google Reauth Error: $result")
                        onError(context.getString(R.string.google_sign_in_unavailable))
                    }
                    is GoogleSignInResult.NoMatchingGoogleCredential -> {
                        val intent = buildLegacyGoogleSignInIntent(context, webClientId)
                        legacyLauncher.launch(intent)
                    }
                }
            }
        }
    }
}
