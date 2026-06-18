package com.kidzone.presentation.home

import com.kidzone.testutil.TestFixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HomeSectionsTest {

    @Test
    fun `recently added section keeps only places from last 14 days sorted by newest first`() {
        val dayMillis = 24L * 60L * 60L * 1000L
        val nowMillis = 1_800_000_000_000L

        val newestFarther = TestFixtures.place(
            id = "newest-farther",
            createdAtMillis = nowMillis - dayMillis
        )
        val olderCloser = TestFixtures.place(
            id = "older-closer",
            createdAtMillis = nowMillis - 2L * dayMillis
        )
        val outsideWindow = TestFixtures.place(
            id = "outside-window",
            createdAtMillis = nowMillis - 15L * dayMillis
        )
        val futurePlace = TestFixtures.place(
            id = "future-place",
            createdAtMillis = nowMillis + dayMillis
        )

        val sections = buildHomeSections(
            placesWithDistance = listOf(
                newestFarther to 8.0,
                olderCloser to 1.0,
                outsideWindow to 0.5,
                futurePlace to 0.2
            ),
            nowMillis = nowMillis
        )

        assertEquals(
            listOf("newest-farther", "older-closer"),
            sections.recentlyAddedPlaces.map { it.place.id }
        )
    }
}
