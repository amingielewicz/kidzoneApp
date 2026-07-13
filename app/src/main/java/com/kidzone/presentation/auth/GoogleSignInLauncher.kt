@file:Suppress("DEPRECATION") // Legacy GoogleSignIn API — intentional fallback

package com.kidzone.presentation.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import timber.log.Timber
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

/**
 * Wynik próby logowania przez Google.
 */
sealed class GoogleSignInResult {
    data class Success(val idToken: String) : GoogleSignInResult()
    data object Cancelled : GoogleSignInResult()
    data object NoMatchingGoogleCredential : GoogleSignInResult()
    data class Error(val message: String) : GoogleSignInResult()

    /**
     * Credential Manager zawiódł – UI powinno uruchomić fallback
     * przez legacy GoogleSignIn Intent API.
     */
    data object FallbackToLegacy : GoogleSignInResult()
}

/**
 * Wyciąga [Activity] z [Context] (nawet jeśli to [ContextWrapper]).
 *
 * Credential Manager wymaga Activity-based context do uruchomienia
 * selectora kont. Compose `LocalContext.current` czasem zwraca
 * ContextWrapper (np. w preview, testach, lub na niektórych OEM-ach
 * jak Xiaomi/MIUI), co powoduje crash "Failed to launch the selector UI".
 *
 * @return Activity lub null jeśli kontekst nie jest powiązany z Activity.
 */
fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return if (ctx is Activity) ctx else null
}

/**
 * Próbuje logowanie przez Credential Manager (nowe API, Android 14+).
 * Jeśli Credential Manager zwróci błąd (np. na Xiaomi/MIUI, starszych
 * urządzeniach, emulatorze bez Google Play) – zwraca [GoogleSignInResult.FallbackToLegacy],
 * sygnalizując UI że powinno użyć legacy Intent-based flow.
 *
 * **WAŻNE:** [activityContext] musi być Activity (nie application/service context).
 * Credential Manager wymaga Activity do wyświetlenia selectora kont.
 * Użyj [findActivity] do wyciągnięcia Activity z Compose `LocalContext.current`.
 */
suspend fun launchGoogleSignIn(
    activityContext: Activity,
    webClientId: String
): GoogleSignInResult {
    val credentialManager = CredentialManager.create(activityContext)

    val googleIdOption = GetSignInWithGoogleOption.Builder(webClientId)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    return try {
        val response = credentialManager.getCredential(activityContext, request)
        val credential = response.credential

        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
            GoogleSignInResult.Success(googleCredential.idToken)
        } else {
            GoogleSignInResult.Error("Unexpected credential type: ${credential.type}")
        }
    } catch (e: GetCredentialCancellationException) {
        GoogleSignInResult.Cancelled
    } catch (e: NoCredentialException) {
        // Credential Manager nie znalazł providera – fallback na legacy
        Timber.w(e, "NoCredentialException – falling back to legacy GoogleSignIn")
        GoogleSignInResult.FallbackToLegacy
    } catch (e: GoogleIdTokenParsingException) {
        GoogleSignInResult.Error(e.message ?: "Could not parse Google token")
    } catch (e: GetCredentialException) {
        // Ogólny błąd Credential Manager – fallback na legacy
        Timber.w(e, "GetCredentialException – falling back to legacy GoogleSignIn")
        GoogleSignInResult.FallbackToLegacy
    } catch (e: Exception) {
        Timber.e(e, "Unexpected error in Credential Manager")
        GoogleSignInResult.FallbackToLegacy
    }
}

// --- Legacy Google Sign-In (Intent-based, działa na każdym telefonie) ---
// Celowo używamy deprecated GoogleSignIn / GoogleSignInOptions jako fallback
// dla urządzeń, na których Credential Manager nie działa (Xiaomi/MIUI, stare
// Play Services, emulatory). Suppression jest świadoma — alternatywy brak.

/**
 * Tworzy Intent dla legacy Google Sign-In.
 * UI uruchamia go przez ActivityResultLauncher.
 */
@Suppress("DEPRECATION")
fun buildLegacyGoogleSignInIntent(context: Context, webClientId: String): Intent {
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(webClientId)
        .requestEmail()
        .build()
    val client = GoogleSignIn.getClient(context, gso)
    // Wyloguj poprzednią sesję żeby zawsze pokazać picker kont
    client.signOut()
    return client.signInIntent
}

/**
 * Parsuje wynik z legacy Google Sign-In Intent.
 * Wołane z onActivityResult / ActivityResultCallback.
 */
@Suppress("DEPRECATION")
fun parseLegacyGoogleSignInResult(data: Intent?): GoogleSignInResult {
    return try {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        val account = task.getResult(ApiException::class.java)
        val idToken = account?.idToken
        if (idToken != null) {
            GoogleSignInResult.Success(idToken)
        } else {
            GoogleSignInResult.Error("Could not get a token from the Google account")
        }
    } catch (e: ApiException) {
        when (e.statusCode) {
            12501 -> GoogleSignInResult.Cancelled // user cancelled
            else -> {
                Timber.e(e, "Legacy GoogleSignIn ApiException: ${e.statusCode}")
                GoogleSignInResult.Error("Google sign-in error (code: ${e.statusCode})")
            }
        }
    } catch (e: Exception) {
        GoogleSignInResult.Error(e.message ?: "Unknown Google sign-in error")
    }
}

/** Nazwa zasobu, którą plugin google-services generuje z google-services.json. */
const val WEB_CLIENT_ID_RES_NAME = "default_web_client_id"
