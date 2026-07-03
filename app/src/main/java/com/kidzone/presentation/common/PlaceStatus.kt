package com.kidzone.presentation.common

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kidzone.domain.model.Place

private const val NEW_PLACE_WINDOW_DAYS = 14L
private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L

fun Place.isNewWithoutReviews(nowMillis: Long = System.currentTimeMillis()): Boolean {
    if (reviewsCount > 0 || createdAtMillis <= 0L) return false

    val ageMillis = nowMillis - createdAtMillis
    return ageMillis in 0..(NEW_PLACE_WINDOW_DAYS * MILLIS_PER_DAY)
}

@Composable
fun NewPlaceBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .widthIn(min = 46.dp)
            .heightIn(min = 24.dp),
        shape = RoundedCornerShape(KidZoneRadii.Badge),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
    ) {
        Text(
            text = "Nowe",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
        )
    }
}
