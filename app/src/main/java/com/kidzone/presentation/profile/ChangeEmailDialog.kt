package com.kidzone.presentation.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import android.util.Patterns

/**
 * Dialog zmiany adresu e-mail.
 *
 * Pola:
 *  - nowy adres e-mail (walidowany formatem przez [Patterns.EMAIL_ADDRESS]),
 *  - aktualne hasło (do reauth).
 *
 * **UWAGA**: po sukcesie e-mail nie zostaje natychmiast zmieniony. Firebase
 * wysyła link weryfikacyjny na nowy adres – dopiero kliknięcie linku
 * finalizuje zmianę. ProfileViewModel pokazuje o tym snackowy info
 * ("Sprawdź skrzynkę..."). Dialog tylko zamyka się po pomyślnym wysłaniu
 * linku.
 *
 * Aktualny e-mail pokazujemy jako wspierające info nad polami – żeby
 * user widział co aktualnie ma, zanim zacznie wpisywać nowy.
 */
@Composable
fun ChangeEmailDialog(
    currentEmail: String,
    isInProgress: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (currentPassword: String, newEmail: String) -> Unit
) {
    var newEmail by rememberSaveable { mutableStateOf("") }
    var currentPassword by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }

    val emailFormatInvalid = newEmail.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()
    val sameAsCurrent = newEmail.equals(currentEmail, ignoreCase = true) && newEmail.isNotEmpty()
    val isFormValid = newEmail.isNotBlank() &&
        !emailFormatInvalid &&
        !sameAsCurrent &&
        currentPassword.isNotBlank()

    AlertDialog(
        onDismissRequest = { if (!isInProgress) onDismiss() },
        title = { Text("Zmiana adresu e-mail") },
        text = {
            Column {
                if (currentEmail.isNotBlank()) {
                    Text(
                        text = "Aktualny adres: $currentEmail",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                }
                OutlinedTextField(
                    value = newEmail,
                    onValueChange = { newEmail = it.trim() },
                    label = { Text("Nowy adres e-mail") },
                    singleLine = true,
                    isError = emailFormatInvalid || sameAsCurrent,
                    enabled = !isInProgress,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    supportingText = {
                        when {
                            newEmail.isBlank() -> Text("Pole wymagane")
                            emailFormatInvalid -> Text("Niepoprawny format adresu")
                            sameAsCurrent -> Text("To jest Twój aktualny adres")
                            else -> Text("Wyślemy link weryfikacyjny na nowy adres")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text("Aktualne hasło") },
                    singleLine = true,
                    enabled = !isInProgress,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    supportingText = {
                        if (currentPassword.isBlank()) {
                            Text("Pole wymagane")
                        }
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { showPassword = !showPassword },
                            enabled = !isInProgress
                        ) {
                            Icon(
                                imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (showPassword) "Ukryj hasło" else "Pokaż hasło"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMessage != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(currentPassword, newEmail) },
                enabled = isFormValid && !isInProgress
            ) {
                if (isInProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Wyślij link")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isInProgress
            ) {
                Text("Anuluj")
            }
        }
    )
}
