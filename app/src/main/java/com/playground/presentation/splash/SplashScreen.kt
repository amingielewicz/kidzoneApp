package com.playground.presentation.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.playground.R

/**
 * Ekran startowy: pokazuje logo / nazwę i decyduje, czy wypchnąć
 * użytkownika do logowania, czy do głównego shella.
 *
 * Wizualnie: pełny ekran w `colorScheme.primary` (BrandBlue) z białym logiem
 * (ikona miejsca), nazwą aplikacji, taglinem i progress indicator pod spodem.
 */
@Composable
fun SplashScreen(
    viewModel: SplashViewModel = hiltViewModel(),
    onSignedIn: () -> Unit,
    onSignedOut: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state) {
        when (state) {
            SplashViewModel.State.SignedIn -> onSignedIn()
            SplashViewModel.State.SignedOut -> onSignedOut()
            SplashViewModel.State.Loading -> Unit
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Tymczasowe logo: ikona Place w kolorze onPrimary. Gdy będzie
            // gotowy assets ic_logo.xml, podmienić na painterResource.
            Icon(
                imageVector = Icons.Filled.Place,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(96.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.height(32.dp))
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
