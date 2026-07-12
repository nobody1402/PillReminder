package com.example.pillreminder.util

import com.example.pillreminder.data.FoodRelation
import java.time.LocalTime

object TableRegexParser {

    fun parse(text: String): List<ParsedPrescriptionItem> {
        val items = mutableListOf<ParsedPrescriptionItem>()
        
        val lines = text.split("\n").map { it.trim() }.filter { it.length > 5 }
        
        for (line in lines) {
            val pattern = Regex("""(\d+)\s+(.+?)\s+(\d+)\s+(.+?)\s+(.+?)$""")
            val match = pattern.find(line)
            
            if (match != null) {
                val name = match.groupValues[2].trim()
                val quantity = match.groupValues[3].trim()
                val dose = match.groupValues[4].trim()
                val time = match.groupValues[5].trim()
                
                items.add(createItem(name, quantity, dose, time, ""))
            }
        }
        
        return items.distinctBy { it.name }
    }

    private fun createItem(
        rawName: String,
        quantityStr: String,
        doseStr: String,
        timeStr: String,
        methodStr: String
    ): ParsedPrescriptionItem {
        val englishName = extractEnglishName(rawName)
        val persianName = findPersianName(englishName)
        val quantity = quantityStr.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
        val timesPerDay = detectTimesPerDay(timeStr)
        val times = buildTimes(timesPerDay)
        val foodRelation = detectFoodRelation(timeStr, doseStr)
        
        val treatmentDays = if (quantity > 0 && timesPerDay > 0) {
            (quantity / timesPerDay).takeIf { it > 0 }
        } else {
            null
        }
        
        val displayName = if (persianName != null) {
            "$persianName ($englishName)"
        } else {
            englishName
        }

        return ParsedPrescriptionItem(
            rawLine = rawName,
            name = displayName,
            formHint = null,
            quantity = quantity,
            suggestedDoseAmount = 1.0,
            suggestedTimesOfDay = times,
            recognizedRule = DrugRuleSuggestion(
                foodRelation = foodRelation,
                note = "تعداد روزهای درمان: ${treatmentDays ?: "نامحدود"} روز",
                englishName = englishName,
                fixedIntervalHours = if (timesPerDay > 0) 24 / timesPerDay else null,
                waitAfterMinutes = 0
            )
        )
    }

    private fun extractEnglishName(raw: String): String {
        val knownDrugs = listOf(
            "ADULT COLD PREPARATIONS",
            "VITAMIN D3", 
            "VITAMIN B1", 
            "VITAMIN B6", 
            "VITAMIN B12",
            "PENICILLIN", 
            "ANTIHISTAMINE", 
            "DECONGESTANT"
        )
        for (drug in knownDrugs) {
            if (raw.contains(drug, ignoreCase = true)) {
                return drug
            }
        }
        return raw
            .replace(Regex("""\d+\s*mg"""), "")
            .replace(Regex("""\d+\s*\[?IU\]?"""), "")
            .replace(Regex("""/\d+"""), "")
            .replace(Regex("""\([^)]*\)"""), "")
            .replace(Regex("""\d+"""), "")
            .replace(Regex("""TABLET|CAPSULE|LIQUID FILLED ORAL"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun findPersianName(englishName: String): String? {
        val map = mapOf(
            "ADULT COLD PREPARATIONS" to "سرماخوردگی بزرگسالان",
            "VITAMIN D3" to "ویتامین دی ۳",
            "VITAMIN B1" to "ویتامین ب ۱",
            "VITAMIN B6" to "ویتامین ب ۶",
            "VITAMIN B12" to "ویتامین ب ۱۲",
            "PENICILLIN" to "پنی‌سیلین",
            "ANTIHISTAMINE" to "آنتی‌هیستامین",
            "DECONGESTANT" to "ضداحتقان"
        )
        return map[englishName] ?: map.keys.firstOrNull { englishName.contains(it, ignoreCase = true) }?.let { map[it] }
    }

    private fun detectTimesPerDay(timeStr: String): Int {
        return when {
            timeStr.contains("سه بار") -> 3
            timeStr.contains("دو بار") -> 2
            timeStr.contains("هر ۲۴ ساعت") || timeStr.contains("یک بار") -> 1
            timeStr.contains("هر دو هفته") -> 0
            timeStr.contains("طبق دستور") -> 2
            else -> 2
        }
    }

    private fun buildTimes(timesPerDay: Int): List<LocalTime> {
        return when (timesPerDay) {
            3 -> listOf(LocalTime.of(8, 0), LocalTime.of(14, 0), LocalTime.of(20, 0))
            2 -> listOf(LocalTime.of(8, 0), LocalTime.of(20, 0))
            1 -> listOf(LocalTime.of(8, 0))
            0 -> listOf(LocalTime.of(8, 0))
            else -> listOf(LocalTime.of(8, 0), LocalTime.of(20, 0))
        }
    }

    private fun detectFoodRelation(timeStr: String, doseStr: String): FoodRelation {
        val fullText = "$timeStr $doseStr"
        return when {
            fullText.contains("معده خالی") || fullText.contains("قبل غذا") -> FoodRelation.BEFORE_FOOD
            fullText.contains("همراه غذا") -> FoodRelation.WITH_FOOD
            fullText.contains("بعد غذا") -> FoodRelation.AFTER_FOOD
            else -> FoodRelation.NO_RELATION
        }
    }
}
