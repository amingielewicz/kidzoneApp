package com.kidzone.presentation.profile

import android.util.Patterns
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.kidzone.R
import com.kidzone.presentation.common.ModalDialogShape
import com.kidzone.presentation.common.ModalPasswordVisibilityButton
import com.kidzone.presentation.common.ModalPrimaryButton
import com.kidzone.presentation.common.ModalTextButton
import com.kidzone.presentation.common.RequiredFieldLabel
import com.kidzone.utils.UiText

/**
 * Dialog zmiany adresu e-mail.
 */
@Suppress("LongMethod", "FunctionNaming")
@Composable
fun ChangeEmailDialog(
    currentEmail: String,
    isInProgress: Boolean,
    errorMessage: UiText?,
    onDismiss: () -> Unit,
    onConfirm: (
        newEmail: String,
        currentPassword: String,
    ) -> Unit,
) {
    var newEmail by rememberSaveable {
        mutableStateOf("")
    }

    var currentPassword by rememberSaveable {
        mutableStateOf("")
    }

    var showCurrentPassword by rememberSaveable {
        mutableStateOf(false)
    }

    val normalizedCurrentEmail =
        currentEmail.trim()

    val normalizedNewEmail =
        newEmail.trim()

    val isEmailValid =
        Patterns.EMAIL_ADDRESS
            .matcher(normalizedNewEmail)
            .matches()

    val isSameEmail =
        normalizedNewEmail.equals(
            normalizedCurrentEmail,
            ignoreCase = true,
        )

    val showInvalidEmailError =
        newEmail.isNotBlank() &&
                !isEmailValid

    val showSameEmailError =
        newEmail.isNotBlank() &&
                isEmailValid &&
                isSameEmail

    val isFormValid =
        isEmailValid &&
                !isSameEmail &&
                currentPassword.isNotBlank()

    AlertDialog(
        onDismissRequest = {
            if (!isInProgress) {
                onDismiss()
            }
        },
        shape = ModalDialogShape,
        title = {
            Text(
                text = stringResource(
                    R.string.change_email,
                ),
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(
                        R.string.current_email_label,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(
                    modifier = Modifier.height(4.dp),
                )

                Text(
                    text = currentEmail,
                    style = MaterialTheme.typography.bodyLarge,
                )

                Spacer(
                    modifier = Modifier.height(16.dp),
                )

                OutlinedTextField(
                    value = newEmail,
                    onValueChange = {
                        newEmail = it
                    },
                    enabled = !isInProgress,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        RequiredFieldLabel(
                            label = stringResource(
                                R.string.new_email_label,
                            ),
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                    ),
                    isError =
                        showInvalidEmailError ||
                                showSameEmailError,
                    supportingText = when {
                        showInvalidEmailError -> {
                            {
                                Text(
                                    text = stringResource(
                                        R.string.invalid_email,
                                    ),
                                    color =
                                        MaterialTheme.colorScheme.error,
                                )
                            }
                        }

                        showSameEmailError -> {
                            {
                                Text(
                                    text = stringResource(
                                        R.string.email_same_as_current,
                                    ),
                                    color =
                                        MaterialTheme.colorScheme.error,
                                )
                            }
                        }

                        else -> null
                    },
                )

                Spacer(
                    modifier = Modifier.height(8.dp),
                )

                ChangeEmailPasswordField(
                    value = currentPassword,
                    onValueChange = {
                        currentPassword = it
                    },
                    showText = showCurrentPassword,
                    onShowTextChange = {
                        showCurrentPassword = it
                    },
                    enabled = !isInProgress,
                )

                if (errorMessage != null) {
                    Spacer(
                        modifier = Modifier.height(8.dp),
                    )

                    Text(
                        text = errorMessage.asString(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        confirmButton = {
            ModalPrimaryButton(
                text = stringResource(
                    R.string.change_email,
                ),
                onClick = {
                    onConfirm(
                        normalizedNewEmail,
                        currentPassword,
                    )
                },
                enabled = isFormValid,
                isLoading = isInProgress,
            )
        },
        dismissButton = {
            ModalTextButton(
                text = stringResource(
                    R.string.cancel,
                ),
                onClick = onDismiss,
                enabled = !isInProgress,
            )
        },
    )
}

/**
 * Pole aktualnego hasła używane podczas zmiany adresu e-mail.
 *
 * Nazwa funkcji jest unikalna, dzięki czemu nie koliduje
 * z pomocniczym polem w ChangePasswordDialog.kt.
 */
@Suppress("FunctionNaming")
@Composable
private fun ChangeEmailPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    showText: Boolean,
    onShowTextChange: (Boolean) -> Unit,
    enabled: Boolean,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        label = {
            RequiredFieldLabel(
                label = stringResource(
                    R.string.current_password,
                ),
            )
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
        ),
        visualTransformation = if (showText) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            ModalPasswordVisibilityButton(
                visible = showText,
                onVisibleChange = onShowTextChange,
                showPasswordContentDescription =
                    stringResource(
                        R.string.show_password,
                    ),
                hidePasswordContentDescription =
                    stringResource(
                        R.string.hide_password,
                    ),
                enabled = enabled,
            )
        },
    )
}
