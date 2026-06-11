package com.kidzone.utils

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class TextNormalizationTest {

    @Nested
    @DisplayName("toTitleCase()")
    inner class ToTitleCase {

        @Test
        fun `capitalizes first letter of each word`() {
            assertEquals("Plac Zabaw Kasztanowa", TextNormalization.toTitleCase("plac zabaw kasztanowa"))
        }

        @Test
        fun `lowercases rest of each word`() {
            assertEquals("Restauracja Niedźwiedź", TextNormalization.toTitleCase("RESTAURACJA NIEDŹWIEDŹ"))
        }

        @Test
        fun `trims leading and trailing whitespace`() {
            assertEquals("Test", TextNormalization.toTitleCase("  test  "))
        }

        @Test
        fun `collapses multiple whitespace`() {
            assertEquals("Ulica Marii Skłodowskiej", TextNormalization.toTitleCase("  ulica  Marii   Skłodowskiej  "))
        }

        @Test
        fun `returns empty string for blank input`() {
            assertEquals("", TextNormalization.toTitleCase(""))
            assertEquals("", TextNormalization.toTitleCase("   "))
        }

        @Test
        fun `preserves digits`() {
            assertEquals("Ulica 3 Maja", TextNormalization.toTitleCase("ulica 3 maja"))
        }

        @Test
        fun `handles single word`() {
            assertEquals("Park", TextNormalization.toTitleCase("park"))
            assertEquals("Park", TextNormalization.toTitleCase("PARK"))
        }

        @Test
        fun `handles single character`() {
            assertEquals("A", TextNormalization.toTitleCase("a"))
        }

        @Test
        fun `handles Polish diacritics correctly`() {
            assertEquals("Żółta Łódź", TextNormalization.toTitleCase("ŻÓŁTA ŁÓDŹ"))
            assertEquals("Ąęść", TextNormalization.toTitleCase("ĄĘŚĆ"))
        }

        @Test
        fun `hyphenated words stay as one word`() {
            // Hyphens are NOT word separators per spec
            assertEquals("Bla-bla", TextNormalization.toTitleCase("BLA-BLA"))
        }

        @Test
        fun `handles mixed case input`() {
            // Known limitation: toTitleCase lowercases rest of word,
            // so "McDonald's" becomes "Mcdonald's" (apostrophe preserved, case lost)
            assertEquals("Mcdonald's", TextNormalization.toTitleCase("McDonald's"))
        }

        @ParameterizedTest
        @CsvSource(
            "'ul. krótka', 'Ul. Krótka'",
            "'30A', '30a'",
            "'plac zabaw nr 5', 'Plac Zabaw Nr 5'"
        )
        fun `various inputs produce expected outputs`(input: String, expected: String) {
            assertEquals(expected, TextNormalization.toTitleCase(input))
        }
    }

    @Nested
    @DisplayName("toSentenceCase()")
    inner class ToSentenceCase {

        @Test
        fun `capitalizes first character only`() {
            assertEquals("Piękne miejsce dla dzieci", TextNormalization.toSentenceCase("piękne miejsce dla dzieci"))
        }

        @Test
        fun `preserves rest of text as-is`() {
            assertEquals("Bardzo Fajne Miejsce", TextNormalization.toSentenceCase("bardzo Fajne Miejsce"))
        }

        @Test
        fun `trims and collapses whitespace`() {
            assertEquals("Tekst z wieloma spacjami", TextNormalization.toSentenceCase("  tekst  z  wieloma  spacjami  "))
        }

        @Test
        fun `returns empty string for blank input`() {
            assertEquals("", TextNormalization.toSentenceCase(""))
            assertEquals("", TextNormalization.toSentenceCase("   "))
        }

        @Test
        fun `handles single character`() {
            assertEquals("A", TextNormalization.toSentenceCase("a"))
        }

        @Test
        fun `handles already capitalized text`() {
            assertEquals("Already Good", TextNormalization.toSentenceCase("Already Good"))
        }

        @Test
        fun `handles Polish diacritics at start`() {
            assertEquals("Ąbcdef", TextNormalization.toSentenceCase("ąbcdef"))
            assertEquals("Żółty pies", TextNormalization.toSentenceCase("żółty pies"))
        }

        @Test
        fun `preserves proper nouns in middle`() {
            // This is the desired behavior - only first char uppercased, rest untouched
            assertEquals("Spotkanie w McDonald's o 15:00", TextNormalization.toSentenceCase("spotkanie w McDonald's o 15:00"))
        }

        @Test
        fun `handles text starting with number`() {
            assertEquals("5 gwiazdek za to miejsce", TextNormalization.toSentenceCase("5 gwiazdek za to miejsce"))
        }

        @Test
        fun `handles text starting with special character`() {
            val result = TextNormalization.toSentenceCase("!wow great place")
            assertEquals("!wow great place", result)
        }
    }
}
