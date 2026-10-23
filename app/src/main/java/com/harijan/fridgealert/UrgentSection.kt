package com.harijan.fridgealert

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 메인 화면 "오늘 챙길 식재료" — 큰 카드를 주르륵 늘어놓는 대신 한 줄짜리 목록으로.
// 곧 만료(오늘~D-3)는 5개까지 보이고, 이미 기한이 지난 품목은 한 줄 요약으로 접어 둔다.
private const val SOON_PREVIEW_COUNT = 5
private val RED = Color(0xFFEF4444)
private val AMBER = Color(0xFFF59E0B)

@Composable
fun UrgentFoodsSection(
    urgentFoods: List<FoodItem>,
    onRowClick: (FoodItem) -> Unit,
    onSettle: (FoodItem) -> Unit,
    onWasteAllExpired: (List<FoodItem>) -> Unit
) {
    val soon = urgentFoods.filter { calculateDDay(it.expiryDate) >= 0 }.sortedBy { it.expiryDate }
    val expired = urgentFoods.filter { calculateDDay(it.expiryDate) < 0 }.sortedByDescending { it.expiryDate }

    var showAllSoon by rememberSaveable { mutableStateOf(false) }
    var showExpired by rememberSaveable { mutableStateOf(false) }
    var confirmWaste by rememberSaveable { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            if (soon.isEmpty()) {
                Text(
                    text = "✨ 3일 안에 기한이 끝나는 식재료가 없어요",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp)
                )
            } else {
                val visible = if (showAllSoon) soon else soon.take(SOON_PREVIEW_COUNT)
                visible.forEachIndexed { i, food ->
                    if (i > 0) RowDivider()
                    CompactFoodRow(food, onClick = { onRowClick(food) }, onSettle = { onSettle(food) })
                }
                if (soon.size > SOON_PREVIEW_COUNT) {
                    RowDivider()
                    TextButton(
                        onClick = { showAllSoon = !showAllSoon },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (showAllSoon) "접기" else "${soon.size - SOON_PREVIEW_COUNT}개 더 보기")
                    }
                }
            }

            if (expired.isNotEmpty()) {
                // 기한 지난 품목은 한 줄 요약 → 눌러서 펼치기, 한번에 버림 처리
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RED.copy(alpha = 0.08f))
                        .clickable { showExpired = !showExpired }
                        .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🗑️ 기한 지난 식재료 ${expired.size}개 " + if (showExpired) "▾" else "▸",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = RED
                        )
                        Text(
                            text = expired.take(3).joinToString(", ") { it.name } + if (expired.size > 3) " 외" else "",
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { confirmWaste = true }) {
                        Text("한번에 정리", color = RED, fontWeight = FontWeight.SemiBold)
                    }
                }
                if (showExpired) {
                    expired.forEachIndexed { i, food ->
                        if (i > 0) RowDivider()
                        CompactFoodRow(food, onClick = { onRowClick(food) }, onSettle = { onSettle(food) })
                    }
                }
            }
        }
    }

    if (confirmWaste) {
        AlertDialog(
            onDismissRequest = { confirmWaste = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            title = { Text("기한 지난 ${expired.size}개 정리", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "모두 '버렸어요'로 기록하고 목록에서 뺍니다. 먹은 품목이 있다면 줄을 펼쳐 하나씩 정리해 주세요.\n\n" +
                            expired.take(6).joinToString(", ") { it.name } + if (expired.size > 6) " 외 ${expired.size - 6}개" else "",
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { onWasteAllExpired(expired); confirmWaste = false },
                    colors = ButtonDefaults.buttonColors(containerColor = RED)
                ) { Text("모두 버렸어요", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmWaste = false }) { Text("취소") } }
        )
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

// 한 줄: [D-day] 이름 / 보관·수량   [정리]
@Composable
private fun CompactFoodRow(food: FoodItem, onClick: () -> Unit, onSettle: () -> Unit) {
    val d = calculateDDay(food.expiryDate)
    val color = when {
        d < 0 -> RED
        d <= 1 -> RED
        else -> AMBER
    }
    val label = when {
        d < 0 -> "D+${-d}"
        d == 0L -> "오늘"
        else -> "D-$d"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .widthIn(min = 52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.14f))
                .padding(horizontal = 6.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1, softWrap = false, textAlign = TextAlign.Center)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                food.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                buildString {
                    append(food.storageType).append(" · ").append(food.quantity).append("개")
                    if (food.openedDate != null) append(" · 개봉")
                    append(" · ~").append(formatDate(food.expiryDate).substring(5))
                },
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        OutlinedButton(
            onClick = onSettle,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text("정리", fontSize = 13.sp)
        }
    }
}
