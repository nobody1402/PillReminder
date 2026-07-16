package com.example.pillreminder.util

import java.time.LocalTime

data class ParsedPrescriptionItem(
    val rawLine: String,
    val name: String,
    val formHint: String?, // مثلا "آمپول"، "شربت"، "سرم"، "قرص"
    val quantity: Int?,
    val suggestedDoseAmount: Double,
    val suggestedTimesOfDay: List<LocalTime>,
    val recognizedRule: DrugRuleSuggestion?,
    val suggestedTreatmentDurationDays: Int? = null,
    val suggestedInventoryCount: Double? = quantity?.toDouble()
)

/**
 * متن خام OCR شده از روی عکس نسخه رو به یک لیست از «پیش‌نویس» داروهای قابل‌تشخیص تبدیل
 * می‌کند. چون نسخه‌های واقعی معمولاً فقط اسم دارو + تعداد رو دارن (نه لزوماً ساعت دقیق
 * مصرف)، این فقط یک پیش‌نویس هوشمند می‌سازه که کاربر قبل از ذخیره نهایی باید بازبینی کنه؛
 * هیچ آلارمی بدون تایید نهایی کاربر در فرم افزودن دارو ذخیره نمی‌شود.
 */
object PrescriptionParser {

    private val formWords = listOf("آمپول", "شربت", "سرم", "قرص", "کپسول", "پماد", "قطره", "اسپری", "شیاف")

    // پیش‌فرض معقول وقتی هیچ قانونی برای دارو شناخته نشد
    private val defaultTimesForForm: Map<String, List<LocalTime>> = mapOf(
        "شربت" to listOf(LocalTime.of(8, 0), LocalTime.of(14, 0), LocalTime.of(20, 0)),
        "قرص" to listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)),
        "کپسول" to listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)),
        "آمپول" to listOf(LocalTime.of(9, 0)),
        "سرم" to listOf(LocalTime.of(9, 0)),
        "پماد" to listOf(LocalTime.of(9, 0), LocalTime.of(21, 0)),
        "قطره" to listOf(LocalTime.of(8, 0), LocalTime.of(14, 0), LocalTime.of(20, 0)),
        "اسپری" to listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)),
        "شیاف" to listOf(LocalTime.of(21, 0))
    )

    fun parse(rawText: String): List<ParsedPrescriptionItem> {
        val lines = rawText
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        return lines.mapNotNull { parseLine(it) }
    }

    private fun parseLine(originalLine: String): ParsedPrescriptionItem? {
        var line = originalLine
            // حذف شماره‌گذاری ابتدای خط، مثل «۱-» یا «2.»
            .replace(Regex("^[\\d۰-۹]+[\\-.\\)]\\s*"), "")
            .trim()
        if (line.length < 2) return null

        // استخراج تعداد از روی کلمه «تعداد» — همراه با کلمه‌ی واحدِ بعدش («عدد»/«واحد») که نباید توی اسم بمونه
        val qtyMatch = Regex("تعداد\\s*([\\d۰-۹]+)\\s*(عدد|واحد)?").find(line)
        val quantity = qtyMatch?.groupValues?.get(1)?.let {
            TimeParseUtils.normalizeDigits(it).toIntOrNull()
        }
        if (qtyMatch != null) line = line.removeRange(qtyMatch.range).trim()

        // استخراج اسم جایگزین داخل پرانتز، مثلا «(استامینوفن)»
        val parenMatch = Regex("\\(([^)]+)\\)").find(line)
        val altName = parenMatch?.groupValues?.get(1)?.trim()
        if (parenMatch != null) line = line.removeRange(parenMatch.range).trim()

        // تشخیص نوع فرآورده (شکل دارویی) از ابتدای خط
        val formHint = formWords.firstOrNull { line.startsWith(it) }
        if (formHint != null) line = line.removePrefix(formHint).trim()

        // حذف اعداد/واحدهای دوز باقی‌مانده (مثل "۵۰۰ میلی گرم"، "۱/۳ – ۲/۳ نیم لیتری"، "۶۴۳"، یا کلمه‌ی تنهای "عدد")
        val cleanedName = line
            .replace(Regex("[\\d۰-۹/.\\-–]+"), " ")
            .replace(Regex("میلی\\s*گرم|میلی\\s*لیتر|گرم|لیتری|نیم|cc|mg|ml|عدد|واحد"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        // اولویت با اسم داخل پرانتز (معمولا اسم ژنریک و قابل‌تشخیص‌تره)، بعد اسم اصلی خط
        val bestName = when {
            cleanedName.isNotBlank() && DrugKnowledgeBase.findRule(cleanedName) != null -> cleanedName
            !altName.isNullOrBlank() && DrugKnowledgeBase.findRule(altName) != null -> altName
            cleanedName.isNotBlank() -> cleanedName
            !altName.isNullOrBlank() -> altName
            else -> return null
        }
        if (bestName.isBlank()) return null

        val rule = DrugKnowledgeBase.findRule(bestName) ?: altName?.let { DrugKnowledgeBase.findRule(it) }

        val suggestedTimes = when {
            rule?.fixedIntervalHours != null -> buildFixedIntervalTimes(rule.fixedIntervalHours)
            formHint != null -> defaultTimesForForm[formHint] ?: listOf(LocalTime.of(8, 0), LocalTime.of(20, 0))
            else -> listOf(LocalTime.of(8, 0), LocalTime.of(20, 0))
        }

        return ParsedPrescriptionItem(
            rawLine = originalLine,
            name = bestName,
            formHint = formHint,
            quantity = quantity,
            suggestedDoseAmount = 1.0,
            suggestedTimesOfDay = suggestedTimes,
            recognizedRule = rule
        )
    }

    private fun buildFixedIntervalTimes(intervalHours: Int): List<LocalTime> {
        if (intervalHours <= 0) return listOf(LocalTime.of(8, 0))
        val startMinutes = 8 * 60 // شروع پیش‌فرض از ۸ صبح؛ کاربر می‌تواند در فرم تغییر دهد
        val numTimes = (24 / intervalHours).coerceAtLeast(1)
        return (0 until numTimes).map { i ->
            val m = (startMinutes + i * intervalHours * 60) % 1440
            LocalTime.of(m / 60, m % 60)
        }.distinct().sorted()
    }
}
