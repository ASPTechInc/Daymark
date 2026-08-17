package com.asptechinc.daymark.utils

import org.joda.time.DateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class DateUtilsTest {
    @Test
    fun testRelativeDateText_Past() {
        val now = DateTime.now()

        assertEquals("1 day ago", relativeDateText(now.minusDays(1), now))
        assertEquals("2 days ago", relativeDateText(now.minusDays(2), now))
        assertEquals("1 week ago", relativeDateText(now.minusWeeks(1), now))
        assertEquals("1 month ago", relativeDateText(now.minusMonths(1), now))
        assertEquals("1 year ago", relativeDateText(now.minusYears(1), now))
    }

    @Test
    fun testRelativeDateText_Future() {
        val now = DateTime.now()

        assertEquals("in 1 day", relativeDateText(now.plusDays(1), now))
        assertEquals("in 2 weeks", relativeDateText(now.plusWeeks(2), now))
        assertEquals("in 3 months", relativeDateText(now.plusMonths(3), now))
        assertEquals("in 1 year", relativeDateText(now.plusYears(1), now))
    }

    @Test
    fun testRelativeDateText_Mixed() {
        val now = DateTime(2026, 8, 17, 12, 0)
        val then = DateTime(2027, 10, 20, 12, 0)

        // 1 year, 2 months, 3 days
        assertEquals("in 1 year, 2 months, 3 days", relativeDateText(then, now))
    }

    @Test
    fun testRelativeDateText_Today() {
        val now = DateTime.now()
        assertEquals("0 days ago", relativeDateText(now, now))
    }

    @Test
    fun testOrdinalDateString() {
        assertEquals("1st January, 2026", DateTime(2026, 1, 1, 0, 0).toOrdinalDateString())
        assertEquals("2nd February, 2026", DateTime(2026, 2, 2, 0, 0).toOrdinalDateString())
        assertEquals("3rd March, 2026", DateTime(2026, 3, 3, 0, 0).toOrdinalDateString())
        assertEquals("4th April, 2026", DateTime(2026, 4, 4, 0, 0).toOrdinalDateString())
        assertEquals("11th May, 2026", DateTime(2026, 5, 11, 0, 0).toOrdinalDateString())
        assertEquals("12th June, 2026", DateTime(2026, 6, 12, 0, 0).toOrdinalDateString())
        assertEquals("13th July, 2026", DateTime(2026, 7, 13, 0, 0).toOrdinalDateString())
        assertEquals("21st August, 2026", DateTime(2026, 8, 21, 0, 0).toOrdinalDateString())
        assertEquals("22nd September, 2026", DateTime(2026, 9, 22, 0, 0).toOrdinalDateString())
        assertEquals("23rd October, 2026", DateTime(2026, 10, 23, 0, 0).toOrdinalDateString())
    }
}
