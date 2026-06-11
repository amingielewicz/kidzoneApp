package com.kidzone.utils

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class PasswordPolicyTest {

    @Nested
    @DisplayName("isValid()")
    inner class IsValid {

        @Test
        fun `valid password with all requirements met`() {
            assertTrue(PasswordPolicy.isValid("StrongP@ss1"))
        }

        @Test
        fun `valid with exactly MIN_LENGTH characters`() {
            // 8 chars: 1 upper, 1 lower, 1 special
            assertTrue(PasswordPolicy.isValid("Abcdef@1"))
        }

        @ParameterizedTest
        @ValueSource(strings = ["Short@1", "Ab@1234", "Xx!5678"])
        fun `invalid when shorter than MIN_LENGTH`(password: String) {
            // These are 7 chars - below minimum of 8
            assertFalse(PasswordPolicy.isValid(password))
        }

        @Test
        fun `invalid without lowercase letter`() {
            assertFalse(PasswordPolicy.isValid("ABCDEFGH@1"))
        }

        @Test
        fun `invalid without uppercase letter`() {
            assertFalse(PasswordPolicy.isValid("abcdefgh@1"))
        }

        @Test
        fun `invalid without special character`() {
            assertFalse(PasswordPolicy.isValid("Abcdefgh1"))
        }

        @Test
        fun `special characters include various symbols`() {
            assertTrue(PasswordPolicy.isValid("Abcdefg!"))
            assertTrue(PasswordPolicy.isValid("Abcdefg@"))
            assertTrue(PasswordPolicy.isValid("Abcdefg#"))
            assertTrue(PasswordPolicy.isValid("Abcdefg\$"))
            assertTrue(PasswordPolicy.isValid("Abcdefg%"))
            assertTrue(PasswordPolicy.isValid("Abcdefg^"))
            assertTrue(PasswordPolicy.isValid("Abcdefg&"))
            assertTrue(PasswordPolicy.isValid("Abcdefg*"))
        }

        @Test
        fun `whitespace does not count as special character`() {
            assertFalse(PasswordPolicy.isValid("Abcdefg 1"))
        }

        @Test
        fun `empty password is invalid`() {
            assertFalse(PasswordPolicy.isValid(""))
        }

        @Test
        fun `password with only special characters is invalid (no letters)`() {
            assertFalse(PasswordPolicy.isValid("!@#\$%^&*"))
        }

        @Test
        fun `Polish diacritics count as lowercase`() {
            // ą is lowercase, contains uppercase, has special char
            assertTrue(PasswordPolicy.isValid("Ąbcdefg!"))
        }

        @Test
        fun `Polish uppercase diacritics count as uppercase`() {
            // Ź is uppercase
            assertTrue(PasswordPolicy.isValid("abcdefŹ!"))
        }
    }

    @Nested
    @DisplayName("evaluate()")
    inner class Evaluate {

        @Test
        fun `returns all rules unsatisfied for empty password`() {
            val results = PasswordPolicy.evaluate("")
            assertEquals(4, results.size)
            assertTrue(results.all { !it.isSatisfied })
        }

        @Test
        fun `returns correct satisfaction per rule`() {
            val results = PasswordPolicy.evaluate("abcdefgh")
            // Length: satisfied (8 chars)
            assertTrue(results[0].isSatisfied)
            // Lowercase: satisfied
            assertTrue(results[1].isSatisfied)
            // Uppercase: NOT satisfied
            assertFalse(results[2].isSatisfied)
            // Special: NOT satisfied
            assertFalse(results[3].isSatisfied)
        }

        @Test
        fun `all satisfied for strong password`() {
            val results = PasswordPolicy.evaluate("StrongP@ss1")
            assertTrue(results.all { it.isSatisfied })
        }

        @Test
        fun `each result has a non-empty label`() {
            val results = PasswordPolicy.evaluate("test")
            assertTrue(results.all { it.label.isNotBlank() })
        }
    }

    @Nested
    @DisplayName("rules")
    inner class Rules {

        @Test
        fun `has exactly 4 rules`() {
            assertEquals(4, PasswordPolicy.rules.size)
        }

        @Test
        fun `MIN_LENGTH is 8`() {
            assertEquals(8, PasswordPolicy.MIN_LENGTH)
        }

        @Test
        fun `DEFAULT_ERROR_MESSAGE is not blank`() {
            assertTrue(PasswordPolicy.DEFAULT_ERROR_MESSAGE.isNotBlank())
            assertTrue(PasswordPolicy.DEFAULT_ERROR_MESSAGE.contains("8"))
        }
    }
}
