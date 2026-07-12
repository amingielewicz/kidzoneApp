@file:Suppress("FunctionNaming", "LongParameterList")

package com.kidzone.presentation.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun KidZoneDropdownMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    enabled: Boolean = true
) {
    DropdownMenuItem(
        text = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        leadingIcon = leadingIcon?.let { icon ->
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(DROPDOWN_MENU_ICON_SIZE)
                )
            }
        },
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        contentPadding = PaddingValues(
            horizontal = DROPDOWN_MENU_HORIZONTAL_PADDING,
            vertical = DROPDOWN_MENU_VERTICAL_PADDING
        )
    )
}

private val DROPDOWN_MENU_HORIZONTAL_PADDING = 16.dp
private val DROPDOWN_MENU_VERTICAL_PADDING = 10.dp
private val DROPDOWN_MENU_ICON_SIZE = 20.dp
