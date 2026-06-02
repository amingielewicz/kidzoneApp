package com.kidzone.presentation.place.details

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var address by remember { mutableStateOf<String?>(null) }
    var isFetching by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            isFetching = true
            scope.launch {
                fetchLocationInternal(context) { lat, lng, addr, err ->
                    latitude = lat; longitude = lng; address = addr; errorMessage = err; isFetching = false
                }
            }
        } else {
            errorMessage = "Brak uprawnienia do lokalizacji"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.MyLocation,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text("Koryguj lokalizacj\u0119") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Pobierz aktualn\u0105 lokalizacj\u0119 GPS, aby zaproponowa\u0107 " +
                        "poprawn\u0105 pozycj\u0119 tego miejsca na mapie.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        isFetching = true
                        errorMessage = null
                        if (hasLocationPermission(context)) {
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
                    if (isFetching) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(if (latitude != null) "Aktualizuj lokalizacj\u0119" else "Pobierz lokalizacj\u0119")
                    }
                }

                if (latitude != null && longitude != null) {
                    Spacer(Modifier.height(8.dp))
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
                    Spacer(Modifier.height(8.dp))
                    Text(text = msg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (latitude != null && longitude != null) onSubmit(latitude!!, longitude!!, address) },
                enabled = latitude != null && longitude != null
            ) { Text("Wy\u015Blij korekt\u0119") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Anuluj") }
        }
    )
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
