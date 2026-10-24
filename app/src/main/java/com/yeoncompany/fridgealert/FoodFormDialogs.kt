package com.yeoncompany.fridgealert

import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.ModalBottomSheet
import java.util.TimeZone
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePicker
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import android.widget.Toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

// 품목 1개당 평균 금액 (가격을 모르는 품목의 리포트 추정용)
const val ESTIMATED_PRICE_PER_ITEM = 4500

fun formatWon(won: Int): String = String.format(Locale.KOREA, "%,d원", won)

@Composable
private fun formFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
)

@Composable
private fun NumberField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, maxLength: Int = 4) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit).take(maxLength)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
        colors = formFieldColors()
    )
}

// 달력(DatePicker)은 UTC 자정 기준 값을 주고받으므로 기기 시간대의 날짜와 맞춰 변환한다
fun localDateToPickerMillis(localMillis: Long): Long {
    val local = Calendar.getInstance().apply { timeInMillis = localMillis }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

fun pickerMillisToExpiry(utcMillis: Long): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
    return Calendar.getInstance().apply {
        clear()
        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), 23, 59, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis
}

fun dDayText(expiry: Long): String {
    val d = calculateDDay(expiry)
    return when {
        d < 0 -> "${-d}일 지남"
        d == 0L -> "오늘까지"
        else -> "${d}일 남음"
    }
}

// 소비기한 입력: 날짜 버튼(달력) + 빠른 선택 + 포장지 촬영. 숫자로 "남은 일수"를 적는 방식은 헷갈려서 바꿨다.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpiryDateField(
    expiry: Long,
    scanned: Boolean,
    onChange: (expiry: Long, fromScan: Boolean) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    var scanning by remember { mutableStateOf(false) }
    val scan = rememberExpiryDateScanner(onBusy = { scanning = it }) { date -> date?.let { onChange(it, true) } }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("소비기한", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.weight(1f)) {
                Text("📅 ${formatDate(expiry)}  (${dDayText(expiry)})", fontSize = 14.sp, maxLines = 1)
            }
            OutlinedButton(onClick = scan, enabled = !scanning) {
                if (scanning) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text("📷 포장지", fontSize = 13.sp)
            }
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("3일" to 3, "1주" to 7, "2주" to 14, "1달" to 30, "3달" to 90).forEach { (label, days) ->
                FilterChip(
                    selected = false,
                    onClick = { onChange(calculateExpiryTimestamp(System.currentTimeMillis(), days), false) },
                    label = { Text("+$label", fontSize = 12.sp) }
                )
            }
        }
        if (scanned) {
            Text("포장지에서 읽은 날짜예요. 맞는지 확인해 주세요.", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
        }
    }

    if (showPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = localDateToPickerMillis(expiry))
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onChange(pickerMillisToExpiry(it), false) }
                    showPicker = false
                }) { Text("확인") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("취소") } }
        ) {
            DatePicker(state = state)
        }
    }
}

// ==========================================
// 직접 등록 (품명 자동 채움 · 바코드 · 포장지 날짜 · 가격)
// ==========================================
@Composable
fun ManualAddDialog(
    resolvePreset: suspend (String) -> UserPreset?,
    lookupBarcode: suspend (String) -> BarcodeProduct?,
    onDismiss: () -> Unit,
    onAdd: (item: FoodItem, barcode: String?) -> Unit,
    // 추가 메뉴에서 "바코드로 등록"을 고르면 창이 열리자마자 스캐너를 띄운다
    startWithBarcode: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var storage by remember { mutableStateOf("냉장") }
    var category by remember { mutableStateOf("일반") }
    var qtyText by remember { mutableStateOf("1") }
    var expiry by remember { mutableStateOf(calculateExpiryTimestamp(System.currentTimeMillis(), 7)) }
    var expiryScanned by remember { mutableStateOf(false) }
    var priceText by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf<String?>(null) }
    var barcodeLoading by remember { mutableStateOf(false) }
    // 사용자가 직접 고친 칸은 자동 채움이 덮어쓰지 않는다
    var storageTouched by remember { mutableStateOf(false) }
    var categoryTouched by remember { mutableStateOf(false) }
    var daysTouched by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(name) {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || barcode != null) {
            if (trimmed.isEmpty()) hint = null
            return@LaunchedEffect
        }
        delay(300) // 입력 중엔 기다렸다가 멈추면 조회
        val default = ShelfLifeMapper.getDefaultPreset(trimmed)
        val learned = resolvePreset(trimmed)
        if (!categoryTouched) category = default.category
        if (!storageTouched) storage = learned?.storageType ?: default.storageType
        if (!daysTouched) expiry = calculateExpiryTimestamp(System.currentTimeMillis(), learned?.shelfDays ?: default.days)
        hint = if (learned != null) "지난번 등록한 값으로 채웠어요" else "품명으로 추천값을 채웠어요"
    }

    fun onBarcode(code: String) {
        barcodeLoading = true
        scope.launch {
            val product = lookupBarcode(code)
            barcodeLoading = false
            barcode = code
            if (product == null) {
                hint = "처음 보는 바코드예요. 이름을 적어 두면 다음부터 자동으로 채워요"
                Toast.makeText(context, "상품 정보를 찾지 못했어요. 이름을 직접 입력해 주세요.", Toast.LENGTH_SHORT).show()
            } else {
                name = product.name
                category = product.category
                storage = product.storageType
                if (!daysTouched) expiry = calculateExpiryTimestamp(System.currentTimeMillis(), product.shelfDays)
                hint = "바코드로 채웠어요 ($code)"
            }
        }
    }

    LaunchedEffect(Unit) {
        if (startWithBarcode) scanBarcode(context, ::onBarcode)
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
                OutlinedButton(
                    onClick = { scanBarcode(context, ::onBarcode) },
                    enabled = !barcodeLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (barcodeLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("▦ 바코드로 채우기")
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("식재료 이름 (예: 사과)") },
                    supportingText = hint?.let { { Text(it, fontSize = 11.sp) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = formFieldColors()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumberField(qtyText, { qtyText = it }, "수량", Modifier.weight(1f), maxLength = 3)
                    NumberField(priceText, { priceText = it }, "가격(원, 선택)", Modifier.weight(1.4f), maxLength = 7)
                }
                ExpiryDateField(expiry = expiry, scanned = expiryScanned) { date, fromScan ->
                    expiry = date
                    expiryScanned = fromScan
                    daysTouched = true
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
                    val now = System.currentTimeMillis()
                    val item = FoodItem(
                        name = name.trim(),
                        category = category,
                        storageType = storage,
                        quantity = (qtyText.toIntOrNull() ?: 1).coerceAtLeast(1),
                        purchaseDate = now,
                        expiryDate = expiry,
                        price = priceText.toIntOrNull()?.takeIf { it > 0 }
                    )
                    onAdd(item, barcode)
                },
                enabled = name.isNotBlank()
            ) { Text("추가", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}

// ==========================================
// 수정 (포장지 날짜 · 가격 · 품목별 알림 · 개봉 취소)
// ==========================================
@Composable
fun EditFoodItemDialog(item: FoodItem, onDismiss: () -> Unit, onSave: (FoodItem) -> Unit) {
    var name by remember { mutableStateOf(item.name) }
    var storage by remember { mutableStateOf(item.storageType) }
    var category by remember { mutableStateOf(item.category) }
    var qtyText by remember { mutableStateOf(item.quantity.toString()) }
    var priceText by remember { mutableStateOf(item.price?.toString() ?: "") }
    var expiry by remember { mutableStateOf(item.expiryDate) }
    var expiryScanned by remember { mutableStateOf(false) }
    var notify by remember { mutableStateOf(item.notifyEnabled) }
    var opened by remember { mutableStateOf(item.openedDate != null) }

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
                    colors = formFieldColors()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumberField(qtyText, { qtyText = it }, "수량", Modifier.weight(1f), maxLength = 3)
                    NumberField(priceText, { priceText = it }, "가격(원, 선택)", Modifier.weight(1.4f), maxLength = 7)
                }
                ExpiryDateField(expiry = expiry, scanned = expiryScanned) { date, fromScan ->
                    expiry = date
                    expiryScanned = fromScan
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("냉장", "냉동", "실온").forEach { loc ->
                        FilterChip(selected = storage == loc, onClick = { storage = loc }, label = { Text(loc) })
                    }
                }
                CategoryChips(selected = category, onSelect = { category = it })

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("이 품목 알림 받기", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("끄면 기한이 다가와도 알리지 않아요", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = notify, onCheckedChange = { notify = it })
                }
                if (item.openedDate != null) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("개봉함 (${formatDate(item.openedDate)})", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("잘못 눌렀다면 끄세요. 기한은 위 날짜에서 고쳐 주세요", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = opened, onCheckedChange = { opened = it })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val newExpiry = expiry
                onSave(
                    item.copy(
                        name = name.trim().ifBlank { item.name },
                        category = category,
                        storageType = storage,
                        quantity = (qtyText.toIntOrNull() ?: 1).coerceAtLeast(1),
                        expiryDate = newExpiry,
                        price = priceText.toIntOrNull()?.takeIf { it > 0 },
                        notifyEnabled = notify,
                        openedDate = if (opened) item.openedDate else null,
                        // 알림을 다시 켜면 "내일 다시"로 미뤄 둔 것도 풀어준다
                        snoozedUntil = if (notify && !item.notifyEnabled) null else item.snoozedUntil
                    )
                )
            }) { Text("저장") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}

// ==========================================
// 개봉 처리
// ==========================================
@Composable
fun OpenFoodDialog(item: FoodItem, onDismiss: () -> Unit, onConfirm: (daysAfterOpen: Int) -> Unit) {
    var daysText by remember { mutableStateOf(ShelfLifeMapper.openedDays(item.name).toString()) }
    val days = daysText.toIntOrNull() ?: 0
    val remaining = calculateDDay(item.expiryDate)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = { Text("[${item.name}] 개봉", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("개봉하면 남은 기한이 짧아져요. 개봉 후 며칠 안에 드실 건가요?", fontSize = 14.sp, lineHeight = 20.sp)
                NumberField(daysText, { daysText = it }, "개봉 후 보관 일수", Modifier.fillMaxWidth(), maxLength = 3)
                Text(
                    if (remaining < days) "원래 기한(${remaining}일 남음)이 더 짧아서 그대로 둡니다."
                    else "소비기한이 ${formatDate(calculateExpiryTimestamp(System.currentTimeMillis(), days))}로 바뀝니다.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = { Button(onClick = { onConfirm(days) }, enabled = daysText.isNotEmpty()) { Text("개봉했어요") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}

// ==========================================
// 소비 / 폐기 (+ 장보기 목록에 추가)
// ==========================================
@Composable
fun ConsumeOrWasteDialog(item: FoodItem, onDismiss: () -> Unit, onSelect: (status: String, addToShopping: Boolean) -> Unit) {
    var addToShopping by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = { Text("[${item.name}] 정리", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("이 식재료를 어떻게 처리하셨나요? 기록과 절약 리포트에 반영됩니다.", fontSize = 14.sp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        // 줄 전체를 하나의 체크 항목으로 (화면 낭독기가 두 번 읽지 않게)
                        .toggleable(value = addToShopping, role = Role.Checkbox) { addToShopping = it },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = addToShopping, onCheckedChange = null)
                    Text("🛒 장보기 목록에 추가", fontSize = 14.sp)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSelect("CONSUMED", addToShopping) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))) {
                Text("맛있게 먹었어요 😋", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Button(onClick = { onSelect("WASTED", addToShopping) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))) {
                Text("버렸어요 🗑️", fontWeight = FontWeight.Bold)
            }
        }
    )
}

// ==========================================
// 절약 리포트 (실제 금액 + 이번 달)
// ==========================================
@Composable
fun SavingsReportDialog(archivedFoods: List<FoodItem>, onDismiss: () -> Unit, onOpenArchive: () -> Unit) {
    val monthStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val thisMonth = archivedFoods.filter { (it.closedDate ?: 0L) >= monthStart }
    var showMonth by remember { mutableStateOf(thisMonth.isNotEmpty()) }
    val target = if (showMonth) thisMonth else archivedFoods

    val consumed = target.filter { it.status == "CONSUMED" }
    val wasted = target.filter { it.status == "WASTED" }
    val total = consumed.size + wasted.size
    val rate = if (total > 0) (consumed.size.toFloat() / total * 100).toInt() else 100
    fun sum(list: List<FoodItem>) = list.sumOf { it.price ?: ESTIMATED_PRICE_PER_ITEM }
    val estimatedCount = target.count { it.price == null }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = { Text("📊 냉장고 절약 리포트", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = showMonth, onClick = { showMonth = true }, label = { Text("이번 달") })
                    FilterChip(selected = !showMonth, onClick = { showMonth = false }, label = { Text("전체") })
                }
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ReportRow("맛있게 소비 😋", "${consumed.size}개 · ${formatWon(sum(consumed))}", Color(0xFF10B981))
                        ReportRow("버린 식재료 🗑️", "${wasted.size}개 · ${formatWon(sum(wasted))}", Color(0xFFEF4444))
                        Spacer(modifier = Modifier.height(4.dp))
                        ReportRow("소비 성공률", "${rate}%", MaterialTheme.colorScheme.primary, bold = true)
                    }
                }
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background((if (wasted.isEmpty()) Color(0xFF10B981) else Color(0xFFEF4444)).copy(alpha = 0.13f))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (total == 0) "아직 정리한 식재료가 없어요"
                        else if (wasted.isEmpty()) "🎉 버린 식재료 없이 ${formatWon(sum(consumed))}어치를 다 먹었어요!"
                        else "💸 ${formatWon(sum(wasted))}어치를 버렸어요. 임박 알림을 활용해 보세요!",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (wasted.isEmpty()) Color(0xFF10B981) else Color(0xFFEF4444)
                    )
                }
                if (estimatedCount > 0) {
                    Text(
                        "가격을 모르는 ${estimatedCount}개는 1개당 ${formatWon(ESTIMATED_PRICE_PER_ITEM)}으로 추정했어요. 영수증으로 등록하거나 수정 화면에서 가격을 넣으면 정확해져요.",
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("확인") } },
        dismissButton = { TextButton(onClick = onOpenArchive) { Text("정리 기록 보기") } }
    )
}

@Composable
private fun ReportRow(label: String, value: String, color: Color, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = color, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Spacer(modifier = Modifier.width(8.dp))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

// ==========================================
// 첫 실행 안내
// ==========================================
@Composable
fun WelcomeDialog(onCamera: () -> Unit, onManual: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = {
            Column {
                Text("🧊 냉장고 알리미", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text("소비기한이 다가오면 알려 드려요", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                WelcomeRow("🧾", "영수증 찍기", "장 본 영수증을 찍으면 품목·가격을 한 번에 등록")
                WelcomeRow("▦", "바코드 · 직접 등록", "바코드를 찍거나 이름만 적어도 기한을 추천")
                WelcomeRow("📷", "포장지 날짜 찍기", "등록·수정 창에서 소비기한 글자를 찍으면 정확한 날짜로")
                WelcomeRow("🔔", "알림에서 바로 정리", "기한 하루 전부터 알림의 '먹었어요' 버튼으로 끝")
            }
        },
        confirmButton = { Button(onClick = onCamera) { Text("영수증 찍기", fontWeight = FontWeight.Bold) } },
        dismissButton = {
            Row {
                TextButton(onClick = onManual) { Text("직접 등록") }
                TextButton(onClick = onDismiss) { Text("둘러보기") }
            }
        }
    )
}

@Composable
private fun WelcomeRow(emoji: String, title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 22.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(desc, fontSize = 12.sp, lineHeight = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ==========================================
// 같은 품목 합치기
// ==========================================
@Composable
fun MergeDuplicatesDialog(
    duplicates: List<Pair<FoodItem, FoodItem>>,
    onMerge: () -> Unit,
    onSeparate: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = { Text("이미 있는 품목이에요", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("기한이 같은 품목이 냉장고에 있어요. 수량을 합칠까요?", fontSize = 14.sp)
                duplicates.take(5).forEach { (new, existing) ->
                    Text(
                        "• ${existing.name}: ${existing.quantity}개 + ${new.quantity}개 → ${existing.quantity + new.quantity}개",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (duplicates.size > 5) Text("외 ${duplicates.size - 5}개", fontSize = 12.sp)
            }
        },
        confirmButton = { Button(onClick = onMerge) { Text("수량 합치기") } },
        dismissButton = { TextButton(onClick = onSeparate) { Text("따로 추가") } }
    )
}

// ==========================================
// 등록 메뉴 (오른쪽 아래 "등록" 버튼)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOptionsSheet(
    onDismiss: () -> Unit,
    onReceiptCamera: () -> Unit,
    onReceiptGallery: () -> Unit,
    onBarcode: () -> Unit,
    onManual: () -> Unit,
    onSample: (() -> Unit)?
) {
    // 반만 열리면 아래 항목이 가려져서 처음부터 끝까지 펼친다
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Column(
            // 맨 아래 항목이 내비게이션 바에 가리지 않게
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("식재료 등록", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
            AddOptionRow("🧾", "영수증 찍기", "장 본 품목과 가격을 한 번에", onReceiptCamera)
            AddOptionRow("🖼️", "갤러리의 영수증 사진", "이미 찍어 둔 영수증으로", onReceiptGallery)
            AddOptionRow("▦", "바코드로 등록", "상품 바코드를 비추면 이름·기한을 채워요", onBarcode)
            AddOptionRow("✏️", "직접 등록", "이름만 적어도 기한을 추천해요", onManual)
            if (onSample != null) AddOptionRow("▶", "샘플 영수증 (개발용)", "개발용 빌드에서만 보여요", onSample)
        }
    }
}

@Composable
private fun AddOptionRow(emoji: String, title: String, desc: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 24.sp)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
