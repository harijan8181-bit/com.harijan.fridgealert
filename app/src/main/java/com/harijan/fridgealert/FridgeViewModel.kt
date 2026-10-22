package com.harijan.fridgealert

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.workDataOf
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.Calendar
import java.util.concurrent.TimeUnit

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

    val shoppingItems = dao.getShoppingItems().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // DB가 바뀌면 홈 화면 위젯도 갱신
    private fun changed() = FridgeWidget.refresh(getApplication())

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
            changed()
        }
    }

    fun updateFoodItem(item: FoodItem) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.update(item)
            // 개봉으로 줄어든 기한은 "원래 보관 일수"가 아니므로 학습하지 않는다
            if (item.openedDate == null) {
                dao.savePreset(UserPreset(item.name, shelfDaysOf(item), item.storageType))
            }
            changed()
        }
    }

    fun decreaseQuantity(item: FoodItem, onZeroReached: () -> Unit) {
        if (item.quantity > 1) {
            viewModelScope.launch(Dispatchers.IO) {
                dao.adjustQuantity(item.id, -1)
                changed()
            }
        } else {
            onZeroReached()
        }
    }

    fun increaseQuantity(item: FoodItem) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.adjustQuantity(item.id, 1)
            changed()
        }
    }

    fun markFoodStatus(item: FoodItem, newStatus: String, addToShopping: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.setStatus(item.id, newStatus, System.currentTimeMillis())
            if (addToShopping) addShoppingInternal(item.name)
            changed()
        }
    }

    // 개봉: 남은 기한과 "개봉 후 N일" 중 더 짧은 쪽으로 줄인다
    fun openFood(item: FoodItem, daysAfterOpen: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val openedExpiry = calculateExpiryTimestamp(now, daysAfterOpen)
            dao.update(item.copy(openedDate = now, expiryDate = minOf(item.expiryDate, openedExpiry)))
            changed()
        }
    }

    fun deleteItem(item: FoodItem) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.delete(item)
            changed()
        }
    }

    // ---- 장보기 목록 ----
    private suspend fun addShoppingInternal(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || dao.countOpenShopping(trimmed) > 0) return
        dao.insertShopping(ShoppingItem(name = trimmed))
    }

    fun addShopping(name: String) {
        viewModelScope.launch(Dispatchers.IO) { addShoppingInternal(name) }
    }

    fun toggleShopping(item: ShoppingItem) {
        viewModelScope.launch(Dispatchers.IO) { dao.updateShopping(item.copy(checked = !item.checked)) }
    }

    fun deleteShopping(item: ShoppingItem) {
        viewModelScope.launch(Dispatchers.IO) { dao.deleteShopping(item) }
    }

    fun clearCheckedShopping() {
        viewModelScope.launch(Dispatchers.IO) { dao.clearCheckedShopping() }
    }

    // ---- 바코드 ----
    // 한 번 등록한 바코드는 기기에 기억해 두고, 처음 보는 바코드만 공공 API로 조회
    suspend fun lookupBarcode(barcode: String): BarcodeProduct? = withContext(Dispatchers.IO) {
        dao.getBarcodeProduct(barcode) ?: try {
            BarcodeLookup.fetch(barcode)?.let {
                BarcodeProduct(barcode, it.name, it.category, it.storageType, it.shelfDays)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun rememberBarcode(product: BarcodeProduct) {
        viewModelScope.launch(Dispatchers.IO) { dao.saveBarcodeProduct(product) }
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

    // 테스트 버튼도 실제 알림 작업을 그대로 한 번 돌린다 (품목별 알림·버튼까지 확인 가능)
    fun triggerTestNotification(context: Context) {
        val request = OneTimeWorkRequestBuilder<ExpiryCheckWorker>()
            .setInputData(workDataOf(ExpiryCheckWorker.KEY_TEST to true))
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }

    suspend fun exportToCsv(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val items = dao.getAllFoodsOnce()
            context.contentResolver.openOutputStream(uri)?.use { os ->
                OutputStreamWriter(os, Charsets.UTF_8).use { writer ->
                    // BOM: 엑셀에서 열어도 한글이 깨지지 않게
                    writer.write("﻿")
                    writer.write(CsvCodec.HEADER + "\n")
                    for (item in items) {
                        writer.write(CsvCodec.encode(item) + "\n")
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
            val existingKeys = dao.getAllFoodsOnce().map { CsvCodec.dedupeKey(it) }.toHashSet()
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                    reader.readLine() // 헤더
                    val items = mutableListOf<FoodItem>()
                    while (true) {
                        val line = reader.readLine() ?: break
                        if (line.isBlank()) continue
                        val item = CsvCodec.decode(line.removePrefix("﻿")) ?: continue
                        if (!existingKeys.add(CsvCodec.dedupeKey(item))) continue
                        items.add(item)
                        count++
                    }
                    if (items.isNotEmpty()) {
                        dao.insertAll(items)
                        changed()
                    }
                }
            }
        } catch (_: Exception) {
            count = -1
        }
        count
    }
}
