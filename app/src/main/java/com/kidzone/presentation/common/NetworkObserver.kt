package com.kidzone.presentation.common

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

enum class NetworkStatus {
    AVAILABLE, UNAVAILABLE
}

fun observeNetworkStatus(context: Context): Flow<NetworkStatus> = callbackFlow {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun currentStatus(): NetworkStatus =
        if (connectivityManager.hasValidatedInternet()) {
            NetworkStatus.AVAILABLE
        } else {
            NetworkStatus.UNAVAILABLE
        }

    trySend(currentStatus())

    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            trySend(currentStatus())
        }

        override fun onLost(network: Network) {
            trySend(currentStatus())
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            trySend(currentStatus())
        }

        override fun onUnavailable() {
            trySend(currentStatus())
        }
    }

    val request = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()
    connectivityManager.registerNetworkCallback(request, callback)

    awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
}.distinctUntilChanged()

fun isNetworkAvailable(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return cm.hasValidatedInternet()
}

private fun ConnectivityManager.hasValidatedInternet(): Boolean {
    val capabilities = activeNetwork?.let(::getNetworkCapabilities)
    return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
