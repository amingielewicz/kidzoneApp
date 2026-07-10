package com.kidzone.presentation.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import com.kidzone.R

@Composable
fun RatingIcon(
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    contentDescription: String? = stringResource(R.string.rating)
) {
    Icon(
        imageVector = Icons.Filled.Star,
        contentDescription = contentDescription,
        tint = MaterialTheme.colorScheme.tertiary,
        modifier = modifier.then(Modifier.size(size))
    )
}