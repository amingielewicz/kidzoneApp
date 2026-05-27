package com.playground.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Profil użytkownika – placeholder.
 *
 * Docelowo: avatar, nazwa, e-mail, liczba dodanych miejsc, liczba opinii,
 * lista odznak, przycisk "Wyloguj".
 */
@Composable
fun ProfileScreen(
    onSignOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Profil",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "TODO: avatar, statystyki (miejsca/opinie), odznaki",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Wyloguj")
        }
    }
}
