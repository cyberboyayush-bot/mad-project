package me.aysh.pocketledger.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class ValidationTest {

    @Test
    fun `email validation`() {
        assertNull(Validation.email("ayush@example.com"))
        assertNull(Validation.email("  a.b+tag@mail.co.in "))
        assertNotNull(Validation.email(""))
        assertNotNull(Validation.email("ayush"))
        assertNotNull(Validation.email("ayush@"))
        assertNotNull(Validation.email("ayush@site"))
    }

    @Test
    fun `name validation`() {
        assertNull(Validation.name("Ayush"))
        assertNotNull(Validation.name("   "))
        assertNotNull(Validation.name("x".repeat(61)))
    }

    @Test
    fun `password rules`() {
        assertNotNull(Validation.newPassword(""))
        assertNotNull(Validation.newPassword("1234567"))
        assertNull(Validation.newPassword("12345678"))
        assertNotNull(Validation.newPassword("x".repeat(129)))
        assertNull(Validation.passwordPresent("a"))
        assertNotNull(Validation.passwordPresent(""))
    }

    @Test
    fun `password strength increases with complexity`() {
        assertEquals(0, Validation.passwordStrength(""))
        assertEquals(1, Validation.passwordStrength("short"))
        assertEquals(1, Validation.passwordStrength("abcdefgh"))
        assertEquals(2, Validation.passwordStrength("abcdefg1"))
        assertEquals(4, Validation.passwordStrength("Abcdefgh1234!"))
    }

    @Test
    fun `amount validation`() {
        assertNull(Validation.amount("250"))
        assertNotNull(Validation.amount(""))
        assertNotNull(Validation.amount("0"))
        assertNotNull(Validation.amount("10000000"))
    }

    @Test
    fun `relative dates`() {
        val today = LocalDate.of(2026, 10, 31)
        assertEquals("Today", Dates.relativeDay(today, today))
        assertEquals("Yesterday", Dates.relativeDay(today.minusDays(1), today))
        assertEquals("Monday, 12 Oct", Dates.relativeDay(LocalDate.of(2026, 10, 12), today))
        assertEquals("12 Oct 2025", Dates.relativeDay(LocalDate.of(2025, 10, 12), today))
        assertEquals("October", Dates.month(YearMonth.of(2026, 10), YearMonth.of(2026, 10)))
        assertEquals("December 2025", Dates.month(YearMonth.of(2025, 12), YearMonth.of(2026, 10)))
    }
}
