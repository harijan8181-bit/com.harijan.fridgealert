package com.harijan.fridgealert

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

// "소비기한 일수" 칸 + 포장지 날짜 촬영 버튼
@Composable
private fun DaysWithLabelScan(
    daysText: String,
    label: String,
    onDaysChange: (String) -> Unit,
    scannedExpiry: Long?,
    onScanned: (Long) -> Unit
) {
    var scanning by remember { mutableStateOf(false) }
    val scan = rememberExpiryDateScanner(onBusy = { scanning = it }) { date -> date?.let(onScanned) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(daysText, onDaysChange, label, Modifier.weight(1f))
            OutlinedButton(onClick = scan, enabled = !scanning) {
                if (scanning) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text("📷 포장지 날짜", fontSize = 12.sp)
            }
        }
        if (scannedExpiry != null) {
            Text(
                "포장지에서 읽은 소비기한: ${formatDate(scannedExpiry)}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary
            )
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
    onAdd: (item: FoodItem, barcode: String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var storage by remember { mutableStateOf("냉장") }
    var category by remember { mutableStateOf("일반") }
    var qtyText by remember { mutableStateOf("1") }
    var shelfDaysText by remember { mutableStateOf("7") }
    var priceText by remember { mutableStateOf("") }
    var scannedExpiry by remember { mutableStateOf<Long?>(null) }
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
        if (!daysTouched) shelfDaysText = (learned?.shelfDays ?: default.days).toString()
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
                if (!daysTouched) shelfDaysText = product.shelfDays.toString()
                hint = "바코드로 채웠어요 ($code)"
            }
        }
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
                DaysWithLabelScan(
                    daysText = shelfDaysText,
                    label = "소비기한 일수",
                    onDaysChange = { shelfDaysText = it; daysTouched = true; scannedExpiry = null },
                    scannedExpiry = scannedExpiry,
                    onScanned = { date ->
                        scannedExpiry = date
                        shelfDaysText = calculateDDay(date).coerceAtLeast(0).toString()
                        daysTouched = true
                    }
                )
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
                    val days = shelfDaysText.toIntOrNull() ?: 7
                    val item = FoodItem(
                        name = name.trim(),
                        category = category,
                        storageType = storage,
                        quantity = (qtyText.toIntOrNull() ?: 1).coerceAtLeast(1),
                        purchaseDate = now,
                        expiryDate = scannedExpiry ?: calculateExpiryTimestamp(now, days),
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
    var remainingDays by remember { mutableStateOf(calculateDDay(item.expiryDate).coerceAtLeast(0).toString()) }
    var scannedExpiry by remember { mutableStateOf<Long?>(null) }
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
                DaysWithLabelScan(
                    daysText = remainingDays,
                    label = "오늘 기준 남은 일수",
                    onDaysChange = { remainingDays = it; scannedExpiry = null },
                    scannedExpiry = scannedExpiry,
                    onScanned = { date ->
                        scannedExpiry = date
                        remainingDays = calculateDDay(date).coerceAtLeast(0).toString()
                    }
                )
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
                            Text("잘못 눌렀다면 끄세요. 기한은 위 칸에서 직접 고쳐 주세요", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = opened, onCheckedChange = { opened = it })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val days = remainingDays.toIntOrNull() ?: 0
                val newExpiry = scannedExpiry ?: calculateExpiryTimestamp(System.currentTimeMillis(), days)
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
                        .clickable { addToShopping = !addToShopping },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = addToShopping, onCheckedChange = { addToShopping = it })
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
// 장보기 목록
// ==========================================
@Composable
fun ShoppingListDialog(
    items: List<ShoppingItem>,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
    onToggle: (ShoppingItem) -> Unit,
    onDelete: (ShoppingItem) -> Unit,
    onClearChecked: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    val checkedCount = items.count { it.checked }
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 20.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = {
            Column {
                Text("🛒 장보기 목록", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("살 것 ${items.size - checkedCount}개 · 담은 것 ${checkedCount}개", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = { Text("살 것 입력 (예: 우유)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = formFieldColors()
                    )
                    Button(onClick = { onAdd(input); input = "" }, enabled = input.isNotBlank()) { Text("추가") }
                }
                if (items.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "비어 있어요.\n식재료를 정리할 때 '장보기 목록에 추가'를 체크해도 돼요.",
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().height(300.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(items, key = { it.id }) { s ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .clickable { onToggle(s) }
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = s.checked, onCheckedChange = { onToggle(s) })
                                Text(
                                    s.name,
                                    fontSize = 15.sp,
                                    modifier = Modifier.weight(1f),
                                    color = if (s.checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                    textDecoration = if (s.checked) TextDecoration.LineThrough else TextDecoration.None
                                )
                                IconButton(onClick = { onDelete(s) }, modifier = Modifier.size(40.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "삭제", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("닫기") } },
        dismissButton = {
            if (checkedCount > 0) TextButton(onClick = onClearChecked) { Text("담은 것 ${checkedCount}개 지우기") }
        }
    )
}

// ==========================================
// 절약 리포트 (실제 금액 + 이번 달)
// ==========================================
@Composable
fun SavingsReportDialog(archivedFoods: List<FoodItem>, onDismiss: () -> Unit) {
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
        confirmButton = { Button(onClick = onDismiss) { Text("확인") } }
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
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
