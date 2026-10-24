package com.yeoncompany.fridgealert

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

// 식품안전나라 "바코드연계제품정보(C005)" 공공 API
// 키는 local.properties 의 FOOD_API_KEY 로 넣는다. 비어 있으면 개발용 sample 키를 쓴다(호출 수 제한 있음, 출시 전 교체).
object BarcodeLookup {
    data class Result(val name: String, val category: String, val storageType: String, val shelfDays: Int)

    // 네트워크 호출 — 반드시 IO 스레드에서
    fun fetch(barcode: String, apiKey: String = BuildConfig.FOOD_API_KEY.ifBlank { "sample" }): Result? {
        val code = barcode.filter(Char::isDigit)
        if (code.length < 8) return null
        val url = URL(
            "https://openapi.foodsafetykorea.go.kr/api/${URLEncoder.encode(apiKey, "UTF-8")}/C005/json/1/1/BAR_CD=$code"
        )
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 5000
            readTimeout = 5000
        }
        return try {
            if (conn.responseCode != 200) return null
            parse(conn.inputStream.bufferedReader().use { it.readText() })
        } finally {
            conn.disconnect()
        }
    }

    fun parse(json: String): Result? {
        val root = JSONObject(json).optJSONObject("C005") ?: return null
        val row = root.optJSONArray("row")?.optJSONObject(0) ?: return null
        val name = row.optString("PRDLST_NM").trim()
        if (name.isEmpty()) return null
        val type = row.optString("PRDLST_DCNM")
        val term = row.optString("POG_DAYCNT")
        val preset = ShelfLifeMapper.getDefaultPreset("$name $type")
        return Result(
            name = name,
            category = preset.category,
            storageType = storageFromText(term) ?: preset.storageType,
            shelfDays = daysFromText(term) ?: preset.days
        )
    }

    // "냉장(0~10℃) 12일", "제조일로부터 12개월까지", "24개월" → 일수
    fun daysFromText(text: String): Int? {
        val m = Regex("""(\d+)\s*(일|개월|년)""").findAll(text).lastOrNull() ?: return null
        val n = m.groupValues[1].toIntOrNull() ?: return null
        return when (m.groupValues[2]) {
            "일" -> n
            "개월" -> n * 30
            else -> n * 365
        }.takeIf { it in 1..3650 }
    }

    fun storageFromText(text: String): String? = when {
        text.contains("냉동") -> "냉동"
        text.contains("냉장") -> "냉장"
        text.contains("실온") || text.contains("상온") -> "실온"
        else -> null
    }
}
