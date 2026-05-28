package com.kidzone.presentation.profile

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Profil użytkownika – na razie pokazuje nazwę + e-mail i przycisk
 * wylogowania. Statystyki (liczba miejsc / opinii) i odznaki dołożymy
 * w kolejnej iteracji.
 */
@Composable
fun ProfileScreen(
    onSignOut: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val user by viewModel.currentUser.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = user?.name?.ifBlank { "Użytkownik" } ?: "Użytkownik",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        user?.email?.takeIf { it.isNotBlank() }?.let { email ->
            Spacer(Modifier.height(4.dp))
            Text(
                text = email,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "TODO: avatar, statystyki (miejsca/opinie), odznaki",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { viewModel.signOut(onSignOut) },
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Wyloguj")
        }
    }
}
