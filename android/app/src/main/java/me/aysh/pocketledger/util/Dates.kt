package me.aysh.pocketledger.util

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

object Dates {

    private val dayMonth = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
    private val dayMonthYear = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
    private val weekdayDayMonth = DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.ENGLISH)
    private val monthYear = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
    private val monthOnly = DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH)

    /** "Today", "Yesterday", "Monday, 12 Oct", or "12 Oct 2025" for other years. */
    fun relativeDay(date: LocalDate, today: LocalDate = LocalDate.now()): String = when {
        date == today -> "Today"
        date == today.minusDays(1) -> "Yesterday"
        date.year == today.year -> date.format(weekdayDayMonth)
        else -> date.format(dayMonthYear)
    }

    fun short(date: LocalDate, today: LocalDate = LocalDate.now()): String = when {
        date == today -> "Today"
        date == today.minusDays(1) -> "Yesterday"
        date.year == today.year -> date.format(dayMonth)
        else -> date.format(dayMonthYear)
    }

    /** "October" for the current year, "October 2025" otherwise. */
    fun month(month: YearMonth, current: YearMonth = YearMonth.now()): String =
        if (month.year == current.year) month.format(monthOnly) else month.format(monthYear)

    fun monthWithYear(month: YearMonth): String = month.format(monthYear)
}
