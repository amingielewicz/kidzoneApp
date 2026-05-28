package com.kidzone.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kidzone.R
import kotlinx.coroutines.launch

/**
 * Ekran logowania - e-mail/haslo + Google + reset hasla.
 *
 * Logika w [LoginViewModel]; tutaj tylko renderowanie stanu i przekazywanie
 * akcji uzytkownika.
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    // Lokalny stan UI - widocznosc hasla. Nie nalezy do ViewModelu, bo to czysto
    // sprawa renderowania, niezalezna od logiki auth.
    var isPasswordVisible by remember { mutableStateOf(false) }

    // Do uruchamiania Google Sign-In z poziomu UI (Credential Manager wymaga Activity context).
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Po pomyslnym logowaniu - nawigacja na main.
    LaunchedEffect(state.isSignedIn) {
        if (state.isSignedIn) onLoginSuccess()
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                label = { RequiredFieldLabel(stringResource(R.string.email)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                label = { RequiredFieldLabel(stringResource(R.string.password)) },
                singleLine = true,
                visualTransformation = if (isPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(
                        onClick = { isPasswordVisible = !isPasswordVisible },
                        enabled = !state.isLoading
                    ) {
                        Icon(
                            imageVector = if (isPasswordVisible) {
                                Icons.Filled.VisibilityOff
                            } else {
                                Icons.Filled.Visibility
                            },
                            contentDescription = if (isPasswordVisible) {
                                "Ukryj hasło"
                            } else {
                                "Pokaż hasło"
                            }
                        )
                    }
                },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            // Komunikat (blad lub info, np. po "zapomnialem hasla").
            state.message?.let { msg ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.isMessageError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.secondary
                    }
                )
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = viewModel::signIn,
                enabled = !state.isLoading && state.isFormValid,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(R.string.login))
                }
            }

            Spacer(Modifier.height(8.dp))
            // Google Sign-In - wymaga: 1) wlaczonego Google providera w Firebase,
            // 2) dodanego SHA-1 debug keystore, 3) aktualnego google-services.json.
            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        // Web Client ID czytamy w runtime, zeby brak konfiguracji
                        // Firebase nie blokowal kompilacji (zasob default_web_client_id
                        // generowany jest dopiero gdy google-services.json ma OAuth web client).
                        val resId = context.resources.getIdentifier(
                            WEB_CLIENT_ID_RES_NAME, "string", context.packageName
                        )
                        val webClientId = if (resId != 0) context.getString(resId) else ""

                        if (webClientId.isBlank()) {
                            viewModel.showInlineMessage(
                                "Włącz Google Sign-In w Firebase Console i pobierz nowy google-services.json do app/"
                            )
                            return@launch
                        }

                        when (val result = launchGoogleSignIn(context, webClientId)) {
                            is GoogleSignInResult.Success ->
                                viewModel.signInWithGoogle(result.idToken)
                            GoogleSignInResult.Cancelled -> Unit // user anulowal - bez komunikatu
                            GoogleSignInResult.NoGoogleAccountOnDevice ->
                                viewModel.showInlineMessage(
                                    "Brak konta Google na urządzeniu. Dodaj konto w Ustawieniach Androida."
                                )
                            is GoogleSignInResult.Error ->
                                viewModel.showInlineMessage(result.message)
                        }
                    }
                },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(stringResource(R.string.login_with_google))
            }

            Spacer(Modifier.height(16.dp))
            TextButton(
                onClick = onNavigateToRegister,
                enabled = !state.isLoading
            ) {
                Text(stringResource(R.string.register))
            }
            TextButton(
                onClick = viewModel::forgotPassword,
                enabled = !state.isLoading
            ) {
                Text(stringResource(R.string.forgot_password))
            }
        }
    }
}

/**
 * Label dla wymaganego pola - tekst + czerwona gwiazdka. Uzywany w
 * OutlinedTextField, gdzie label musi byc Composable.
 */
@Composable
private fun RequiredFieldLabel(text: String) {
    val errorColor = MaterialTheme.colorScheme.error
    Text(
        buildAnnotatedString {
            append(text)
            withStyle(SpanStyle(color = errorColor)) {
                append(" *")
            }
        }
    )
}
