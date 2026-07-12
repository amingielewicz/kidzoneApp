package com.kidzone.presentation.place.details

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Accessible
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.BabyChangingStation
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Deck
import androidx.compose.material.icons.filled.Fence
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Microwave
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stroller
import androidx.compose.material.icons.filled.Toys
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kidzone.R
import com.kidzone.domain.model.Amenity
import com.kidzone.domain.model.Place
import com.kidzone.domain.model.PlaceCategory
import com.kidzone.presentation.common.style
import com.kidzone.presentation.place.add.PLACE_NAME_MAX_LENGTH

private const val COMMENT_MAX_LENGTH = 500
internal const val CHANGE_REQUEST_COMMENT_PREFIX = "__KIDZONE_COMMENT__:"
@Suppress("MagicNumber")
private val SelectedAmenityColor = Color(0xFF2E7D32)
@Suppress("MagicNumber")
private val FocusedFieldColor = Color(0xFF1976D2)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SuggestEditSheet(
    place: Place,
    onSubmit: (name: String, description: String, category: String, amenities: Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember { mutableStateOf(place.name) }
    var description by remember { mutableStateOf(place.description) }
    var selectedCategory by remember { mutableStateOf(place.category) }
    var selectedAmenities by remember { mutableStateOf(place.amenities.map { it.name }.toSet()) }
    var comment by remember { mutableStateOf("") }

    val originalAmenities = remember(place.amenities) { place.amenities.map { it.name }.toSet() }
    val hasChanges = name.trim() != place.name ||
        description.trim() != place.description ||
        selectedCategory != place.category ||
        selectedAmenities != originalAmenities
    val canSubmit = name.trim().isNotBlank() && comment.trim().isNotBlank() && hasChanges

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .heightIn(max = 700.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.suggest_edit),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.suggest_edit_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(PLACE_NAME_MAX_LENGTH) },
                label = { Text(stringResource(R.string.place_name_label)) },
                singleLine = true,
                supportingText = {
                    Text("${name.length}/$PLACE_NAME_MAX_LENGTH")
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.place_description_label)) },
                minLines = 2,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            CategoryDropdownSuggest(
                selected = selectedCategory,
                onSelected = { selectedCategory = it }
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.amenities_label),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            val applicable = remember(selectedCategory) {
                Amenity.forCategory(selectedCategory)
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                applicable.forEach { amenity ->
                    val selected = amenity.name in selectedAmenities
                    FilterChip(
                        selected = selected,
                        onClick = {
                            selectedAmenities = if (selected) {
                                selectedAmenities - amenity.name
                            } else {
                                selectedAmenities + amenity.name
                            }
                        },
                        label = { Text(stringResource(amenity.labelRes)) },
                        leadingIcon = {
                            Icon(
                                imageVector = amenityIcon(amenity),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (selected) SelectedAmenityColor else MaterialTheme.colorScheme.outlineVariant
                        ),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.White,
                            labelColor = MaterialTheme.colorScheme.onSurface,
                            iconColor = MaterialTheme.colorScheme.onSurface,
                            selectedContainerColor = SelectedAmenityColor,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        )
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it.take(COMMENT_MAX_LENGTH) },
                label = {
                    Row {
                        Text("Opisz, co i dlaczego zmieniono")
                        Text(" *", color = MaterialTheme.colorScheme.error)
                    }
                },
                minLines = 3,
                maxLines = 5,
                supportingText = {
                    Text(
                        text = "${comment.length}/$COMMENT_MAX_LENGTH",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FocusedFieldColor,
                    focusedLabelColor = FocusedFieldColor,
                    cursorColor = FocusedFieldColor
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    val payloadAmenities = selectedAmenities +
                        "$CHANGE_REQUEST_COMMENT_PREFIX${comment.trim()}"
                    onSubmit(name, description, selectedCategory.name, payloadAmenities)
                },
                enabled = canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(stringResource(R.string.submit_suggested_edit))
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Suppress("CyclomaticComplexMethod")
private fun amenityIcon(amenity: Amenity): ImageVector = when (amenity) {
    Amenity.CHANGING_TABLE -> Icons.Filled.BabyChangingStation
    Amenity.TOILET -> Icons.Filled.Wc
    Amenity.WHEELCHAIR_ACCESSIBLE, Amenity.WIDE_DOORS -> Icons.AutoMirrored.Filled.Accessible
    Amenity.PARKING, Amenity.FAMILY_PARKING -> Icons.Filled.LocalParking
    Amenity.FENCED -> Icons.Filled.Fence
    Amenity.SOFT_SURFACE, Amenity.SOFT_SAFETY -> Icons.Filled.Grass
    Amenity.SHADE, Amenity.REST_AREAS, Amenity.PARENT_ZONE -> Icons.Filled.Deck
    Amenity.TODDLER_ZONE, Amenity.AGE_ZONES, Amenity.PARENT_CHILD_ROOM -> Icons.Filled.ChildCare
    Amenity.KIDS_MENU, Amenity.KIDS_TABLEWARE, Amenity.FAST_SERVICE -> Icons.Filled.Restaurant
    Amenity.HIGH_CHAIR -> Icons.Filled.Chair
    Amenity.KIDS_ENTERTAINMENT, Amenity.SENSORY_TOYS, Amenity.TOY_SANITIZATION -> Icons.Filled.Toys
    Amenity.MONITORING -> Icons.Filled.Videocam
    Amenity.LOCKERS -> Icons.Filled.Lock
    Amenity.WIFI -> Icons.Filled.Wifi
    Amenity.MICROWAVE -> Icons.Filled.Microwave
    Amenity.NO_LOUD_MUSIC, Amenity.QUIET_AREAS, Amenity.QUIET_FEEDING -> Icons.AutoMirrored.Filled.VolumeOff
    Amenity.DRINKING_WATER, Amenity.BREASTFEEDING_AREA -> Icons.Filled.WaterDrop
    Amenity.STROLLER_RENTAL -> Icons.Filled.Stroller
    Amenity.KIDS_CORNER_VISIBLE, Amenity.EVENING_LIGHTING -> Icons.Filled.Star
    Amenity.LOW_TRAFFIC, Amenity.PICNIC_AREA, Amenity.SAFE_PATHS,
    Amenity.FAMILY_FAST_TRACK, Amenity.LOST_CHILD_POINT, Amenity.KID_FRIENDLY_SIGNS,
    Amenity.ANIMATOR -> Icons.Filled.LocationOn
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdownSuggest(
    selected: PlaceCategory,
    onSelected: (PlaceCategory) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedStyle = selected.style

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = stringResource(selected.labelRes),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.category_label)) },
            leadingIcon = {
                Icon(
                    imageVector = selectedStyle.icon,
                    contentDescription = null,
                    tint = selectedStyle.color
                )
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            PlaceCategory.entries.forEach { category ->
                val style = category.style
                DropdownMenuItem(
                    text = { Text(stringResource(category.labelRes)) },
                    leadingIcon = {
                        Icon(
                            imageVector = style.icon,
                            contentDescription = null,
                            tint = style.color
                        )
                    },
                    onClick = {
                        onSelected(category)
                        expanded = false
                    }
                )
            }
        }
    }
}
