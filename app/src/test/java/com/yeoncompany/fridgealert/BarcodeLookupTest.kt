package com.yeoncompany.fridgealert

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BarcodeLookupTest {
    @Test
    fun `API 응답에서 이름·보관방법·기한을 읽는다`() {
        val json = """{"C005":{"total_count":"1","row":[{"PRDLST_NM":"서울우유1000","BAR_CD":"8801115114154",
            "POG_DAYCNT":"냉장(0~10℃) 12일","PRDLST_DCNM":"우유"}],"RESULT":{"CODE":"INFO-000"}}}"""
        val r = BarcodeLookup.parse(json)!!
        assertEquals("서울우유1000", r.name)
        assertEquals("냉장", r.storageType)
        assertEquals(12, r.shelfDays)
        assertEquals("유제품", r.category)
    }

    @Test
    fun `결과가 없으면 null`() {
        assertNull(BarcodeLookup.parse("""{"C005":{"total_count":"0","RESULT":{"CODE":"INFO-200"}}}"""))
    }

    @Test
    fun `기한 문구를 일수로`() {
        assertEquals(360, BarcodeLookup.daysFromText("제조일로부터 12개월까지"))
        assertEquals(730, BarcodeLookup.daysFromText("2년"))
        assertNull(BarcodeLookup.daysFromText("별도 표기"))
    }
}
