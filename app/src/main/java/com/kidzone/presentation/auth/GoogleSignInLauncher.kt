package com.kidzone.presentation.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

/**
 * Wynik proby logowania przez Google. Domain-friendly typ, ktory ekran moze
 * zmappowac na komunikat dla uzytkownika.
 */
sealed class GoogleSignInResult {
    /** Logowanie sie powiodlo - mozna podac [idToken] do FirebaseAuth. */
    data class Success(val idToken: String) : GoogleSignInResult()

    /** Uzytkownik anulowal dialog wyboru konta. Nie pokazujemy bledu. */
    data object Cancelled : GoogleSignInResult()

    /** Na urzadzeniu nie ma zadnego konta Google (lub nie zaakceptowane). */
    data object NoGoogleAccountOnDevice : GoogleSignInResult()

    /** Pozostale bledy - pokazujemy [message] w UI. */
    data class Error(val message: String) : GoogleSignInResult()
}

/**
 * Uruchamia natywny dialog Credential Manager z opcja "Zaloguj sie przez
 * Google". Zwraca idToken, ktory mozna podac do FirebaseAuth.signInWithCredential.
 *
 * Wymaga:
 *  - skonfigurowanego Google Sign-In w Firebase Console (zob. [WEB_CLIENT_ID_RES_NAME]),
 *  - dodanego SHA-1 fingerprint debug keystore w Firebase Console,
 *  - aktualnego google-services.json w app/, w ktorym Firebase wygenerowal
 *    OAuth client typu 3 (Web) - jego wartosc dostajemy przez
 *    [R.string.default_web_client_id] generowane przez plugin google-services.
 *
 * @param context kontekst Activity (np. [androidx.compose.ui.platform.LocalContext])
 * @param webClientId Web OAuth Client ID z google-services.json
 */
suspend fun launchGoogleSignIn(
    context: Context,
    webClientId: String
): GoogleSignInResult {
    val credentialManager = CredentialManager.create(context)

    val googleIdOption = GetGoogleIdOption.Builder()
        // false -> pokaze wszystkie konta na urzadzeniu (przy pierwszym logowaniu).
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(webClientId)
        .setAutoSelectEnabled(false)
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
        GoogleSignInResult.NoGoogleAccountOnDevice
    } catch (e: GoogleIdTokenParsingException) {
        GoogleSignInResult.Error(e.message ?: "Blad parsowania tokena Google")
    } catch (e: GetCredentialException) {
        GoogleSignInResult.Error(e.message ?: "Blad logowania przez Google")
    }
}

/** Nazwa zasobu, ktora plugin google-services generuje z google-services.json. */
const val WEB_CLIENT_ID_RES_NAME = "default_web_client_id"
