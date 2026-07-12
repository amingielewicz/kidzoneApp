@file:Suppress("FunctionNaming")

package com.kidzone.presentation.common

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kidzone.R
import com.kidzone.domain.model.PlaceCategory

@Composable
fun KidZoneCategoryFilterBar(
    selectedCategory: PlaceCategory?,
    onCategorySelected: (PlaceCategory?) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(CATEGORY_FILTER_SPACING)
    ) {
        KidZoneFilterChip(
            selected = selectedCategory == null,
            onClick = { onCategorySelected(null) },
            label = stringResource(R.string.category_all)
        )
        PlaceCategory.entries.forEach { category ->
            val style = category.style
            KidZoneFilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = stringResource(category.labelRes),
                icon = style.icon,
                inactiveContentColor = style.color
            )
        }
    }
}

private val CATEGORY_FILTER_SPACING = 8.dp
