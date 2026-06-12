package com.kidzone.sync

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException

/**
 * Utilities for detecting network-related errors.
 *
 * Used by repositories to decide whether to queue an operation offline
 * vs. propagate the error to the UI.
 */
object NetworkUtils {

    /**
     * Returns true if the exception indicates a network connectivity issue
     * (as opposed to a server-side error or validation failure).
     *
     * Network errors are candidates for offline queueing because they will
     * likely succeed when connectivity is restored.
     */
    fun isNetworkError(e: Exception): Boolean {
        return when (e) {
            is UnknownHostException -> true    // DNS resolution failed (no internet)
            is SocketTimeoutException -> true  // Connection timed out
            is IOException -> {
                // Firebase Firestore wraps network issues in IOException
                val message = e.message?.lowercase().orEmpty()
                message.contains("network") ||
                    message.contains("unavailable") ||
                    message.contains("connection") ||
                    message.contains("timeout") ||
                    message.contains("failed to connect")
            }
            is TimeoutException -> true        // Our AppConfig.WRITE_TIMEOUT_MS exceeded
            else -> {
                // Check for Firebase-specific offline indicators
                val cause = e.cause
                cause is IOException || cause is UnknownHostException
            }
        }
    }
}
