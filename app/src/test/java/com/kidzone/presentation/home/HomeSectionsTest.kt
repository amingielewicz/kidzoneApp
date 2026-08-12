package com.kidzone.presentation.home

import com.kidzone.data.remote.PerformanceConfig
import com.kidzone.testutil.TestFixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HomeSectionsTest {

    @Test
    fun `recently added section keeps only places from 5km radius sorted by newest first`() {
        val now = System.currentTimeMillis()
        val newestFar = TestFixtures.place(
            id = "newest-far",
            createdAtMillis = now
        )
        val olderNear = TestFixtures.place(
            id = "older-near",
            createdAtMillis = now - 1000L
        )
        val newestNear = TestFixtures.place(
            id = "newest-near",
            createdAtMillis = now - 500L
        )

        val sections = buildHomeSections(
            placesWithDistance = listOf(
                newestFar to 6.0,   // Outside 5km
                olderNear to 1.0,   // Inside 5km
                newestNear to 0.5   // Inside 5km
            )
        )

        assertEquals(
            listOf("newest-near", "older-near"),
            sections.recentlyAddedPlaces.map { it.place.id }
        )
    }

    @Test
    fun `sections use performance config limits and top radius`() {
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
            createdAtMillis = System.currentTimeMillis()
        )
        val lowerRatedNear = TestFixtures.place(
            id = "lower-rated-near",
            averageRating = 4.0,
            reviewsCount = 3,
            createdAtMillis = System.currentTimeMillis() - 2000L
        )
        val unratedNearest = TestFixtures.place(
            id = "unrated-nearest",
            averageRating = 0.0,
            reviewsCount = 0,
            createdAtMillis = System.currentTimeMillis() - 3000L
        )

        val sections = buildHomeSections(
            placesWithDistance = listOf(
                highRatedFar to 8.0,
                lowerRatedNear to 2.0,
                unratedNearest to 0.5
            ),
            performanceConfig = config
        )

        assertEquals(
            listOf("unrated-nearest", "lower-rated-near"),
            sections.nearbyPlaces.map { it.place.id }
        )
        assertEquals(listOf("lower-rated-near"), sections.topPlaces.map { it.place.id })
        // Recently added is limited to 1, and high-rated-far (newest) is outside 5km radius
        assertEquals(listOf("lower-rated-near"), sections.recentlyAddedPlaces.map { it.place.id })
    }
}
