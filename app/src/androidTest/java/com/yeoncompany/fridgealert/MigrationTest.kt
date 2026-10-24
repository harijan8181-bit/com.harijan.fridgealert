package com.yeoncompany.fridgealert

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// v3(첫 출시 구조)에 저장된 식재료가 v4로 올라가도 남아 있는지 확인 (실기기/에뮬레이터에서 실행)
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val dbName = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun migrate3To4_keepsFoods() {
        helper.createDatabase(dbName, 3).use { db ->
            db.execSQL(
                "INSERT INTO food_items (name, category, storageType, quantity, purchaseDate, expiryDate, status) " +
                        "VALUES ('우유', '유제품', '냉장', 2, 1000, 2000, 'ACTIVE')"
            )
        }
        helper.runMigrationsAndValidate(dbName, 4, true).use { db ->
            db.query("SELECT name, quantity, notifyEnabled, price FROM food_items").use { c ->
                assertEquals(1, c.count)
                c.moveToFirst()
                assertEquals("우유", c.getString(0))
                assertEquals(2, c.getInt(1))
                assertEquals(1, c.getInt(2)) // 새 칸 기본값: 알림 켬
                assertEquals(true, c.isNull(3)) // 가격 없음
            }
        }
    }
}
