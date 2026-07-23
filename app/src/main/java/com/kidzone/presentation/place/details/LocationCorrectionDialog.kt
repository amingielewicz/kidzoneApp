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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kidzone.R
import com.kidzone.presentation.common.KidZoneActionDialog
import com.kidzone.presentation.common.LocationActionIcon
import com.kidzone.presentation.common.OfflineAwareSubmitButton
import com.kidzone.presentation.common.rememberLocationServiceEnabled
import com.kidzone.presentation.common.requestLocationPermissionOrOpenSettings
import com.kidzone.utils.GeoUtils
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
    val scope = rememberCoroutineScope()
    var locationRefreshSignal by remember { mutableIntStateOf(0) }
    val locationServiceEnabled = rememberLocationServiceEnabled(locationRefreshSignal)

    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var address by remember { mutableStateOf<String?>(null) }
    var isFetching by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val locationPermissionDenied = stringResource(R.string.location_permission_denied)
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            if (isLocationServiceEnabled(context)) {
                locationRefreshSignal++
                isFetching = true
                scope.launch {
                    fetchLocationInternal(context) { lat, lng, addr, err ->
                        latitude = lat; longitude = lng; address = addr; errorMessage =
                        err; isFetching = false
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
                onClick = {
                    if (latitude != null && longitude != null) onSubmit(
                        latitude!!,
                        longitude!!,
                        address
                    )
                },
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
                    locationRefreshSignal++

                    when {
                        !hasLocationPermission(context) -> {
                            isFetching = false

                            requestLocationPermissionOrOpenSettings(
                                context = context,
                                requestPermission = {
                                    locationPermissionLauncher.launch(
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    )
                                }
                            )
                        }

                        !isLocationServiceEnabled(context) -> {
                            isFetching = false

                            context.startActivity(
                                Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                            )
                        }

                        else -> {
                            isFetching = true

                            scope.launch {
                                fetchLocationInternal(context) { lat, lng, addr, err ->
                                    latitude = lat
                                    longitude = lng
                                    address = addr
                                    errorMessage = err
                                    isFetching = false
                                }
                            }
                        }
                    }
                },
                enabled = !isFetching,
                modifier = Modifier.fillMaxWidth()
            ) {
                val isReady = hasLocationPermission(context) && locationServiceEnabled
                if (isFetching) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.add_place_fetching_location))
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

            val currentLat = latitude
            val currentLng = longitude
            if (currentLat != null && currentLng != null) {
                Text(
                    text = "GPS: ${GeoUtils.formatCoordinates(currentLat, currentLng)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!address.isNullOrBlank()) {
                    Text(
                        text = address!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            errorMessage?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private suspend fun fetchLocationInternal(
    context: android.content.Context,
    onResult: (lat: Double?, lng: Double?, address: String?, error: String?) -> Unit
) {
    if (!isLocationServiceEnabled(context)) {
        onResult(null, null, null, context.getString(R.string.error_location_service_disabled))
        return
    }
    try {
        val coords = fetchCurrentLocation(context)
        if (coords == null) {
            onResult(null, null, null, context.getString(R.string.error_location_timeout))
            return
        }
        val addr = runCatching { reverseGeocode(context, coords.first, coords.second) }.getOrNull()
        onResult(coords.first, coords.second, addr, null)
    } catch (e: Exception) {
        onResult(null, null, null, context.getString(R.string.error_location_timeout))
    }
}
