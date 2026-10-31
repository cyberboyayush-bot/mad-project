package me.aysh.pocketledger.util

import kotlin.math.abs

object Money {

    /** ₹10,00,000, matching the server limit. */
    const val MAX_PAISE = 100_000_000L

    /**
     * Formats paise as Indian rupees with lakh/crore grouping, e.g. 12345678 -> "₹1,23,456.78".
     * Paise are hidden when they are zero unless [alwaysShowPaise] is set.
     */
    fun format(paise: Long, alwaysShowPaise: Boolean = false, withSymbol: Boolean = true): String {
        val negative = paise < 0
        val absolute = abs(paise)
        val rupees = absolute / 100
        val fraction = absolute % 100
        val body = buildString {
            append(groupIndian(rupees))
            if (alwaysShowPaise || fraction != 0L) append('.').append(fraction.toString().padStart(2, '0'))
        }
        return buildString {
            if (negative) append('−')
            if (withSymbol) append('₹')
            append(body)
        }
    }

    /** Short form for tight spaces: ₹950, ₹12.5K, ₹3.2L, ₹1.1Cr. */
    fun formatCompact(paise: Long): String {
        val rupees = abs(paise) / 100.0
        val sign = if (paise < 0) "−" else ""
        val text = when {
            rupees >= 1_00_00_000 -> trim(rupees / 1_00_00_000) + "Cr"
            rupees >= 1_00_000 -> trim(rupees / 1_00_000) + "L"
            rupees >= 1_000 -> trim(rupees / 1_000) + "K"
            else -> rupees.toLong().toString()
        }
        return "$sign₹$text"
    }

    /** Groups digits Indian style: last three, then pairs. 1234567 -> "12,34,567". */
    fun groupIndian(value: Long): String {
        val digits = value.toString()
        if (digits.length <= 3) return digits
        val lastThree = digits.takeLast(3)
        val rest = digits.dropLast(3)
        val pairs = rest.reversed().chunked(2).joinToString(",").reversed()
        return "$pairs,$lastThree"
    }

    /**
     * Parses user input like "1,250.5" into paise. Returns null for anything that isn't a
     * positive amount with at most two decimal places within [MAX_PAISE].
     */
    fun parseToPaise(input: String): Long? {
        val cleaned = input.trim().replace(",", "").removePrefix("₹").trim()
        if (!AMOUNT_PATTERN.matches(cleaned)) return null
        val parts = cleaned.split('.')
        val rupees = parts[0].ifEmpty { "0" }.toLongOrNull() ?: return null
        val fraction = parts.getOrNull(1)?.padEnd(2, '0')?.toLong() ?: 0L
        if (rupees > MAX_PAISE / 100) return null
        val paise = rupees * 100 + fraction
        return paise.takeIf { it in 1..MAX_PAISE }
    }

    /** Converts paise back into an editable string without grouping, e.g. 125050 -> "1250.5". */
    fun toInput(paise: Long): String {
        val rupees = paise / 100
        val fraction = paise % 100
        return when {
            fraction == 0L -> rupees.toString()
            fraction % 10 == 0L -> "$rupees.${fraction / 10}"
            else -> "$rupees.${fraction.toString().padStart(2, '0')}"
        }
    }

    /** Keeps only characters that can form a valid amount while the user types. */
    fun sanitizeInput(input: String): String {
        val filtered = input.filter { it.isDigit() || it == '.' }
        val dot = filtered.indexOf('.')
        val normalized = if (dot == -1) filtered else {
            filtered.substring(0, dot + 1) + filtered.substring(dot + 1).replace(".", "").take(2)
        }
        val integerPart = normalized.substringBefore('.').trimStart('0').ifEmpty { if (normalized.isEmpty()) "" else "0" }
        val limited = integerPart.take(7)
        return if (dot == -1) limited else "$limited.${normalized.substringAfter('.')}"
    }

    private fun trim(value: Double): String {
        val rounded = Math.round(value * 10) / 10.0
        return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
    }

    private val AMOUNT_PATTERN = Regex("""^\d{0,9}(\.\d{1,2})?$""")
}
