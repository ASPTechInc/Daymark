package com.asptechinc.daymark.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class DateUtilsTest {
    @Test
    fun testRelativeDateText_Past() {
        val now = LocalDateTime.now()

        assertEquals("1 day ago", relativeDateText(now.minusDays(1), now))
        assertEquals("2 days ago", relativeDateText(now.minusDays(2), now))
        assertEquals("1 week ago", relativeDateText(now.minusWeeks(1), now))
        assertEquals("1 month ago", relativeDateText(now.minusMonths(1), now))
        assertEquals("1 year ago", relativeDateText(now.minusYears(1), now))
    }

    @Test
    fun testRelativeDateText_Future() {
        // Use a fixed date to avoid month-end boundary issues (e.g., Aug 31st -> Nov 30th)
        val now = LocalDateTime.of(2026, 1, 1, 12, 0)

        assertEquals("In 1 day", relativeDateText(now.plusDays(1), now))
        assertEquals("In 2 weeks", relativeDateText(now.plusWeeks(2), now))
        assertEquals("In 3 months", relativeDateText(now.plusMonths(3), now))
        assertEquals("In 1 year", relativeDateText(now.plusYears(1), now))
    }

    @Test
    fun testRelativeDateText_Mixed() {
        val now = LocalDateTime.of(2026, 8, 17, 12, 0)
        val then = LocalDateTime.of(2027, 10, 20, 12, 0)

        // Period handles this differently than Joda.
        // 2026-08-17 to 2027-10-20
        // 2026-08-17 to 2027-08-17 is 1 year
        // 2027-08-17 to 2027-10-17 is 2 months
        // 2027-10-17 to 2027-10-20 is 3 days
        assertEquals("In 1 year, 2 months, 3 days", relativeDateText(then, now))
    }

    @Test
    fun testRelativeDateText_Today() {
        val now = LocalDateTime.now()
        assertEquals("Today", relativeDateText(now, now))
    }

    @Test
    fun testOrdinalDateString() {
        assertEquals("1st January, 2026", LocalDateTime.of(2026, 1, 1, 0, 0).toOrdinalDateString())
        assertEquals("2nd February, 2026", LocalDateTime.of(2026, 2, 2, 0, 0).toOrdinalDateString())
        assertEquals("3rd March, 2026", LocalDateTime.of(2026, 3, 3, 0, 0).toOrdinalDateString())
        assertEquals("4th April, 2026", LocalDateTime.of(2026, 4, 4, 0, 0).toOrdinalDateString())
        assertEquals("11th May, 2026", LocalDateTime.of(2026, 5, 11, 0, 0).toOrdinalDateString())
        assertEquals("12th June, 2026", LocalDateTime.of(2026, 6, 12, 0, 0).toOrdinalDateString())
        assertEquals("13th July, 2026", LocalDateTime.of(2026, 7, 13, 0, 0).toOrdinalDateString())
        assertEquals("21st August, 2026", LocalDateTime.of(2026, 8, 21, 0, 0).toOrdinalDateString())
        assertEquals(
            "22nd September, 2026",
            LocalDateTime.of(2026, 9, 22, 0, 0).toOrdinalDateString(),
        )
        assertEquals(
            "23rd October, 2026",
            LocalDateTime.of(2026, 10, 23, 0, 0).toOrdinalDateString(),
        )
    }

    @Test
    fun testRelativeDateText_TimeUnits() {
        val now = LocalDateTime.of(2026, 8, 17, 12, 0)
        val past = now.minusDays(400) // 1 year, 1 month, 4 days (roughly)

        // 0: Year, month, weeks, day (Default)
        // 400 days = 1 year (365) + 35 days
        // 35 days = 1 month (31 in July) + 4 days
        // Period.between(2025-07-13, 2026-08-17)
        // 2025-07-13 to 2026-07-13 is 1 year
        // 2026-07-13 to 2026-08-13 is 1 month
        // 2026-08-13 to 2026-08-17 is 4 days
        assertEquals("1 year, 1 month, 4 days ago", relativeDateText(past, now, 0))

        // 1: Year only
        assertEquals("1 year ago", relativeDateText(past, now, 1))

        // 2: Months only
        // 2025-07-13 to 2026-08-17
        assertEquals("13 months ago", relativeDateText(past, now, 2))

        // 3: Weeks only
        // 400 / 7 = 57.14
        assertEquals("57 weeks ago", relativeDateText(past, now, 3))

        // 4: Days only
        assertEquals("400 days ago", relativeDateText(past, now, 4))
    }
}
