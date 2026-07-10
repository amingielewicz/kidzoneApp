package com.kidzone.presentation.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Suppress("MagicNumber")
val ModalActionColor = Color(0xFF1976D2)

@Suppress("MagicNumber")
val ModalDisabledColor = Color(0xFF757575)

@Suppress("MagicNumber")
val ModalEyeIconColor = Color(0xFF757575)

@Suppress("MagicNumber")
val ModalDangerColor = Color(0xFFD32F2F)

val ModalContentPadding = 24.dp

val ModalDialogShape = RoundedCornerShape(KidZoneRadii.Card)

/**
 * Główny przycisk akcji używany w dialogach.
 *
 * Domyślnie ma niebieskie tło. Kolor można nadpisać,
 * np. dla destrukcyjnej akcji usunięcia konta.
 */
@Suppress("LongParameterList", "FunctionNaming")
@Composable
fun ModalPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    containerColor: Color = ModalActionColor,
    disabledContainerColor: Color = ModalDisabledColor,
    loadingContent: @Composable (() -> Unit)? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier,
        shape = RoundedCornerShape(KidZoneRadii.Control),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = Color.White,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = Color.White.copy(alpha = 0.75f),
        ),
    ) {
        when {
            isLoading && loadingContent != null -> {
                loadingContent()
            }

            isLoading -> {
                ModalButtonLoadingIndicator()
            }

            else -> {
                Text(text = text)
            }
        }
    }
}

/**
 * Tekstowy przycisk pomocniczy używany do akcji:
 * Zamknij, Anuluj oraz Wstecz.
 */
@Suppress("FunctionNaming")
@Composable
fun ModalTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors = ButtonDefaults.textButtonColors(
            contentColor = ModalActionColor,
            disabledContentColor = ModalActionColor.copy(alpha = 0.38f),
        ),
    ) {
        Text(text = text)
    }
}

/**
 * Etykieta pola wymaganego z czerwoną gwiazdką.
 */
@Suppress("FunctionNaming")
@Composable
fun RequiredFieldLabel(
    label: String,
) {
    Text(
        text = buildAnnotatedString {
            append(label)
            append(" ")

            withStyle(
                SpanStyle(
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium,
                ),
            ) {
                append("*")
            }
        },
    )
}

/**
 * Przycisk zmiany widoczności hasła.
 */
@Suppress("LongParameterList", "FunctionNaming")
@Composable
fun ModalPasswordVisibilityButton(
    visible: Boolean,
    onVisibleChange: (Boolean) -> Unit,
    showPasswordContentDescription: String? = null,
    hidePasswordContentDescription: String? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    IconButton(
        onClick = {
            onVisibleChange(!visible)
        },
        enabled = enabled,
        modifier = modifier,
    ) {
        Icon(
            imageVector = if (visible) {
                Icons.Filled.VisibilityOff
            } else {
                Icons.Filled.Visibility
            },
            contentDescription = if (visible) {
                hidePasswordContentDescription
            } else {
                showPasswordContentDescription
            },
            tint = ModalEyeIconColor,
        )
    }
}

/**
 * Domyślny wskaźnik ładowania wyświetlany wewnątrz przycisku.
 */
@Suppress("FunctionNaming")
@Composable
fun ModalButtonLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
) {
    CircularProgressIndicator(
        modifier = modifier.size(18.dp),
        strokeWidth = 2.dp,
        color = color,
    )
}

/**
 * Wspólny dialog dla długich treści tekstowych,
 * np. regulaminu i polityki prywatności.
 */
@Suppress("LongParameterList", "FunctionNaming")
@Composable
fun ModalScrollableTextDialog(
    title: String,
    content: String,
    closeText: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    maxContentHeight: Dp = 520.dp,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        shape = ModalDialogShape,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxContentHeight)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
            ) {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            ModalTextButton(
                text = closeText,
                onClick = onDismiss,
            )
        },
    )
}
