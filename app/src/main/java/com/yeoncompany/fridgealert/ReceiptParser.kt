package com.yeoncompany.fridgealert

import java.util.Calendar

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
    var isChecked: Boolean = true,
    var price: Int? = null
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

    // 개봉 후 권장 보관 일수 (모르는 품목은 3일)
    fun openedDays(name: String): Int {
        val lower = name.lowercase()
        return when {
            lower.contains("우유") || lower.contains("밀크") || lower.contains("주스") -> 3
            lower.contains("요거트") || lower.contains("요구르트") -> 2
            lower.contains("두부") -> 2
            lower.contains("햄") || lower.contains("소시지") || lower.contains("베이컨") -> 3
            lower.contains("치즈") -> 7
            lower.contains("버터") -> 30
            lower.contains("케첩") || lower.contains("마요") || lower.contains("소스") ||
                    lower.contains("드레싱") || lower.contains("잼") -> 30
            lower.contains("김치") -> 14
            lower.contains("통조림") || lower.contains("참치") -> 2
            else -> 3
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
                    isChecked = true,
                    price = findPrice(trimmed)
                )
            )
        }
        return ReceiptParseResult(purchaseDateMillis, results.distinctBy { it.name })
    }

    // 줄에서 마지막 금액(보통 합계 칸)을 고른다. 용량(300g)·수량(2개)·바코드 숫자는 제외
    private val MONEY = Regex("""(?<![\d.,])(\d{1,3}(?:,\d{3})+|\d{3,7})(?![\d,]|\s*(kg|g|ml|mL|L|l|구|봉|팩|매|통|망|단|개|EA|입))""")

    fun findPrice(line: String): Int? =
        MONEY.findAll(line).mapNotNull { it.groupValues[1].replace(",", "").toIntOrNull() }
            .filter { it in 100..1_000_000 }
            .lastOrNull()

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
