package com.yeoncompany.fridgealert

import org.junit.Assert.assertEquals
import org.junit.Test

class DateUtilsTest {
    @Test
    fun `D-day 계산`() {
        val now = System.currentTimeMillis()
        assertEquals(0L, calculateDDay(now))
        assertEquals(3L, calculateDDay(calculateExpiryTimestamp(now, 3)))
        assertEquals(-2L, calculateDDay(calculateExpiryTimestamp(now, -2)))
    }

    @Test
    fun `경과일은 음수가 되지 않는다`() {
        val now = System.currentTimeMillis()
        assertEquals(0L, calculateElapsedDays(calculateExpiryTimestamp(now, 5)))
        assertEquals(4L, calculateElapsedDays(calculateExpiryTimestamp(now, -4)))
    }
}
