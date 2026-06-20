package com.kidzone.presentation.map

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class MapAccessibilityLabelsTest {

    @Test
    fun `map controls expose stable accessibility labels`() {
        val labels = listOf(
            MapAccessibilityLabels.LOADING_PLACES,
            MapAccessibilityLabels.ZOOM_IN,
            MapAccessibilityLabels.ZOOM_OUT,
        )

        labels.forEach { label ->
            assertFalse(label.isBlank())
        }
        assertEquals(labels.size, labels.toSet().size)
    }
}
