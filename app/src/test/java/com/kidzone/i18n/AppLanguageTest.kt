package com.kidzone.i18n

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {

    @Test
    fun `returns matching language from persisted value`() {
        assertEquals(AppLanguage.POLISH, AppLanguage.fromStorageValue("pl"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromStorageValue("en"))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromStorageValue("system"))
    }

    @Test
    fun `falls back to system language for unknown persisted value`() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromStorageValue(null))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromStorageValue("de"))
    }
}
