package com.kidzone.presentation.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

object KidZoneRadii {
    val Card = 8.dp
    val Control = 8.dp
    val Badge = 50.dp
}

object KidZoneSpacing {
    val Screen = 16.dp
    val Card = 16.dp
    val CardCompact = 12.dp
    val Gap = 12.dp
    val GapSmall = 8.dp
    val GapTiny = 4.dp
}

object KidZoneElevation {
    val Card = 0.dp
}

val KidZoneCardPadding = PaddingValues(KidZoneSpacing.Card)

@Composable
fun KidZoneCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(KidZoneRadii.Card),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = KidZoneElevation.Card),
        content = content
    )
}
