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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kidzone.R

/**
 * Ekran startowy: pokazuje logo aplikacji i decyduje, czy wypchnąć
 * użytkownika do logowania, czy do głównego shella.
 *
 * Wizualnie: tło w bardzo jasnym chłodnym błękicie (#F5F8FB – echo
 * brand-blue logo) z logiem brandowym (PNG z napisem i nazwą aplikacji
 * wewnątrz) zajmującym 75% szerokości i progress indicator w kolorze
 * brand-blue pod spodem.
 *
 * Tło NIE jest `colorScheme.primary` (BrandBlue), bo logo ma napisy
 * w kolorze szarym z transparentem – na ciemnym tle znikały. Wybrany
 * jasny błękit jest na tyle delikatny, że logo pozostaje czytelne,
 * a jednocześnie podkreśla błękitne akcenty z palety logo (~43% pikseli).
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
            .background(Color(0xFFF5F8FB))
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo brandowe (zawiera w sobie napis i nazwę aplikacji),
            // dlatego pod spodem nie dajemy już osobnego Text(app_name).
            // fillMaxWidth(0.75f) zamiast sztywnego dp – logo z napisem
            // skaluje się procentowo lepiej niż przy stałej wysokości.
            Image(
                painter = painterResource(R.drawable.ic_splash_logo),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.fillMaxWidth(0.75f)
            )
            Spacer(modifier = Modifier.height(32.dp))
            CircularProgressIndicator(
                // Brand-blue na białym tle – kontrastowo i spójnie z resztą
                // aplikacji (FilterChip selected, FAB itd. też używają primary).
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
