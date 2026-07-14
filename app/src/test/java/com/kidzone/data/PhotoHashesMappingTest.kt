package com.kidzone.data

import com.kidzone.data.local.PlaceEntity
import com.kidzone.data.remote.dto.PlaceDto
import com.kidzone.testutil.TestFixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PhotoHashesMappingTest {

    @Test
    fun `PlaceDto maps URL hashes to domain`() {
        val hashes = mapOf("https://example.com/a.webp" to "abc123")

        val place = PlaceDto(photoHashes = hashes).toDomain()

        assertEquals(hashes, place.photoHashes)
    }

    @Test
    fun `PlaceDto ignores legacy hash list`() {
        val place = PlaceDto(photoHashes = listOf("legacy-hash")).toDomain()

        assertEquals(emptyMap<String, String>(), place.photoHashes)
    }

    @Test
    fun `PlaceEntity round trips URL hash map as JSON`() {
        val hashes = mapOf(
            "https://example.com/a.webp" to "abc123",
            "https://example.com/b.webp" to "def456"
        )
        val original = TestFixtures.place(photoUrls = hashes.keys.toList()).copy(photoHashes = hashes)

        val restored = PlaceEntity.fromDomain(original).toDomain()

        assertEquals(hashes, restored.photoHashes)
    }

    @Test
    fun `PlaceEntity treats legacy pipe separated hashes as empty map`() {
        val entity = PlaceEntity.fromDomain(TestFixtures.place()).copy(photoHashes = "abc|def")

        assertEquals(emptyMap<String, String>(), entity.toDomain().photoHashes)
    }
}
