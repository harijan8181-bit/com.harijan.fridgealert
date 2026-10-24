package com.yeoncompany.fridgealert

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
