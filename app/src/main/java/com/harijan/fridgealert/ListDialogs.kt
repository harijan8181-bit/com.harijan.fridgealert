package com.harijan.fridgealert

import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.room.Delete

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
    onDelete: (FoodItem) -> Unit,
    onOpen: (FoodItem) -> Unit
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
                                onDelete = { onDelete(food) },
                                onOpen = { onOpen(food) }
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
    onDelete: (FoodItem) -> Unit,
    onOpen: (FoodItem) -> Unit
) {
    var storageFilter by remember { mutableStateOf("전체") }
    // 정렬과 접은 분류는 창을 다시 열어도 유지 (앱을 끄면 초기화)
    var sortMode by rememberSaveable { mutableStateOf("기한순") }
    val collapsed = rememberSaveable(saver = listSaver(save = { it.toList() }, restore = { it.toMutableStateList() })) {
        mutableStateListOf<String>()
    }

    val locationFiltered = remember(activeFoods, storageFilter) {
        if (storageFilter == "전체") activeFoods
        else activeFoods.filter { it.storageType.contains(storageFilter) }
    }

    val groupedByCategory = remember(locationFiltered, sortMode) {
        val sorted = when (sortMode) {
            "등록순" -> locationFiltered.sortedByDescending { it.purchaseDate }
            "이름순" -> locationFiltered.sortedBy { it.name }
            else -> locationFiltered.sortedBy { it.expiryDate }
        }
        // 분류도 가장 급한 품목이 있는 분류부터
        sorted.groupBy { it.category }.entries
            .sortedBy { (_, list) -> list.minOf { it.expiryDate } }
            .associate { it.key to it.value }
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

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf("기한순", "등록순", "이름순").forEach { mode ->
                        FilterChip(selected = sortMode == mode, onClick = { sortMode = mode }, label = { Text(mode, fontSize = 12.sp) })
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        if (collapsed.isEmpty()) collapsed.addAll(groupedByCategory.keys) else collapsed.clear()
                    }) { Text(if (collapsed.isEmpty()) "모두 접기" else "모두 펼치기", fontSize = 12.sp) }
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
                            val isCollapsed = category in collapsed
                            item(key = "header_$category") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                        .clickable { if (isCollapsed) collapsed.remove(category) else collapsed.add(category) }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
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
                                        text = "${itemsInCategory.size}개 " + if (isCollapsed) "▸" else "▾",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (!isCollapsed) items(itemsInCategory, key = { it.id }) { food ->
                                FoodItemCard(
                                    food = food,
                                    onDecreaseQty = { onDecreaseQty(food) },
                                    onIncreaseQty = { onIncreaseQty(food) },
                                    onOpenStatusModal = { onOpenStatusModal(food) },
                                    onEdit = { onEdit(food) },
                                    onDelete = { onDelete(food) },
                                    onOpen = { onOpen(food) }
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
