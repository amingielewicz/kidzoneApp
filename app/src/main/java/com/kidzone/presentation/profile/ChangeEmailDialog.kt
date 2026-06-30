package com.kidzone.presentation.profile

import android.util.Patterns
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.kidzone.R
import com.kidzone.utils.UiText

/**
 * Dialog zmiany adresu e-mail.
 */
@Suppress("LongMethod", "CyclomaticComplexMethod", "FunctionNaming")
@Composable
fun ChangeEmailDialog(
    currentEmail: String,
    isInProgress: Boolean,
    errorMessage: UiText?,
    onDismiss: () -> Unit,
    onConfirm: (currentPassword: String, newEmail: String) -> Unit,
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
        title = { Text(stringResource(R.string.change_email_title)) },
        text = {
            Column {
                if (currentEmail.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.current_email_label, currentEmail),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                OutlinedTextField(
                    value = newEmail,
                    onValueChange = { newEmail = it.trim() },
                    label = { Text(stringResource(R.string.new_email_label)) },
                    singleLine = true,
                    isError = emailFormatInvalid || sameAsCurrent,
                    enabled = !isInProgress,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    supportingText = {
                        when {
                            newEmail.isBlank() -> Text(stringResource(R.string.field_required))
                            emailFormatInvalid -> Text(stringResource(R.string.invalid_email_format))
                            sameAsCurrent -> Text(stringResource(R.string.email_already_set))
                            else -> Text(stringResource(R.string.email_verification_hint))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text(stringResource(R.string.current_password)) },
                    singleLine = true,
                    enabled = !isInProgress,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    supportingText = {
                        if (currentPassword.isBlank()) {
                            Text(stringResource(R.string.field_required))
                        }
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { showPassword = !showPassword },
                            enabled = !isInProgress,
                        ) {
                            Icon(
                                imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (showPassword) {
                                    stringResource(R.string.hide_password)
                                } else {
                                    stringResource(R.string.show_password)
                                },
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (errorMessage != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = errorMessage.asString(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(currentPassword, newEmail) },
                enabled = isFormValid && !isInProgress,
            ) {
                if (isInProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(stringResource(R.string.send_link))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isInProgress,
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
