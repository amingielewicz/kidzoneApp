package com.kidzone.presentation.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kidzone.domain.model.Amenity

/**
 * Wspólny komponent siatki udogodnień.
 * Używany w AddPlace, PlaceDetails i filtrach.
 */
@Suppress("LongParameterList", "FunctionNaming")
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AmenitiesFlowGrid(
    amenities: List<Amenity>,
    modifier: Modifier = Modifier,
    selectedAmenities: Set<Amenity> = emptySet(),
    onToggle: ((Amenity) -> Unit)? = null,
    enabled: Boolean = true,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(8.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(10.dp)
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement
    ) {
        amenities.forEach { amenity ->
            AmenityChip(
                amenity = amenity,
                selected = if (onToggle != null) amenity in selectedAmenities else false,
                enabled = enabled,
                onClick = onToggle?.let { toggle -> { toggle(amenity) } }
            )
        }
    }
}

/**
 * Pojedynczy "Chip" udogodnienia z ikoną i tekstem.
 */
@Suppress("FunctionNaming")
@Composable
fun AmenityChip(
    amenity: Amenity,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val border = BorderStroke(
        width = 1.dp,
        color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant
    )

    Surface(
        modifier = modifier.let { baseModifier ->
            if (onClick != null) {
                baseModifier.clickable(enabled = enabled, onClick = onClick)
            } else {
                baseModifier
            }
        },
        shape = RoundedCornerShape(999.dp),
        color = containerColor,
        contentColor = contentColor,
        border = border
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = amenityIcon(amenity),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(amenity.labelRes),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
