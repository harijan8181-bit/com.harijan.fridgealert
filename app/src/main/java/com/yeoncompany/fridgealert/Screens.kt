package com.yeoncompany.fridgealert

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 자주 쓰는 "내용물"과 "장보기"는 팝업 대신 일반 화면으로 (뒤로가기로 메인 복귀)

@Composable
private fun SubScreenTopBar(title: String, subtitle: String?, onBack: () -> Unit, actions: @Composable () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", tint = MaterialTheme.colorScheme.onSurface)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
        actions()
    }
}

private fun categoryLabel(category: String) = when (category) {
    "유제품" -> "🥛 유제품"
    "육류" -> "🥩 육류"
    "신선식품" -> "🥚 신선식품"
    "채소" -> "🥬 채소"
    "과일" -> "🍎 과일"
    "수산물" -> "🐟 수산물"
    "냉동식품" -> "🥟 냉동식품"
    "가공식품" -> "🍱 가공식품"
    "베이커리" -> "🍞 베이커리"
    "반찬" -> "🥢 반찬"
    else -> "📦 $category"
}

// ==========================================
// 냉장고 내용물 (검색 포함)
// ==========================================
@Composable
fun StoredFoodsScreen(
    activeFoods: List<FoodItem>,
    focusSearch: Boolean,
    onBack: () -> Unit,
    onOpenArchive: () -> Unit,
    onDecreaseQty: (FoodItem) -> Unit,
    onIncreaseQty: (FoodItem) -> Unit,
    onOpenStatusModal: (FoodItem) -> Unit,
    onEdit: (FoodItem) -> Unit,
    onDelete: (FoodItem) -> Unit,
    onOpen: (FoodItem) -> Unit,
    contentPadding: PaddingValues
) {
    var query by rememberSaveable { mutableStateOf("") }
    var storageFilter by rememberSaveable { mutableStateOf("전체") }
    var sortMode by rememberSaveable { mutableStateOf("기한순") }
    val collapsed = rememberSaveable(saver = listSaver(save = { it.toList() }, restore = { it.toMutableStateList() })) {
        mutableStateListOf<String>()
    }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(focusSearch) { if (focusSearch) focusRequester.requestFocus() }

    val filtered = remember(activeFoods, storageFilter, query) {
        activeFoods
            .filter { storageFilter == "전체" || it.storageType == storageFilter }
            .filter { query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) }
    }
    val grouped = remember(filtered, sortMode) {
        val sorted = when (sortMode) {
            "등록순" -> filtered.sortedByDescending { it.purchaseDate }
            "이름순" -> filtered.sortedBy { it.name }
            else -> filtered.sortedBy { it.expiryDate }
        }
        // 분류도 가장 급한 품목이 있는 분류부터
        sorted.groupBy { it.category }.entries
            .sortedBy { (_, list) -> list.minOf { it.expiryDate } }
            .associate { it.key to it.value }
    }
    // 검색 중에는 접힌 분류도 펼쳐서 결과가 숨지 않게
    val searching = query.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(bottom = contentPadding.calculateBottomPadding())
    ) {
        SubScreenTopBar(
            title = "냉장고 속 식재료",
            subtitle = "보관 중 ${activeFoods.size}개",
            onBack = onBack
        ) {
            TextButton(onClick = onOpenArchive) {
                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(4.dp))
                Text("기록")
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .widthIn(max = 640.dp)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("이름으로 찾기 (예: 우유)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, contentDescription = "검색어 지우기") }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {}),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("전체", "냉장", "냉동", "실온").forEach { s ->
                    FilterChip(selected = storageFilter == s, onClick = { storageFilter = s }, label = { Text(s) })
                }
                Spacer(modifier = Modifier.size(8.dp))
                listOf("기한순", "등록순", "이름순").forEach { mode ->
                    FilterChip(selected = sortMode == mode, onClick = { sortMode = mode }, label = { Text(mode, fontSize = 12.sp) })
                }
            }
        }

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                Text(
                    if (searching) "'$query'에 해당하는 식재료가 없어요" else "보관 중인 식재료가 없어요",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally).widthIn(max = 640.dp).fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${filtered.size}개",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                )
                if (!searching) {
                    TextButton(onClick = { if (collapsed.isEmpty()) collapsed.addAll(grouped.keys) else collapsed.clear() }) {
                        Text(if (collapsed.isEmpty()) "모두 접기" else "모두 펼치기", fontSize = 12.sp)
                    }
                }
            }
            LazyColumn(
                modifier = Modifier.align(Alignment.CenterHorizontally).widthIn(max = 640.dp).fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                grouped.forEach { (category, list) ->
                    val isCollapsed = !searching && category in collapsed
                    item(key = "header_$category") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clickable { if (isCollapsed) collapsed.remove(category) else collapsed.add(category) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                categoryLabel(category),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${list.size}개 " + if (isCollapsed) "▸" else "▾",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (!isCollapsed) items(list, key = { it.id }) { food ->
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
}

// ==========================================
// 장보기 목록
// ==========================================
@Composable
fun ShoppingScreen(
    items: List<ShoppingItem>,
    onBack: () -> Unit,
    onAdd: (String) -> Unit,
    onToggle: (ShoppingItem) -> Unit,
    onDelete: (ShoppingItem) -> Unit,
    onClearChecked: () -> Unit,
    contentPadding: PaddingValues
) {
    var input by rememberSaveable { mutableStateOf("") }
    val checkedCount = items.count { it.checked }
    fun submit() {
        if (input.isNotBlank()) { onAdd(input); input = "" }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(bottom = contentPadding.calculateBottomPadding())
    ) {
        SubScreenTopBar(
            title = "장보기 목록",
            subtitle = "살 것 ${items.size - checkedCount}개 · 담은 것 ${checkedCount}개",
            onBack = onBack
        ) {
            if (checkedCount > 0) TextButton(onClick = onClearChecked) { Text("담은 것 지우기") }
        }
        Column(
            modifier = Modifier.align(Alignment.CenterHorizontally).widthIn(max = 640.dp).padding(horizontal = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("살 것 입력 (예: 우유)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                Button(onClick = { submit() }, enabled = input.isNotBlank()) { Text("추가") }
            }
        }
        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 60.dp, start = 24.dp, end = 24.dp), contentAlignment = Alignment.Center) {
                Text(
                    "비어 있어요.\n식재료를 정리할 때 '장보기 목록에 추가'를 체크해도 돼요.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.align(Alignment.CenterHorizontally).widthIn(max = 640.dp).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(items, key = { it.id }) { s ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .toggleable(value = s.checked, role = androidx.compose.ui.semantics.Role.Checkbox) { onToggle(s) }
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = s.checked, onCheckedChange = null, modifier = Modifier.padding(12.dp))
                        Text(
                            s.name,
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f),
                            color = if (s.checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            textDecoration = if (s.checked) TextDecoration.LineThrough else TextDecoration.None
                        )
                        IconButton(onClick = { onDelete(s) }) {
                            Icon(Icons.Default.Close, contentDescription = "${s.name} 목록에서 빼기", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}
