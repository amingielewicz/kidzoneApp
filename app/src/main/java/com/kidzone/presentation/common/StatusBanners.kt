@file:Suppress("MagicNumber")

package com.kidzone.presentation.common

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kidzone.R
import com.kidzone.presentation.place.add.isLocationServiceEnabled

private val GpsDisabledContainer = Color(0xFF5E35B1)
private val GpsDisabledContent = Color.White
private val GpsDisabledButtonContainer = Color.White
private val GpsDisabledButtonContent = Color(0xFF4527A0)

private val GpsAcquiringContainer = Color(0xFF00796B)
private val GpsAcquiringContent = Color.White
private val SystemStatusIconColor = Color(0xFF9E9E9E)

@Composable
fun rememberNetworkStatus(): State<NetworkStatus> {
    val context = LocalContext.current
    return remember(context) { observeNetworkStatus(context) }
        .collectAsState(
            initial = if (isNetworkAvailable(context)) NetworkStatus.AVAILABLE
            else NetworkStatus.UNAVAILABLE
        )
}

@Composable
fun rememberLocationServiceEnabled(refreshSignal: Int = 0): Boolean {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val enabled = remember { mutableStateOf(isLocationServiceEnabled(context)) }

    LaunchedEffect(refreshSignal) {
        enabled.value = isLocationServiceEnabled(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                enabled.value = isLocationServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return enabled.value
}

@Composable
fun SystemStatusIcons(
    isNetworkAvailable: Boolean,
    isLocationAvailable: Boolean,
    onNetworkClick: () -> Unit,
    onLocationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isNetworkAvailable && isLocationAvailable) return

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isNetworkAvailable) {
            IconButton(
                onClick = onNetworkClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SignalWifiOff,
                    contentDescription = "Brak internetu",
                    tint = SystemStatusIconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        if (!isLocationAvailable) {
            IconButton(
                onClick = onLocationClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.GpsOff,
                    contentDescription = "Brak lokalizacji GPS",
                    tint = SystemStatusIconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun NoInternetBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                liveRegion = LiveRegionMode.Assertive
            },
        color = MaterialTheme.colorScheme.errorContainer,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.SignalWifiOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.no_internet_banner),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun GpsDisabledBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                liveRegion = LiveRegionMode.Polite
            },
        color = GpsDisabledContainer,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.GpsOff,
                contentDescription = null,
                tint = GpsDisabledContent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.gps_disabled_banner),
                style = MaterialTheme.typography.bodySmall,
                color = GpsDisabledContent,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = GpsDisabledButtonContainer,
                    contentColor = GpsDisabledButtonContent
                )
            ) {
                Text(stringResource(R.string.enable), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/**
 * Banner "Ustalanie lokalizacji…"
 */
@Composable
fun GpsAcquiringBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                liveRegion = LiveRegionMode.Polite
            },
        color = GpsAcquiringContainer,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = GpsAcquiringContent
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.gps_acquiring_banner),
                style = MaterialTheme.typography.bodySmall,
                color = GpsAcquiringContent,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
