package com.playword.presentation.place.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Lista miejsc – placeholder.
 *
 * Docelowo: LazyColumn kart (zdjęcie, nazwa, kategoria, ocena), filtry górne,
 * pull-to-refresh, klik karty → [onOpenPlaceDetails].
 */
@Composable
fun PlaceListScreen(
    onOpenPlaceDetails: (placeId: String) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "TODO: lista miejsc + filtry",
            style = MaterialTheme.typography.titleMedium
        )
    }
}
