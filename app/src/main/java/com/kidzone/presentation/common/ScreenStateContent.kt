package com.kidzone.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        androidx.compose.material3.Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
        )
        Text(
            text = message,
            modifier = Modifier.padding(vertical = 16.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(onClick = onRetry) { Text(androidx.compose.ui.res.stringResource(R.string.retry)) }
        if (fallbackActionLabel != null && onFallbackAction != null) {
            OutlinedButton(onClick = onFallbackAction) { Text(fallbackActionLabel) }
        }
    }
}
