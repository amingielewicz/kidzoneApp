package com.kidzone.logging

import android.util.Log
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import timber.log.Timber

class CrashlyticsTreeTest {

    @Test
    fun `ignores debug and info logs`() {
        val sink = RecordingCrashlyticsSink()
        val tree = CrashlyticsTree(sink)

        withPlantedTree(tree) {
            Timber.tag("Auth").d("debug message")
            Timber.tag("Auth").i("info message")
        }

        assertTrue(sink.messages.isEmpty())
    }

    @Test
    fun `logs warning breadcrumbs`() {
        val sink = RecordingCrashlyticsSink()
        val tree = CrashlyticsTree(sink)

        withPlantedTree(tree) {
            Timber.tag("Profile").w("save failed")
        }

        assertEquals(listOf("W/Profile: save failed"), sink.messages)
    }

    @Test
    fun `does not include raw throwable details in Crashlytics`() {
        val sink = RecordingCrashlyticsSink()
        val tree = CrashlyticsTree(sink)
        val secret = "adam@example.com"
        val error = IllegalStateException("request failed for $secret")

        withPlantedTree(tree) {
            Timber.tag("Sync").e(error, "sync failed")
        }

        assertEquals(listOf("E/Sync: sync failed"), sink.messages)
        assertFalse(sink.messages.single().contains(secret))
        assertFalse(sink.messages.single().contains(error.message.orEmpty()))
    }

    @Test
    fun `redacts email and long token values`() {
        val sink = RecordingCrashlyticsSink()
        val tree = CrashlyticsTree(sink)
        val token = "a".repeat(90)

        withPlantedTree(tree) {
            Timber.tag("Auth").e("user adam@example.com token=$token")
        }

        val message = sink.messages.single()
        assertTrue(message.contains("[redacted-email]"))
        assertTrue(message.contains("[redacted-token]"))
        assertFalse(message.contains("adam@example.com"))
        assertFalse(message.contains(token))
    }

    @Test
    fun `limits breadcrumb length`() {
        val sink = RecordingCrashlyticsSink()
        val tree = CrashlyticsTree(sink)

        withPlantedTree(tree) {
            Timber.tag("Long").e("x".repeat(1_000))
        }

        assertTrue(sink.messages.single().length <= 520)
    }

    private fun withPlantedTree(tree: Timber.Tree, block: () -> Unit) {
        Timber.plant(tree)
        try {
            block()
        } finally {
            Timber.uproot(tree)
        }
    }

    private class RecordingCrashlyticsSink : CrashlyticsSink {
        val messages = mutableListOf<String>()

        override fun log(message: String) {
            messages += message
        }
    }
}
