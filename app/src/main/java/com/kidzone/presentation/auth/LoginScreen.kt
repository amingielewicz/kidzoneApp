package com.kidzone.presentation.auth

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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kidzone.R
import com.kidzone.presentation.common.NetworkStatus
import com.kidzone.presentation.common.NoInternetBanner
import com.kidzone.presentation.common.rememberNetworkStatus
import kotlinx.coroutines.launch

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

    // Do uruchamiania Google Sign-In z poziomu UI (Credential Manager
    // wymaga Activity context).
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Po pomyślnym logowaniu - nawigacja na main.
    LaunchedEffect(state.isSignedIn) {
        if (state.isSignedIn) onLoginSuccess()
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

    val networkStatus by rememberNetworkStatus()

    Scaffold(
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (networkStatus == NetworkStatus.UNAVAILABLE) {
                    NoInternetBanner()
                    Spacer(Modifier.height(12.dp))
                }

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
                            text = "Zaloguj się, by zacząć odkrywać miejsca",
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

                        // Komunikat (błąd lub info, np. po "zapomniałem hasła").
                        state.message?.let { msg ->
                            Spacer(Modifier.height(4.dp))
                            MessageBanner(
                                text = msg,
                                isError = state.isMessageError
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = viewModel::signIn,
                            enabled = !state.isLoading && state.isFormValid,
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
                                        viewModel.showInlineMessage(
                                            "Włącz Google Sign-In w Firebase Console " +
                                                "i pobierz nowy google-services.json do app/"
                                        )
                                        return@launch
                                    }

                                    when (val result = launchGoogleSignIn(context, webClientId)) {
                                        is GoogleSignInResult.Success ->
                                            viewModel.signInWithGoogle(result.idToken)
                                        GoogleSignInResult.Cancelled -> Unit // user anulował
                                        GoogleSignInResult.NoMatchingGoogleCredential ->
                                            viewModel.showInlineMessage(
                                                "Nie udało się znaleźć pasującego konta Google. " +
                                                    "Sprawdź, czy Google Sign-In jest włączony w Firebase, " +
                                                    "SHA-1 aplikacji jest dodany i masz aktualny google-services.json."
                                            )
                                        is GoogleSignInResult.Error ->
                                            viewModel.showInlineMessage(result.message)
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
                        text = "Nie masz jeszcze konta?",
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
        modifier = Modifier.fillMaxWidth()
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
            text = "lub",
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
