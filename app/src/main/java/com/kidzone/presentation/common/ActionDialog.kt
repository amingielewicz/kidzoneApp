@file:Suppress("FunctionNaming", "LongParameterList")

package com.kidzone.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kidzone.R

/**
 * 🎯 Odpowiedzialności:
 * - Standaryzacja wyglądu i zachowania dialogów akcji w aplikacji.
 * - Spójne rozmieszczenie ikon, tytułów i przycisków potwierdzenia.
 *
 * ✅ Gwarancje:
 * - Wykorzystanie motywu Material Design 3.
 * - Prawidłowe zachowanie na różnych szerokościach ekranu ([ACTION_DIALOG_MAX_WIDTH]).
 */
@Composable
fun KidZoneActionDialog(
    title: String,
    icon: ImageVector,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    confirmButton: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .widthIn(max = ACTION_DIALOG_MAX_WIDTH),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = ACTION_DIALOG_TONAL_ELEVATION
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(ACTION_DIALOG_PADDING),
                verticalArrangement = Arrangement.spacedBy(ACTION_DIALOG_SECTION_SPACING)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall
                )
                content()
                confirmButton()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        }
    }
}

private val ACTION_DIALOG_PADDING = 24.dp
private val ACTION_DIALOG_SECTION_SPACING = 16.dp
private val ACTION_DIALOG_MAX_WIDTH = 560.dp
private val ACTION_DIALOG_TONAL_ELEVATION = 6.dp
