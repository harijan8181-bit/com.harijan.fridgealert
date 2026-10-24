package com.yeoncompany.fridgealert

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class ExpiryDateParserTest {
    private val now = Calendar.getInstance().apply { clear(); set(2026, Calendar.OCTOBER, 22, 12, 0) }.timeInMillis

    private fun ymd(millis: Long?): String? = millis?.let {
        val c = Calendar.getInstance().apply { timeInMillis = it }
        "%04d-%02d-%02d".format(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `소비기한 표시가 붙은 날짜를 고른다`() {
        assertEquals("2026-11-03", ymd(ExpiryDateParser.parse("제조일자 2026.10.20\n소비기한 2026.11.03까지", now)))
    }

    @Test
    fun `표시가 없으면 더 늦은 날짜를 고른다`() {
        assertEquals("2026-11-10", ymd(ExpiryDateParser.parse("2026.10.20\n2026.11.10", now)))
    }

    @Test
    fun `짧은 연도와 붙여 쓴 날짜`() {
        assertEquals("2026-12-01", ymd(ExpiryDateParser.parse("EXP 26.12.01", now)))
        assertEquals("2026-12-05", ymd(ExpiryDateParser.parse("유통기한 261205", now)))
    }

    @Test
    fun `날짜가 없거나 말이 안 되면 null`() {
        assertNull(ExpiryDateParser.parse("국산 콩 100%", now))
        assertNull(ExpiryDateParser.parse("소비기한 2026.13.45", now))
        assertNull(ExpiryDateParser.parse("소비기한 2040.01.01", now))
    }
}
