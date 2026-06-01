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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kidzone.domain.model.Amenity
import com.kidzone.domain.model.PlaceCategory

/**
 * Pojedyncza sekcja w sheecie filtrów udogodnień.
 *
 * @property title tytuł sekcji wyświetlany w headerze
 * @property amenities lista udogodnień należących do sekcji
 * @property matchingCategories kategorie dla których ta sekcja jest istotna –
 *   gdy aktualnie wybrana kategoria do nich pasuje, sekcja jest auto-rozwinięta.
 *   Pusty zbiór = sekcja "ogólna" (zawsze może być relevant).
 */
private data class AmenitySection(
    val title: String,
    val amenities: List<Amenity>,
    val matchingCategories: Set<PlaceCategory>
)

// Kolejność sekcji odzwierciedla kolejność kategorii w PlaceCategory enum:
// Ogólne (wspólne) → Plac zabaw → Sala zabaw → Kawiarnia/Restauracja →
// Park → Atrakcja. Spójne z chipami kategorii na ekranie listy.
private val SHEET_SECTIONS: List<AmenitySection> = listOf(
    AmenitySection(
        title = "Ogólne",
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
        title = "Plac zabaw",
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
        title = "Sala zabaw",
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
        title = "Kawiarnia / Restauracja",
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
        title = "Park",
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
        title = "Atrakcja",
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
 *
 * Zachowanie zależy od wybranej kategorii:
 *  - **Wszystkie** (selectedCategory = null): pokazuje WSZYSTKIE udogodnienia
 *    pogrupowane w sekcje (jak dotychczas).
 *  - **Konkretna kategoria** (np. Plac zabaw): pokazuje tylko udogodnienia
 *    pasujące do tej kategorii (sprawdza [Amenity.applicableCategories]).
 *    Udogodnienia "wspólne" (Przewijak, Czysta toaleta itp.) są zawsze
 *    widoczne, bo ich `applicableCategories` zawiera wszystkie/większość
 *    kategorii.
 *
 * @param totalResultsCount aktualna liczba miejsc po wszystkich filtrach
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
                    text = "Filtry udogodnień",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (selectedAmenities.isNotEmpty()) {
                    TextButton(onClick = onClearAll) {
                        Text("Wyczyść (${selectedAmenities.size})")
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
                // Filtruj sekcje – gdy wybrana kategoria, pokazujemy tylko
                // sekcje pasujące do niej (+ sekcję "Ogólne" zawsze).
                val visibleSections = remember(selectedCategory) {
                    if (selectedCategory == null) {
                        SHEET_SECTIONS
                    } else {
                        SHEET_SECTIONS.filter { section ->
                            // "Ogólne" (matchingCategories = empty) zawsze widoczne
                            section.matchingCategories.isEmpty() ||
                                selectedCategory in section.matchingCategories
                        }
                    }
                }

                // Filtruj udogodnienia wewnątrz sekcji – gdy kategoria jest
                // wybrana, pokaż tylko te amenities które mają tę kategorię
                // w swoich applicableCategories.
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

                // Stan rozwinięcia per sekcja – init zależnie od selectedCategory.
                val expandedMap = remember(selectedCategory) {
                    mutableStateMapOf<String, Boolean>().apply {
                        sectionsWithFilteredAmenities.forEach { section ->
                            // Gdy kategoria wybrana – auto-expand pasujące sekcje.
                            // Gdy "Wszystkie" – auto-expand sekcję "Ogólne".
                            put(
                                section.title,
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
                        expanded = expandedMap[section.title] ?: false,
                        onToggleExpand = {
                            expandedMap[section.title] = !(expandedMap[section.title] ?: false)
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
                Text("Pokaż wyniki ($totalResultsCount)")
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

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpand)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = section.title,
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
                contentDescription = if (expanded) "Zwiń" else "Rozwiń",
                modifier = Modifier.rotate(rotation)
            )
        }

        AnimatedVisibility(visible = expanded) {
            val context = androidx.compose.ui.platform.LocalContext.current
            // Udogodnienia w sekcji alfabetycznie po polskim labelu
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
