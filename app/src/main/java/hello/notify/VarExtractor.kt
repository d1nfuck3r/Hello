package hello.notify

object VarExtractor {
    fun extract(text: String): Map<String, String> {
        val m = mutableMapOf<String, String>()
        Regex("""([\d,]+\.\d{2})""").find(text)?.groupValues?.get(1)
            ?.let { m["ยอด"] = it.replace(",", "") }
        Regex("""(\d+)\s*(?:แต้ม|คะแนน)""").find(text)?.groupValues?.get(1)
            ?.let { m["แต้ม"] = it }
        Regex("""(?:จาก|โอนจาก|by|from)\s+([^\s/\n]+(?:\s+[^\s/\n]+)?)""", RegexOption.IGNORE_CASE)
            .find(text)?.groupValues?.get(1)?.trim()
            ?.let { m["ชื่อ"] = it }
        m["ข้อความ"] = text
        m["หัวข้อ"] = text.lines().firstOrNull()?.trim() ?: text
        return m
    }

    fun format(template: String, vars: Map<String, String>): String {
        var out = template
        vars.forEach { (k, v) -> out = out.replace("{$k}", v) }
        // ลบ placeholder ที่เหลือ — ใช้ loop แทน regex หลีก ICU PatternSyntaxException
        var result = out
        var start = result.indexOf('{')
        while (start != -1) {
            val end = result.indexOf('}', start)
            if (end == -1) break
            result = result.removeRange(start, end + 1)
            start = result.indexOf('{', start)
        }
        return result.trim()
    }
}
