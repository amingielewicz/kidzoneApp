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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.kidzone.presentation.common.passwordRequirementText
import com.kidzone.utils.PasswordPolicy
import com.kidzone.utils.UiText

/**
 * Dialog zmiany hasła.
 */
@Suppress("LongMethod", "FunctionNaming")
@Composable
fun ChangePasswordDialog(
    isInProgress: Boolean,
    errorMessage: UiText?,
    onDismiss: () -> Unit,
    onConfirm: (
        currentPassword: String,
        newPassword: String,
    ) -> Unit,
) {
    var currentPassword by rememberSaveable {
        mutableStateOf("")
    }

    var newPassword by rememberSaveable {
        mutableStateOf("")
    }

    var confirmPassword by rememberSaveable {
        mutableStateOf("")
    }

    var showPasswords by rememberSaveable {
        mutableStateOf(false)
    }

    val isNewPasswordValid =
        PasswordPolicy.isValid(newPassword)

    val passwordsMismatch =
        confirmPassword.isNotEmpty() &&
                confirmPassword != newPassword

    val confirmPasswordHelp =
        confirmPasswordSupportingTextRes(
            passwordsMismatch = passwordsMismatch,
        )

    val isFormValid =
        currentPassword.isNotBlank() &&
                isNewPasswordValid &&
                confirmPassword.isNotBlank() &&
                confirmPassword == newPassword

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
                    R.string.change_password_title,
                ),
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                PasswordField(
                    value = currentPassword,
                    onValueChange = {
                        currentPassword = it
                    },
                    label = stringResource(
                        R.string.current_password,
                    ),
                    showText = showPasswords,
                    onToggleVisibility = {
                        showPasswords = !showPasswords
                    },
                    enabled = !isInProgress,
                )

                Spacer(
                    modifier = Modifier.size(8.dp),
                )

                PasswordField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                    },
                    label = stringResource(
                        R.string.new_password,
                    ),
                    isError =
                        newPassword.isNotEmpty() &&
                                !isNewPasswordValid,
                    showText = showPasswords,
                    onToggleVisibility = {
                        showPasswords = !showPasswords
                    },
                    enabled = !isInProgress,
                )

                if (newPassword.isNotEmpty()) {
                    Spacer(
                        modifier = Modifier.size(8.dp),
                    )

                    PasswordRequirements(
                        password = newPassword,
                    )
                }

                Spacer(
                    modifier = Modifier.size(8.dp),
                )

                PasswordField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                    },
                    label = stringResource(
                        R.string.repeat_new_password,
                    ),
                    isError = passwordsMismatch,
                    supportingText =
                        confirmPasswordHelp?.let {
                            stringResource(it)
                        },
                    showText = showPasswords,
                    onToggleVisibility = {
                        showPasswords = !showPasswords
                    },
                    enabled = !isInProgress,
                )

                if (errorMessage != null) {
                    Spacer(
                        modifier = Modifier.size(8.dp),
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
                    R.string.change_password_action,
                ),
                onClick = {
                    onConfirm(
                        currentPassword,
                        newPassword,
                    )
                },
                enabled = isFormValid,
                isLoading = isInProgress,
            )
        },
        dismissButton = {
            ModalTextButton(
                text = stringResource(R.string.cancel),
                onClick = onDismiss,
                enabled = !isInProgress,
            )
        },
    )
}

private fun confirmPasswordSupportingTextRes(
    passwordsMismatch: Boolean,
): Int? = if (passwordsMismatch) {
    R.string.passwords_do_not_match
} else {
    null
}

/**
 * Lista wymagań stawianych nowemu hasłu.
 */
@Suppress("FunctionNaming")
@Composable
private fun PasswordRequirements(
    password: String,
) {
    val statuses = PasswordPolicy.evaluate(password)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        statuses.forEach { status ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val icon = if (status.isSatisfied) {
                    Icons.Filled.CheckCircle
                } else {
                    Icons.Filled.Cancel
                }

                val color = if (status.isSatisfied) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.4f,
                    )
                }

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp),
                )

                Spacer(
                    modifier = Modifier.size(6.dp),
                )

                Text(
                    text = passwordRequirementText(
                        status.labelKey,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (status.isSatisfied) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.7f,
                        )
                    },
                )
            }
        }
    }
}

/**
 * Pole hasła korzystające ze wspólnego przycisku
 * pokazywania i ukrywania treści.
 */
@Suppress("LongParameterList", "FunctionNaming")
@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    showText: Boolean,
    onToggleVisibility: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        isError = isError,
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
        label = {
            RequiredFieldLabel(
                label = label,
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
                onVisibleChange = {
                    onToggleVisibility()
                },
                enabled = enabled,
            )
        },
        supportingText = supportingText?.let { message ->
            {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
    )
}
