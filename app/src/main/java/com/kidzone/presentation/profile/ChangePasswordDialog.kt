package com.kidzone.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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

    var showCurrentPassword by rememberSaveable {
        mutableStateOf(false)
    }

    var showNewPassword by rememberSaveable {
        mutableStateOf(false)
    }

    var showConfirmPassword by rememberSaveable {
        mutableStateOf(false)
    }

    val isNewPasswordValid =
        PasswordPolicy.isValid(newPassword)

    val passwordsMismatch =
        confirmPassword.isNotEmpty() &&
                confirmPassword != newPassword

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
                    showText = showCurrentPassword,
                    onShowTextChange = {
                        showCurrentPassword = it
                    },
                    enabled = !isInProgress,
                )

                Spacer(
                    modifier = Modifier.height(8.dp),
                )

                PasswordField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                    },
                    label = stringResource(
                        R.string.new_password,
                    ),
                    showText = showNewPassword,
                    onShowTextChange = {
                        showNewPassword = it
                    },
                    enabled = !isInProgress,
                    isError =
                        newPassword.isNotEmpty() &&
                                !isNewPasswordValid,
                )

                if (newPassword.isNotEmpty()) {
                    Spacer(
                        modifier = Modifier.height(8.dp),
                    )

                    PasswordRequirements(
                        password = newPassword,
                    )
                }

                Spacer(
                    modifier = Modifier.height(8.dp),
                )

                PasswordField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                    },
                    label = stringResource(
                        R.string.repeat_new_password,
                    ),
                    showText = showConfirmPassword,
                    onShowTextChange = {
                        showConfirmPassword = it
                    },
                    enabled = !isInProgress,
                    isError = passwordsMismatch,
                    supportingText = if (passwordsMismatch) {
                        stringResource(
                            R.string.passwords_do_not_match,
                        )
                    } else {
                        null
                    },
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
 * Lista wymagań nowego hasła.
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
            PasswordRequirementRow(
                isSatisfied = status.isSatisfied,
                text = passwordRequirementText(
                    status.labelKey,
                ),
            )
        }
    }
}

/**
 * Pojedynczy wiersz wymagania hasła.
 */
@Suppress("FunctionNaming")
@Composable
private fun PasswordRequirementRow(
    isSatisfied: Boolean,
    text: String,
) {
    val icon = if (isSatisfied) {
        Icons.Filled.CheckCircle
    } else {
        Icons.Filled.Cancel
    }

    val iconColor = if (isSatisfied) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(
            alpha = 0.4f,
        )
    }

    val textColor = if (isSatisfied) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(
            alpha = 0.7f,
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(16.dp),
        )

        Spacer(
            modifier = Modifier.size(6.dp),
        )

        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = textColor,
        )
    }
}

/**
 * Pojedyncze pole hasła.
 */
@Suppress("LongParameterList", "FunctionNaming")
@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    showText: Boolean,
    onShowTextChange: (Boolean) -> Unit,
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

