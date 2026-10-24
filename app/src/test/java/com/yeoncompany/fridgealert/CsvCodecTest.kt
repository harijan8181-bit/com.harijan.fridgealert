package com.yeoncompany.fridgealert

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CsvCodecTest {
    @Test
    fun `쉼표와 따옴표가 든 이름도 그대로 복원된다`() {
        val item = FoodItem(
            name = "두부, \"국산\"", category = "신선식품", storageType = "냉장", quantity = 2,
            purchaseDate = 1000L, expiryDate = 2000L, status = "CONSUMED",
            openedDate = 1500L, notifyEnabled = false, price = 1450, closedDate = 1800L
        )
        assertEquals(item, CsvCodec.decode(CsvCodec.encode(item)))
    }

    @Test
    fun `예전 7칸 백업도 읽는다`() {
        val item = CsvCodec.decode("우유,유제품,냉장,1,1000,2000,ACTIVE")!!
        assertEquals("우유", item.name)
        assertNull(item.price)
        assertEquals(true, item.notifyEnabled)
    }

    @Test
    fun `잘못된 줄과 상태값`() {
        assertNull(CsvCodec.decode("이름만,있음"))
        assertEquals("ACTIVE", CsvCodec.decode("우유,유제품,냉장,0,1,2,???")!!.status)
        assertEquals(1, CsvCodec.decode("우유,유제품,냉장,0,1,2,ACTIVE")!!.quantity)
    }
}
