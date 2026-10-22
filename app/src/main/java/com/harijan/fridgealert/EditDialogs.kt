package com.harijan.fridgealert

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.room.Delete

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
                                        text = "${item.storageType} • ${item.category} • ${item.shelfDays}일 보관" + (item.price?.let { " • ${formatWon(it)}" } ?: ""),
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
        var editPrice by remember { mutableStateOf(target.price?.toString() ?: "") }

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
                        value = editPrice,
                        onValueChange = { editPrice = it.filter(Char::isDigit).take(7) },
                        label = { Text("가격(원)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                            storageType = editStorage,
                            price = editPrice.toIntOrNull()?.takeIf { it > 0 }
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
