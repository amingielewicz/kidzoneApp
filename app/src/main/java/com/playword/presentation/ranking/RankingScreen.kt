package com.playword.presentation.ranking

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Ranking miejsc i użytkowników – placeholder.
 *
 * Docelowo: zakładki "TOP 10 miejsc" / "Ranking użytkowników",
 * sekcja odznak (odkrywca, recenzent, ekspert rodzinny).
 */
@Composable
fun RankingScreen(
    onOpenPlaceDetails: (placeId: String) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "TODO: TOP 10 miejsc + ranking użytkowników + odznaki",
            style = MaterialTheme.typography.titleMedium
        )
    }
}
