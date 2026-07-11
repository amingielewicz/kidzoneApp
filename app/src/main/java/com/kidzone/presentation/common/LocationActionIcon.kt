@file:Suppress("FunctionNaming")

package com.kidzone.presentation.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LocationActionIcon(
    isReady: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(LOCATION_ACTION_CONTAINER_SIZE),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.MyLocation,
            contentDescription = null,
            modifier = Modifier.size(LOCATION_ACTION_ICON_SIZE),
            tint = if (isReady) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        if (!isReady) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .size(LOCATION_ACTION_STATUS_ICON_SIZE)
                    .align(Alignment.BottomEnd)
            )
        }
    }
}

private val LOCATION_ACTION_CONTAINER_SIZE = 20.dp
private val LOCATION_ACTION_ICON_SIZE = 18.dp
private val LOCATION_ACTION_STATUS_ICON_SIZE = 12.dp
