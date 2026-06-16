package com.kidzone.utils

import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FirebaseErrorMapperTest {

    @Test
    fun `maps firestore permission error to auth message`() {
        val error = FirebaseFirestoreException(
            "Missing permission",
            FirebaseFirestoreException.Code.PERMISSION_DENIED
        )

        val message = error.toPlacesErrorMessage(FALLBACK)

        assertEquals(FIRESTORE_PERMISSION_ERROR_MESSAGE, message)
    }

    @Test
    fun `maps missing firestore index to configuration message`() {
        val error = FirebaseFirestoreException(
            "The query requires an index",
            FirebaseFirestoreException.Code.FAILED_PRECONDITION
        )

        val message = error.toPlacesErrorMessage(FALLBACK)

        assertEquals(FIRESTORE_CONFIGURATION_ERROR_MESSAGE, message)
    }

    @Test
    fun `maps io error to network message`() {
        val error = IOException("Unable to resolve host")

        val message = error.toPlacesErrorMessage(FALLBACK)

        assertEquals(NETWORK_ERROR_MESSAGE, message)
    }

    @Test
    fun `maps wrapped io error to network message`() {
        val error = IllegalStateException("Listener failed", IOException("Connection closed"))

        val message = error.toPlacesErrorMessage(FALLBACK)

        assertEquals(NETWORK_ERROR_MESSAGE, message)
    }

    @Test
    fun `keeps fallback for unknown errors`() {
        val error = IllegalArgumentException("Unexpected")

        val message = error.toPlacesErrorMessage(FALLBACK)

        assertEquals(FALLBACK, message)
    }

    private companion object {
        const val FALLBACK = "Nie udało się wczytać miejsc"
    }
}
