package com.kidzone.presentation.common

import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun CoroutineScope.runOnlineOrShowOffline(
    networkStatus: NetworkStatus,
    snackbarHostState: SnackbarHostState,
    offlineMessage: String,
    action: () -> Unit
) {
    if (networkStatus == NetworkStatus.UNAVAILABLE) {
        launch { snackbarHostState.showSnackbar(offlineMessage) }
    } else {
        action()
    }
}
