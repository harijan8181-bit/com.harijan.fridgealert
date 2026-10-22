package com.harijan.fridgealert

import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
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
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

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
    val status: String = "ACTIVE" // "ACTIVE", "CONSUMED", "WASTED"
)

@Entity(tableName = "user_presets")
data class UserPreset(
    @PrimaryKey val keyword: String,
    val shelfDays: Int,
    val storageType: String
)

@Dao
interface FoodDao {
    @Query("SELECT * FROM food_items WHERE status = 'ACTIVE' ORDER BY expiryDate ASC")
    fun getActiveFoods(): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_items WHERE status != 'ACTIVE' ORDER BY expiryDate DESC")
    fun getArchivedFoods(): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_items")
    suspend fun getAllFoodsOnce(): List<FoodItem>

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

    @Query("SELECT * FROM user_presets WHERE keyword = :keyword LIMIT 1")
    suspend fun getPreset(keyword: String): UserPreset?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePreset(preset: UserPreset)
}

// 스키마를 app/schemas에 기록해 두면 나중에 컬럼을 추가할 때 AutoMigration으로 데이터를 지키며 올릴 수 있다.
// 구조를 바꿀 땐 version을 올리고 autoMigrations = [AutoMigration(from = 3, to = 4)] 또는 Migration을 추가할 것.
@Database(entities = [FoodItem::class, UserPreset::class], version = 3, exportSchema = true)
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
                    // 업그레이드 때 마이그레이션이 없으면 데이터를 지우지 않고 바로 오류가 나서 테스트에서 걸리게 한다.
                    // (앱을 구버전으로 되돌리는 다운그레이드만 초기화 허용)
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// ==========================================
// 2. WorkManager 알림 스케줄러
// ==========================================
const val NOTIFICATION_CHANNEL_ID = "fridge_expiry_channel"

fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = "냉장고 소비기한 알림"
        val descriptionText = "소비기한 임박 및 당일 만료 식품을 안내합니다."
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}

class ExpiryCheckWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val dao = AppDatabase.getDatabase(context).foodDao()
            val activeFoods = dao.getActiveFoods().firstOrNull() ?: emptyList()

            val urgentItems = mutableListOf<String>()
            val expiredItems = mutableListOf<String>()

            for (food in activeFoods) {
                val dDay = calculateDDay(food.expiryDate)
                if (dDay < 0) {
                    expiredItems.add(food.name)
                } else if (dDay <= 3) {
                    urgentItems.add("${food.name}(D-${dDay})")
                }
            }

            if (urgentItems.isNotEmpty() || expiredItems.isNotEmpty()) {
                val message = buildString {
                    if (urgentItems.isNotEmpty()) {
                        append("⚠️ 임박: ${urgentItems.take(3).joinToString(", ")}")
                        if (urgentItems.size > 3) append(" 외 ${urgentItems.size - 3}건")
                    }
                    if (expiredItems.isNotEmpty()) {
                        if (isNotEmpty()) append(" | ")
                        append("🚨 만료: ${expiredItems.take(2).joinToString(", ")}")
                    }
                }
                sendNotification(context, "냉장고 소비기한 알림", message)
            }
            Result.success()
        } catch (_: Exception) {
            Result.failure()
        }
    }

    companion object {
        fun sendNotification(context: Context, title: String, content: String) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            val notifyIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("open_stored_dialog", true)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                1001,
                notifyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            NotificationManagerCompat.from(context).notify(1001, builder.build())
        }
    }
}

// ==========================================
// 3. 영수증 OCR 정제 & 매핑
// ==========================================
data class ParsedItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    var name: String,
    var category: String,
    var storageType: String = "냉장",
    var quantity: Int = 1,
    var shelfDays: Int,
    var isChecked: Boolean = true
)

data class ReceiptParseResult(
    val purchaseDateMillis: Long,
    val items: List<ParsedItem>
)

object ShelfLifeMapper {
    data class PresetResult(val category: String, val storageType: String, val days: Int)

    fun getDefaultPreset(name: String): PresetResult {
        val lower = name.lowercase()
        return when {
            lower.contains("우유") || lower.contains("밀크") -> PresetResult("유제품", "냉장", 14)
            lower.contains("요거트") || lower.contains("요구르트") -> PresetResult("유제품", "냉장", 10)
            lower.contains("치즈") || lower.contains("버터") -> PresetResult("유제품", "냉장", 30)
            // "김"(김치→실온), "배"(배추→과일) 오분류를 막기 위해 먼저 검사
            lower.contains("김치") -> PresetResult("반찬", "냉장", 30)
            lower.contains("배추") || lower.contains("양배추") -> PresetResult("채소", "냉장", 7)
            lower.contains("란") || lower.contains("계란") || lower.contains("달걀") -> PresetResult("신선식품", "냉장", 30)
            lower.contains("두부") || lower.contains("콩나물") || lower.contains("숙주") -> PresetResult("신선식품", "냉장", 5)
            lower.contains("삼겹") || lower.contains("목살") || lower.contains("소고기") ||
                    lower.contains("한우") || lower.contains("돼지") || lower.contains("닭") -> PresetResult("육류", "냉장", 4)
            lower.contains("만두") || lower.contains("너겟") || lower.contains("피자") || lower.contains("아이스") -> PresetResult("냉동식품", "냉동", 90)
            lower.contains("생선") || lower.contains("고등어") || lower.contains("연어") ||
                    lower.contains("갈치") || lower.contains("오징어") || lower.contains("새우") -> PresetResult("수산물", "냉장", 3)
            lower.contains("상추") || lower.contains("깻잎") || lower.contains("시금치") ||
                    lower.contains("샐러드") || lower.contains("버섯") || lower.contains("양파") -> PresetResult("채소", "냉장", 5)
            lower.contains("사과") || lower.contains("배") || lower.contains("바나나") ||
                    lower.contains("포도") || lower.contains("오렌지") -> PresetResult("과일", "냉장", 14)
            lower.contains("라면") || lower.contains("통조림") || lower.contains("햇반") || lower.contains("김") -> PresetResult("가공식품", "실온", 180)
            lower.contains("식빵") || lower.contains("빵") || lower.contains("케이크") -> PresetResult("베이커리", "실온", 4)
            else -> PresetResult("일반", "냉장", 7)
        }
    }
}

object ReceiptParser {
    private val NOISE_WORDS = listOf(
        "상호", "대표", "사업자", "TEL", "전화", "주소", "영수증", "POS", "카드", "승인",
        "부가세", "과세", "면세", "합계", "할인", "결제", "거스름돈", "포인트", "현금", "가맹점",
        "단가", "수량", "금액", "쿠폰", "매출", "문의", "반품", "교환", "매장", "점포", "사업장"
    )

    fun parse(rawText: String): ReceiptParseResult {
        val results = mutableListOf<ParsedItem>()
        val lines = rawText.lines()

        val purchaseDateMillis = lines.firstNotNullOfOrNull { findDate(it) } ?: System.currentTimeMillis()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.length < 2) continue
            if (NOISE_WORDS.any { trimmed.contains(it, ignoreCase = true) }) continue
            if (isStoreNameLine(trimmed)) continue

            val qtyMatch = Regex("""(\d+)\s*(개|EA|입)""").find(trimmed)
            val detectedQty = qtyMatch?.groupValues?.get(1)?.toIntOrNull() ?: 1

            var cleaned = trimmed
                // "1L", "300g", "30구" 같은 용량 표기가 "L", "g", "구"로 남지 않도록 단위째 제거
                .replace(Regex("""\d+(\.\d+)?\s*(kg|g|ml|mL|L|l|구|봉|팩|매|통|망|단)(?![가-힣A-Za-z])"""), "")
                .replace(Regex("""[\d,]+(\s*원|\s*개|\s*EA|\s*입)?"""), "")
                .replace(Regex("""[^\w가-힣\s]"""), " ")
                .trim()

            if (cleaned.length < 2) continue
            if (cleaned.all { it.isDigit() || it.isWhitespace() }) continue
            cleaned = cleaned.replace(Regex("""\s+"""), " ")

            val preset = ShelfLifeMapper.getDefaultPreset(cleaned)
            results.add(
                ParsedItem(
                    name = cleaned,
                    category = preset.category,
                    storageType = preset.storageType,
                    quantity = detectedQty,
                    shelfDays = preset.days,
                    isChecked = true
                )
            )
        }
        return ReceiptParseResult(purchaseDateMillis, results.distinctBy { it.name })
    }

    // 2026-10-22, 2026.10.22, 2026년 10월 22일, 2026-1-5, 26.10.22, 26/10/22
    private val FULL_DATE = Regex("""(20\d{2})\s*[-/.년]\s*(\d{1,2})\s*[-/.월]\s*(\d{1,2})""")
    private val SHORT_DATE = Regex("""(?<!\d)(\d{2})[-/.](\d{1,2})[-/.](\d{1,2})(?!\d)""")

    private fun findDate(line: String): Long? {
        FULL_DATE.find(line)?.let { m ->
            toValidDate(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt())?.let { return it }
        }
        SHORT_DATE.find(line)?.let { m ->
            toValidDate(2000 + m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt())?.let { return it }
        }
        return null
    }

    // 존재하지 않는 날짜, 미래 날짜, 2년보다 오래된 날짜는 전화번호·바코드 등 오인식으로 보고 버린다
    private fun toValidDate(year: Int, month: Int, day: Int): Long? {
        if (month !in 1..12 || day !in 1..31) return null
        val cal = Calendar.getInstance().apply {
            isLenient = false
            clear()
            set(year, month - 1, day)
        }
        val millis = try { cal.timeInMillis } catch (_: Exception) { return null }
        val now = System.currentTimeMillis()
        val twoYears = 2L * 365 * 24 * 60 * 60 * 1000
        if (millis > now + 24L * 60 * 60 * 1000 || millis < now - twoYears) return null
        return millis
    }

    private val STORE_WORDS = listOf(
        "이마트", "홈플러스", "롯데마트", "코스트코", "트레이더스", "하나로", "노브랜드",
        "GS25", "CU", "세븐일레븐", "이마트24", "편의점", "슈퍼", "마트", "상회", "식자재"
    )
    private val PRICE = Regex("""\d{1,3}(,\d{3})+|\d{4,}""")

    // 가격이 없는 줄 중 [상호] 형태, "~점"으로 끝나는 지점명, 마트·편의점 이름이 든 줄은 상호명으로 본다
    // (가격이 있으면 "이마트 피코크 ..." 같은 상품일 수 있으니 남긴다)
    private fun isStoreNameLine(line: String): Boolean {
        if (PRICE.containsMatchIn(line)) return false
        if (line.startsWith("[") && line.endsWith("]")) return true
        if (Regex("""\S+점$""").containsMatchIn(line)) return true
        return STORE_WORDS.any { line.contains(it, ignoreCase = true) }
    }
}

// ==========================================
// 4. ViewModel
// ==========================================
class FridgeViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).foodDao()
    private val prefs = application.getSharedPreferences("fridge_prefs", Context.MODE_PRIVATE)

    init {
        // 앱을 켤 때마다 일정을 덮어쓰지 않도록 이미 등록돼 있으면 유지
        rescheduleNotification(
            hour = prefs.getInt("alert_hour", 9),
            minute = prefs.getInt("alert_minute", 0),
            policy = ExistingPeriodicWorkPolicy.KEEP
        )
    }

    val activeFoods = dao.getActiveFoods().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val archivedFoods = dao.getArchivedFoods().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    suspend fun resolvePreset(name: String): UserPreset? = dao.getPreset(name)

    // 학습값은 "오늘부터 남은 일수"가 아니라 "구매일→소비기한" 전체 보관 일수여야 한다
    private fun shelfDaysOf(item: FoodItem): Int =
        (calculateDDay(item.expiryDate) + calculateElapsedDays(item.purchaseDate)).toInt().coerceAtLeast(1)

    fun addFoodItems(items: List<FoodItem>) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertAll(items)
            for (item in items) {
                dao.savePreset(UserPreset(item.name, shelfDaysOf(item), item.storageType))
            }
        }
    }

    fun updateFoodItem(item: FoodItem) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.update(item)
            dao.savePreset(UserPreset(item.name, shelfDaysOf(item), item.storageType))
        }
    }

    fun decreaseQuantity(item: FoodItem, onZeroReached: () -> Unit) {
        if (item.quantity > 1) {
            viewModelScope.launch(Dispatchers.IO) {
                dao.adjustQuantity(item.id, -1)
            }
        } else {
            onZeroReached()
        }
    }

    fun increaseQuantity(item: FoodItem) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.adjustQuantity(item.id, 1)
        }
    }

    fun markFoodStatus(item: FoodItem, newStatus: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.update(item.copy(status = newStatus))
        }
    }

    fun deleteItem(item: FoodItem) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.delete(item)
        }
    }

    fun setAlertTime(hour: Int, minute: Int) {
        prefs.edit().putInt("alert_hour", hour).putInt("alert_minute", minute).apply()
        rescheduleNotification(hour, minute)
    }

    fun getAlertHour(): Int = prefs.getInt("alert_hour", 9)
    fun getAlertMinute(): Int = prefs.getInt("alert_minute", 0)

    // UPDATE 정책은 기존 등록 시각을 유지해서 바꾼 알림 시간이 반영되지 않으므로, 시간 변경 시엔 재등록한다
    private fun rescheduleNotification(
        hour: Int,
        minute: Int,
        policy: ExistingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
    ) {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(now)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        val initialDelay = target.timeInMillis - now.timeInMillis

        val workRequest = PeriodicWorkRequestBuilder<ExpiryCheckWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(getApplication()).enqueueUniquePeriodicWork(
            "DailyExpiryCheckWork",
            policy,
            workRequest
        )
    }

    fun triggerTestNotification(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val active = activeFoods.value
            val urgentCount = active.count { calculateDDay(it.expiryDate) <= 3 }
            val message = if (urgentCount > 0) {
                "냉장고에 소비기한이 임박한 식재료가 ${urgentCount}개 있습니다!"
            } else {
                "현재 냉장고의 모든 식재료가 신선하게 보관 중입니다. (총 ${active.size}개)"
            }
            ExpiryCheckWorker.sendNotification(context, "소비기한 알리미 테스트", message)
        }
    }

    suspend fun exportToCsv(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val items = dao.getAllFoodsOnce()
            context.contentResolver.openOutputStream(uri)?.use { os ->
                OutputStreamWriter(os, Charsets.UTF_8).use { writer ->
                    // BOM: 엑셀에서 열어도 한글이 깨지지 않게
                    writer.write("﻿")
                    writer.write("name,category,storageType,quantity,purchaseDate,expiryDate,status\n")
                    for (item in items) {
                        val row = listOf(
                            item.name, item.category, item.storageType, item.quantity.toString(),
                            item.purchaseDate.toString(), item.expiryDate.toString(), item.status
                        )
                        writer.write(row.joinToString(",") { csvEscape(it) } + "\n")
                    }
                }
            } ?: return@withContext false
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun importFromCsv(context: Context, uri: Uri): Int = withContext(Dispatchers.IO) {
        var count = 0
        try {
            // 같은 백업을 두 번 복원해도 중복 등록되지 않도록 기존 항목과 비교
            val existingKeys = dao.getAllFoodsOnce()
                .map { "${it.name}|${it.purchaseDate}|${it.expiryDate}|${it.status}" }
                .toHashSet()
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                    reader.readLine() // 헤더
                    val items = mutableListOf<FoodItem>()
                    while (true) {
                        val line = reader.readLine() ?: break
                        if (line.isBlank()) continue
                        val tokens = parseCsvLine(line)
                        if (tokens.size >= 7) {
                            val key = "${tokens[0]}|${tokens[4]}|${tokens[5]}|${tokens[6]}"
                            if (!existingKeys.add(key)) continue
                            items.add(
                                FoodItem(
                                    name = tokens[0],
                                    category = tokens[1],
                                    storageType = tokens[2],
                                    quantity = tokens[3].toIntOrNull() ?: 1,
                                    purchaseDate = tokens[4].toLongOrNull() ?: System.currentTimeMillis(),
                                    expiryDate = tokens[5].toLongOrNull() ?: System.currentTimeMillis(),
                                    status = tokens[6]
                                )
                            )
                            count++
                        }
                    }
                    if (items.isNotEmpty()) {
                        dao.insertAll(items)
                    }
                }
            }
        } catch (_: Exception) {
            count = -1
        }
        count
    }

    private fun csvEscape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else value

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                inQuotes && c == '"' && i + 1 < line.length && line[i + 1] == '"' -> { sb.append('"'); i++ }
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> { result.add(sb.toString()); sb.clear() }
                else -> sb.append(c)
            }
            i++
        }
        result.add(sb.toString())
        return result
    }
}

// ==========================================
// 5. 메인 액티비티
// ==========================================
class MainActivity : ComponentActivity() {
    private var openStoredOnStart = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // 화면 회전 등 재생성 때는 같은 인텐트가 다시 들어오므로 최초 실행일 때만 반영
        if (savedInstanceState == null) {
            openStoredOnStart.value = intent.getBooleanExtra("open_stored_dialog", false)
        }

        createNotificationChannel(this)
        MobileAds.initialize(this) {}

        setContent {
            PremiumAdaptiveTheme {
                MainFridgeScreen(
                    initialOpenStored = openStoredOnStart.value,
                    onResetInitialOpen = { openStoredOnStart.value = false }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("open_stored_dialog", false)) {
            openStoredOnStart.value = true
        }
    }
}

// ==========================================
// 6. 반응형 테마
// ==========================================
@Composable
fun PremiumAdaptiveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            surface = Color(0xFF090D16),
            onSurface = Color(0xFFF8FAFC),
            onSurfaceVariant = Color(0xFF94A3B8),
            surfaceContainer = Color(0xFF131B2E),
            surfaceContainerHigh = Color(0xFF1E293B),
            primary = Color(0xFF6366F1),
            outlineVariant = Color(0xFF1E293B)
        )
    } else {
        lightColorScheme(
            surface = Color(0xFFF8FAFC),
            onSurface = Color(0xFF0F172A),
            onSurfaceVariant = Color(0xFF64748B),
            surfaceContainer = Color(0xFFFFFFFF),
            surfaceContainerHigh = Color(0xFFF1F5F9),
            primary = Color(0xFF4F46E5),
            outlineVariant = Color(0xFFE2E8F0)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

// ==========================================
// 7. 메인 화면 레이아웃
// ==========================================
@Composable
fun MainFridgeScreen(
    viewModel: FridgeViewModel = viewModel(),
    initialOpenStored: Boolean = false,
    onResetInitialOpen: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val activeFoods by viewModel.activeFoods.collectAsState()
    val archivedFoods by viewModel.archivedFoods.collectAsState()

    var isOcrLoading by remember { mutableStateOf(false) }

    var showSearchDialog by remember { mutableStateOf(false) }
    var showStoredFoodsDialog by remember { mutableStateOf(false) }
    var showArchivedDialog by remember { mutableStateOf(false) }

    LaunchedEffect(initialOpenStored) {
        if (initialOpenStored) {
            showStoredFoodsDialog = true
            onResetInitialOpen()
        }
    }

    // 알림 권한은 카메라와 별개로 첫 실행 때 요청 (카메라를 안 쓰면 Android 13+에서 알림이 영영 안 오던 문제)
    // 알림이 실제로 켜져 있는지 (휴대폰 설정에서 끄고 돌아올 수도 있으니 화면에 돌아올 때마다 다시 확인)
    var notificationsOn by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    LifecycleResumeEffect(Unit) {
        notificationsOn = NotificationManagerCompat.from(context).areNotificationsEnabled()
        onPauseOrDispose { }
    }
    var alertTimeLabel by remember {
        mutableStateOf(String.format(Locale.KOREA, "%d:%02d", viewModel.getAlertHour(), viewModel.getAlertMinute()))
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { notificationsOn = NotificationManagerCompat.from(context).areNotificationsEnabled() }
    var notificationAsked by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (notificationAsked) return@LaunchedEffect
        notificationAsked = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val scannedItems = remember { mutableStateListOf<ParsedItem>() }
    var detectedPurchaseDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showScanConfirmDialog by remember { mutableStateOf(false) }
    var showManualAddDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showRecipeDialog by remember { mutableStateOf(false) }

    var targetFoodForStatus by remember { mutableStateOf<FoodItem?>(null) }
    var editingFoodItem by remember { mutableStateOf<FoodItem?>(null) }
    var pendingDeleteFood by remember { mutableStateOf<FoodItem?>(null) }

    var tempCameraImageUri by rememberSaveable { mutableStateOf<Uri?>(null) } // 카메라 앱으로 갔다 오며 화면이 재생성돼도 경로 유지

    // fromFilePath는 EXIF 회전을 반영하고 백그라운드에서 디코딩한다 (세로로 찍은 영수증이 옆으로 누워 인식률이 떨어지던 문제)
    fun runOcrOnUri(uri: Uri, onFinished: () -> Unit = {}) {
        isOcrLoading = true
        coroutineScope.launch {
            val image = try {
                withContext(Dispatchers.IO) { InputImage.fromFilePath(context, uri) }
            } catch (e: Exception) {
                isOcrLoading = false
                onFinished()
                Toast.makeText(context, "이미지 로드 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val recognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())

            recognizer.process(image)
                .addOnCompleteListener {
                    recognizer.close()
                    onFinished()
                }
                .addOnSuccessListener { visionText ->
                    isOcrLoading = false
                    val parseResult = ReceiptParser.parse(visionText.text)
                    if (parseResult.items.isEmpty()) {
                        Toast.makeText(context, "식재료를 찾지 못했습니다. 직접 등록을 이용해 주세요.", Toast.LENGTH_SHORT).show()
                    } else {
                        detectedPurchaseDate = parseResult.purchaseDateMillis
                        scannedItems.clear()
                        coroutineScope.launch {
                            val updatedList = parseResult.items.map { item ->
                                val custom = viewModel.resolvePreset(item.name)
                                if (custom != null) {
                                    item.copy(shelfDays = custom.shelfDays, storageType = custom.storageType)
                                } else {
                                    item
                                }
                            }
                            scannedItems.addAll(updatedList)
                            showScanConfirmDialog = true
                        }
                    }
                }
                .addOnFailureListener { e ->
                    isOcrLoading = false
                    Toast.makeText(context, "OCR 실패: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    val highResCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = tempCameraImageUri
        // 촬영한 임시 사진은 인식 후 지워서 캐시에 쌓이지 않게 한다
        val cleanUp = { uri?.let { context.contentResolver.delete(it, null, null) }; tempCameraImageUri = null }
        if (success && uri != null) {
            runOcrOnUri(uri, onFinished = { cleanUp() })
        } else {
            cleanUp()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions[Manifest.permission.CAMERA] == true) {
            try {
                val tempFile = File.createTempFile("receipt_capture_", ".jpg", context.cacheDir)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
                tempCameraImageUri = uri
                highResCameraLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, "카메라 초기화 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    fun checkAndLaunchCamera() {
        // 알림 권한은 시작 시 따로 요청하므로 여기선 카메라만 본다 (알림을 거부하면 촬영할 때마다 권한 창이 뜨던 문제)
        val permissions = listOf(Manifest.permission.CAMERA)
        if (permissions.all { ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {
            try {
                val tempFile = File.createTempFile("receipt_capture_", ".jpg", context.cacheDir)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
                tempCameraImageUri = uri
                highResCameraLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, "카메라 준비 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { runOcrOnUri(it) }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let {
            coroutineScope.launch {
                val ok = viewModel.exportToCsv(context, it)
                Toast.makeText(context, if (ok) "CSV 백업 완료!" else "백업 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            coroutineScope.launch {
                val count = viewModel.importFromCsv(context, it)
                if (count >= 0) {
                    Toast.makeText(context, "${count}개 식재료 복원 성공!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "복원 실패", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val urgentFoods = activeFoods.filter { calculateDDay(it.expiryDate) <= 3 }
    val urgentCount = urgentFoods.size
    // 레시피는 아직 먹을 수 있는 임박 재료만 (기한 지난 재료로 요리를 권하지 않도록)
    val recipeFoods = urgentFoods.filter { calculateDDay(it.expiryDate) >= 0 }
    val freshRate = if (activeFoods.isNotEmpty()) {
        ((activeFoods.size - urgentCount).toFloat() / activeFoods.size.toFloat() * 100).toInt()
    } else 100

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        bottomBar = {
            ZeroShiftAdmobContainer()
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = statusBarPadding + 8.dp,
                    bottom = 20.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 상단 헤더
                item {
                    FridgeHeader(
                        onSearchClick = { showSearchDialog = true },
                        onStoredFoodsClick = { showStoredFoodsDialog = true },
                        onArchivedClick = { showArchivedDialog = true },
                        onRecipeClick = { showRecipeDialog = true },
                        onReportClick = { showReportDialog = true },
                        onSettingsClick = { showSettingsDialog = true },
                        onDirectAddClick = { showManualAddDialog = true }
                    )
                }

                // 신선도 게이지 히어로 카드
                item {
                    FridgeHeroCard(
                        freshnessRate = freshRate,
                        urgentCount = urgentCount,
                        notificationsOn = notificationsOn,
                        alertTimeLabel = alertTimeLabel,
                        onNotificationOffClick = {
                            // 이미 한 번 거부하면 권한 창이 다시 안 뜨므로 앱 알림 설정 화면으로 보낸다
                            val intent = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                            try {
                                context.startActivity(intent)
                            } catch (_: android.content.ActivityNotFoundException) {
                                Toast.makeText(context, "휴대폰 설정 > 앱 > 냉장고 알리미에서 알림을 허용해 주세요.", Toast.LENGTH_LONG).show()
                            }
                        }
                    )
                }

                // 영수증 스캔 퀵 액션
                item {
                    QuickActionSection(
                        onCameraScan = { checkAndLaunchCamera() },
                        onGalleryScan = { galleryLauncher.launch("image/*") },
                        onSampleTest = {
                            val sampleReceipt = """
                                [이마트 역삼점]
                                2026-09-24 18:30:12 POS: 02
                                신선란 30구     6,980원 1개
                                서울우유 1L     2,850원 2개
                                찌개용 두부 300g 1,450원 1개
                                한돈 삼겹살 600g 14,500원 1개
                                비비고 왕교자 만두 9,200원 1개
                                신라면 멀티팩   4,300원 1개
                                사과 1봉        8,900원 1개
                                합계금액: 48,180원
                            """.trimIndent()
                            val parseResult = ReceiptParser.parse(sampleReceipt)
                            detectedPurchaseDate = parseResult.purchaseDateMillis
                            scannedItems.clear()
                            scannedItems.addAll(parseResult.items)
                            showScanConfirmDialog = true
                        }
                    )
                }

                // 요약 통계 카드
                item {
                    FridgeStatGrid(
                        activeCount = activeFoods.size,
                        urgentCount = urgentCount,
                        onActiveClick = { showStoredFoodsDialog = true },
                        onUrgentClick = { showStoredFoodsDialog = true }
                    )
                }

                // 냉장고 파먹기 배너
                if (recipeFoods.isNotEmpty()) {
                    item {
                        RecipeBanner(urgentFoods = recipeFoods, onClick = { showRecipeDialog = true })
                    }
                }

                // 메인 화면 임박 품목 섹션
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🚨 소비 임박 식품 (3일 이내)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "내용물 전체보기 🧊",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { showStoredFoodsDialog = true }
                        )
                    }
                }

                if (urgentFoods.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.padding(22.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "✨ 임박한 식재료가 없습니다!\n냉장고 내부의 모든 품목은 상단 🧊 아이콘을 눌러 확인하세요.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                } else {
                    items(urgentFoods, key = { it.id }) { food ->
                        FoodItemCard(
                            food = food,
                            onDecreaseQty = {
                                viewModel.decreaseQuantity(food, onZeroReached = {
                                    targetFoodForStatus = food
                                })
                            },
                            onIncreaseQty = { viewModel.increaseQuantity(food) },
                            onOpenStatusModal = { targetFoodForStatus = food },
                            onEdit = { editingFoodItem = food },
                            onDelete = { pendingDeleteFood = food }
                        )
                    }
                }
            }

            if (isOcrLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text("영수증 원본 해상도 1초 분석 중...", color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        }
    }

    // 🔍 [넓이 제한 해제] 검색 팝업
    if (showSearchDialog) {
        SearchFoodsDialog(
            allFoods = activeFoods,
            onDismiss = { showSearchDialog = false },
            onDecreaseQty = { food ->
                viewModel.decreaseQuantity(food, onZeroReached = { targetFoodForStatus = food })
            },
            onIncreaseQty = { food -> viewModel.increaseQuantity(food) },
            onOpenStatusModal = { food -> targetFoodForStatus = food },
            onEdit = { food -> editingFoodItem = food },
            onDelete = { food -> pendingDeleteFood = food }
        )
    }

    // 🧊 [넓이 제한 해제 & 상세 정보] 품목별 구분 내용물 팝업
    if (showStoredFoodsDialog) {
        StoredFoodsCategorizedDialog(
            activeFoods = activeFoods,
            onDismiss = { showStoredFoodsDialog = false },
            onDecreaseQty = { food ->
                viewModel.decreaseQuantity(food, onZeroReached = { targetFoodForStatus = food })
            },
            onIncreaseQty = { food -> viewModel.increaseQuantity(food) },
            onOpenStatusModal = { food -> targetFoodForStatus = food },
            onEdit = { food -> editingFoodItem = food },
            onDelete = { food -> pendingDeleteFood = food }
        )
    }

    // 📜 [넓이 제한 해제] 기록 팝업
    if (showArchivedDialog) {
        ArchivedRecordsDialog(
            archivedFoods = archivedFoods,
            onDismiss = { showArchivedDialog = false },
            onDelete = { food -> pendingDeleteFood = food }
        )
    }

    // [넓이 제한 해제] 스캔 확인 팝업
    if (showScanConfirmDialog) {
        ScanConfirmDialog(
            items = scannedItems,
            purchaseDateMillis = detectedPurchaseDate,
            onDismiss = { showScanConfirmDialog = false },
            onConfirmAdd = { selectedItems, baseDate ->
                val foodEntities = selectedItems.map { item ->
                    val expiryMillis = calculateExpiryTimestamp(baseDate, item.shelfDays)
                    FoodItem(
                        name = item.name,
                        category = item.category,
                        storageType = item.storageType,
                        quantity = item.quantity,
                        purchaseDate = baseDate,
                        expiryDate = expiryMillis,
                        status = "ACTIVE"
                    )
                }
                viewModel.addFoodItems(foodEntities)
                showScanConfirmDialog = false
                Toast.makeText(context, "${foodEntities.size}개 품목이 냉장고에 등록되었습니다!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 수동 등록 팝업
    if (showManualAddDialog) {
        ManualAddDialog(
            resolvePreset = { viewModel.resolvePreset(it) },
            onDismiss = { showManualAddDialog = false },
            onAdd = { name, category, storage, qty, days ->
                val now = System.currentTimeMillis()
                val entity = FoodItem(
                    name = name,
                    category = category,
                    storageType = storage,
                    quantity = qty,
                    purchaseDate = now,
                    expiryDate = calculateExpiryTimestamp(now, days),
                    status = "ACTIVE"
                )
                viewModel.addFoodItems(listOf(entity))
                showManualAddDialog = false
            }
        )
    }

    // 수정 팝업
    editingFoodItem?.let { target ->
        EditFoodItemDialog(
            item = target,
            onDismiss = { editingFoodItem = null },
            onSave = { updated ->
                viewModel.updateFoodItem(updated)
                editingFoodItem = null
            }
        )
    }

    // 소비 vs 폐기 선택 팝업
    targetFoodForStatus?.let { target ->
        ConsumeOrWasteDialog(
            item = target,
            onDismiss = { targetFoodForStatus = null },
            onSelect = { status ->
                viewModel.markFoodStatus(target, status)
                targetFoodForStatus = null
            }
        )
    }

    // 삭제 확인 팝업 (검색·내용물·기록 팝업보다 뒤에 그려야 그 위에 뜬다)
    pendingDeleteFood?.let { target ->
        DeleteConfirmDialog(
            item = target,
            onDismiss = { pendingDeleteFood = null },
            onConfirm = {
                viewModel.deleteItem(target)
                pendingDeleteFood = null
            }
        )
    }

    // 레시피 추천 팝업
    if (showRecipeDialog) {
        RecipeRecommendationDialog(
            urgentFoods = recipeFoods,
            onDismiss = { showRecipeDialog = false },
            onSearch = { query ->
                val encoded = URLEncoder.encode(query, "UTF-8")
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://m.10000recipe.com/recipe/list.html?q=$encoded"))
                try {
                    context.startActivity(intent)
                } catch (_: android.content.ActivityNotFoundException) {
                    Toast.makeText(context, "웹 브라우저를 열 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
                showRecipeDialog = false
            }
        )
    }

    // 절약 리포트 팝업
    if (showReportDialog) {
        SavingsReportDialog(archivedFoods = archivedFoods, onDismiss = { showReportDialog = false })
    }

    // 설정 팝업
    if (showSettingsDialog) {
        SettingsDialog(
            currentHour = viewModel.getAlertHour(),
            currentMinute = viewModel.getAlertMinute(),
            onDismiss = { showSettingsDialog = false },
            onSaveAlertTime = { h, m ->
                viewModel.setAlertTime(h, m)
                alertTimeLabel = String.format(Locale.KOREA, "%d:%02d", h, m)
            },
            onTestNotification = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) {
                    Toast.makeText(context, "알림 권한이 꺼져 있습니다. 휴대폰 설정 > 앱 > 냉장고 알리미에서 허용해 주세요.", Toast.LENGTH_LONG).show()
                } else {
                    viewModel.triggerTestNotification(context)
                }
            },
            onExportCsv = { exportCsvLauncher.launch("fridge_backup_${SimpleDateFormat("yyyyMMdd", Locale.KOREA).format(Date())}.csv") },
            onImportCsv = { importCsvLauncher.launch(arrayOf("text/*", "application/vnd.ms-excel", "*/*")) }
        )
    }
}

// ==========================================
// 8. 상단 헤더
// ==========================================
@Composable
fun FridgeHeader(
    onSearchClick: () -> Unit,
    onStoredFoodsClick: () -> Unit,
    onArchivedClick: () -> Unit,
    onRecipeClick: () -> Unit,
    onReportClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onDirectAddClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "스마트 소비기한 매니저 🧊",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "내 냉장고 관리",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = (-0.5).sp
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            HeaderIconButton(icon = Icons.Default.Search, onClick = onSearchClick, contentDescription = "검색 창")
            HeaderIconButton(icon = Icons.Default.Kitchen, onClick = onStoredFoodsClick, contentDescription = "내용물 창")
            HeaderIconButton(icon = Icons.Default.History, onClick = onArchivedClick, contentDescription = "기록")
            HeaderIconButton(icon = Icons.Default.Restaurant, onClick = onRecipeClick, contentDescription = "레시피")
            HeaderIconButton(icon = Icons.Default.Share, onClick = onReportClick, contentDescription = "절약 리포트")
            HeaderIconButton(icon = Icons.Default.Settings, onClick = onSettingsClick, contentDescription = "설정")

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { onDirectAddClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "직접 추가",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun HeaderIconButton(icon: ImageVector, onClick: () -> Unit, contentDescription: String) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
fun FridgeHeroCard(
    freshnessRate: Int,
    urgentCount: Int,
    notificationsOn: Boolean,
    alertTimeLabel: String,
    onNotificationOffClick: () -> Unit
) {
    var progressTarget by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(durationMillis = 1000),
        label = "HeroProgress"
    )

    LaunchedEffect(freshnessRate) {
        progressTarget = (freshnessRate / 100f).coerceIn(0f, 1f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF4F46E5), Color(0xFF7C3AED), Color(0xFF2563EB))
                )
            )
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color.White.copy(alpha = 0.18f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = if (urgentCount > 0) "⚠️ 소비 임박 ${urgentCount}건" else "✨ 모든 식재료 안전",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                if (notificationsOn) {
                    Text(
                        text = "🔔 매일 $alertTimeLabel 알림",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(Color(0xFFEF4444).copy(alpha = 0.85f))
                            .clickable { onNotificationOffClick() }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "🔕 알림 꺼짐 · 켜기",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }

            Column {
                Text(
                    text = "냉장고 신선도 안전율",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${freshnessRate}",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = " %",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 5.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.25f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White)
                )
            }
        }
    }
}

@Composable
fun QuickActionSection(
    onCameraScan: () -> Unit,
    onGalleryScan: () -> Unit,
    onSampleTest: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = onCameraScan,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("영수증 1초 스캔 (고화질)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clickable { onGalleryScan() },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("갤러리 사진", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clickable { onSampleTest() },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("샘플 테스트", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
fun FridgeStatGrid(
    activeCount: Int,
    urgentCount: Int,
    onActiveClick: () -> Unit,
    onUrgentClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        InteractiveCard(
            modifier = Modifier.weight(1f),
            onClick = onActiveClick,
            icon = Icons.Default.Kitchen,
            iconTint = Color(0xFF10B981),
            title = "보관 중인 품목",
            value = "${activeCount}개"
        )
        InteractiveCard(
            modifier = Modifier.weight(1f),
            onClick = onUrgentClick,
            icon = Icons.Default.Warning,
            iconTint = if (urgentCount > 0) Color(0xFFEF4444) else Color(0xFFF59E0B),
            title = "임박 (3일 내)",
            value = "${urgentCount}개"
        )
    }
}

@Composable
fun RecipeBanner(urgentFoods: List<FoodItem>, onClick: () -> Unit) {
    val sampleIngredients = urgentFoods.take(2).joinToString(", ") { it.name }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF59E0B).copy(alpha = 0.12f),
        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🍳", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "오늘 뭐 해먹지? (냉장고 파먹기)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSystemInDarkTheme()) Color(0xFFFBBF24) else Color(0xFFB45309)
                    )
                    Text(
                        text = "[$sampleIngredients] 조합 레시피 보러가기",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = "조회 →",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSystemInDarkTheme()) Color(0xFFFBBF24) else Color(0xFFB45309)
            )
        }
    }
}

// ==========================================
// [업그레이드] 식재료 정보 상세 카드 (구매일/경과일수/소비기한/보관위치 체계적 표시)
// ==========================================
@Composable
fun FoodItemCard(
    food: FoodItem,
    onDecreaseQty: () -> Unit,
    onIncreaseQty: () -> Unit,
    onOpenStatusModal: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isArchived = food.status != "ACTIVE"
    val dDay = calculateDDay(food.expiryDate)
    val elapsedDays = calculateElapsedDays(food.purchaseDate)

    val badgeColor = when {
        food.status == "CONSUMED" -> Color(0xFF10B981)
        food.status == "WASTED" -> Color(0xFFEF4444)
        dDay < 0 -> Color(0xFFEF4444)
        dDay <= 3 -> Color(0xFFF59E0B)
        else -> Color(0xFF10B981)
    }

    val badgeText = when {
        food.status == "CONSUMED" -> "소비완료 😋"
        food.status == "WASTED" -> "폐기 🗑️"
        dDay < 0 -> "D+${-dDay} 만료"
        dDay == 0L -> "D-Day 오늘"
        else -> "D-${dDay} (${dDay}일 남음)"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // 상단 행: 이름, 보관위치/카테고리 뱃지, D-Day 상태
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (isArchived) badgeColor.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                            .clickable { onOpenStatusModal() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "상태 변경",
                            tint = if (isArchived) badgeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = food.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isArchived) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                textDecoration = if (isArchived) TextDecoration.LineThrough else TextDecoration.None
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = food.storageType,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = food.category,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Text(text = badgeText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = badgeColor)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // [상세 정보 패널] 구매일자, 경과일수, 소비기한, 상태
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "📅 등록/구매일", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${formatDate(food.purchaseDate)} (${elapsedDays}일째 보관)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "⏳ 소비기한 만료", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = formatDate(food.expiryDate),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 하단: 수량 조절 버튼 & 수정 / 삭제 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(onClick = onDecreaseQty, modifier = Modifier.size(40.dp)) {
                        Text("-", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Text(
                        text = "${food.quantity}개",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                    IconButton(onClick = onIncreaseQty, modifier = Modifier.size(40.dp)) {
                        Text("+", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "수정", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "삭제", tint = Color(0xFFEF4444).copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun InteractiveCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    icon: ImageVector,
    iconTint: Color,
    title: String,
    value: String
) {
    // 클릭과 같은 interactionSource를 써야 눌림 축소 효과가 실제로 동작한다 (전에는 연결이 안 돼 항상 1.0)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        label = "CardPressScale"
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier.scale(scale),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ==========================================
// 9. [CLS 제로 & 높이 완전 고정] 애드몹 독(Dock)
// ==========================================
@Composable
fun ZeroShiftAdmobContainer() {
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val totalDockHeight = 50.dp + navBarPadding

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(totalDockHeight),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = navBarPadding),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(width = 320.dp, height = 50.dp)
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    modifier = Modifier.size(width = 320.dp, height = 50.dp),
                    factory = { context ->
                        AdView(context).apply {
                            setAdSize(AdSize.BANNER)
                            adUnitId = "ca-app-pub-3940256099942544/6300978111"
                            loadAd(AdRequest.Builder().build())
                        }
                    },
                    onRelease = { it.destroy() }
                )
            }
        }
    }
}

// ==========================================
// 10. [넓이 제한 해제] 🔍 검색 & 🧊 품목별 내용물 & 📜 기록 팝업
// ==========================================

// 🔍 1. 검색 팝업 (가로 넓이 제한 해제)
@Composable
fun SearchFoodsDialog(
    allFoods: List<FoodItem>,
    onDismiss: () -> Unit,
    onDecreaseQty: (FoodItem) -> Unit,
    onIncreaseQty: (FoodItem) -> Unit,
    onOpenStatusModal: (FoodItem) -> Unit,
    onEdit: (FoodItem) -> Unit,
    onDelete: (FoodItem) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(allFoods, searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else allFoods.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 20.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🔍 식재료 검색", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "닫기", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("식재료 이름 입력 (예: 우유, 두부, 삼겹살)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                if (searchQuery.isBlank()) {
                    Box(modifier = Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
                        Text("검색어를 입력하면 보관 중인 품목을 바로 찾습니다.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else if (filtered.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
                        Text("'$searchQuery'에 해당하는 식재료가 없습니다.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.8f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filtered, key = { it.id }) { food ->
                            FoodItemCard(
                                food = food,
                                onDecreaseQty = { onDecreaseQty(food) },
                                onIncreaseQty = { onIncreaseQty(food) },
                                onOpenStatusModal = { onOpenStatusModal(food) },
                                onEdit = { onEdit(food) },
                                onDelete = { onDelete(food) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                Text("닫기")
            }
        }
    )
}

// 🧊 2. 냉장고 내용물 팝업 (가로 넓이 제한 해제 & 품목별 대화면 노출)
@Composable
fun StoredFoodsCategorizedDialog(
    activeFoods: List<FoodItem>,
    onDismiss: () -> Unit,
    onDecreaseQty: (FoodItem) -> Unit,
    onIncreaseQty: (FoodItem) -> Unit,
    onOpenStatusModal: (FoodItem) -> Unit,
    onEdit: (FoodItem) -> Unit,
    onDelete: (FoodItem) -> Unit
) {
    var storageFilter by remember { mutableStateOf("전체") }

    val locationFiltered = remember(activeFoods, storageFilter) {
        if (storageFilter == "전체") activeFoods
        else activeFoods.filter { it.storageType.contains(storageFilter) }
    }

    val groupedByCategory = remember(locationFiltered) {
        locationFiltered.groupBy { it.category }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 18.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("🧊 냉장고 속 식재료", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("품목별 분류 보관함 (총 ${locationFiltered.size}개)", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "닫기", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val tabs = listOf("전체", "냉장 🧊", "냉동 ❄️", "실온 🧺")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    tabs.forEach { tab ->
                        val isSelected = storageFilter == tab || (storageFilter == "전체" && tab == "전체") || tab.contains(storageFilter)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable {
                                    storageFilter = when {
                                        tab.contains("냉장") -> "냉장"
                                        tab.contains("냉동") -> "냉동"
                                        tab.contains("실온") -> "실온"
                                        else -> "전체"
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (locationFiltered.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
                        Text("보관 중인 식재료가 없습니다.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.85f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        groupedByCategory.forEach { (category, itemsInCategory) ->
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = when (category) {
                                            "유제품" -> "🥛 유제품"
                                            "육류" -> "🥩 육류"
                                            "신선식품" -> "🥚 신선식품"
                                            "채소" -> "🥬 채소"
                                            "과일" -> "🍎 과일"
                                            "수산물" -> "🐟 수산물"
                                            "냉동식품" -> "🥟 냉동가공"
                                            "가공식품" -> "🍱 가공식품"
                                            "베이커리" -> "🍞 베이커리"
                                            else -> "📦 $category"
                                        },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "${itemsInCategory.size}개",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            items(itemsInCategory, key = { it.id }) { food ->
                                FoodItemCard(
                                    food = food,
                                    onDecreaseQty = { onDecreaseQty(food) },
                                    onIncreaseQty = { onIncreaseQty(food) },
                                    onOpenStatusModal = { onOpenStatusModal(food) },
                                    onEdit = { onEdit(food) },
                                    onDelete = { onDelete(food) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                Text("닫기")
            }
        }
    )
}

// 📜 3. 기록 팝업 (가로 넓이 제한 해제)
@Composable
fun ArchivedRecordsDialog(
    archivedFoods: List<FoodItem>,
    onDismiss: () -> Unit,
    onDelete: (FoodItem) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 20.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("📜 기록", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("소비 및 폐기된 식품 내역 (총 ${archivedFoods.size}건)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "닫기", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            if (archivedFoods.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                    Text("완료된 기록이 없습니다.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.75f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(archivedFoods, key = { it.id }) { food ->
                        val isConsumed = food.status == "CONSUMED"
                        val tagColor = if (isConsumed) Color(0xFF10B981) else Color(0xFFEF4444)
                        val tagText = if (isConsumed) "맛있게 소비 😋" else "폐기 🗑️"

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = food.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${food.category} • ${food.quantity}개 • 소비기한: ${formatDate(food.expiryDate)}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(tagColor.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(text = tagText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = tagColor)
                                    }

                                    IconButton(onClick = { onDelete(food) }, modifier = Modifier.size(26.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = "삭제", tint = Color(0xFFEF4444).copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                Text("닫기")
            }
        }
    )
}

// ==========================================
// 11. 기타 다이얼로그 모달 모음 (넓이 최적화 적용)
// ==========================================
@Composable
fun ScanConfirmDialog(
    items: List<ParsedItem>,
    purchaseDateMillis: Long,
    onDismiss: () -> Unit,
    onConfirmAdd: (List<ParsedItem>, Long) -> Unit
) {
    val currentItems = remember { mutableStateListOf(*items.toTypedArray()) }
    var itemToEdit by remember { mutableStateOf<ParsedItem?>(null) }

    val selectedCount = currentItems.count { it.isChecked }
    val isAllSelected = currentItems.isNotEmpty() && currentItems.all { it.isChecked }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 20.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = {
            Column {
                Text("영수증 품목 분석 완료", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "결제일: ${formatDate(purchaseDateMillis)} (항목 터치 시 수정 가능)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable {
                            val targetState = !isAllSelected
                            currentItems.indices.forEach { idx ->
                                currentItems[idx] = currentItems[idx].copy(isChecked = targetState)
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isAllSelected,
                            onCheckedChange = { checked ->
                                currentItems.indices.forEach { idx ->
                                    currentItems[idx] = currentItems[idx].copy(isChecked = checked)
                                }
                            },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isAllSelected) "전체 해제" else "전체 선택",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "${selectedCount}/${currentItems.size}개 선택됨",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.7f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(currentItems) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clickable { itemToEdit = item }
                                .padding(vertical = 4.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Checkbox(
                                    checked = item.isChecked,
                                    onCheckedChange = { checked ->
                                        val index = currentItems.indexOf(item)
                                        if (index != -1) {
                                            currentItems[index] = item.copy(isChecked = checked)
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "${item.name} (${item.quantity}개)",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${item.storageType} • ${item.category} • ${item.shelfDays}일 보관",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            IconButton(
                                onClick = { currentItems.remove(item) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "삭제", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalSelected = currentItems.filter { it.isChecked }
                    if (finalSelected.isNotEmpty()) {
                        onConfirmAdd(finalSelected, purchaseDateMillis)
                    }
                },
                enabled = selectedCount > 0,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(if (selectedCount > 0) "선택한 ${selectedCount}개 넣기" else "식재료를 선택하세요", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
    )

    itemToEdit?.let { target ->
        var editName by remember { mutableStateOf(target.name) }
        var editStorage by remember { mutableStateOf(target.storageType) }
        var editDays by remember { mutableStateOf(target.shelfDays.toString()) }
        var editQty by remember { mutableStateOf(target.quantity.toString()) }

        AlertDialog(
            onDismissRequest = { itemToEdit = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            title = { Text("품목 세부 설정", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("품목명") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    OutlinedTextField(
                        value = editQty,
                        onValueChange = { editQty = it },
                        label = { Text("수량") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    OutlinedTextField(
                        value = editDays,
                        onValueChange = { editDays = it },
                        label = { Text("보관 일수") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("냉장", "냉동", "실온").forEach { loc ->
                            FilterChip(
                                selected = editStorage == loc,
                                onClick = {
                                    editStorage = loc
                                    if (loc == "냉동" && target.storageType != "냉동") {
                                        editDays = ((editDays.toIntOrNull() ?: 7) + 60).toString()
                                    }
                                },
                                label = { Text(loc) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val index = currentItems.indexOf(target)
                    if (index != -1) {
                        currentItems[index] = target.copy(
                            name = editName,
                            quantity = (editQty.toIntOrNull() ?: 1).coerceAtLeast(1),
                            shelfDays = editDays.toIntOrNull() ?: target.shelfDays,
                            storageType = editStorage
                        )
                    }
                    itemToEdit = null
                }) { Text("적용") }
            }
        )
    }
}

val FOOD_CATEGORIES = listOf(
    "유제품", "신선식품", "육류", "수산물", "채소", "과일",
    "냉동식품", "가공식품", "베이커리", "반찬", "일반"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryChips(selected: String, onSelect: (String) -> Unit) {
    // 예전 데이터에 목록 밖 분류가 있어도 선택된 채로 보이게 포함
    val options = if (selected in FOOD_CATEGORIES) FOOD_CATEGORIES else FOOD_CATEGORIES + selected
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("분류", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { cat ->
                FilterChip(selected = selected == cat, onClick = { onSelect(cat) }, label = { Text(cat, fontSize = 12.sp) })
            }
        }
    }
}

@Composable
fun ManualAddDialog(
    resolvePreset: suspend (String) -> UserPreset?,
    onDismiss: () -> Unit,
    onAdd: (name: String, category: String, storage: String, qty: Int, days: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var storage by remember { mutableStateOf("냉장") }
    var category by remember { mutableStateOf("일반") }
    var qtyText by remember { mutableStateOf("1") }
    var shelfDaysText by remember { mutableStateOf("7") }
    // 사용자가 직접 고친 칸은 자동 채움이 덮어쓰지 않는다
    var storageTouched by remember { mutableStateOf(false) }
    var categoryTouched by remember { mutableStateOf(false) }
    var daysTouched by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(name) {
        val trimmed = name.trim()
        if (trimmed.length < 1) {
            hint = null
            return@LaunchedEffect
        }
        delay(300) // 입력 중엔 기다렸다가 멈추면 조회
        val default = ShelfLifeMapper.getDefaultPreset(trimmed)
        val learned = resolvePreset(trimmed)
        if (!categoryTouched) category = default.category
        if (!storageTouched) storage = learned?.storageType ?: default.storageType
        if (!daysTouched) shelfDaysText = (learned?.shelfDays ?: default.days).toString()
        hint = if (learned != null) "지난번 등록한 값으로 채웠어요" else "품명으로 추천값을 채웠어요"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = { Text("식재료 직접 등록", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("식재료 이름 (예: 사과)") },
                    supportingText = hint?.let { { Text(it, fontSize = 11.sp) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { qtyText = it.filter(Char::isDigit).take(3) },
                        label = { Text("수량") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    OutlinedTextField(
                        value = shelfDaysText,
                        onValueChange = { shelfDaysText = it.filter(Char::isDigit).take(4); daysTouched = true },
                        label = { Text("소비기한 일수") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("냉장", "냉동", "실온").forEach { loc ->
                        FilterChip(selected = storage == loc, onClick = { storage = loc; storageTouched = true }, label = { Text(loc) })
                    }
                }
                CategoryChips(selected = category, onSelect = { category = it; categoryTouched = true })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val days = shelfDaysText.toIntOrNull() ?: 7
                    val qty = (qtyText.toIntOrNull() ?: 1).coerceAtLeast(1)
                    onAdd(name.trim(), category, storage, qty, days)
                },
                enabled = name.isNotBlank()
            ) { Text("추가", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}

@Composable
fun EditFoodItemDialog(item: FoodItem, onDismiss: () -> Unit, onSave: (FoodItem) -> Unit) {
    var name by remember { mutableStateOf(item.name) }
    var storage by remember { mutableStateOf(item.storageType) }
    var category by remember { mutableStateOf(item.category) }
    var qtyText by remember { mutableStateOf(item.quantity.toString()) }
    var remainingDays by remember { mutableStateOf(calculateDDay(item.expiryDate).coerceAtLeast(0).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = { Text("식재료 정보 수정", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("품목명") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { qtyText = it.filter(Char::isDigit).take(3) },
                        label = { Text("수량") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    OutlinedTextField(
                        value = remainingDays,
                        onValueChange = { remainingDays = it.filter(Char::isDigit).take(4) },
                        label = { Text("오늘 기준 남은 일수") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("냉장", "냉동", "실온").forEach { loc ->
                        FilterChip(selected = storage == loc, onClick = { storage = loc }, label = { Text(loc) })
                    }
                }
                CategoryChips(selected = category, onSelect = { category = it })
            }
        },
        confirmButton = {
            Button(onClick = {
                val days = remainingDays.toIntOrNull() ?: 0
                val newExpiry = calculateExpiryTimestamp(System.currentTimeMillis(), days)
                onSave(
                    item.copy(
                        name = name.trim().ifBlank { item.name },
                        category = category,
                        storageType = storage,
                        quantity = (qtyText.toIntOrNull() ?: 1).coerceAtLeast(1),
                        expiryDate = newExpiry
                    )
                )
            }) { Text("저장") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}

@Composable
fun ConsumeOrWasteDialog(item: FoodItem, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = { Text("[${item.name}] 정리", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = { Text("이 식재료를 어떻게 처리하셨나요? 기록과 절약 리포트에 반영됩니다.", fontSize = 14.sp) },
        confirmButton = {
            Button(onClick = { onSelect("CONSUMED") }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))) {
                Text("맛있게 먹었어요 😋", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Button(onClick = { onSelect("WASTED") }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))) {
                Text("버렸어요 🗑️", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun DeleteConfirmDialog(item: FoodItem, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val isArchived = item.status != "ACTIVE"
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = { Text("[${item.name}] 삭제", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Text(
                if (isArchived) "이 기록을 삭제할까요? 절약 리포트에서도 빠지며 되돌릴 수 없습니다."
                else "냉장고에서 삭제할까요? 되돌릴 수 없습니다.\n다 먹었거나 버렸다면 체크 아이콘으로 정리하면 기록과 절약 리포트에 남습니다.",
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))) {
                Text("삭제", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}

@Composable
fun RecipeRecommendationDialog(urgentFoods: List<FoodItem>, onDismiss: () -> Unit, onSearch: (String) -> Unit) {
    val selectedIngredients = remember { mutableStateListOf<String>().apply { addAll(urgentFoods.map { it.name }) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = { Text("🍳 냉장고 파먹기 요리 추천", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("임박한 식재료를 조합해 인기 레시피를 찾아보세요:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                // 상단 레시피 아이콘은 임박 재료가 없어도 열 수 있다
                if (urgentFoods.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "3일 안에 기한이 끝나는 재료가 없어요.\n(이미 기한이 지난 재료는 추천에서 빠집니다)",
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else LazyColumn(modifier = Modifier.height(220.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(urgentFoods) { food ->
                        val isChecked = selectedIngredients.contains(food.name)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clickable {
                                    if (isChecked) selectedIngredients.remove(food.name)
                                    else selectedIngredients.add(food.name)
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isChecked) Icons.Default.CheckCircle else Icons.Default.Close,
                                contentDescription = null,
                                tint = if (isChecked) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = food.name, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val query = selectedIngredients.joinToString(" ") + " 요리 레시피"
                    onSearch(query)
                },
                // 재료를 모두 해제하면 " 요리 레시피"라는 빈 검색이 나가던 문제
                enabled = selectedIngredients.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("만개의레시피 검색", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("닫기") } }
    )
}

@Composable
fun SavingsReportDialog(archivedFoods: List<FoodItem>, onDismiss: () -> Unit) {
    val consumedCount = archivedFoods.count { it.status == "CONSUMED" }
    val wastedCount = archivedFoods.count { it.status == "WASTED" }
    val total = consumedCount + wastedCount
    val rate = if (total > 0) (consumedCount.toFloat() / total * 100).toInt() else 100
    val savedWonEstimate = consumedCount * 4500

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = { Text("📊 냉장고 절약 리포트", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("식재료 낭비를 줄이고 알뜰하게 소비한 통계입니다.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("맛있게 소비한 품목 😋", fontSize = 13.sp, color = Color(0xFF10B981))
                            Text("${consumedCount}개", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("기한 지나 폐기된 품목 🗑️", fontSize = 13.sp, color = Color(0xFFEF4444))
                            Text("${wastedCount}개", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("식재료 소비 성공률", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("${rate}%", fontSize = 15.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF10B981).copy(alpha = 0.15f)).padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎉 약 ${String.format(Locale.KOREA, "%,d", savedWonEstimate)}원 절약 성공!", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("확인") } }
    )
}

@Composable
fun SettingsDialog(
    currentHour: Int,
    currentMinute: Int,
    onDismiss: () -> Unit,
    onSaveAlertTime: (Int, Int) -> Unit,
    onTestNotification: () -> Unit,
    onExportCsv: () -> Unit,
    onImportCsv: () -> Unit
) {
    // 숫자로 바로 묶으면 칸을 비우거나 "9"→"15"로 고쳐 쓸 수 없어서 글자 상태로 받는다
    var hourText by remember { mutableStateOf(currentHour.toString()) }
    var minuteText by remember { mutableStateOf(currentMinute.toString()) }
    val hour = hourText.toIntOrNull()?.coerceIn(0, 23) ?: currentHour
    val minute = minuteText.toIntOrNull()?.coerceIn(0, 59) ?: currentMinute

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = { Text("환경설정 & 데이터 관리", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("매일 푸시 알림 수신 시간 설정", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = hourText,
                        onValueChange = { hourText = it.filter(Char::isDigit).take(2) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text("시 (0~23)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    OutlinedTextField(
                        value = minuteText,
                        onValueChange = { minuteText = it.filter(Char::isDigit).take(2) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text("분 (0~59)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                Button(
                    onClick = onTestNotification,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Text("🔔 지금 알림 테스트 전송", color = MaterialTheme.colorScheme.onSurface)
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("데이터 백업 및 복원 (CSV)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onExportCsv,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("CSV 백업")
                    }
                    Button(
                        onClick = onImportCsv,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Text("CSV 복원", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSaveAlertTime(hour, minute)
                onDismiss()
            }) { Text("저장 완료") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("닫기") } }
    )
}

// ==========================================
// 12. 날짜 및 시간 계산 유틸리티
// ==========================================
fun calculateExpiryTimestamp(fromTime: Long, days: Int): Long {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = fromTime
        add(Calendar.DAY_OF_YEAR, days)
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }
    return calendar.timeInMillis
}

fun calculateDDay(expiryMillis: Long): Long {
    val nowCal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val targetCal = Calendar.getInstance().apply {
        timeInMillis = expiryMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val diff = targetCal.timeInMillis - nowCal.timeInMillis
    return diff / (24 * 60 * 60 * 1000L)
}

// 구매 후 오늘까지 보관 경과일수 계산
fun calculateElapsedDays(purchaseMillis: Long): Long {
    val nowCal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val purchaseCal = Calendar.getInstance().apply {
        timeInMillis = purchaseMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val diff = nowCal.timeInMillis - purchaseCal.timeInMillis
    return (diff / (24 * 60 * 60 * 1000L)).coerceAtLeast(0)
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA)
    return sdf.format(Date(timestamp))
}