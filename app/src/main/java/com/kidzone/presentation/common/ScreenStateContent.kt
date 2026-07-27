package com.kidzone.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kidzone.R

/**
 * Wspólny komponent Compose odpowiedzialny za renderowanie stanów ekranu.
 *
 * @param state bieżący stan ekranu ([ScreenState]).
 * @param onRetry akcja wywoływana po kliknięciu przycisku "Spróbuj ponownie" w stanie Error.
 * @param modifier modyfikator układu.
 * @param loading composable wyświetlany w stanie [ScreenState.Loading] (np. skeleton).
 * @param content composable wyświetlany w stanie [ScreenState.Content], otrzymuje załadowane dane.
 * @param empty composable wyświetlany w stanie [ScreenState.Empty] (opcjonalny).
 * @param fallbackActionLabel etykieta dodatkowego przycisku akcji w stanie Error (np. "Wyloguj").
 * @param onFallbackAction akcja wywoływana przez dodatkowy przycisk w stanie Error.
 */
@Suppress("FunctionNaming", "LongParameterList")
@Composable
fun <T> ScreenStateContent(
    state: ScreenState<T>,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    loading: @Composable () -> Unit,
    content: @Composable (T) -> Unit,
    empty: @Composable () -> Unit = {},
    fallbackActionLabel: String? = null,
    onFallbackAction: (() -> Unit)? = null,
) {
    when (state) {
        ScreenState.Loading -> loading()
        ScreenState.Empty -> empty()
        is ScreenState.Content -> content(state.data)
        is ScreenState.Error -> ScreenErrorContent(
            message = state.message?.asString(LocalContext.current)
                ?: androidx.compose.ui.res.stringResource(R.string.screen_load_error),
            onRetry = onRetry,
            fallbackActionLabel = fallbackActionLabel,
            onFallbackAction = onFallbackAction,
            modifier = modifier,
        )
    }
}

@Suppress("FunctionNaming")
@Composable
private fun ScreenErrorContent(
    message: String,
    onRetry: () -> Unit,
    fallbackActionLabel: String?,
    onFallbackAction: (() -> Unit)?,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        androidx.compose.material3.Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = message,
            modifier = Modifier.padding(top = 16.dp, bottom = 24.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(androidx.compose.ui.res.stringResource(R.string.retry))
        }
        if (fallbackActionLabel != null && onFallbackAction != null) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onFallbackAction,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(fallbackActionLabel)
            }
        }
    }
}
