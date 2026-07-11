package com.example.pillreminder.util

import com.example.pillreminder.data.FoodRelation
import java.time.LocalTime
import kotlin.math.abs

/**
 * پارسر اختصاصی برای فرمت جدولیِ «سامانه نسخه الکترونیک» (تامین اجتماعی و مشابه):
 * ستون‌های ردیف / عنوان دارو / تعداد / مقادیر مصرف / زمان مصرف / طریقه مصرف.
 */
object TablePrescriptionParser {

    private val allHeaderKeywords = listOf("ردیف", "عنوان", "دارو", "تعداد", "مقادیر", "زمان", "طریقه", "مصرف")

    private fun norm(s: String) = DrugKnowledgeBase.normalize(s)

    private fun centerX(w: OcrWord) = (w.left + w.right) / 2
    private fun centerY(w: OcrWord) = (w.top + w.bottom) / 2

    private fun clusterRows(words: List<OcrWord>): List<List<OcrWord>> {
        if (words.isEmpty()) return emptyList()
        val sorted = words.sortedBy { centerY(it) }
        
        // محاسبه ارتفاع متوسط برای تشخیص سطرها
        val avgHeight = sorted.map { it.bottom - it.top }.average().takeIf { it > 0 } ?: 30.0
        val rows = mutableListOf<MutableList<OcrWord>>()
        
        for (w in sorted) {
            val cy = centerY(w)
            val currentRow = rows.lastOrNull()
            
            // اگر سطر جدید باشد یا فاصله زیادی داشته باشد
            val fitsCurrentRow = currentRow != null &&
                abs(cy - currentRow.map { centerY(it) }.average()) < avgHeight * 0.8
            
            if (fitsCurrentRow) {
                currentRow!!.add(w)
            } else {
                rows.add(mutableListOf(w))
            }
        }
        
        return rows
    }

    private fun headerScore(row: List<OcrWord>): Int {
        val texts = row.map { norm(it.text) }
        return allHeaderKeywords.count { kw -> texts.any { it == norm(kw) || it.contains(norm(kw)) } }
    }

    private fun findWordX(row: List<OcrWord>, keyword: String): Int? =
        row.firstOrNull { norm(it.text) == norm(keyword) || norm(it.text).contains(norm(keyword)) }
            ?.let { centerX(it) }

    private fun detectColumnAnchors(headerRow: List<OcrWord>): Map<String, Int>? {
        // پیدا کردن موقعیت ستون‌ها با کلمات کلیدی
        val radif = findWordX(headerRow, "ردیف")
        val onvan = findWordX(headerRow, "عنوان")
        val darou = findWordX(headerRow, "دارو")
        val tedad = findWordX(headerRow, "تعداد")
        val meghdar = findWordX(headerRow, "مقادیر")
        val zaman = findWordX(headerRow, "زمان")
        val tarighe = findWordX(headerRow, "طریقه")

        // اگر ستون‌های کلیدی پیدا نشد، از روش جایگزین استفاده کن
        if (onvan == null && darou == null) return null

        // ستون عنوان دارو
        val drugColumn = listOfNotNull(onvan, darou).average().toInt()
        
        // ستون تعداد
        val countColumn = tedad ?: (drugColumn + 200)
        
        // ستون مقادیر مصرف
        val doseColumn = meghdar ?: (countColumn + 150)
        
        // ستون زمان مصرف
        val timeColumn = zaman ?: (doseColumn + 150)
        
        // ستون طریقه مصرف
        val methodColumn = tarighe ?: (timeColumn + 150)

        val result = mutableMapOf<String, Int>()
        radif?.let { result["ردیف"] = it }
        result["عنوان دارو"] = drugColumn
        result["تعداد"] = countColumn
        result["مقادیر مصرف"] = doseColumn
        result["زمان مصرف"] = timeColumn
        result["طریقه مصرف"] = methodColumn

        return result
    }

    private fun detectFoodRelation(methodText: String): FoodRelation {
        val n = norm(methodText)
        return when {
            n.contains(norm("معده خالی")) || n.contains(norm("قبل از غذا")) || n.contains("empty stomach") -> FoodRelation.BEFORE_FOOD
            n.contains(norm("همراه")) && n.contains(norm("غذا")) -> FoodRelation.WITH_FOOD
            n.contains(norm("بعد از غذا")) || n.contains("after meal") -> FoodRelation.AFTER_FOOD
            else -> FoodRelation.NO_RELATION
        }
    }

    private fun detectTimes(timingText: String, doseText: String): Pair<List<LocalTime>, Boolean> {
        val n = norm("$timingText $doseText")
        return when {
            n.contains("هر ۲۴ ساعت") || n.contains("هر ۲۴ ساعت") || n.contains("یک بار در روز") -> 
                listOf(LocalTime.of(8, 0)) to false
            
            n.contains("سه بار در روز") || n.contains("۳ بار") -> 
                listOf(LocalTime.of(8, 0), LocalTime.of(14, 0), LocalTime.of(20, 0)) to false
            
            n.contains("دو بار در روز") || n.contains("۲ بار") -> 
                listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)) to false
            
            n.contains("هر دو هفته") || n.contains("هر دو هفته یک بار") -> 
                listOf(LocalTime.of(8, 0)) to true
            
            n.contains("طبق دستور") || n.contains("as directed") -> 
                listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)) to true
            
            n.contains("هر 8 ساعت") || n.contains("هر ۸ ساعت") -> 
                listOf(LocalTime.of(8, 0), LocalTime.of(16, 0), LocalTime.of(0, 0)) to false
            
            n.contains("هر 6 ساعت") || n.contains("هر ۶ ساعت") -> 
                listOf(LocalTime.of(6, 0), LocalTime.of(12, 0), LocalTime.of(18, 0), LocalTime.of(0, 0)) to false
            
            else -> listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)) to true
        }
    }

    private fun extractDrugName(text: String): String {
        // حذف اعداد و اطلاعات اضافی
        var cleaned = text
            .replace(Regex("\\([^)]*\\)"), "") // حذف پرانتز
            .replace(Regex("\\d+\\s*mg"), "") // حذف میلی‌گرم
            .replace(Regex("\\d+\\s*[IU]"), "") // حذف واحدهای بین‌المللی
            .replace(Regex("/\\d+"), "") // حذف کسرها
            .replace(Regex("\\d+"), "") // حذف اعداد
            .replace(Regex("\\s+"), " ")
            .trim()
        
        // اگر اسم خالی شد، متن اصلی را برگردان
        return if (cleaned.isBlank() || cleaned.length < 2) {
            text.replace(Regex("\\s+"), " ").trim()
        } else {
            cleaned
        }
    }

    fun looksLikeTable(words: List<OcrWord>): Boolean {
        if (words.size < 10) return false
        val rows = clusterRows(words)
        // جستجوی کلمات کلیدی در سطرهای اول
        val firstRows = rows.take(3)
        return firstRows.any { headerScore(it) >= 2 }
    }

    fun parse(words: List<OcrWord>): List<ParsedPrescriptionItem>? {
        val rows = clusterRows(words)
        if (rows.size < 2) return null

        // پیدا کردن سطر هدر
        val headerRowIndex = rows.indices.maxByOrNull { headerScore(rows[it]) } ?: return null
        if (headerScore(rows[headerRowIndex]) < 2) return null

        val anchors = detectColumnAnchors(rows[headerRowIndex]) ?: return null
        val drugColumnX = anchors["عنوان دارو"] ?: return null

        val items = mutableListOf<ParsedPrescriptionItem>()
        
        for (row in rows.drop(headerRowIndex + 1)) {
            if (row.size < 2) continue

            val buckets = mutableMapOf<String, MutableList<OcrWord>>()
            for (word in row) {
                val wx = centerX(word)
                var nearestColumn = "سایر"
                var minDist = Double.MAX_VALUE
                
                for ((key, x) in anchors) {
                    val dist = abs(x - wx).toDouble()
                    if (dist < minDist) {
                        minDist = dist
                        nearestColumn = key
                    }
                }
                
                // اگر فاصله خیلی زیاد بود، نادیده بگیر
                if (minDist < 300) {
                    buckets.getOrPut(nearestColumn) { mutableListOf() }.add(word)
                }
            }

            fun colText(key: String): String =
                (buckets[key] ?: emptyList())
                    .sortedBy { it.left }
                    .joinToString(" ") { it.text }

            // استخراج اطلاعات از هر ستون
            val drugNameRaw = colText("عنوان دارو").trim()
            if (drugNameRaw.isBlank() || drugNameRaw.length < 2) continue
            
            val drugName = extractDrugName(drugNameRaw)
            
            val quantityText = colText("تعداد")
            val quantity = TimeParseUtils.normalizeDigits(quantityText)
                .filter { it.isDigit() }
                .toIntOrNull()

            val doseText = colText("مقادیر مصرف")
            val timingText = colText("زمان مصرف")
            val methodText = colText("طریقه مصرف")

            // تشخیص رابطه با غذا
            val foodRelation = detectFoodRelation(methodText)
            
            // تشخیص ساعت‌ها
            val (times, needsManual) = detectTimes(timingText, doseText)

            // ساخت یادداشت
            val note = buildString {
                append("طبق نسخه: ")
                if (doseText.isNotBlank()) append("دوز $doseText, ")
                if (timingText.isNotBlank()) append("زمان $timingText, ")
                if (methodText.isNotBlank()) append("طریق $methodText")
                if (needsManual) {
                    append("\n⚠️ نیاز به تنظیم دستی ساعت‌ها دارد")
                }
            }

            items.add(
                ParsedPrescriptionItem(
                    rawLine = drugNameRaw,
                    name = drugName,
                    formHint = null,
                    quantity = quantity,
                    suggestedDoseAmount = 1.0,
                    suggestedTimesOfDay = times,
                    recognizedRule = DrugRuleSuggestion(
                        foodRelation = foodRelation,
                        note = note.trimEnd(',', ' '),
                        englishName = drugName,
                        fixedIntervalHours = null,
                        waitAfterMinutes = null
                    )
                )
            )
        }
        
        return items.ifEmpty { null }
    }
}

// اضافه کردن کلاس DrugRuleSuggestion اگر وجود ندارد
data class DrugRuleSuggestion(
    val foodRelation: FoodRelation? = null,
    val note: String = "",
    val englishName: String? = null,
    val fixedIntervalHours: Int? = null,
    val waitAfterMinutes: Int? = null
)
