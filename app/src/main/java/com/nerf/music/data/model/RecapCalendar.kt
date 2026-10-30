package com.nerf.music.data.model

import java.time.LocalDate
import java.time.Month

data class RecapCalendarState(
    val recapYear: Int,
    val isInReleaseWindow: Boolean
)

object RecapCalendar {
    // 2026 is the product's first Recap season, so its live preview can be explored immediately.
    private const val FIRST_RECAP_YEAR = 2026

    fun forDate(date: LocalDate = LocalDate.now()): RecapCalendarState {
        val recapYear = if (date.year > FIRST_RECAP_YEAR && date.month == Month.JANUARY) {
            date.year - 1
        } else {
            date.year
        }
        val isInReleaseWindow = date.year == FIRST_RECAP_YEAR ||
            date.month == Month.DECEMBER ||
            date.month == Month.JANUARY
        return RecapCalendarState(recapYear, isInReleaseWindow)
    }
}
