package com.kidzone.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kidzone.R
import com.kidzone.presentation.auth.rememberGoogleSignInLauncher
import com.kidzone.presentation.common.ModalDangerColor
import com.kidzone.presentation.common.ModalDialogShape
import com.kidzone.presentation.common.ModalPasswordVisibilityButton
import com.kidzone.presentation.common.ModalTextButton
import com.kidzone.presentation.common.OfflineAwareSubmitButton
import com.kidzone.presentation.common.RequiredFieldLabel
import androidx.compose.material3.TextButton
import com.kidzone.utils.UiText

/**
 * Dialog potwierdzenia usunięcia konta.
 */
@Suppress(
    "LongParameterList",
    "LongMethod",
    "FunctionNaming",
)
@Composable
fun DeleteAccountDialog(
    placesCount: Int,
    reviewsCount: Int,
    isInProgress: Boolean,
    isOffline: Boolean,
    errorMessage: UiText?,
    isGoogleUser: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (currentPassword: String) -> Unit = {},
    onConfirmGoogle: (idToken: String) -> Unit = {},
) {
    var isConfirmedByPrompt by rememberSaveable {
        mutableStateOf(false)
    }

    if (!isConfirmedByPrompt) {
        DeleteConfirmationPrompt(
            onConfirm = { isConfirmedByPrompt = true },
            onDismiss = onDismiss
        )
        return
    }

    var password by rememberSaveable {
        mutableStateOf("")
    }

    var showPassword by rememberSaveable {
        mutableStateOf(false)
    }

    val isFormValid =
        isGoogleUser || password.isNotBlank()

    val googleSignInLauncher = if (isGoogleUser) {
        rememberGoogleSignInLauncher(
            onTokenReceived = { idToken ->
                onConfirmGoogle(idToken)
            },
            onError = {
                // Błąd jest obsługiwany przez ViewModel.
            },
        )
    } else {
        null
    }

    AlertDialog(
        onDismissRequest = {
            if (!isInProgress) {
                onDismiss()
            }
        },
        shape = ModalDialogShape,
        icon = {
            DeleteAccountWarningIcon()
        },
        title = {
            Text(
                text = stringResource(
                    R.string.delete_account,
                ),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(
                        R.string.delete_account_subtitle,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )

                Spacer(
                    modifier = Modifier.height(12.dp),
                )

                BulletLine(
                    text = stringResource(
                        R.string.delete_account_bullet_1,
                    ),
                )

                Spacer(
                    modifier = Modifier.height(8.dp),
                )

                BulletLine(
                    text = stringResource(
                        R.string.delete_account_bullet_2,
                        reviewsCount,
                    ),
                    boldPrefix = stringResource(
                        R.string.delete_account_reviews_prefix,
                    ),
                )

                Spacer(
                    modifier = Modifier.height(8.dp),
                )

                BulletLine(
                    text = stringResource(
                        R.string.delete_account_bullet_3,
                        placesCount,
                    ),
                    boldPrefix = stringResource(
                        R.string.delete_account_places_prefix,
                    ),
                )

                Spacer(
                    modifier = Modifier.height(12.dp),
                )

                Text(
                    text = stringResource(
                        R.string.delete_account_gdpr_info,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        horizontal = 4.dp,
                    ),
                )

                Spacer(
                    modifier = Modifier.height(12.dp),
                )

                if (isGoogleUser) {
                    GoogleAccountConfirmation()
                } else {
                    PasswordAccountConfirmation(
                        password = password,
                        onPasswordChange = {
                            password = it
                        },
                        showPassword = showPassword,
                        onShowPasswordChange = {
                            showPassword = it
                        },
                        enabled = !isInProgress,
                    )
                }

                if (errorMessage != null) {
                    Spacer(
                        modifier = Modifier.height(8.dp),
                    )

                    Text(
                        text = errorMessage.asString(),
                        color = ModalDangerColor,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        confirmButton = {
            OfflineAwareSubmitButton(
                label = if (isGoogleUser) {
                    stringResource(
                        R.string.confirm_google_button,
                    )
                } else {
                    stringResource(
                        R.string.delete_account,
                    )
                },
                onClick = {
                    if (isGoogleUser) {
                        googleSignInLauncher?.invoke()
                    } else {
                        onConfirm(password)
                    }
                },
                isOffline = isOffline,
                enabled = isFormValid,
                isLoading = isInProgress,
                offlineLabel = stringResource(
                    R.string.delete_account_offline_action,
                ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ModalDangerColor,
                    contentColor = MaterialTheme.colorScheme.onError,
                    disabledContainerColor = ModalDangerColor.copy(
                        alpha = 0.35f,
                    ),
                    disabledContentColor = MaterialTheme.colorScheme.onError.copy(
                        alpha = 0.75f,
                    ),
                ),
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

@Composable
private fun DeleteConfirmationPrompt(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = ModalDialogShape,
        icon = { DeleteAccountWarningIcon() },
        title = {
            Text(
                text = "Czy na pewno chcesz usunąć konto?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = "Ta operacja jest nieodwracalna. Twoje dane profilowe zostaną usunięte, a dodane przez Ciebie treści zostaną zanonimizowane.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = ModalDangerColor)
            ) {
                Text("Usuń konto", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            ModalTextButton(
                text = stringResource(R.string.cancel),
                onClick = onDismiss
            )
        }
    )
}

@Suppress("FunctionNaming")
@Composable
private fun DeleteAccountWarningIcon() {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(
                color = ModalDangerColor.copy(
                    alpha = 0.12f,
                ),
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.WarningAmber,
            contentDescription = null,
            tint = ModalDangerColor,
            modifier = Modifier.size(26.dp),
        )
    }
}


@Suppress("FunctionNaming")
@Composable
private fun GoogleAccountConfirmation() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant
            .copy(alpha = 0.6f),
    )

    Spacer(
        modifier = Modifier.height(12.dp),
    )

    Text(
        text = stringResource(
            R.string.delete_account_confirm_google,
        ),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
    )
}

@Suppress("LongParameterList", "FunctionNaming")
@Composable
private fun PasswordAccountConfirmation(
    password: String,
    onPasswordChange: (String) -> Unit,
    showPassword: Boolean,
    onShowPasswordChange: (Boolean) -> Unit,
    enabled: Boolean,
) {
    Text(
        text = stringResource(
            R.string.delete_account_confirm_password,
        ),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
    )

    Spacer(
        modifier = Modifier.height(8.dp),
    )

    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = {
            RequiredFieldLabel(
                label = stringResource(
                    R.string.current_password,
                ),
            )
        },
        singleLine = true,
        enabled = enabled,
        visualTransformation = if (showPassword) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
        ),
        trailingIcon = {
            ModalPasswordVisibilityButton(
                visible = showPassword,
                onVisibleChange = onShowPasswordChange,
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
        modifier = Modifier.fillMaxWidth(),
    )
}

@Suppress("FunctionNaming")
@Composable
private fun BulletLine(
    text: String,
    boldPrefix: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(
                top = 1.dp,
            ),
        )

        Spacer(
            modifier = Modifier.width(8.dp),
        )

        Text(
            text = buildAnnotatedString {
                if (
                    !boldPrefix.isNullOrBlank() &&
                    text.startsWith(boldPrefix)
                ) {
                    withStyle(
                        style = SpanStyle(
                            fontWeight = FontWeight.SemiBold,
                        ),
                    ) {
                        append(boldPrefix)
                    }

                    append(
                        text.removePrefix(boldPrefix),
                    )
                } else {
                    append(text)
                }
            },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}
