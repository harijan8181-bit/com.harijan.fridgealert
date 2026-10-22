package com.harijan.fridgealert

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ReceiptParserTest {
    private fun ymd(millis: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        return "%04d-%02d-%02d".format(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
    }

    // 오늘 기준 며칠 전 날짜를 영수증 형식 문자열로
    private fun daysAgo(days: Int, pattern: String): String {
        val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -days) }
        return pattern
            .replace("yyyy", "%04d".format(c.get(Calendar.YEAR)))
            .replace("yy", "%02d".format(c.get(Calendar.YEAR) % 100))
            .replace("MM", "%02d".format(c.get(Calendar.MONTH) + 1))
            .replace("dd", "%02d".format(c.get(Calendar.DAY_OF_MONTH)))
    }

    @Test
    fun `품명에서 용량 단위와 가격을 떼어낸다`() {
        val r = ReceiptParser.parse(
            """
            서울우유 1L     2,850원 2개
            찌개용 두부 300g 1,450원 1개
            신선란 30구     6,980원 1개
            배추 1통 3,000원 1개
            """.trimIndent()
        )
        assertEquals(listOf("서울우유", "찌개용 두부", "신선란", "배추"), r.items.map { it.name })
        assertEquals(2, r.items[0].quantity)
        assertEquals(listOf(2850, 1450, 6980, 3000), r.items.map { it.price })
    }

    @Test
    fun `상호명 줄은 식재료로 잡지 않는다`() {
        val r = ReceiptParser.parse(
            """
            [이마트 역삼점]
            홈플러스 강남점
            GS25 역삼역점
            이마트 피코크 짜장 3,980원 1개
            """.trimIndent()
        )
        assertEquals(listOf("이마트 피코크 짜장"), r.items.map { it.name })
    }

    @Test
    fun `여러 날짜 형식을 구매일로 읽는다`() {
        val expected = ymd(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -3) }.timeInMillis)
        for (pattern in listOf("yyyy-MM-dd 18:30", "yyyy년 MM월 dd일", "yy.MM.dd 12:00", "yy/MM/dd")) {
            val r = ReceiptParser.parse(daysAgo(3, pattern) + "\n우유 1,000원")
            assertEquals(pattern, expected, ymd(r.purchaseDateMillis))
        }
    }

    @Test
    fun `미래 날짜와 전화번호는 구매일로 쓰지 않는다`() {
        val today = ymd(System.currentTimeMillis())
        assertEquals(today, ymd(ReceiptParser.parse("2099-01-01\n우유 1,000원").purchaseDateMillis))
        assertEquals(today, ymd(ReceiptParser.parse("TEL 02-123-4567\n우유 1,000원").purchaseDateMillis))
    }

    @Test
    fun `금액 추출은 용량·바코드를 무시한다`() {
        assertEquals(14500, ReceiptParser.findPrice("한돈 삼겹살 600g 14,500원 1개"))
        assertNull(ReceiptParser.findPrice("8801234567890"))
        assertNull(ReceiptParser.findPrice("우유 2개"))
    }

    @Test
    fun `김치와 배추를 엉뚱한 분류로 보내지 않는다`() {
        assertEquals("냉장", ShelfLifeMapper.getDefaultPreset("포기김치").storageType)
        assertEquals("채소", ShelfLifeMapper.getDefaultPreset("배추").category)
        assertTrue(ShelfLifeMapper.openedDays("서울우유") <= 3)
        assertFalse(ShelfLifeMapper.openedDays("케첩") <= 3)
    }
}
