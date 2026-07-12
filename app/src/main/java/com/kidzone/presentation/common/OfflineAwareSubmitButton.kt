@file:Suppress("FunctionNaming", "LongParameterList")

package com.kidzone.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kidzone.R

@Composable
fun OfflineAwareSubmitButton(
    label: String,
    onClick: () -> Unit,
    isOffline: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    offlineLabel: String = stringResource(R.string.add_place_offline_save_action),
    offlineHint: String = stringResource(R.string.add_place_offline_hint_short)
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(OFFLINE_SUBMIT_HINT_SPACING),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onClick,
            enabled = enabled && !isOffline && !isLoading,
            colors = colors,
            modifier = Modifier
                .fillMaxWidth()
                .height(OFFLINE_SUBMIT_BUTTON_HEIGHT)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(OFFLINE_SUBMIT_PROGRESS_SIZE),
                    strokeWidth = OFFLINE_SUBMIT_PROGRESS_STROKE_WIDTH,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    text = if (isOffline) offlineLabel else label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (isOffline) {
            Text(
                text = offlineHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private val OFFLINE_SUBMIT_BUTTON_HEIGHT = 48.dp
private val OFFLINE_SUBMIT_PROGRESS_SIZE = 20.dp
private val OFFLINE_SUBMIT_PROGRESS_STROKE_WIDTH = 2.dp
private val OFFLINE_SUBMIT_HINT_SPACING = 6.dp
