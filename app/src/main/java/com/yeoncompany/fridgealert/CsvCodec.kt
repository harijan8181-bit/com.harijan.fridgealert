package com.yeoncompany.fridgealert

// CSV 백업 형식. 앞 7칸은 v1.1 백업과 같아서 예전 백업 파일도 그대로 복원된다.
object CsvCodec {
    const val HEADER = "name,category,storageType,quantity,purchaseDate,expiryDate,status,openedDate,notifyEnabled,price,closedDate"

    fun encode(item: FoodItem): String = listOf(
        item.name, item.category, item.storageType, item.quantity.toString(),
        item.purchaseDate.toString(), item.expiryDate.toString(), item.status,
        item.openedDate?.toString() ?: "", if (item.notifyEnabled) "1" else "0",
        item.price?.toString() ?: "", item.closedDate?.toString() ?: ""
    ).joinToString(",") { escape(it) }

    // 형식이 맞지 않는 줄은 null
    fun decode(line: String): FoodItem? {
        val t = parseLine(line)
        if (t.size < 7) return null
        val now = System.currentTimeMillis()
        return FoodItem(
            name = t[0],
            category = t[1],
            storageType = t[2],
            quantity = (t[3].toIntOrNull() ?: 1).coerceAtLeast(1),
            purchaseDate = t[4].toLongOrNull() ?: now,
            expiryDate = t[5].toLongOrNull() ?: now,
            status = t[6].takeIf { it in setOf("ACTIVE", "CONSUMED", "WASTED") } ?: "ACTIVE",
            openedDate = t.getOrNull(7)?.toLongOrNull(),
            notifyEnabled = t.getOrNull(8) != "0",
            price = t.getOrNull(9)?.toIntOrNull(),
            closedDate = t.getOrNull(10)?.toLongOrNull()
        )
    }

    // 같은 백업을 두 번 복원해도 중복 등록되지 않게 비교하는 키
    fun dedupeKey(item: FoodItem) = "${item.name}|${item.purchaseDate}|${item.expiryDate}|${item.status}"

    fun escape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else value

    fun parseLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                inQuotes && c == '"' && i + 1 < line.length && line[i + 1] == '"' -> { sb.append('"'); i++ }
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> { result.add(sb.toString()); sb.clear() }
                else -> sb.append(c)
            }
            i++
        }
        result.add(sb.toString())
        return result
    }
}
