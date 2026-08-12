package com.kidzone.presentation.home

import com.kidzone.data.remote.PerformanceConfig
import com.kidzone.testutil.TestFixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HomeSectionsTest {

    @Test
    fun `recently added section keeps only places from last 30 days sorted by newest first`() {
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
            createdAtMillis = nowMillis - 31L * dayMillis
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
            listOf("future-place", "newest-farther", "older-closer"),
            sections.recentlyAddedPlaces.map { it.place.id }
        )
    }

    @Test
    fun `sections use performance config limits and top radius`() {
        val nowMillis = 1_800_000_000_000L
        val config = PerformanceConfig(
            homeNearbyLimit = 2,
            homeTopPlacesLimit = 1,
            homeRecentlyAddedLimit = 1,
            homeTopPlacesRadiusKm = 3.0
        )

        val highRatedFar = TestFixtures.place(
            id = "high-rated-far",
            averageRating = 5.0,
            reviewsCount = 10,
            createdAtMillis = nowMillis - 1_000L
        )
        val lowerRatedNear = TestFixtures.place(
            id = "lower-rated-near",
            averageRating = 4.0,
            reviewsCount = 3,
            createdAtMillis = nowMillis - 2_000L
        )
        val unratedNearest = TestFixtures.place(
            id = "unrated-nearest",
            averageRating = 0.0,
            reviewsCount = 0,
            createdAtMillis = nowMillis - 3_000L
        )

        val sections = buildHomeSections(
            placesWithDistance = listOf(
                highRatedFar to 8.0,
                lowerRatedNear to 2.0,
                unratedNearest to 0.5
            ),
            performanceConfig = config,
            nowMillis = nowMillis
        )

        assertEquals(
            listOf("unrated-nearest", "lower-rated-near"),
            sections.nearbyPlaces.map { it.place.id }
        )
        assertEquals(listOf("lower-rated-near"), sections.topPlaces.map { it.place.id })
        assertEquals(listOf("high-rated-far"), sections.recentlyAddedPlaces.map { it.place.id })
    }
}
