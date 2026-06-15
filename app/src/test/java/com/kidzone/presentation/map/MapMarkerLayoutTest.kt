package com.kidzone.presentation.map

import com.kidzone.testutil.TestFixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MapMarkerLayoutTest {

    @Test
    fun `overlapping places collapse into one cluster marker`() {
        val places = overlappingPlaces()

        val markers = buildMapMarkerItems(places, expandedClusterKey = null)

        assertEquals(1, markers.size)
        assertNull(markers.single().place)
        assertNotNull(markers.single().cluster)
        assertEquals(3, markers.single().cluster!!.places.size)
    }

    @Test
    fun `expanded cluster spiderfies every place around center`() {
        val places = overlappingPlaces()
        val clusterKey = clusterKeyFor(places.first())

        val markers = buildMapMarkerItems(places, expandedClusterKey = clusterKey)

        assertEquals(3, markers.size)
        assertTrue(markers.all { it.place != null })
        assertTrue(markers.all { it.isSpiderfied })
        assertEquals(places.map { it.id }.toSet(), markers.map { it.place!!.id }.toSet())
        assertTrue(markers.map { it.position }.toSet().size > 1)
        markers.forEach { marker ->
            assertNotEquals(
                places.first().latitude to places.first().longitude,
                marker.position.latitude to marker.position.longitude
            )
        }
    }

    @Test
    fun `non overlapping places stay as individual markers`() {
        val places = listOf(
            TestFixtures.place(id = "p1", latitude = 52.10, longitude = 21.10),
            TestFixtures.place(id = "p2", latitude = 52.20, longitude = 21.20)
        )

        val markers = buildMapMarkerItems(places, expandedClusterKey = null)

        assertEquals(2, markers.size)
        assertTrue(markers.all { it.place != null })
        assertTrue(markers.all { it.cluster == null })
    }

    @Test
    fun `country zoom clusters nearby places into one count marker`() {
        val places = listOf(
            TestFixtures.place(id = "p1", latitude = 52.10, longitude = 21.10),
            TestFixtures.place(id = "p2", latitude = 52.12, longitude = 21.12),
            TestFixtures.place(id = "p3", latitude = 53.20, longitude = 22.20)
        )

        val markers = buildMapMarkerItems(
            places = places,
            expandedClusterKey = null,
            zoom = 6f
        )

        assertEquals(2, markers.size)
        assertEquals(2, markers.single { it.cluster != null }.cluster!!.places.size)
    }

    @Test
    fun `expanded country zoom cluster spiderfies its grouped places`() {
        val places = listOf(
            TestFixtures.place(id = "p1", latitude = 52.10, longitude = 21.10),
            TestFixtures.place(id = "p2", latitude = 52.12, longitude = 21.12)
        )
        val collapsed = buildMapMarkerItems(
            places = places,
            expandedClusterKey = null,
            zoom = 6f
        )
        val clusterKey = collapsed.single().cluster!!.key

        val expanded = buildMapMarkerItems(
            places = places,
            expandedClusterKey = clusterKey,
            zoom = 6f
        )

        assertEquals(2, expanded.size)
        assertTrue(expanded.all { it.place != null })
        assertTrue(expanded.all { it.isSpiderfied })
    }

    private fun overlappingPlaces() = listOf(
        TestFixtures.place(id = "p1", latitude = 52.123456, longitude = 21.123456),
        TestFixtures.place(id = "p2", latitude = 52.123456, longitude = 21.123456),
        TestFixtures.place(id = "p3", latitude = 52.123456, longitude = 21.123456)
    )
}
