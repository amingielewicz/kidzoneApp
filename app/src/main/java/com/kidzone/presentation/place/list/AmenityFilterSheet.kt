package com.kidzone.presentation.place.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.annotation.StringRes
import com.kidzone.domain.model.Amenity
import com.kidzone.domain.model.PlaceCategory
import com.kidzone.R

/**
 * Pojedyncza sekcja w sheecie filtrów udogodnień.
 *
 * @property titleRes id zasobu tytułu sekcji wyświetlanego w headerze
 * @property amenities lista udogodnień należących do sekcji
 * @property matchingCategories kategorie dla których ta sekcja jest istotna –
 *   gdy aktualnie wybrana kategoria do nich pasuje, sekcja jest auto-rozwinięta.
 *   Pusty zbiór = sekcja "ogólna" (zawsze może być relevant).
 */
private data class AmenitySection(
    @StringRes val titleRes: Int,
    val amenities: List<Amenity>,
    val matchingCategories: Set<PlaceCategory>
)

// Kolejność sekcji odzwierciedla kolejność kategorii w PlaceCategory enum:
// Ogólne (wspólne) → Plac zabaw → Sala zabaw → Kawiarnia/Restauracja →
// Park → Atrakcja. Spójne z chipami kategorii na ekranie listy.
private val SHEET_SECTIONS: List<AmenitySection> = listOf(
    AmenitySection(
        titleRes = R.string.amenity_section_general,
        amenities = listOf(
            Amenity.CHANGING_TABLE,
            Amenity.TOILET,
            Amenity.STROLLER_ACCESS,
            Amenity.PARKING,
            Amenity.WIDE_DOORS,
            Amenity.FAMILY_PARKING,
            Amenity.KID_FRIENDLY_SIGNS,
            Amenity.WIFI,
            Amenity.QUIET_AREAS
        ),
        matchingCategories = emptySet()
    ),
    AmenitySection(
        titleRes = R.string.amenity_section_playground,
        amenities = listOf(
            Amenity.FENCING,
            Amenity.SOFT_SURFACE,
            Amenity.SHADED_BENCHES,
            Amenity.TODDLER_ZONE,
            Amenity.CAR_FREE_AREA,
            Amenity.SOFT_PROTECTION,
            Amenity.GOOD_LIGHTING
        ),
        matchingCategories = setOf(PlaceCategory.PLAYGROUND)
    ),
    AmenitySection(
        titleRes = R.string.amenity_section_play_room,
        amenities = listOf(
            Amenity.AGE_ZONES,
            Amenity.ANIMATOR,
            Amenity.MONITORING,
            Amenity.TOY_SANITIZATION,
            Amenity.PARENT_ZONE,
            Amenity.LOCKERS,
            Amenity.SOFT_PROTECTION
        ),
        matchingCategories = setOf(PlaceCategory.PLAY_ROOM)
    ),
    AmenitySection(
        titleRes = R.string.amenity_section_food,
        amenities = listOf(
            Amenity.KIDS_MENU,
            Amenity.HIGH_CHAIR,
            Amenity.KIDS_TABLEWARE,
            Amenity.FAST_SERVICE,
            Amenity.KIDS_ENTERTAINMENT,
            Amenity.KIDS_CORNER_VISIBLE,
            Amenity.QUIET_FEEDING,
            Amenity.MICROWAVE,
            Amenity.NO_LOUD_MUSIC,
            Amenity.SENSORY_TOYS
        ),
        matchingCategories = setOf(PlaceCategory.CAFE, PlaceCategory.RESTAURANT)
    ),
    AmenitySection(
        titleRes = R.string.amenity_section_park,
        amenities = listOf(
            Amenity.PICNIC_AREA,
            Amenity.SAFE_PATHS,
            Amenity.DRINKING_WATER,
            Amenity.BREASTFEEDING_AREA,
            Amenity.GOOD_LIGHTING
        ),
        matchingCategories = setOf(PlaceCategory.PARK)
    ),
    AmenitySection(
        titleRes = R.string.amenity_section_attraction,
        amenities = listOf(
            Amenity.STROLLER_RENTAL,
            Amenity.REST_AREAS,
            Amenity.FAMILY_FAST_TRACK,
            Amenity.PARENT_CHILD_ROOM,
            Amenity.LOST_CHILD_POINT
        ),
        matchingCategories = setOf(PlaceCategory.ATTRACTION)
    )
)

/**
 * Bottom sheet z filtrami udogodnień.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AmenityFilterSheet(
    sheetState: SheetState,
    selectedCategory: PlaceCategory?,
    selectedAmenities: Set<Amenity>,
    totalResultsCount: Int,
    onAmenityToggled: (Amenity) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .heightIn(max = 600.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.amenity_filters_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (selectedAmenities.isNotEmpty()) {
                    TextButton(onClick = onClearAll) {
                        Text(stringResource(R.string.clear_with_count, selectedAmenities.size))
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Sekcje (scroll)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                val visibleSections = remember(selectedCategory) {
                    if (selectedCategory == null) {
                        SHEET_SECTIONS
                    } else {
                        SHEET_SECTIONS.filter { section ->
                            section.matchingCategories.isEmpty() ||
                                selectedCategory in section.matchingCategories
                        }
                    }
                }

                val sectionsWithFilteredAmenities = remember(selectedCategory, visibleSections) {
                    if (selectedCategory == null) {
                        visibleSections
                    } else {
                        visibleSections.map { section ->
                            section.copy(
                                amenities = section.amenities.filter { amenity ->
                                    selectedCategory in amenity.applicableCategories
                                }
                            )
                        }.filter { it.amenities.isNotEmpty() }
                    }
                }

                val expandedMap = remember(selectedCategory) {
                    mutableStateMapOf<Int, Boolean>().apply {
                        sectionsWithFilteredAmenities.forEach { section ->
                            put(
                                section.titleRes,
                                if (selectedCategory == null) {
                                    section.matchingCategories.isEmpty() // Ogólne
                                } else {
                                    section.shouldAutoExpand(selectedCategory)
                                }
                            )
                        }
                    }
                }

                sectionsWithFilteredAmenities.forEach { section ->
                    SectionItem(
                        section = section,
                        expanded = expandedMap[section.titleRes] ?: false,
                        onToggleExpand = {
                            expandedMap[section.titleRes] = !(expandedMap[section.titleRes] ?: false)
                        },
                        selectedAmenities = selectedAmenities,
                        onAmenityToggled = onAmenityToggled
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            HorizontalDivider()

            // Footer – Pokaż wyniki
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                colors = ButtonDefaults.buttonColors()
            ) {
                Text(stringResource(R.string.show_results_with_count, totalResultsCount))
            }
        }
    }
}

private fun AmenitySection.shouldAutoExpand(selectedCategory: PlaceCategory?): Boolean {
    if (selectedCategory == null) return false
    if (matchingCategories.isEmpty()) return true // "Ogólne" always expanded
    return selectedCategory in matchingCategories
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun SectionItem(
    section: AmenitySection,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    selectedAmenities: Set<Amenity>,
    onAmenityToggled: (Amenity) -> Unit
) {
    val selectedInSection = section.amenities.count { it in selectedAmenities }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevronRotation")
    val title = stringResource(section.titleRes)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(
                    onClickLabel = if (expanded) {
                        stringResource(R.string.collapse_section, title)
                    } else {
                        stringResource(R.string.expand_section, title)
                    },
                    role = Role.Button,
                    onClick = onToggleExpand
                )
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            if (selectedInSection > 0) {
                Box(
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = "$selectedInSection / ${section.amenities.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Text(
                    text = "${section.amenities.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = null,
                modifier = Modifier.rotate(rotation)
            )
        }

        AnimatedVisibility(visible = expanded) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val sortedAmenities = remember(section, context) {
                section.amenities.sortedBy { context.getString(it.labelRes).lowercase() }
            }
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                sortedAmenities.forEach { amenity ->
                    FilterChip(
                        selected = amenity in selectedAmenities,
                        onClick = { onAmenityToggled(amenity) },
                        label = { Text(stringResource(amenity.labelRes)) }
                    )
                }
            }
        }

        HorizontalDivider()
    }
}
