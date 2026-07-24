package com.kidzone.utils

import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.StorageException
import com.kidzone.R
import java.io.IOException

/**
 * 🎯 Odpowiedzialności:
 * - Mapowanie wyjątków SDK Firebase (Auth, Firestore, Storage) na zlokalizowane komunikaty UI ([UiText]).
 * - Wykrywanie przyczyn źródłowych błędów w łańcuchu wyjątków (Exception chain).
 *
 * ✅ Gwarancje:
 * - Zawsze zwraca czytelny komunikat, korzystając z domyślnego fallbacku w razie nieznanego błędu.
 * - Prawidłowo identyfikuje błędy braku sieci ([IOException]).
 */
@Suppress("SpreadOperator")
fun Throwable.toAuthErrorMessage(fallbackRes: Int = R.string.error_unknown): UiText =
    when (this) {
        is AuthException -> UiText.StringResource(
            messageRes.takeIf { it != 0 } ?: fallbackRes,
            *args
        )
        else -> if (hasCause<IOException>()) {
            UiText.StringResource(R.string.error_no_internet)
        } else {
            UiText.StringResource(fallbackRes)
        }
    }
fun Throwable.toPlacesErrorMessage(fallback: UiText): UiText {
    val firestoreError = findCause<FirebaseFirestoreException>()

    return when {
        this is RepositoryException -> UiText.StringResource(this.messageRes)
        this is AuthException -> UiText.StringResource(this.messageRes, *this.args)
        firestoreError != null -> firestoreError.toFirestoreMessage(fallback)
        hasCause<IOException>() -> UiText.StringResource(R.string.error_no_internet)
        else -> fallback
    }
}

fun Throwable.toUploadErrorMessage(fallback: UiText = UiText.StringResource(R.string.error_upload_failed)): UiText {
    val storageError = findCause<StorageException>()
    if (storageError != null) {
        return storageError.toStorageMessage(fallback)
    }

    return if (hasCause<IOException>()) {
        UiText.StringResource(R.string.error_no_internet)
    } else {
        fallback
    }
}

private fun FirebaseFirestoreException.toFirestoreMessage(fallback: UiText): UiText =
    when (code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED,
        FirebaseFirestoreException.Code.UNAUTHENTICATED -> UiText.StringResource(R.string.error_permission_denied)

        FirebaseFirestoreException.Code.UNAVAILABLE,
        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
        FirebaseFirestoreException.Code.ABORTED -> UiText.StringResource(R.string.error_server_temporary)

        FirebaseFirestoreException.Code.FAILED_PRECONDITION -> UiText.StringResource(R.string.error_config_update)
        FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED -> UiText.StringResource(R.string.error_quota_exceeded)
        else -> fallback
    }

private fun StorageException.toStorageMessage(fallback: UiText): UiText =
    when (errorCode) {
        StorageException.ERROR_NOT_AUTHENTICATED,
        StorageException.ERROR_NOT_AUTHORIZED -> UiText.StringResource(R.string.error_storage_permission)

        StorageException.ERROR_QUOTA_EXCEEDED -> UiText.StringResource(R.string.error_storage_quota)
        StorageException.ERROR_RETRY_LIMIT_EXCEEDED -> UiText.StringResource(R.string.error_storage_retry_limit)
        else -> fallback
    }

private inline fun <reified T : Throwable> Throwable.findCause(): T? =
    generateSequence(this) { it.cause }.filterIsInstance<T>().firstOrNull()

private inline fun <reified T : Throwable> Throwable.hasCause(): Boolean =
    findCause<T>() != null
