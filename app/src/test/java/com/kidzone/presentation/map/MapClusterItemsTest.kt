package com.kidzone.presentation.map

import com.kidzone.testutil.TestFixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MapClusterItemsTest {

    @Test
    fun `single places keep their real coordinates`() {
        val places = listOf(
            TestFixtures.place(id = "p1", latitude = 52.10, longitude = 21.10),
            TestFixtures.place(id = "p2", latitude = 52.20, longitude = 21.20)
        )

        val items = buildPlaceClusterItems(places)

        assertEquals(2, items.size)
        assertEquals(
            places.map { it.latitude to it.longitude },
            items.map { it.position.latitude to it.position.longitude }
        )
    }

    @Test
    fun `places with identical coordinates are spread for high zoom clicks`() {
        val places = listOf(
            TestFixtures.place(id = "p1", latitude = 52.123456, longitude = 21.123456),
            TestFixtures.place(id = "p2", latitude = 52.123456, longitude = 21.123456),
            TestFixtures.place(id = "p3", latitude = 52.123456, longitude = 21.123456)
        )

        val items = buildPlaceClusterItems(places)

        assertEquals(3, items.size)
        assertEquals(places.map { it.id }.toSet(), items.map { it.place.id }.toSet())
        assertTrue(items.map { it.position }.toSet().size > 1)
        items.forEach { item ->
            assertNotEquals(
                places.first().latitude to places.first().longitude,
                item.position.latitude to item.position.longitude
            )
        }
    }

    @Test
    fun `cluster labels are bucketed by tens and capped at ninety plus`() {
        assertEquals("2", clusterCountLabel(2))
        assertEquals("9", clusterCountLabel(9))
        assertEquals("10+", clusterCountLabel(10))
        assertEquals("20+", clusterCountLabel(29))
        assertEquals("90+", clusterCountLabel(90))
        assertEquals("90+", clusterCountLabel(140))
    }
}
