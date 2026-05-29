package com.kidzone.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp

/**
 * Minimalna długość nowego hasła egzekwowana po stronie UI.
 *
 * Ta sama wartość obowiązuje w Firebase Auth (FirebaseAuthWeakPasswordException
 * leci poniżej 6 znaków) – walidacja UI to "uprzejma" warstwa, żeby user nie
 * musiał czekać na round-trip po naturalnym błędzie.
 */
private const val MIN_PASSWORD_LENGTH = 6

/**
 * Dialog zmiany hasła.
 *
 * 3 pola:
 *  - aktualne hasło (do reauth),
 *  - nowe hasło,
 *  - powtórz nowe hasło.
 *
 * Walidacje (klient-side):
 *  - wszystkie 3 pola niepuste,
 *  - nowe hasło >= 6 znaków,
 *  - oba "nowe" pola identyczne.
 *
 * Błędy z repo (np. niepoprawne aktualne hasło) lądują w [errorMessage] –
 * dialog pozostaje otwarty, user widzi co poprawić.
 *
 * Pola przeżywają rotację (rememberSaveable), ale **dialog nie pamięta
 * wpisanego hasła między otwarciami** – kolejny `open` resetuje state
 * (rememberSaveable jest scope'owany do composition, dialog znika z drzewa
 * gdy `isOpen=false`).
 */
@Composable
fun ChangePasswordDialog(
    isInProgress: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (currentPassword: String, newPassword: String) -> Unit
) {
    var currentPassword by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var showPasswords by rememberSaveable { mutableStateOf(false) }

    // Walidacja "soft" – pokazujemy supportingText dopiero gdy pole jest
    // dotknięte (niepuste), żeby pusty formularz nie był od razu czerwony.
    val newPasswordTooShort = newPassword.isNotEmpty() && newPassword.length < MIN_PASSWORD_LENGTH
    val passwordsMismatch = confirmPassword.isNotEmpty() && confirmPassword != newPassword
    val isFormValid = currentPassword.isNotBlank() &&
        newPassword.length >= MIN_PASSWORD_LENGTH &&
        confirmPassword == newPassword

    AlertDialog(
        onDismissRequest = { if (!isInProgress) onDismiss() },
        title = { Text("Zmiana hasła") },
        text = {
            Column {
                PasswordField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = "Aktualne hasło",
                    showText = showPasswords,
                    onToggleVisibility = { showPasswords = !showPasswords },
                    enabled = !isInProgress
                )
                Spacer(Modifier.height(8.dp))
                PasswordField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = "Nowe hasło",
                    isError = newPasswordTooShort,
                    supportingText = "Min. $MIN_PASSWORD_LENGTH znaków",
                    showText = showPasswords,
                    onToggleVisibility = { showPasswords = !showPasswords },
                    enabled = !isInProgress
                )
                Spacer(Modifier.height(8.dp))
                PasswordField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = "Powtórz nowe hasło",
                    isError = passwordsMismatch,
                    supportingText = if (passwordsMismatch) "Hasła nie są takie same" else null,
                    showText = showPasswords,
                    onToggleVisibility = { showPasswords = !showPasswords },
                    enabled = !isInProgress
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
                onClick = { onConfirm(currentPassword, newPassword) },
                enabled = isFormValid && !isInProgress
            ) {
                if (isInProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Zmień hasło")
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

/**
 * Pojedyncze pole hasła z wspólnym toggle "pokaż/ukryj".
 *
 * Świadomie współdzielimy [showText] między 3 pola – jeśli user kliknie
 * na jakimkolwiek "oczku", pokazują się wszystkie 3. Tak jest mniej
 * frustrująco niż per-field, kiedy walczy z mismatch i chce sprawdzić
 * co wpisał w obu polach "nowego" hasła.
 */
@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    showText: Boolean,
    onToggleVisibility: () -> Unit,
    enabled: Boolean,
    isError: Boolean = false,
    supportingText: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        enabled = enabled,
        visualTransformation = if (showText) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = onToggleVisibility, enabled = enabled) {
                Icon(
                    imageVector = if (showText) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (showText) "Ukryj hasło" else "Pokaż hasło"
                )
            }
        },
        supportingText = supportingText?.let { { Text(it) } },
        modifier = Modifier.fillMaxWidth()
    )
}
