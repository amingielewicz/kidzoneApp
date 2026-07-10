@file:Suppress("FunctionNaming", "LongMethod", "MatchingDeclarationName")

package com.kidzone.presentation.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kidzone.R

data class SortMenuOption<T>(
    val value: T,
    val label: String
)

@Composable
fun <T> KidZoneSortMenu(
    current: T,
    currentLabel: String,
    options: List<SortMenuOption<T>>,
    onChange: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        AssistChip(
            onClick = { expanded = true },
            label = {
                Text(text = stringResource(R.string.sort_prefix, currentLabel))
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = null,
                    modifier = Modifier.size(SORT_MENU_ICON_SIZE)
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(SORT_MENU_ICON_SIZE)
                )
            },
            shape = SORT_MENU_CHIP_SHAPE,
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shadowElevation = SORT_MENU_SHADOW_ELEVATION,
            shape = SORT_MENU_DROPDOWN_SHAPE
        ) {
            options.forEach { option ->
                val selected = option.value == current
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    trailingIcon = if (selected) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(SORT_MENU_ICON_SIZE)
                            )
                        }
                    } else {
                        null
                    },
                    onClick = {
                        expanded = false
                        onChange(option.value)
                    }
                )
            }
        }
    }
}

private val SORT_MENU_ICON_SIZE = 18.dp
private val SORT_MENU_SHADOW_ELEVATION = 4.dp
private const val SORT_MENU_CHIP_SHAPE_PERCENT = 50
private val SORT_MENU_CHIP_SHAPE = RoundedCornerShape(SORT_MENU_CHIP_SHAPE_PERCENT)
private val SORT_MENU_DROPDOWN_SHAPE = RoundedCornerShape(12.dp)
