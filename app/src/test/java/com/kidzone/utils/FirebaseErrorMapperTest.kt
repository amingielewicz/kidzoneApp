package com.kidzone.utils

import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.StorageException
import com.kidzone.R
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FirebaseErrorMapperTest {

    @Test
    fun `maps firestore permission error to auth message`() {
        val error = FirebaseFirestoreException(
            "Missing permission",
            FirebaseFirestoreException.Code.PERMISSION_DENIED
        )

        val message = error.toPlacesErrorMessage(FALLBACK)

        assertTrue(message is UiText.StringResource)
        assertEquals(R.string.error_permission_denied, (message as UiText.StringResource).resId)
    }

    @Test
    fun `maps missing firestore index to configuration message`() {
        val error = FirebaseFirestoreException(
            "The query requires an index",
            FirebaseFirestoreException.Code.FAILED_PRECONDITION
        )

        val message = error.toPlacesErrorMessage(FALLBACK)

        assertTrue(message is UiText.StringResource)
        assertEquals(R.string.error_config_update, (message as UiText.StringResource).resId)
    }

    @Test
    fun `maps io error to network message`() {
        val error = IOException("Unable to resolve host")

        val message = error.toPlacesErrorMessage(FALLBACK)

        assertTrue(message is UiText.StringResource)
        assertEquals(R.string.error_no_internet, (message as UiText.StringResource).resId)
    }

    @Test
    fun `maps wrapped io error to network message`() {
        val error = IllegalStateException("Listener failed", IOException("Connection closed"))

        val message = error.toPlacesErrorMessage(FALLBACK)

        assertTrue(message is UiText.StringResource)
        assertEquals(R.string.error_no_internet, (message as UiText.StringResource).resId)
    }

    @Test
    fun `keeps fallback for unknown errors`() {
        val error = IllegalArgumentException("Unexpected")

        val message = error.toPlacesErrorMessage(FALLBACK)

        assertEquals(FALLBACK, message)
    }

    @Test
    fun `maps storage permission error to safe upload message`() {
        val error = storageException(StorageException.ERROR_NOT_AUTHORIZED)

        val message = error.toUploadErrorMessage()

        assertTrue(message is UiText.StringResource)
        assertEquals(R.string.error_storage_permission, (message as UiText.StringResource).resId)
    }

    @Test
    fun `maps storage retry limit to safe upload message`() {
        val error = storageException(StorageException.ERROR_RETRY_LIMIT_EXCEEDED)

        val message = error.toUploadErrorMessage()

        assertTrue(message is UiText.StringResource)
        assertEquals(R.string.error_storage_retry_limit, (message as UiText.StringResource).resId)
    }

    @Test
    fun `maps wrapped upload io error to network message`() {
        val error = IllegalStateException("Upload failed", IOException("socket closed"))

        val message = error.toUploadErrorMessage()

        assertTrue(message is UiText.StringResource)
        assertEquals(R.string.error_no_internet, (message as UiText.StringResource).resId)
    }

    @Test
    fun `keeps upload fallback for unknown errors`() {
        val error = IllegalArgumentException("raw firebase message")

        val message = error.toUploadErrorMessage()

        assertTrue(message is UiText.StringResource)
        assertEquals(R.string.error_upload_failed, (message as UiText.StringResource).resId)
    }

    private fun storageException(errorCode: Int): StorageException {
        val error = mockk<StorageException>()
        every { error.errorCode } returns errorCode
        return error
    }

    private companion object {
        val FALLBACK = UiText.DynamicString("fallback")
    }
}
