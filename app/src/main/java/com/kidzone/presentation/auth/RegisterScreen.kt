package com.kidzone.presentation.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.kidzone.utils.PasswordPolicy

/**
 * Ekran rejestracji - nazwa, e-mail, hasło zgodne z [PasswordPolicy].
 *
 * Layout dopasowany do [LoginScreen]: gradient tła, logo brandu w nagłówku,
 * karta z formularzem, leading-iconki w polach. Pod polem hasła pokazujemy
 * checklist wymagań, dzięki któremu user widzi w czasie rzeczywistym, co
 * jeszcze musi zrobić.
 *
 * Tytuł karty "Stwórz konto" + subtitle "Dołącz do społeczności kidZone".
 * Stopka pod kartą - skrót "Masz już konto? Zaloguj się" prowadzi z
 * powrotem do LoginScreen przez [onBack] (Navigation popBackStack).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    var isPasswordVisible by remember { mutableStateOf(false) }
    val networkStatus by rememberNetworkStatus()

    LaunchedEffect(state.isRegistered) {
        // Nie nawigujemy od razu – pokazujemy komunikat o weryfikacji emaila.
        // User musi sam kliknąć "Przejdź do logowania" po przeczytaniu.
    }

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFF5F8FB),
            Color(0xFFFFFFFF)
        )
    )

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { /* tytuł świadomie pusty - hierarchia w karcie */ },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
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
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (networkStatus == NetworkStatus.UNAVAILABLE) {
                    NoInternetBanner()
                    Spacer(Modifier.height(8.dp))
                }

                // Logo brandu - mniejsze niż na LoginScreen, bo ekran ma
                // jeszcze TopAppBar i nagłówek karty pod spodem. fillMaxWidth(0.4f)
                // daje proporcję ~120-180dp na typowych telefonach.
                Image(
                    painter = painterResource(R.drawable.ic_splash_logo),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .widthIn(max = 180.dp)
                )

                Spacer(Modifier.height(8.dp))

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
                            text = "Stwórz konto",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Dołącz do społeczności kidZone i zacznij odkrywać miejsca przyjazne dzieciom",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )

                        Spacer(Modifier.height(20.dp))

                        OutlinedTextField(
                            value = state.name,
                            onValueChange = viewModel::onNameChange,
                            label = { RequiredFieldLabel("Nazwa użytkownika") },
                            leadingIcon = {
                                Icon(Icons.Filled.Person, contentDescription = null)
                            },
                            trailingIcon = {
                                if (state.name.isNotEmpty() && state.isNameValid) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            isError = state.name.isNotEmpty() && !state.isNameValid,
                            supportingText = {
                                Text(
                                    text = "Widoczna w opiniach, miejscach i rankingu",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            },
                            enabled = !state.isLoading,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = state.email,
                            onValueChange = viewModel::onEmailChange,
                            label = { RequiredFieldLabel(stringResource(R.string.email)) },
                            leadingIcon = {
                                Icon(Icons.Filled.Email, contentDescription = null)
                            },
                            trailingIcon = {
                                if (state.email.isNotEmpty() && state.isEmailValid) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            isError = state.email.isNotEmpty() && !state.isEmailValid,
                            supportingText = {
                                if (state.email.isNotEmpty() && !state.isEmailValid) {
                                    Text("Niepoprawny format e-maila")
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            enabled = !state.isLoading,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = state.password,
                            onValueChange = viewModel::onPasswordChange,
                            label = { RequiredFieldLabel(stringResource(R.string.password)) },
                            leadingIcon = {
                                Icon(Icons.Filled.Lock, contentDescription = null)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            isError = state.password.isNotEmpty() && !state.isPasswordValid,
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

                        // Checklist wymagań hasła. Pojawia się dopiero po
                        // wpisaniu pierwszego znaku, żeby pusty formularz
                        // nie wyglądał na "obstawiony" wymaganiami.
                        if (state.password.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            PasswordRequirementsChecklist(password = state.password)
                        }

                        state.errorMessage?.let { msg ->
                            Spacer(Modifier.height(12.dp))
                            ErrorMessageBanner(text = msg)
                        }

                        // Po rejestracji: komunikat o weryfikacji emaila
                        if (state.isRegistered) {
                            Spacer(Modifier.height(16.dp))
                            androidx.compose.material3.Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Email,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        text = "Konto utworzone!",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = "Wysłaliśmy link weryfikacyjny na podany adres e-mail. " +
                                            "Kliknij link w wiadomości, aby potwierdzić konto i móc się zalogować.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    Button(
                                        onClick = onRegisterSuccess,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Przejdź do logowania")
                                    }
                                }
                            }
                        }

                        if (!state.isRegistered) {
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = viewModel::register,
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
                                    text = stringResource(R.string.register),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Stopka - powrót do logowania.
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Masz już konto?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                    TextButton(
                        onClick = onBack,
                        enabled = !state.isLoading
                    ) {
                        Text(
                            text = stringResource(R.string.login),
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
 * Lista wymagań hasła z dynamicznym kolorowaniem - zielona ikona +
 * tekst, gdy reguła spełniona; szara, gdy jeszcze nie. Generowana
 * z [PasswordPolicy.evaluate] - jedno źródło prawdy dla całej apki.
 */
@Composable
internal fun PasswordRequirementsChecklist(password: String) {
    val statuses = PasswordPolicy.evaluate(password)
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        statuses.forEach { status ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                val (icon, color) = if (status.isSatisfied) {
                    Icons.Filled.CheckCircle to MaterialTheme.colorScheme.secondary
                } else {
                    Icons.Filled.Cancel to MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = status.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (status.isSatisfied) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    }
                )
            }
        }
    }
}

/**
 * Label dla wymaganego pola - tekst + czerwona gwiazdka.
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

@Composable
private fun ErrorMessageBanner(text: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}
