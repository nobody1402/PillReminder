package com.example.pillreminder.util

import com.example.pillreminder.data.FoodRelation
import java.time.LocalTime
import kotlin.math.abs

object TablePrescriptionParser {

    private fun centerX(w: OcrWord) = (w.left + w.right) / 2
    private fun centerY(w: OcrWord) = (w.top + w.bottom) / 2

    private fun clusterRows(words: List<OcrWord>): List<List<OcrWord>> {
        if (words.isEmpty()) return emptyList()
        val sorted = words.sortedBy { centerY(it) }
        val avgHeight = sorted.map { it.bottom - it.top }.average().takeIf { it > 0 } ?: 30.0
        val rows = mutableListOf<MutableList<OcrWord>>()
        
        for (w in sorted) {
            val cy = centerY(w)
            val currentRow = rows.lastOrNull()
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

    private fun isHeaderRow(row: List<OcrWord>): Boolean {
        val keywords = listOf("ردیف", "عنوان", "دارو", "تعداد", "مقادیر", "زمان", "طریقه", "مصرف")
        val texts = row.map { it.text }
        return keywords.count { kw -> texts.any { it.contains(kw) } } >= 3
    }

    private fun findColumnX(row: List<OcrWord>, keyword: String): Int? {
        return row.firstOrNull { it.text.contains(keyword) }?.let { (it.left + it.right) / 2 }
    }

    fun parse(words: List<OcrWord>): List<ParsedPrescriptionItem>? {
        if (words.size < 10) return null

        val rows = clusterRows(words)
        if (rows.size < 2) return null

        // پیدا کردن سطر هدر
        var headerIndex = -1
        for (i in rows.indices) {
            if (isHeaderRow(rows[i])) {
                headerIndex = i
                break
            }
        }
        if (headerIndex == -1) return null

        val headerRow = rows[headerIndex]
        
        // پیدا کردن موقعیت ستون‌ها - همه را nullable بگیر
        val colDrugNullable = findColumnX(headerRow, "عنوان")
        val colDrug = colDrugNullable ?: findColumnX(headerRow, "دارو")
        
        // اگر ستون دارو پیدا نشد، خروجی
        if (colDrug == null) return null
        
        // حالا colDrug از نوع Int است (غیر nullable)
        val colCount = findColumnX(headerRow, "تعداد") ?: (colDrug + 200)
        val colDose = findColumnX(headerRow, "مقادیر") ?: (colCount + 150)
        val colTime = findColumnX(headerRow, "زمان") ?: (colDose + 150)
        val colMethod = findColumnX(headerRow, "طریقه") ?: (colTime + 150)

        val items = mutableListOf<ParsedPrescriptionItem>()
        val columns = mapOf(
            "drug" to colDrug,
            "count" to colCount,
            "dose" to colDose,
            "time" to colTime,
            "method" to colMethod
        )

        for (row in rows.drop(headerIndex + 1)) {
            if (row.size < 2) continue

            // دسته‌بندی کلمات هر سطر بر اساس نزدیک‌ترین ستون
            val buckets = mutableMapOf<String, MutableList<OcrWord>>()
            for (word in row) {
                val wx = centerX(word)
                var bestKey = "other"
                var bestDist = Double.MAX_VALUE
                for ((key, x) in columns) {
                    val dist = abs(x - wx).toDouble()
                    if (dist < bestDist) {
                        bestDist = dist
                        bestKey = key
                    }
                }
                if (bestDist < 250) {
                    buckets.getOrPut(bestKey) { mutableListOf() }.add(word)
                }
            }

            fun getText(key: String): String {
                return (buckets[key] ?: emptyList()).joinToString(" ") { it.text }.trim()
            }

            val drugNameRaw = getText("drug")
            if (drugNameRaw.isEmpty() || drugNameRaw.length < 2) continue

            // پاک کردن اسم دارو از اعداد و اطلاعات اضافی
            var drugName = drugNameRaw
                .replace(Regex("\\([^)]*\\)"), "")
                .replace(Regex("\\d+\\s*mg"), "")
                .replace(Regex("\\d+\\s*[IU]"), "")
                .replace(Regex("/\\d+"), "")
                .replace(Regex("\\d+"), "")
                .trim()
            if (drugName.isEmpty()) drugName = drugNameRaw

            val countText = getText("count")
            val quantity = countText.replace(Regex("[^0-9]"), "").toIntOrNull()

            val doseText = getText("dose")
            val timeText = getText("time")
            val methodText = getText("method")

            // تشخیص ساعت‌ها
            val fullText = "$timeText $doseText"
            val times = when {
                fullText.contains("هر ۲۴ ساعت") || fullText.contains("یک بار") -> 
                    listOf(LocalTime.of(8, 0))
                fullText.contains("سه بار") -> 
                    listOf(LocalTime.of(8, 0), LocalTime.of(14, 0), LocalTime.of(20, 0))
                fullText.contains("دو بار") -> 
                    listOf(LocalTime.of(8, 0), LocalTime.of(20, 0))
                fullText.contains("هر دو هفته") || fullText.contains("طبق دستور") -> 
                    listOf(LocalTime.of(8, 0))
                else -> listOf(LocalTime.of(8, 0), LocalTime.of(20, 0))
            }

            // تشخیص رابطه با غذا
            val foodRelation = when {
                methodText.contains("معده خالی") || methodText.contains("قبل غذا") -> FoodRelation.BEFORE_FOOD
                methodText.contains("همراه غذا") -> FoodRelation.WITH_FOOD
                methodText.contains("بعد غذا") -> FoodRelation.AFTER_FOOD
                else -> FoodRelation.NO_RELATION
            }

            val note = buildString {
                append("طبق نسخه: ")
                if (doseText.isNotEmpty()) append("دوز $doseText, ")
                if (timeText.isNotEmpty()) append("زمان $timeText, ")
                if (methodText.isNotEmpty()) append("طریق $methodText")
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

        return if (items.isEmpty()) null else items
    }
}
