package com.kidzone.presentation.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kidzone.R

/**
 * Ekran startowy: pokazuje logo aplikacji i decyduje, czy wypchnąć
 * użytkownika do logowania, czy do głównego shella.
 *
 * Wizualnie: pełny ekran w `colorScheme.primary` (BrandBlue) z logiem
 * brandowym (PNG z napisem i nazwą aplikacji wewnątrz) i progress
 * indicatorem pod spodem.
 *
 * Świadomie nie pokazujemy osobnego `Text(app_name)` ani `Text(app_tagline)`
 * – logo zawiera już je w sobie, dublowanie wyglądałoby krzywo.
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
            // Logo brandowe (zawiera w sobie napis i nazwę aplikacji),
            // dlatego pod spodem nie dajemy już osobnego Text(app_name).
            // fillMaxWidth(0.6f) zamiast sztywnego dp – logo z napisem
            // skaluje się procentowo lepiej niż przy stałej wysokości.
            Image(
                painter = painterResource(R.drawable.ic_splash_logo),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.fillMaxWidth(0.6f)
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
