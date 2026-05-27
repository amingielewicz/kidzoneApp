package com.playground.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.playground.R

/**
 * Ekran rejestracji - nazwa, e-mail, haslo (min. 6 znakow).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    // Lokalny stan UI - widocznosc hasla.
    var isPasswordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(state.isRegistered) {
        if (state.isRegistered) onRegisterSuccess()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.register)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { RequiredFieldLabel("Nazwa") },
                singleLine = true,
                isError = state.name.isNotEmpty() && !state.isNameValid,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                label = { RequiredFieldLabel(stringResource(R.string.email)) },
                singleLine = true,
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
                singleLine = true,
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
                supportingText = { Text("Min. 6 znaków") },
                modifier = Modifier.fillMaxWidth()
            )

            state.errorMessage?.let { msg ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = viewModel::register,
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
                    Text(stringResource(R.string.register))
                }
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
