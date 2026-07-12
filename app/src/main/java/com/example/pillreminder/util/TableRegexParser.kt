package com.example.pillreminder.util

import com.example.pillreminder.data.FoodRelation
import java.time.LocalTime

/**
 * پارسر با Regex مخصوص فرمت جدول نسخه الکترونیک
 */
object TableRegexParser {

    fun parse(text: String): List<ParsedPrescriptionItem> {
        // نرمال‌سازی متن
        val normalizedText = text
            .replace(Regex("""\s+"""), " ")
            .trim()
        
        val items = mutableListOf<ParsedPrescriptionItem>()
        
        // ====== الگوی اصلی برای هر دارو ======
        val pattern = Regex(
            """(\d+)\s+""" +  // شماره ردیف
            """([A-Za-z0-9\s\/\(\)\[\],\-]+?)""" +  // نام دارو
            """\s+(\d+)\s+""" +  // تعداد
            """([^\d]+?)\s+""" +  // دوز (مقادیر مصرف)
            """(.+?)$"""  // زمان مصرف
        )
        
        val matches = pattern.findAll(normalizedText)
        for (match in matches) {
            val name = match.groupValues[2].trim()
            val quantity = match.groupValues[3].trim()
            val dose = match.groupValues[4].trim()
            val time = match.groupValues[5].trim()
            
            items.add(createItem(name, quantity, dose, time, ""))
        }
        
        // اگر الگوی اصلی جواب نداد، از الگوی ساده‌تر استفاده کن
        if (items.isEmpty()) {
            val simplePattern = Regex(
                """([A-Za-z\s]+?)\s+(\d+)\s+([^\d]+?)\s+(.+?)$"""
            )
            val simpleMatches = simplePattern.findAll(normalizedText)
            for (match in simpleMatches) {
                val name = match.groupValues[1].trim()
                val quantity = match.groupValues[2].trim()
                val dose = match.groupValues[3].trim()
                val time = match.groupValues[4].trim()
                
                if (name.length > 3) {
                    items.add(createItem(name, quantity, dose, time, ""))
                }
            }
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
        val name = extractDrugName(rawName)
        val quantity = quantityStr.replace(Regex("[^0-9]"), "").toIntOrNull()
        val dose = extractDose(doseStr)
        val times = extractTimes(timeStr)
        val foodRelation = detectFoodRelation(methodStr)
        val rule = DrugKnowledgeBase.findRule(name)
        
        val note = buildString {
            if (doseStr.isNotEmpty()) append("دوز: $doseStr, ")
            if (timeStr.isNotEmpty()) append("زمان: $timeStr")
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
                englishName = name,
                fixedIntervalHours = null,
                waitAfterMinutes = 0
            )
        )
    }

    private fun extractDrugName(raw: String): String {
        // حذف اطلاعات اضافی
        var cleaned = raw
            .replace(Regex("""\d+\s*mg"""), "")
            .replace(Regex("""\d+\s*\[IU]"""), "")
            .replace(Regex("""/\d+"""), "")
            .replace(Regex("""\([^)]*\)"""), "")
            .replace(Regex("""\d+"""), "")
            .replace(Regex("""TABLET|CAPSULE|LIQUID FILLED ORAL|FILM COATED"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
        
        // اگر اسم معروفی در متن هست، آن را استخراج کن
        val knownDrugs = listOf(
            "VITAMIN D", "VITAMIN B", "PENICILLIN", 
            "ANTIHISTAMINE", "DECONGESTANT", "ADULT COLD"
        )
        for (drug in knownDrugs) {
            if (raw.contains(drug, ignoreCase = true)) {
                return drug
            }
        }
        
        return if (cleaned.isEmpty() || cleaned.length < 2) {
            raw.replace(Regex("""\s+"""), " ").trim()
        } else {
            cleaned
        }
    }

    private fun extractDose(doseStr: String): Double {
        val normalized = doseStr.lowercase()
        return when {
            normalized.contains("یک عدد") || normalized.contains("یک") -> 1.0
            normalized.contains("دو عدد") || normalized.contains("دو") -> 2.0
            normalized.contains("سه عدد") || normalized.contains("سه") -> 3.0
            normalized.contains("نصف") -> 0.5
            else -> 1.0
        }
    }

    private fun extractTimes(timeStr: String): List<LocalTime> {
        val normalized = timeStr.lowercase()
        return when {
            normalized.contains("هر ۲۴ ساعت") || normalized.contains("یک بار") -> 
                listOf(LocalTime.of(8, 0))
            normalized.contains("سه بار") -> 
                listOf(LocalTime.of(8, 0), LocalTime.of(14, 0), LocalTime.of(20, 0))
            normalized.contains("دو بار") -> 
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
