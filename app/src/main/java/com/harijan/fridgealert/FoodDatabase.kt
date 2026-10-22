package com.harijan.fridgealert

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// ==========================================
// 1. Room DB & Entity
// ==========================================
@Entity(tableName = "food_items")
data class FoodItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val storageType: String = "냉장", // "냉장", "냉동", "실온"
    val quantity: Int = 1,
    val purchaseDate: Long,
    val expiryDate: Long,
    val status: String = "ACTIVE", // "ACTIVE", "CONSUMED", "WASTED"
    // v4: 개봉한 날 (null이면 미개봉)
    val openedDate: Long? = null,
    // v4: 품목별 알림 on/off
    @ColumnInfo(defaultValue = "1") val notifyEnabled: Boolean = true,
    // v4: 구매 금액(원, 이 줄 합계). 모르면 null → 리포트에서 추정값 사용
    val price: Int? = null,
    // v4: 알림에서 "내일 다시"를 누르면 이 시각 전까지는 알리지 않음
    val snoozedUntil: Long? = null,
    // v4: 처리(소비/폐기)한 시각 — 월별 리포트용
    val closedDate: Long? = null
)

@Entity(tableName = "user_presets")
data class UserPreset(
    @PrimaryKey val keyword: String,
    val shelfDays: Int,
    val storageType: String
)

// 바코드 → 상품 정보 (API 결과나 사용자가 고친 값을 기억해 두고 다음부터 오프라인으로 바로 채움)
@Entity(tableName = "barcode_products")
data class BarcodeProduct(
    @PrimaryKey val barcode: String,
    val name: String,
    val category: String,
    val storageType: String,
    val shelfDays: Int
)

@Entity(tableName = "shopping_items")
data class ShoppingItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(defaultValue = "0") val checked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface FoodDao {
    @Query("SELECT * FROM food_items WHERE status = 'ACTIVE' ORDER BY expiryDate ASC")
    fun getActiveFoods(): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_items WHERE status = 'ACTIVE' ORDER BY expiryDate ASC")
    suspend fun getActiveFoodsOnce(): List<FoodItem>

    @Query("SELECT * FROM food_items WHERE status != 'ACTIVE' ORDER BY COALESCE(closedDate, expiryDate) DESC")
    fun getArchivedFoods(): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_items")
    suspend fun getAllFoodsOnce(): List<FoodItem>

    @Query("SELECT * FROM food_items WHERE id = :id")
    suspend fun getFood(id: Long): FoodItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<FoodItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: FoodItem)

    @Update
    suspend fun update(item: FoodItem)

    @Delete
    suspend fun delete(item: FoodItem)

    // 연속 탭 시 화면의 옛 수량을 덮어쓰지 않도록 DB에서 직접 증감
    @Query("UPDATE food_items SET quantity = MAX(1, quantity + :delta) WHERE id = :id")
    suspend fun adjustQuantity(id: Long, delta: Int)

    @Query("UPDATE food_items SET status = :status, closedDate = :closedAt WHERE id = :id")
    suspend fun setStatus(id: Long, status: String, closedAt: Long?)

    @Query("UPDATE food_items SET snoozedUntil = :until WHERE id = :id")
    suspend fun setSnooze(id: Long, until: Long?)

    @Query("SELECT * FROM user_presets WHERE keyword = :keyword LIMIT 1")
    suspend fun getPreset(keyword: String): UserPreset?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePreset(preset: UserPreset)

    @Query("SELECT * FROM barcode_products WHERE barcode = :barcode LIMIT 1")
    suspend fun getBarcodeProduct(barcode: String): BarcodeProduct?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveBarcodeProduct(product: BarcodeProduct)

    // ---- 장보기 목록 ----
    @Query("SELECT * FROM shopping_items ORDER BY checked ASC, createdAt DESC")
    fun getShoppingItems(): Flow<List<ShoppingItem>>

    @Insert
    suspend fun insertShopping(item: ShoppingItem)

    @Update
    suspend fun updateShopping(item: ShoppingItem)

    @Delete
    suspend fun deleteShopping(item: ShoppingItem)

    @Query("DELETE FROM shopping_items WHERE checked = 1")
    suspend fun clearCheckedShopping()

    @Query("SELECT COUNT(*) FROM shopping_items WHERE checked = 0 AND name = :name")
    suspend fun countOpenShopping(name: String): Int
}

// 스키마는 app/schemas에 기록된다. 구조를 바꿀 땐 version을 올리고 AutoMigration(또는 Migration)을 꼭 추가할 것.
// 마이그레이션이 빠지면 업데이트 후 앱이 바로 오류로 멈춘다 (데이터는 지워지지 않음).
@Database(
    entities = [FoodItem::class, UserPreset::class, ShoppingItem::class, BarcodeProduct::class],
    version = 4,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 3, to = 4)]
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fridge_notifier_v3.db"
                )
                    // 앱을 구버전으로 되돌리는 다운그레이드만 초기화 허용
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
