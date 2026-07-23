@file:Suppress("FunctionNaming", "LongParameterList")

package com.kidzone.presentation.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * 🎯 Odpowiedzialności:
 * - Renderowanie ujednoliconych chipów filtrujących (Filter Chips) zgodnie z Material Design 3.
 * - Obsługa spójnej kolorystyki dla stanów aktywnych i nieaktywnych.
 */
@Composable
fun KidZoneFilterChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    inactiveContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val selectedContainerColor = MaterialTheme.colorScheme.secondary
    val selectedContentColor = MaterialTheme.colorScheme.onSecondary
    val inactiveContainerColor = MaterialTheme.colorScheme.surface
    val inactiveBorderColor = MaterialTheme.colorScheme.outlineVariant
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        label = {
            Text(text = label)
        },
        leadingIcon = icon?.let {
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null
                )
            }
        },
        shape = FILTER_CHIP_SHAPE,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = inactiveContainerColor,
            labelColor = inactiveContentColor,
            iconColor = inactiveContentColor,
            selectedContainerColor = selectedContainerColor,
            selectedLabelColor = selectedContentColor,
            selectedLeadingIconColor = selectedContentColor
        ),
        border = BorderStroke(
            width = FILTER_CHIP_BORDER_WIDTH,
            color = if (selected) selectedContainerColor else inactiveBorderColor
        )
    )
}

private val FILTER_CHIP_BORDER_WIDTH = 1.dp
private const val FILTER_CHIP_SHAPE_PERCENT = 50
private val FILTER_CHIP_SHAPE = RoundedCornerShape(FILTER_CHIP_SHAPE_PERCENT)
