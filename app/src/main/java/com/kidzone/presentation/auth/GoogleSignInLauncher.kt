package com.kidzone.presentation.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
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

private const val TAG = "GoogleSignInLauncher"

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
 * Próbuje logowanie przez Credential Manager (nowe API, Android 14+).
 * Jeśli Credential Manager zwróci błąd (np. na Xiaomi/MIUI, starszych
 * urządzeniach, emulatorze bez Google Play) – zwraca [GoogleSignInResult.FallbackToLegacy],
 * sygnalizując UI że powinno użyć legacy Intent-based flow.
 */
suspend fun launchGoogleSignIn(
    context: Context,
    webClientId: String
): GoogleSignInResult {
    val credentialManager = CredentialManager.create(context)

    val googleIdOption = GetSignInWithGoogleOption.Builder(webClientId)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    return try {
        val response = credentialManager.getCredential(context, request)
        val credential = response.credential

        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
            GoogleSignInResult.Success(googleCredential.idToken)
        } else {
            GoogleSignInResult.Error("Nieoczekiwany typ credencjala: ${credential.type}")
        }
    } catch (e: GetCredentialCancellationException) {
        GoogleSignInResult.Cancelled
    } catch (e: NoCredentialException) {
        // Credential Manager nie znalazł providera – fallback na legacy
        Log.w(TAG, "NoCredentialException – falling back to legacy GoogleSignIn", e)
        GoogleSignInResult.FallbackToLegacy
    } catch (e: GoogleIdTokenParsingException) {
        GoogleSignInResult.Error(e.message ?: "Błąd parsowania tokena Google")
    } catch (e: GetCredentialException) {
        // Ogólny błąd Credential Manager – fallback na legacy
        Log.w(TAG, "GetCredentialException – falling back to legacy GoogleSignIn", e)
        GoogleSignInResult.FallbackToLegacy
    } catch (e: Exception) {
        Log.e(TAG, "Unexpected error in Credential Manager", e)
        GoogleSignInResult.FallbackToLegacy
    }
}

// --- Legacy Google Sign-In (Intent-based, działa na każdym telefonie) ---

/**
 * Tworzy Intent dla legacy Google Sign-In.
 * UI uruchamia go przez ActivityResultLauncher.
 */
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
fun parseLegacyGoogleSignInResult(data: Intent?): GoogleSignInResult {
    return try {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        val account = task.getResult(ApiException::class.java)
        val idToken = account?.idToken
        if (idToken != null) {
            GoogleSignInResult.Success(idToken)
        } else {
            GoogleSignInResult.Error("Nie udało się pobrać tokena z konta Google")
        }
    } catch (e: ApiException) {
        when (e.statusCode) {
            12501 -> GoogleSignInResult.Cancelled // user cancelled
            else -> {
                Log.e(TAG, "Legacy GoogleSignIn ApiException: ${e.statusCode}", e)
                GoogleSignInResult.Error("Błąd logowania Google (kod: ${e.statusCode})")
            }
        }
    } catch (e: Exception) {
        GoogleSignInResult.Error(e.message ?: "Nieznany błąd logowania Google")
    }
}

/** Nazwa zasobu, którą plugin google-services generuje z google-services.json. */
const val WEB_CLIENT_ID_RES_NAME = "default_web_client_id"
