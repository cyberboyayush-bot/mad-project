package me.aysh.pocketledger.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {

    @Test
    fun `formats with Indian digit grouping`() {
        assertEquals("₹0", Money.format(0))
        assertEquals("₹999", Money.format(99_900))
        assertEquals("₹1,000", Money.format(100_000))
        assertEquals("₹12,345", Money.format(1_234_500))
        assertEquals("₹1,23,456.78", Money.format(12_345_678))
        assertEquals("₹10,00,000", Money.format(100_000_000))
        assertEquals("₹1,00,00,000", Money.format(1_000_000_000))
    }

    @Test
    fun `shows paise only when needed unless forced`() {
        assertEquals("₹12.50", Money.format(1_250))
        assertEquals("₹12.05", Money.format(1_205))
        assertEquals("₹12.00", Money.format(1_200, alwaysShowPaise = true))
        assertEquals("12", Money.format(1_200, withSymbol = false))
    }

    @Test
    fun `formats negative values`() {
        assertEquals("−₹1,500", Money.format(-150_000))
    }

    @Test
    fun `compact format`() {
        assertEquals("₹950", Money.formatCompact(95_000))
        assertEquals("₹12.5K", Money.formatCompact(1_250_000))
        assertEquals("₹3K", Money.formatCompact(300_000))
        assertEquals("₹3.2L", Money.formatCompact(32_000_000))
        assertEquals("₹1.1Cr", Money.formatCompact(1_100_000_000))
    }

    @Test
    fun `parses valid amounts`() {
        assertEquals(100L, Money.parseToPaise("1"))
        assertEquals(125_050L, Money.parseToPaise("1250.5"))
        assertEquals(125_005L, Money.parseToPaise("1,250.05"))
        assertEquals(50L, Money.parseToPaise(".5"))
        assertEquals(10_000L, Money.parseToPaise(" ₹100 "))
        assertEquals(100_000_000L, Money.parseToPaise("1000000"))
    }

    @Test
    fun `rejects invalid amounts`() {
        listOf("", "0", "0.00", "abc", "1.234", "-5", "1000000.01", "99999999999", "1..2").forEach {
            assertNull("expected null for '$it'", Money.parseToPaise(it))
        }
    }

    @Test
    fun `round trips through the input format`() {
        listOf(1L, 10L, 100L, 150L, 125_050L, 100_000_000L).forEach {
            assertEquals(it, Money.parseToPaise(Money.toInput(it)))
        }
        assertEquals("1250.5", Money.toInput(125_050))
        assertEquals("12.05", Money.toInput(1_205))
    }

    @Test
    fun `sanitizes typed input`() {
        assertEquals("", Money.sanitizeInput(""))
        assertEquals("12", Money.sanitizeInput("1a2"))
        assertEquals("7", Money.sanitizeInput("007"))
        assertEquals("0.5", Money.sanitizeInput("0.5"))
        assertEquals("0.", Money.sanitizeInput("."))
        assertEquals("1.23", Money.sanitizeInput("1.2.3"))
        assertEquals("1.99", Money.sanitizeInput("1.999"))
        assertEquals("1234567", Money.sanitizeInput("123456789"))
    }
}
