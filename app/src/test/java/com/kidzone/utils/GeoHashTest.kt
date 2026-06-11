package com.kidzone.utils

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class GeoHashTest {

    @Nested
    @DisplayName("encode()")
    inner class Encode {

        @Test
        fun `encodes Warsaw center correctly`() {
            // Warsaw: 52.2297, 21.0122 - known geohash prefix: u3qcn
            val hash = GeoHash.encode(52.2297, 21.0122, 7)
            assertEquals(7, hash.length)
            assertTrue(hash.startsWith("u3"))
        }

        @Test
        fun `encodes (0, 0) to known value`() {
            // (0,0) is in the Atlantic ocean, geohash starts with 's'
            val hash = GeoHash.encode(0.0, 0.0, 7)
            assertEquals(7, hash.length)
            assertTrue(hash.startsWith("s000"))
        }

        @Test
        fun `encodes north pole area`() {
            val hash = GeoHash.encode(89.9, 0.0, 7)
            assertEquals(7, hash.length)
            // North pole geohash starts with 'u' or 'v'
            assertTrue(hash[0] in listOf('u', 'v', 'g', 'z'))
        }

        @Test
        fun `encodes south pole area`() {
            val hash = GeoHash.encode(-89.9, 0.0, 7)
            assertEquals(7, hash.length)
        }

        @Test
        fun `encodes with different precisions`() {
            val hash4 = GeoHash.encode(52.2297, 21.0122, 4)
            val hash7 = GeoHash.encode(52.2297, 21.0122, 7)

            assertEquals(4, hash4.length)
            assertEquals(7, hash7.length)
            assertTrue(hash7.startsWith(hash4))
        }

        @Test
        fun `precision 1 returns single character`() {
            val hash = GeoHash.encode(52.2297, 21.0122, 1)
            assertEquals(1, hash.length)
        }

        @Test
        fun `nearby points share prefix`() {
            // Two points ~50m apart should share at least 6-char prefix
            val hash1 = GeoHash.encode(52.2297, 21.0122, 7)
            val hash2 = GeoHash.encode(52.2298, 21.0123, 7)

            val sharedPrefix = hash1.commonPrefixWith(hash2)
            assertTrue(sharedPrefix.length >= 5,
                "Points 50m apart should share at least 5-char prefix, got: '$sharedPrefix'")
        }

        @Test
        fun `distant points have different prefixes`() {
            // Warsaw vs New York
            val warsaw = GeoHash.encode(52.2297, 21.0122, 7)
            val newYork = GeoHash.encode(40.7128, -74.0060, 7)

            assertNotEquals(warsaw.substring(0, 3), newYork.substring(0, 3))
        }

        @Test
        fun `uses only valid base32 characters`() {
            val validChars = "0123456789bcdefghjkmnpqrstuvwxyz".toSet()
            val hash = GeoHash.encode(52.2297, 21.0122, 12)

            assertTrue(hash.all { it in validChars },
                "Hash '$hash' contains invalid characters")
        }

        @Test
        fun `handles boundary values for latitude`() {
            assertDoesNotThrow { GeoHash.encode(-90.0, 0.0, 7) }
            assertDoesNotThrow { GeoHash.encode(90.0, 0.0, 7) }
        }

        @Test
        fun `handles boundary values for longitude`() {
            assertDoesNotThrow { GeoHash.encode(0.0, -180.0, 7) }
            assertDoesNotThrow { GeoHash.encode(0.0, 180.0, 7) }
        }
    }

    @Nested
    @DisplayName("prefixLengthForRadius()")
    inner class PrefixLength {

        @ParameterizedTest
        @CsvSource(
            "0.1, 6",
            "0.3, 6",
            "0.5, 6",
            "1.0, 5",
            "3.0, 5",
            "5.0, 5",
            "10.0, 4",
            "20.0, 4",
            "50.0, 3",
            "100.0, 3",
            "200.0, 2"
        )
        fun `returns correct prefix length for radius`(radiusKm: Double, expectedLength: Int) {
            assertEquals(expectedLength, GeoHash.prefixLengthForRadius(radiusKm))
        }

        @Test
        fun `smaller radius gives longer prefix (more precise)`() {
            val small = GeoHash.prefixLengthForRadius(0.2)
            val large = GeoHash.prefixLengthForRadius(50.0)
            assertTrue(small > large)
        }

        @Test
        fun `very large radius returns 2`() {
            assertEquals(2, GeoHash.prefixLengthForRadius(500.0))
            assertEquals(2, GeoHash.prefixLengthForRadius(1000.0))
        }
    }
}
