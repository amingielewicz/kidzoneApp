package com.kidzone.utils

import com.google.firebase.firestore.FirebaseFirestoreException
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

private inline fun <reified T : Throwable> Throwable.findCause(): T? =
    generateSequence(this) { it.cause }.filterIsInstance<T>().firstOrNull()

private inline fun <reified T : Throwable> Throwable.hasCause(): Boolean =
    findCause<T>() != null
