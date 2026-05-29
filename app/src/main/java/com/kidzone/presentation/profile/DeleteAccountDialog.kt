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
 * Pokazuje:
 *  - ikonę ostrzeżenia + tytuł "Usuń konto?",
 *  - listę tego co zostanie skasowane (X miejsc, Y opinii, avatar, dane konta),
 *  - explicit "operacja nieodwracalna",
 *  - pole hasła dla reauth.
 *
 * CTA jest typu `error` (czerwone), żeby na ostatniej milicze user widział,
 * że robi coś poważnego.
 *
 * Świadomie nie wymagamy dodatkowego "wpisz USUŃ żeby potwierdzić" – sam
 * fakt że trzeba wstawić aktualne hasło jest wystarczająco silnym
 * potwierdzeniem (analogiczne UX jak w GitHub / Google).
 *
 * @param placesCount liczba miejsc usera (pokazana w ostrzeżeniu)
 * @param reviewsCount liczba opinii usera
 */
@Composable
fun DeleteAccountDialog(
    placesCount: Int,
    reviewsCount: Int,
    isInProgress: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (currentPassword: String) -> Unit
) {
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    val isFormValid = password.isNotBlank()

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
                    text = "Operacja jest nieodwracalna. Zostanie usunięte:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                BulletLine(text = "Twoje konto i dane profilu (imię, nazwisko, avatar)")
                BulletLine(text = "$reviewsCount " + pluralize(reviewsCount, "opinia", "opinie", "opinii"))
                BulletLine(
                    text = "$placesCount " + pluralize(placesCount, "miejsce", "miejsca", "miejsc") +
                        " które dodałaś/eś"
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Opinie innych użytkowników wystawione na Twoich miejscach " +
                        "mogą zostać widoczne jako osierocone do czasu ręcznego sprzątnięcia.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
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
                onClick = { onConfirm(password) },
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
                    Text("Usuń konto")
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
