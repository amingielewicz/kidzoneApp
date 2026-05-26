package com.playword.presentation.place.add

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.playword.R

/**
 * Dodawanie nowego miejsca – placeholder.
 *
 * Docelowo: formularz (nazwa, opis, kategoria, lokalizacja z mapy, adres,
 * checkboxy udogodnień, dodawanie zdjęć), zapis przez [com.playword.domain.repository.PlaceRepository].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlaceScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_place)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "TODO: formularz dodawania miejsca",
                style = MaterialTheme.typography.titleMedium
            )
            // [onSaved] wywołamy po pomyślnym zapisie miejsca.
        }
    }
}
