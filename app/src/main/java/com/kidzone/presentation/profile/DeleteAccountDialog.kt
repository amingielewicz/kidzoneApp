package com.kidzone.presentation.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.kidzone.R
import com.kidzone.presentation.auth.rememberGoogleSignInLauncher
import com.kidzone.utils.UiText

/**
 * Dialog potwierdzenia usunięcia konta.
 */
@Suppress("LongParameterList", "LongMethod", "FunctionNaming")
@Composable
fun DeleteAccountDialog(
    placesCount: Int,
    reviewsCount: Int,
    isInProgress: Boolean,
    errorMessage: UiText?,
    isGoogleUser: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (currentPassword: String) -> Unit = {},
    onConfirmGoogle: (idToken: String) -> Unit = {},
) {
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    val isFormValid = if (isGoogleUser) true else password.isNotBlank()

    val googleSignInLauncher = if (isGoogleUser) {
        rememberGoogleSignInLauncher(
            onTokenReceived = { idToken -> onConfirmGoogle(idToken) },
            onError = { /* Handled by errorMessage from VM */ },
        )
    } else {
        null
    }

    AlertDialog(
        onDismissRequest = { if (!isInProgress) onDismiss() },
        icon = {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = { Text(stringResource(R.string.delete_account)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.delete_account_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                BulletLine(text = stringResource(R.string.delete_account_bullet_1))
                BulletLine(
                    text = stringResource(R.string.delete_account_bullet_2, reviewsCount),
                )
                BulletLine(
                    text = stringResource(R.string.delete_account_bullet_3, placesCount),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.delete_account_gdpr_info),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))

                if (isGoogleUser) {
                    Text(
                        text = stringResource(R.string.delete_account_confirm_google),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.delete_account_confirm_password),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.current_password)) },
                        singleLine = true,
                        enabled = !isInProgress,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
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
                }

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
                onClick = {
                    if (isGoogleUser) {
                        googleSignInLauncher?.invoke()
                    } else {
                        onConfirm(password)
                    }
                },
                enabled = isFormValid && !isInProgress,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                if (isInProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    Text(
                        if (isGoogleUser) stringResource(R.string.confirm_google_button)
                        else stringResource(R.string.delete_account),
                    )
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

@Composable
private fun BulletLine(text: String) {
    Row {
        Text(text = "•  ", style = MaterialTheme.typography.bodyMedium)
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}
