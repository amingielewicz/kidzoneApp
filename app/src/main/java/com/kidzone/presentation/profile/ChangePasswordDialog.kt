package com.kidzone.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.kidzone.utils.PasswordPolicy

/**
 * Dialog zmiany hasła.
 *
 * 3 pola:
 *  - aktualne hasło (do reauth),
 *  - nowe hasło,
 *  - powtórz nowe hasło.
 *
 * Walidacje (klient-side, jednolite z rejestracją - zob. [PasswordPolicy]):
 *  - wszystkie 3 pola niepuste,
 *  - nowe hasło spełnia [PasswordPolicy] (8+ znaków, mała + duża litera,
 *    znak specjalny),
 *  - oba "nowe" pola identyczne.
 *
 * Pod polem "Nowe hasło" pokazujemy dynamiczny checklist wymagań -
 * user widzi w czasie rzeczywistym, co jeszcze musi spełnić.
 *
 * Błędy z repo (np. niepoprawne aktualne hasło) lądują w [errorMessage] -
 * dialog pozostaje otwarty, user widzi co poprawić.
 *
 * Pola przeżywają rotację (rememberSaveable), ale dialog nie pamięta
 * wpisanego hasła między otwarciami - kolejny `open` resetuje state
 * (rememberSaveable jest scope'owany do composition, dialog znika
 * z drzewa gdy `isOpen=false`).
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

    val isNewPasswordValid = PasswordPolicy.isValid(newPassword)
    val passwordsMismatch = confirmPassword.isNotEmpty() && confirmPassword != newPassword
    val confirmPasswordHelp = confirmPasswordSupportingText(confirmPassword, passwordsMismatch)
    val isFormValid = currentPassword.isNotBlank() &&
        isNewPasswordValid &&
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
                    supportingText = if (currentPassword.isBlank()) "Pole wymagane" else null,
                    showText = showPasswords,
                    onToggleVisibility = { showPasswords = !showPasswords },
                    enabled = !isInProgress
                )
                Spacer(Modifier.size(8.dp))
                PasswordField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = "Nowe hasło",
                    isError = newPassword.isNotEmpty() && !isNewPasswordValid,
                    supportingText = if (newPassword.isBlank()) "Pole wymagane" else null,
                    showText = showPasswords,
                    onToggleVisibility = { showPasswords = !showPasswords },
                    enabled = !isInProgress
                )
                if (newPassword.isNotEmpty()) {
                    Spacer(Modifier.size(6.dp))
                    PasswordRequirements(password = newPassword)
                }
                Spacer(Modifier.size(8.dp))
                PasswordField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = "Powtórz nowe hasło",
                    isError = passwordsMismatch,
                    supportingText = confirmPasswordHelp,
                    showText = showPasswords,
                    onToggleVisibility = { showPasswords = !showPasswords },
                    enabled = !isInProgress
                )
                if (errorMessage != null) {
                    Spacer(Modifier.size(8.dp))
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

private fun confirmPasswordSupportingText(
    confirmPassword: String,
    passwordsMismatch: Boolean
): String? = when {
    confirmPassword.isBlank() -> "Pole wymagane"
    passwordsMismatch -> "Hasła nie są takie same"
    else -> null
}

/**
 * Wewnętrzna lista wymagań - "lokalna" wersja checklisty z rejestracji
 * (RegisterScreen.PasswordRequirementsChecklist), trzymana osobno żeby
 * dialog mógł się kompilować bez kross-modułowych zależności na ekranie
 * auth. Treść identyczna - obie korzystają z [PasswordPolicy.evaluate].
 */
@Composable
private fun PasswordRequirements(password: String) {
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
 * Pojedyncze pole hasła z wspólnym toggle "pokaż/ukryj".
 *
 * Świadomie współdzielimy [showText] między 3 pola - jeśli user kliknie
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
        visualTransformation = if (showText) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = onToggleVisibility, enabled = enabled) {
                Icon(
                    imageVector = if (showText) {
                        Icons.Filled.VisibilityOff
                    } else {
                        Icons.Filled.Visibility
                    },
                    contentDescription = if (showText) "Ukryj hasło" else "Pokaż hasło"
                )
            }
        },
        supportingText = supportingText?.let { { Text(it) } },
        modifier = Modifier.fillMaxWidth()
    )
}
