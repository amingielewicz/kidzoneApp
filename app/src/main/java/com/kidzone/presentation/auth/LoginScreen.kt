package com.kidzone.presentation.auth

import android.util.Patterns
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import com.kidzone.presentation.common.SystemStatusIcons
import com.kidzone.R
import com.kidzone.presentation.common.NetworkStatus
import com.kidzone.presentation.common.rememberNetworkStatus
import com.kidzone.utils.UiText
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Ekran logowania - e-mail/hasło + Google + reset hasła.
 *
 * Wizualnie:
 *  - subtelny pionowy gradient w tle (od jasnego błękitu do białego),
 *    który echuje brand-blue z palety, ale nie konkuruje z treścią,
 *  - logo brandu w nagłówku (PNG zawierający już nazwę i tagline),
 *  - karta z formularzem (e-mail, hasło) - "uniesiona" względem tła,
 *  - separator z napisem "lub" oddzielający logowanie e-mailem od Google,
 *  - akcje pomocnicze (rejestracja, reset hasła) w stopce.
 *
 * Logika autoryzacji nadal w [LoginViewModel]; tutaj tylko renderowanie
 * stanu i przekazywanie akcji użytkownika.
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    // Lokalny stan UI - widoczność hasła. Nie należy do ViewModelu, bo to
    // czysto sprawa renderowania, niezależna od logiki auth.
    var isPasswordVisible by remember { mutableStateOf(false) }
    val emailFormatInvalid = state.email.isNotBlank() &&
        !Patterns.EMAIL_ADDRESS.matcher(state.email.trim()).matches()

    // Do uruchamiania Google Sign-In z poziomu UI (Credential Manager
    // wymaga Activity context).
    val context = LocalContext.current
    val activity = context.findActivity()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val networkStatus by rememberNetworkStatus()
    val isNetworkAvailable = networkStatus == NetworkStatus.AVAILABLE
    val connectionErrorMessage = stringResource(R.string.error_no_internet)

    // Legacy Google Sign-In launcher (fallback dla Xiaomi/MIUI/emulatorów
    // gdzie Credential Manager nie działa)
    val legacyGoogleSignInLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { activityResult ->
        val result = parseLegacyGoogleSignInResult(activityResult.data)
        when (result) {
            is GoogleSignInResult.Success -> viewModel.signInWithGoogle(result.idToken)
            GoogleSignInResult.Cancelled -> Unit
            is GoogleSignInResult.Error -> {
                val technical = when(result) {
                    is GoogleSignInResult.Error.ConfigurationError -> result.technicalMessage
                    is GoogleSignInResult.Error.TokenError -> result.technicalMessage
                    is GoogleSignInResult.Error.ServiceError -> "(${result.code}) ${result.technicalMessage}"
                    is GoogleSignInResult.Error.UnknownError -> result.technicalMessage
                }
                Timber.w("Google Sign-In Error (legacy): $technical")
                viewModel.showErrorMessage(UiText.StringResource(R.string.google_sign_in_unavailable))
            }
            else -> Unit
        }
    }

    // Po pomyślnym logowaniu - nawigacja na main.
    LaunchedEffect(state.isSignedIn) {
        if (state.isSignedIn) onLoginSuccess()
    }

    LaunchedEffect(state.message, state.isMessageError) {
        val message = state.message
        if (message != null && state.isMessageError) {
            snackbarHostState.showSnackbar(message.asString(context))
            viewModel.consumeMessage()
        }
    }

    // Kolory tła gradient są celowo oparte na #F5F8FB (jak SplashScreen) +
    // czysty biały - dzięki temu przejście Splash -> Login jest płynne,
    // bez "klatki" o innym tonie.
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFF5F8FB),
            Color(0xFFFFFFFF)
        )
    )

    fun showConnectionError() {
        coroutineScope.launch {
            snackbarHostState.showSnackbar(connectionErrorMessage)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(padding)
        ) {
            SystemStatusIcons(
                isNetworkAvailable = isNetworkAvailable,
                isLocationAvailable = true,
                onNetworkClick = ::showConnectionError,
                onLocationClick = {},
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 16.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                Spacer(Modifier.height(24.dp))

                // Logo brandu - ten sam asset, którego używa SplashScreen.
                // fillMaxWidth(0.6f) zamiast sztywnego dp - logo z napisem
                // skaluje się procentowo przy różnych szerokościach ekranu.
                Image(
                    painter = painterResource(R.drawable.ic_splash_logo),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .widthIn(max = 280.dp)
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.app_tagline),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )

                Spacer(Modifier.height(24.dp))

                // Karta z formularzem - lekka elevacja na jasnym tle, żeby
                // wizualnie odseparować pola od gradientu.
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 480.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.login),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.login_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )

                        Spacer(Modifier.height(20.dp))

                        OutlinedTextField(
                            value = state.email,
                            onValueChange = viewModel::onEmailChange,
                            label = { RequiredFieldLabel(stringResource(R.string.email)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Email,
                                    contentDescription = null
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            isError = emailFormatInvalid,
                            supportingText = {
                                when {
                                    state.email.isBlank() -> Text(stringResource(R.string.field_required))
                                    emailFormatInvalid -> Text(stringResource(R.string.invalid_email))
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            enabled = !state.isLoading,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = state.password,
                            onValueChange = viewModel::onPasswordChange,
                            label = { RequiredFieldLabel(stringResource(R.string.password)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = null
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            visualTransformation = if (isPasswordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            supportingText = {
                                if (state.password.isBlank()) {
                                    Text(stringResource(R.string.field_required))
                                }
                            },
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
                                            stringResource(R.string.hide_password)
                                        } else {
                                            stringResource(R.string.show_password)
                                        }
                                    )
                                }
                            },
                            enabled = !state.isLoading,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // "Nie pamiętam hasła" - umieszczone tuż pod polem
                        // hasła i wyrównane do prawej, jak w typowych
                        // formularzach logowania.
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            TextButton(
                                onClick = viewModel::forgotPassword,
                                enabled = !state.isLoading,
                                contentPadding = PaddingValues(
                                    horizontal = 4.dp, vertical = 4.dp
                                )
                            ) {
                                Text(
                                    text = stringResource(R.string.forgot_password),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        // Komunikat informacyjny (np. po "zapomniałem hasła") lub błąd blokady
                        val infoMessage = state.message?.takeIf { !state.isMessageError }
                        val banMessage = state.banMessage
                        
                        if (infoMessage != null || banMessage != null) {
                            Spacer(Modifier.height(4.dp))
                            MessageBanner(
                                text = (infoMessage ?: banMessage)!!.asString(),
                                isError = banMessage != null
                            )
                        }

                        // Szczegółowy powód blokady
                        val banReason = state.banReason
                        if (banMessage != null && banReason != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = banReason.asString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }

                        // Przycisk "Wyślij ponownie" link weryfikacyjny
                        if (state.showResendVerification) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = viewModel::resendVerificationEmail,
                                enabled = !state.isLoading,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.resend_verification_link))
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (isNetworkAvailable) {
                                    viewModel.signIn()
                                } else {
                                    viewModel.showConnectionError()
                                }
                            },
                            enabled = !state.isLoading && state.isFormValid && !emailFormatInvalid,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.login),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Separator "lub" oddzielający logowanie e-mailem
                        // od logowania Google.
                        OrDivider()

                        Spacer(Modifier.height(16.dp))

                        // Google Sign-In - wymaga: 1) włączonego Google providera
                        // w Firebase, 2) dodanego SHA-1 debug keystore,
                        // 3) aktualnego google-services.json.
                        OutlinedButton(
                            onClick = {
                                if (!isNetworkAvailable) {
                                    viewModel.showConnectionError()
                                    return@OutlinedButton
                                }
                                coroutineScope.launch {
                                    // Web Client ID czytamy w runtime, żeby brak
                                    // konfiguracji Firebase nie blokował kompilacji
                                    // (zasób default_web_client_id generowany jest
                                    // dopiero gdy google-services.json ma OAuth
                                    // web client).
                                    val resId = context.resources.getIdentifier(
                                        WEB_CLIENT_ID_RES_NAME, "string", context.packageName
                                    )
                                    val webClientId = if (resId != 0) {
                                        context.getString(resId)
                                    } else {
                                        ""
                                    }

                                    if (webClientId.isBlank()) {
                                        Timber.e("Google Sign-In Error: Web Client ID is missing in strings.xml")
                                        viewModel.showErrorMessage(
                                            UiText.StringResource(R.string.google_sign_in_not_configured)
                                        )
                                        return@launch
                                    }

                                    when (val result = launchGoogleSignIn(
                                        activity ?: run {
                                            Timber.e("Google Sign-In Error: Activity context is missing")
                                            viewModel.showErrorMessage(
                                                UiText.StringResource(R.string.google_sign_in_missing_activity)
                                            )
                                            return@launch
                                        },
                                        webClientId
                                    )) {
                                        is GoogleSignInResult.Success ->
                                            viewModel.signInWithGoogle(result.idToken)
                                        GoogleSignInResult.Cancelled -> Unit // user anulował
                                        GoogleSignInResult.NoMatchingGoogleCredential,
                                        GoogleSignInResult.FallbackToLegacy -> {
                                            // Credential Manager nie działa (Xiaomi/MIUI/emulator)
                                            // – uruchamiamy legacy Google Sign-In Intent
                                            val intent = buildLegacyGoogleSignInIntent(context, webClientId)
                                            legacyGoogleSignInLauncher.launch(intent)
                                        }
                                        is GoogleSignInResult.Error -> {
                                            val details = when (result) {
                                                is GoogleSignInResult.Error.ConfigurationError ->
                                                    "Config: ${result.technicalMessage}"

                                                is GoogleSignInResult.Error.TokenError ->
                                                    "Token: ${result.technicalMessage}"

                                                is GoogleSignInResult.Error.ServiceError ->
                                                    "Service (${result.code}): ${result.technicalMessage}"

                                                is GoogleSignInResult.Error.UnknownError ->
                                                    "Unknown: ${result.technicalMessage}"
                                            }
                                            Timber.w("Google Sign-In Error: $details")
                                            viewModel.showErrorMessage(
                                                UiText.StringResource(R.string.google_sign_in_unavailable)
                                            )
                                        }
                                    }
                                }
                            },
                            enabled = !state.isLoading,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            // Logo "G" Google - prosta wersja kółka z literą.
                            // Świadomie nie używamy oficjalnego SVG Google
                            // (licencja Google brand assets wymaga osobnego
                            // approvalu); to placeholder w brand-blue.
                            GoogleGlyph()
                            Spacer(Modifier.size(12.dp))
                            Text(
                                text = stringResource(R.string.login_with_google),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Stopka - zachęta do rejestracji.
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.login_no_account),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                    TextButton(
                        onClick = onNavigateToRegister,
                        enabled = !state.isLoading
                    ) {
                        Text(
                            text = stringResource(R.string.register),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Label dla wymaganego pola - tekst + czerwona gwiazdka. Używany w
 * OutlinedTextField, gdzie label musi być Composable.
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

/**
 * Banner komunikatu (błąd lub info). Tło tonalne z palety błędu /
 * secondary, żeby był wyraźnie widoczny w karcie, ale nie krzyczący.
 */
@Composable
private fun MessageBanner(text: String, isError: Boolean) {
    val containerColor = if (isError) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = if (isError) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                liveRegion = if (isError) {
                    LiveRegionMode.Assertive
                } else {
                    LiveRegionMode.Polite
                }
            }
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

/**
 * Pozioma linia z napisem "lub" w środku - oddziela logowanie e-mailem
 * od opcji "Zaloguj się przez Google".
 */
@Composable
private fun OrDivider() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(R.string.auth_divider_or),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}

/**
 * Mini-glyph "G" w kolorze brand-blue. Placeholder pod oficjalne logo
 * Google (Google Brand Guidelines ograniczają wykorzystanie ich SVG bez
 * brand approvalu - na MVP zostawiamy tę wersję, w produkcji wymienić
 * na materiał z Google Identity).
 */
@Composable
private fun GoogleGlyph() {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "G",
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}
