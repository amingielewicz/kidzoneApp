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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/**
 * Dialog potwierdzenia usunięcia konta.
 *
 * Obsługuje dwa typy kont:
 *  - **Email/password:** pole hasła dla reauth
 *  - **Google:** przycisk "Zaloguj się przez Google" (Credential Manager)
 *
 * @param isGoogleUser true gdy `signInProvider == GOOGLE`
 * @param onConfirm callback dla email/password (z hasłem)
 * @param onConfirmGoogle callback dla Google (z idToken)
 */
@Composable
fun DeleteAccountDialog(
    placesCount: Int,
    reviewsCount: Int,
    isInProgress: Boolean,
    errorMessage: String?,
    isGoogleUser: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (currentPassword: String) -> Unit = {},
    onConfirmGoogle: (idToken: String) -> Unit = {}
) {
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    val isFormValid = if (isGoogleUser) true else password.isNotBlank()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Google Sign-In launcher for reauth
    val googleSignInLauncher = if (isGoogleUser) {
        com.kidzone.presentation.auth.rememberGoogleSignInLauncher(
            onTokenReceived = { idToken -> onConfirmGoogle(idToken) },
            onError = { /* Handled by errorMessage from VM */ }
        )
    } else null

    AlertDialog(
        onDismissRequest = { if (!isInProgress) onDismiss() },
        icon = {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Usunąć konto?") },
        text = {
            Column {
                Text(
                    text = "Operacja jest nieodwracalna. Po usunięciu konta:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                BulletLine(text = "Twoje dane osobowe (profil, email, avatar) zostaną usunięte")
                BulletLine(
                    text = "Twoje opinie ($reviewsCount) zostaną zanonimizowane - " +
                            "treść pozostanie, autor zmieni się na \"Nieaktywny użytkownik\""

                )
                BulletLine(
                    text = "Twoje miejsca ($placesCount) pozostaną widoczne, " +
                        "ale bez powiązania z Twoim kontem"
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Treści tworzone przez Ciebie stanowią wartość dla społeczności " +
                        "i pozostaną dostępne w formie zanonimizowanej (zgodnie z RODO).",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))

                if (isGoogleUser) {
                    Text(
                        text = "Zaloguj się ponownie przez Google, aby potwierdzić usunięcie konta:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        text = "Wpisz aktualne hasło, aby potwierdzić:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Aktualne hasło") },
                        singleLine = true,
                        enabled = !isInProgress,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
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
                }

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
                onClick = {
                    if (isGoogleUser) {
                        googleSignInLauncher?.invoke()
                    } else {
                        onConfirm(password)
                    }
                },
                enabled = isFormValid && !isInProgress,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                if (isInProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(if (isGoogleUser) "Potwierdź przez Google" else "Usuń konto")
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

@Composable
private fun BulletLine(text: String) {
    Text(
        text = "•  $text",
        style = MaterialTheme.typography.bodyMedium
    )
}

/**
 * Polska deklinacja liczebnikowa: 1 → [one], 2..4 → [few], reszta → [many].
 *
 * Reguła zgodna z Unicode CLDR `pl`:
 *  - one: n == 1
 *  - few: n%10 in 2..4 && n%100 not in 12..14
 *  - many: pozostałe
 *
 * Używane w komunikacie ostrzegawczym ("3 opinie zostaną usunięte" /
 * "5 opinii zostanie usuniętych"). Trzymane lokalnie w pliku, bo to
 * jedyne miejsce w aplikacji, gdzie tego potrzebujemy – wyciągniemy
 * do utility'ki, gdy pojawi się drugi konsument.
 */
private fun pluralize(count: Int, one: String, few: String, many: String): String {
    val n = kotlin.math.abs(count)
    val mod10 = n % 10
    val mod100 = n % 100
    return when {
        n == 1 -> one
        mod10 in 2..4 && mod100 !in 12..14 -> few
        else -> many
    }
}
