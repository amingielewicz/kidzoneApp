@file:Suppress("CyclomaticComplexMethod", "FunctionNaming", "LongMethod")

package com.kidzone.presentation.place.details

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kidzone.R
import com.kidzone.presentation.common.KidZoneActionDialog
import com.kidzone.presentation.common.LocationActionIcon
import com.kidzone.presentation.common.OfflineAwareSubmitButton
import com.kidzone.presentation.place.add.LOCATION_SERVICE_DISABLED_MESSAGE
import com.kidzone.presentation.place.add.LOCATION_TIMEOUT_USER_MESSAGE
import com.kidzone.presentation.place.add.fetchCurrentLocation
import com.kidzone.presentation.place.add.hasLocationPermission
import com.kidzone.presentation.place.add.isLocationServiceEnabled
import com.kidzone.presentation.place.add.reverseGeocode
import kotlinx.coroutines.launch

@Composable
fun LocationCorrectionDialog(
    onSubmit: (latitude: Double, longitude: Double, address: String?) -> Unit,
    onDismiss: () -> Unit,
    isOffline: Boolean = false
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var address by remember { mutableStateOf<String?>(null) }
    var isFetching by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var locationServiceEnabled by remember { mutableStateOf(isLocationServiceEnabled(context)) }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                locationServiceEnabled = isLocationServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val locationPermissionDenied = stringResource(R.string.location_permission_denied)
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            locationServiceEnabled = isLocationServiceEnabled(context)
            if (locationServiceEnabled) {
                isFetching = true
                scope.launch {
                    fetchLocationInternal(context) { lat, lng, addr, err ->
                        latitude = lat; longitude = lng; address = addr; errorMessage = err; isFetching = false
                    }
                }
            } else {
                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
        } else {
            errorMessage = locationPermissionDenied
        }
    }

    KidZoneActionDialog(
        title = stringResource(R.string.correct_location),
        icon = Icons.Filled.MyLocation,
        onDismiss = onDismiss,
        confirmButton = {
            OfflineAwareSubmitButton(
                label = stringResource(R.string.submit_correction),
                onClick = { if (latitude != null && longitude != null) onSubmit(latitude!!, longitude!!, address) },
                isOffline = isOffline,
                enabled = latitude != null && longitude != null
            )
        }
    ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.location_correction_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(
                    onClick = {
                        errorMessage = null
                        locationServiceEnabled = isLocationServiceEnabled(context)
                        if (!locationServiceEnabled) {
                            isFetching = false
                            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        } else if (hasLocationPermission(context)) {
                            isFetching = true
                            scope.launch {
                                fetchLocationInternal(context) { lat, lng, addr, err ->
                                    latitude = lat; longitude = lng; address = addr; errorMessage = err; isFetching = false
                                }
                            }
                        } else {
                            isFetching = false
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    },
                    enabled = !isFetching,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val isReady = hasLocationPermission(context) && locationServiceEnabled
                    if (isFetching) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.size(8.dp))
                        Text("Pobieranie lokalizacji...")
                    } else {
                        LocationActionIcon(isReady = isReady)
                        Spacer(Modifier.size(8.dp))
                        Text(
                            when {
                                !hasLocationPermission(context) -> stringResource(R.string.location_permission_action)
                                !locationServiceEnabled -> stringResource(R.string.gps_disabled_title)
                                latitude != null -> stringResource(R.string.update_location_action)
                                else -> stringResource(R.string.fetch_location)
                            }
                        )
                    }
                }

                if (latitude != null && longitude != null) {
                    Text(
                        text = "GPS: %.5f, %.5f".format(latitude, longitude),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!address.isNullOrBlank()) {
                        Text(text = address!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                errorMessage?.let { msg ->
                    Text(text = msg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
    }
}

private suspend fun fetchLocationInternal(
    context: android.content.Context,
    onResult: (lat: Double?, lng: Double?, address: String?, error: String?) -> Unit
) {
    if (!isLocationServiceEnabled(context)) {
        onResult(null, null, null, LOCATION_SERVICE_DISABLED_MESSAGE)
        return
    }
    try {
        val coords = fetchCurrentLocation(context)
        if (coords == null) { onResult(null, null, null, LOCATION_TIMEOUT_USER_MESSAGE); return }
        val addr = runCatching { reverseGeocode(context, coords.first, coords.second) }.getOrNull()
        onResult(coords.first, coords.second, addr, null)
    } catch (e: Exception) {
        onResult(null, null, null, LOCATION_TIMEOUT_USER_MESSAGE)
    }
}
