package com.example.pillreminder.util

import com.example.pillreminder.data.FoodRelation
import java.time.LocalTime

object TableRegexParser {

    fun parse(text: String): List<ParsedPrescriptionItem> {
        val items = mutableListOf<ParsedPrescriptionItem>()
        
        // خطوط را جدا کن
        val lines = text.split("\n").map { it.trim() }.filter { it.length > 5 }
        
        for (line in lines) {
            // الگو: عدد + اسم دارو + عدد + توضیحات
            val pattern = Regex("""(\d+)\s+([A-Za-z\s\(\)\/\-]+?)\s+(\d+)\s+(.+)$""")
            val match = pattern.find(line)
            
            if (match != null) {
                val name = match.groupValues[2].trim()
                val quantity = match.groupValues[3].trim()
                val rest = match.groupValues[4].trim()
                
                // تشخیص زمان مصرف
                val time = when {
                    rest.contains("three", ignoreCase = true) || rest.contains("3", ignoreCase = true) -> "سه بار در روز"
                    rest.contains("two", ignoreCase = true) || rest.contains("2", ignoreCase = true) -> "دو بار در روز"
                    rest.contains("24", ignoreCase = true) -> "هر ۲۴ ساعت"
                    rest.contains("week", ignoreCase = true) -> "هر دو هفته یک بار"
                    rest.contains("directed", ignoreCase = true) || rest.contains("دستور") -> "طبق دستور"
                    else -> "طبق دستور"
                }
                
                // تشخیص دوز
                val dose = when {
                    rest.contains("one", ignoreCase = true) || rest.contains("یک", ignoreCase = true) -> 1.0
                    rest.contains("two", ignoreCase = true) || rest.contains("دو", ignoreCase = true) -> 2.0
                    else -> 1.0
                }
                
                // استخراج نام اصلی دارو
                val cleanName = extractDrugName(name)
                
                items.add(
                    ParsedPrescriptionItem(
                        rawLine = line,
                        name = cleanName,
                        formHint = null,
                        quantity = quantity.toIntOrNull(),
                        suggestedDoseAmount = dose,
                        suggestedTimesOfDay = extractTimes(time),
                        recognizedRule = DrugRuleSuggestion(
                            foodRelation = FoodRelation.NO_RELATION,
                            note = "طبق نسخه: $rest",
                            englishName = cleanName,
                            fixedIntervalHours = null,
                            waitAfterMinutes = 0
                        )
                    )
                )
            }
        }
        
        return items.distinctBy { it.name }
    }

    private fun extractDrugName(raw: String): String {
        // حذف اطلاعات اضافی
        var cleaned = raw
            .replace(Regex("""\d+\s*mg"""), "")
            .replace(Regex("""\d+\s*\[?IU\]?"""), "")
            .replace(Regex("""/\d+"""), "")
            .replace(Regex("""\([^)]*\)"""), "")
            .replace(Regex("""\d+"""), "")
            .replace(Regex("""TABLET|CAPSULE|LIQUID FILLED ORAL"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
        
        // اگر خالی شد، متن اصلی را با حذف اعداد برگردان
        if (cleaned.isEmpty() || cleaned.length < 2) {
            cleaned = raw.replace(Regex("""\d+"""), "").trim()
        }
        
        return cleaned
    }

    private fun extractTimes(timeStr: String): List<LocalTime> {
        return when {
            timeStr.contains("هر ۲۴ ساعت") || timeStr.contains("یک بار") -> 
                listOf(LocalTime.of(8, 0))
            timeStr.contains("سه بار") -> 
                listOf(LocalTime.of(8, 0), LocalTime.of(14, 0), LocalTime.of(20, 0))
            timeStr.contains("دو بار") -> 
                listOf(LocalTime.of(8, 0), LocalTime.of(20, 0))
            timeStr.contains("هر دو هفته") -> 
                listOf(LocalTime.of(8, 0))
            else -> listOf(LocalTime.of(8, 0), LocalTime.of(20, 0))
        }
    }
}
