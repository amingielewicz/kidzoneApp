package com.playword.presentation.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Ekran mapy – placeholder.
 *
 * Docelowo: GoogleMap (maps-compose) z pinezkami z [com.playword.domain.model.Place],
 * filtrami (kategoria, najlepiej oceniane, darmowe) i przyciskiem "blisko mnie".
 * Na klik pinezki: pokazujemy bottom sheet z miniaturą + przyciskiem "Zobacz szczegóły"
 * wywołującym [onOpenPlaceDetails].
 */
@Composable
fun MapScreen(
    onOpenPlaceDetails: (placeId: String) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "TODO: GoogleMap + pinezki + filtry",
            style = MaterialTheme.typography.titleMedium
        )
    }
}
