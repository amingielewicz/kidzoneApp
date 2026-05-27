package com.playground.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.playground.R

/**
 * Ekran startowy po zalogowaniu – placeholder.
 *
 * Docelowo: hero z hasłem, sekcje "Top miejsca", "Blisko Ciebie", CTA do mapy.
 */
@Composable
fun HomeScreen(
    onOpenPlaceDetails: (placeId: String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = stringResource(R.string.app_tagline),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "TODO: hero, top miejsca, blisko Ciebie, CTA do mapy",
            style = MaterialTheme.typography.bodyLarge
        )
        // [onOpenPlaceDetails] wywołamy po kliknięciu karty miejsca w sekcjach.
    }
}
