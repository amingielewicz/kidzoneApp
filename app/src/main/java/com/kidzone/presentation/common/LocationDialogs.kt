package com.kidzone.presentation.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.kidzone.R

/**
 * 🎯 Odpowiedzialności:
 * - Wyświetlanie systemowych dialogów zachęcających do włączenia usług lokalizacji.
 */
@Composable
@Suppress("FunctionNaming")
fun EnableLocationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.MyLocation,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text(stringResource(R.string.gps_disabled_title)) },
        text = { Text(stringResource(R.string.error_location_service_disabled)) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.home_enable_location))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
