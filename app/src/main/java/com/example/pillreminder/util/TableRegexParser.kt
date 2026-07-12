package com.example.pillreminder.util

import com.example.pillreminder.data.FoodRelation
import java.time.LocalTime

/**
 * پارسر با Regex مخصوص فرمت جدول نسخه الکترونیک
 * این پارسر از متن خام OCR استفاده می‌کند و با الگوهای Regex داروها را استخراج می‌کند
 */
object TableRegexParser {

    fun parse(text: String): List<ParsedPrescriptionItem> {
        val lines = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        val items = mutableListOf<ParsedPrescriptionItem>()
        
        var currentDrug = ""
        var currentQuantity = ""
        var currentDose = ""
        var currentTime = ""
        var currentMethod = ""
        var isInTable = false
        var foundHeader = false

        for (line in lines) {
            // تشخیص هدر جدول
            if (line.contains("ردیف") && line.contains("عنوان") && line.contains("دارو")) {
                foundHeader = true
                isInTable = true
                continue
            }

            if (!foundHeader) continue

            // خطوط خالی را رد کن
            if (line.length < 3) continue

            // ========== الگوهای تشخیص دارو ==========

            // الگوی 1: ردیف + نام دارو + تعداد + دوز + زمان + طریق
            // مثال: "۱ ADULT COLD PREPARATIONS (4-4) 325 mg/10 mg/25 mg TABLET ۲۰ -یک عدد سه بار در روز"
            val fullPattern = Regex("""^(\d+)\s+(.+?)\s+(\d+)\s+(.+?)\s+(.+?)$""")
            val fullMatch = fullPattern.find(line)
            if (fullMatch != null) {
                val name = fullMatch.groupValues[2].trim()
                val quantity = fullMatch.groupValues[3].trim()
                val dose = fullMatch.groupValues[4].trim()
                val time = fullMatch.groupValues[5].trim()
                
                items.add(createItem(name, quantity, dose, time, ""))
                continue
            }

            // الگوی 2: نام دارو + تعداد + دوز + زمان
            // مثال: "VITAMIN D3 50000 [IU] CAPSULE, LIQUID FILLED ORAL ۲۰ -یک عدد هر دو هفته یک بار"
            val pattern2 = Regex("""^(.+?)\s+(\d+)\s+(.+?)\s+(.+?)$""")
            val match2 = pattern2.find(line)
            if (match2 != null) {
                val name = match2.groupValues[1].trim()
                val quantity = match2.groupValues[2].trim()
                val dose = match2.groupValues[3].trim()
                val time = match2.groupValues[4].trim()
                
                items.add(createItem(name, quantity, dose, time, ""))
                continue
            }

            // الگوی 3: فقط نام دارو + مقدار (برای موارد ساده)
            val pattern3 = Regex("""^(.+?)\s+(\d+)\s+(.+?)$""")
            val match3 = pattern3.find(line)
            if (match3 != null) {
                val name = match3.groupValues[1].trim()
                val quantity = match3.groupValues[2].trim()
                val rest = match3.groupValues[3].trim()
                
                // تشخیص اینکه rest شامل دوز است یا زمان
                if (rest.contains("دستور") || rest.contains("ساعت") || rest.contains("بار")) {
                    items.add(createItem(name, quantity, "", rest, ""))
                } else {
                    items.add(createItem(name, quantity, rest, "", ""))
                }
                continue
            }

            // اگر خط حاوی نام دارو بود ولی الگوها قبول نکردند، ذخیره کن برای ادامه
            if (line.contains("TABLET") || line.contains("CAPSULE") || 
                line.contains("mg") || line.contains("IU") ||
                line.contains("VITAMIN") || line.contains("PENICILLIN") ||
                line.contains("ANTIHISTAMINE")) {
                
                // اگر قبلاً دارویی داشتیم، آن را ذخیره کن
                if (currentDrug.isNotEmpty()) {
                    items.add(createItem(currentDrug, currentQuantity, currentDose, currentTime, currentMethod))
                }
                
                // شروع داروی جدید
                currentDrug = line
                currentQuantity = ""
                currentDose = ""
                currentTime = ""
                currentMethod = ""
            } else if (currentDrug.isNotEmpty()) {
                // اطلاعات اضافی به داروی فعلی اضافه کن
                when {
                    line.matches(Regex("""\d+""")) -> currentQuantity = line
                    line.contains("دستور") -> currentTime = line
                    line.contains("بار") -> currentTime = line
                    line.contains("ساعت") -> currentTime = line
                    line.contains("عدد") -> currentDose = line
                    else -> currentDrug += " $line"
                }
            }
        }

        // آخرین دارو را اضافه کن
        if (currentDrug.isNotEmpty()) {
            items.add(createItem(currentDrug, currentQuantity, currentDose, currentTime, currentMethod))
        }

        // حذف موارد تکراری
        return items.distinctBy { extractDrugName(it.name) }
    }

    private fun createItem(
        rawName: String,
        quantityStr: String,
        doseStr: String,
        timeStr: String,
        methodStr: String
    ): ParsedPrescriptionItem {
        // استخراج نام دارو
        val name = extractDrugName(rawName)
        
        // تعداد
        val quantity = quantityStr.replace(Regex("[^0-9]"), "").toIntOrNull()
        
        // دوز
        val dose = extractDose(doseStr)
        
        // زمان مصرف
        val times = extractTimes(timeStr)
        
        // رابطه با غذا
        val foodRelation = detectFoodRelation(methodStr)
        
        // قانون تشخیص
        val rule = DrugKnowledgeBase.findRule(name)
        
        // متن توضیح
        val note = buildString {
            if (doseStr.isNotEmpty()) append("دوز: $doseStr, ")
            if (timeStr.isNotEmpty()) append("زمان: $timeStr, ")
            if (methodStr.isNotEmpty()) append("طریق: $methodStr")
        }.trimEnd(',', ' ')

        return ParsedPrescriptionItem(
            rawLine = rawName,
            name = name,
            formHint = null,
            quantity = quantity,
            suggestedDoseAmount = dose,
            suggestedTimesOfDay = times,
            recognizedRule = DrugRuleSuggestion(
                foodRelation = foodRelation,
                note = note.ifEmpty { "طبق نسخه" },
                englishName = name
            )
        )
    }

    private fun extractDrugName(raw: String): String {
        // حذف اعداد و واحدها
        var cleaned = raw
            .replace(Regex("""\d+\s*mg"""), "")
            .replace(Regex("""\d+\s*\[IU]"""), "")
            .replace(Regex("""/\d+"""), "")
            .replace(Regex("""\([^)]*\)"""), "")
            .replace(Regex("""\d+"""), "")
            .replace(Regex("""TABLET|CAPSULE|LIQUID FILLED ORAL"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
        
        // اگر خالی شد، متن اصلی را برگردان
        return if (cleaned.isEmpty() || cleaned.length < 2) {
            raw.replace(Regex("""\s+"""), " ").trim()
        } else {
            cleaned
        }
    }

    private fun extractDose(doseStr: String): Double {
        val normalized = doseStr.lowercase()
        return when {
            normalized.contains("یک عدد") -> 1.0
            normalized.contains("دو عدد") -> 2.0
            normalized.contains("سه عدد") -> 3.0
            normalized.contains("نصف") -> 0.5
            normalized.contains("یک‌چهارم") -> 0.25
            else -> 1.0
        }
    }

    private fun extractTimes(timeStr: String): List<LocalTime> {
        val normalized = timeStr.lowercase()
        return when {
            normalized.contains("هر ۲۴ ساعت") || normalized.contains("یک بار در روز") -> 
                listOf(LocalTime.of(8, 0))
            normalized.contains("سه بار در روز") -> 
                listOf(LocalTime.of(8, 0), LocalTime.of(14, 0), LocalTime.of(20, 0))
            normalized.contains("دو بار در روز") -> 
                listOf(LocalTime.of(8, 0), LocalTime.of(20, 0))
            normalized.contains("هر دو هفته") -> 
                listOf(LocalTime.of(8, 0))
            normalized.contains("طبق دستور") -> 
                listOf(LocalTime.of(8, 0), LocalTime.of(20, 0))
            else -> listOf(LocalTime.of(8, 0), LocalTime.of(20, 0))
        }
    }

    private fun detectFoodRelation(methodStr: String): FoodRelation {
        val normalized = methodStr.lowercase()
        return when {
            normalized.contains("معده خالی") || normalized.contains("قبل غذا") -> FoodRelation.BEFORE_FOOD
            normalized.contains("همراه غذا") -> FoodRelation.WITH_FOOD
            normalized.contains("بعد غذا") -> FoodRelation.AFTER_FOOD
            else -> FoodRelation.NO_RELATION
        }
    }
}
