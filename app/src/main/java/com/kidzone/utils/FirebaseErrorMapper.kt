package com.kidzone.utils

import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.StorageException
import java.io.IOException

const val NETWORK_ERROR_MESSAGE =
    "Brak połączenia z internetem. Sprawdź sieć i spróbuj ponownie."

const val SERVER_TEMPORARY_ERROR_MESSAGE =
    "Nie udało się połączyć z serwerem. Spróbuj ponownie za chwilę."

const val FIRESTORE_PERMISSION_ERROR_MESSAGE =
    "Nie masz uprawnień do tych danych. Zaloguj się ponownie."

const val FIRESTORE_CONFIGURATION_ERROR_MESSAGE =
    "Dane wymagają aktualizacji konfiguracji. Spróbuj ponownie później."

const val FIRESTORE_QUOTA_ERROR_MESSAGE =
    "Usługa jest chwilowo przeciążona. Spróbuj ponownie za moment."

const val UPLOAD_ERROR_MESSAGE =
    "Nie udało się wgrać zdjęcia. Spróbuj ponownie."

const val STORAGE_PERMISSION_ERROR_MESSAGE =
    "Nie masz uprawnień do tego pliku. Zaloguj się ponownie."

const val STORAGE_QUOTA_ERROR_MESSAGE =
    "Nie udało się wgrać zdjęcia, bo usługa jest chwilowo przeciążona. Spróbuj ponownie później."

const val STORAGE_RETRY_LIMIT_ERROR_MESSAGE =
    "Nie udało się wgrać zdjęcia przez niestabilne połączenie. Spróbuj ponownie."

fun Throwable.toPlacesErrorMessage(fallback: String): String {
    val firestoreError = findCause<FirebaseFirestoreException>()
    if (firestoreError != null) {
        return firestoreError.toFirestoreMessage(fallback)
    }

    return if (hasCause<IOException>()) {
        NETWORK_ERROR_MESSAGE
    } else {
        fallback
    }
}

fun Throwable.toUploadErrorMessage(fallback: String = UPLOAD_ERROR_MESSAGE): String {
    val storageError = findCause<StorageException>()
    if (storageError != null) {
        return storageError.toStorageMessage(fallback)
    }

    return if (hasCause<IOException>()) {
        NETWORK_ERROR_MESSAGE
    } else {
        fallback
    }
}

private fun FirebaseFirestoreException.toFirestoreMessage(fallback: String): String =
    when (code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED,
        FirebaseFirestoreException.Code.UNAUTHENTICATED -> FIRESTORE_PERMISSION_ERROR_MESSAGE

        FirebaseFirestoreException.Code.UNAVAILABLE,
        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
        FirebaseFirestoreException.Code.ABORTED -> SERVER_TEMPORARY_ERROR_MESSAGE

        FirebaseFirestoreException.Code.FAILED_PRECONDITION -> FIRESTORE_CONFIGURATION_ERROR_MESSAGE
        FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED -> FIRESTORE_QUOTA_ERROR_MESSAGE
        else -> fallback
    }

private fun StorageException.toStorageMessage(fallback: String): String =
    when (errorCode) {
        StorageException.ERROR_NOT_AUTHENTICATED,
        StorageException.ERROR_NOT_AUTHORIZED -> STORAGE_PERMISSION_ERROR_MESSAGE

        StorageException.ERROR_QUOTA_EXCEEDED -> STORAGE_QUOTA_ERROR_MESSAGE
        StorageException.ERROR_RETRY_LIMIT_EXCEEDED -> STORAGE_RETRY_LIMIT_ERROR_MESSAGE
        else -> fallback
    }

private inline fun <reified T : Throwable> Throwable.findCause(): T? =
    generateSequence(this) { it.cause }.filterIsInstance<T>().firstOrNull()

private inline fun <reified T : Throwable> Throwable.hasCause(): Boolean =
    findCause<T>() != null
