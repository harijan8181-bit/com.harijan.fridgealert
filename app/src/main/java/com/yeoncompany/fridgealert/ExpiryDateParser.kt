package com.yeoncompany.fridgealert

import java.util.Calendar

// 포장지 사진의 OCR 글자에서 소비기한 날짜를 찾는다.
// 영수증과 달리 미래 날짜가 정답이고, 제조일이 같이 찍혀 있는 경우가 많다.
object ExpiryDateParser {
    private val KEYWORDS = listOf("소비기한", "유통기한", "품질유지", "까지", "EXP", "BEST BEFORE", "BB")
    private val MAKE_KEYWORDS = listOf("제조", "MFG", "PROD")

    private val FULL = Regex("""(20\d{2})\s*[-/.년]\s*(\d{1,2})\s*[-/.월]\s*(\d{1,2})""")
    private val SHORT = Regex("""(?<!\d)(\d{2})\s*[-/.]\s*(\d{1,2})\s*[-/.]\s*(\d{1,2})(?!\d)""")
    // 20261103, 261103 처럼 붙여 쓴 날짜 (키워드가 있는 줄에서만 인정)
    private val COMPACT = Regex("""(?<!\d)(20)?(\d{2})(\d{2})(\d{2})(?!\d)""")

    fun parse(text: String, now: Long = System.currentTimeMillis()): Long? {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val candidates = mutableListOf<Pair<Long, Int>>() // 날짜, 점수

        lines.forEachIndexed { i, line ->
            val upper = line.uppercase()
            val hasKeyword = KEYWORDS.any { upper.contains(it) }
            val prevHasKeyword = i > 0 && KEYWORDS.any { lines[i - 1].uppercase().contains(it) }
            val isMakeLine = MAKE_KEYWORDS.any { upper.contains(it) } && !hasKeyword

            val dates = mutableListOf<Long>()
            FULL.findAll(line).forEach { m -> toDate(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt(), now)?.let(dates::add) }
            if (dates.isEmpty()) {
                SHORT.findAll(line).forEach { m -> toDate(2000 + m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt(), now)?.let(dates::add) }
            }
            if (dates.isEmpty() && (hasKeyword || prevHasKeyword)) {
                COMPACT.findAll(line).forEach { m -> toDate(2000 + m.groupValues[2].toInt(), m.groupValues[3].toInt(), m.groupValues[4].toInt(), now)?.let(dates::add) }
            }

            for (d in dates) {
                val score = when {
                    hasKeyword -> 3
                    prevHasKeyword -> 2
                    isMakeLine -> -5
                    else -> 0
                }
                candidates.add(d to score)
            }
        }
        if (candidates.isEmpty()) return null
        // 점수가 같으면 더 늦은 날짜(제조일보다 소비기한이 뒤)
        return candidates.maxWith(compareBy<Pair<Long, Int>> { it.second }.thenBy { it.first }).first
    }

    // 한 달 전 ~ 5년 뒤만 소비기한으로 인정
    private fun toDate(year: Int, month: Int, day: Int, now: Long): Long? {
        if (month !in 1..12 || day !in 1..31) return null
        val cal = Calendar.getInstance().apply {
            isLenient = false
            clear()
            set(year, month - 1, day, 23, 59, 59)
        }
        val millis = try { cal.timeInMillis } catch (_: Exception) { return null }
        val dayMs = 24L * 60 * 60 * 1000
        if (millis < now - 30 * dayMs || millis > now + 5 * 365 * dayMs) return null
        return millis
    }
}
