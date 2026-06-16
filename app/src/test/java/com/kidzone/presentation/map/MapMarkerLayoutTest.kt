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
    fun `country zoom clusters Lodz area places into one marker`() {
        val places = listOf(
            TestFixtures.place(id = "p1", latitude = 51.722, longitude = 19.356),
            TestFixtures.place(id = "p2", latitude = 51.735, longitude = 19.428),
            TestFixtures.place(id = "p3", latitude = 51.744, longitude = 19.456),
            TestFixtures.place(id = "p4", latitude = 51.756, longitude = 19.469),
            TestFixtures.place(id = "p5", latitude = 51.762, longitude = 19.503),
            TestFixtures.place(id = "p6", latitude = 51.775, longitude = 19.401),
            TestFixtures.place(id = "p7", latitude = 51.781, longitude = 19.456),
            TestFixtures.place(id = "p8", latitude = 51.794, longitude = 19.512),
            TestFixtures.place(id = "p9", latitude = 51.806, longitude = 19.381),
            TestFixtures.place(id = "p10", latitude = 51.821, longitude = 19.474)
        )

        val markers = buildMapMarkerItems(
            places = places,
            expandedClusterKey = null,
            zoom = 6f
        )

        assertEquals(1, markers.size)
        assertEquals(10, markers.single().cluster!!.places.size)
    }

    @Test
    fun `city zoom shows Lodz area places as individual markers`() {
        val places = listOf(
            TestFixtures.place(id = "p1", latitude = 51.722, longitude = 19.356),
            TestFixtures.place(id = "p2", latitude = 51.735, longitude = 19.428),
            TestFixtures.place(id = "p3", latitude = 51.744, longitude = 19.456),
            TestFixtures.place(id = "p4", latitude = 51.756, longitude = 19.469)
        )

        val markers = buildMapMarkerItems(
            places = places,
            expandedClusterKey = null,
            zoom = 13f
        )

        assertEquals(4, markers.size)
        assertTrue(markers.all { it.place != null })
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

    @Test
    fun `expanded place ids render real markers even at country zoom`() {
        val places = listOf(
            TestFixtures.place(id = "p1", latitude = 52.10, longitude = 21.10),
            TestFixtures.place(id = "p2", latitude = 52.12, longitude = 21.12)
        )

        val expanded = buildMapMarkerItems(
            places = places,
            expandedClusterKey = null,
            expandedPlaceIds = places.map { it.id }.toSet(),
            zoom = 6f
        )

        assertEquals(2, expanded.size)
        assertTrue(expanded.all { it.place != null })
        assertTrue(expanded.none { it.cluster != null })
        assertEquals(places.map { it.latitude to it.longitude }, expanded.map {
            it.position.latitude to it.position.longitude
        })
    }

    @Test
    fun `expanded place ids spiderfy markers with same coordinates`() {
        val places = (1..10).map { index ->
            TestFixtures.place(
                id = "p$index",
                latitude = 51.760,
                longitude = 19.458
            )
        }

        val expanded = buildMapMarkerItems(
            places = places,
            expandedClusterKey = null,
            expandedPlaceIds = places.map { it.id }.toSet(),
            zoom = 16f
        )

        assertEquals(10, expanded.size)
        assertTrue(expanded.all { it.place != null })
        assertTrue(expanded.all { it.isSpiderfied })
        assertTrue(expanded.map { it.position }.toSet().size > 1)
    }

    private fun overlappingPlaces() = listOf(
        TestFixtures.place(id = "p1", latitude = 52.123456, longitude = 21.123456),
        TestFixtures.place(id = "p2", latitude = 52.123456, longitude = 21.123456),
        TestFixtures.place(id = "p3", latitude = 52.123456, longitude = 21.123456)
    )
}
