// PrescriptionParser.kt
package com.example.pillreminder.util

import java.time.LocalTime

data class PrescriptionItem(
    val name: String,
    val quantity: Int? = null,
    val formHint: String? = null,
    val suggestedTimesOfDay: List<LocalTime> = listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)),
    val suggestedDoseAmount: Double = 1.0,
    val recognizedRule: DrugRuleSuggestion? = null
)

object PrescriptionParser {
    
    fun parse(text: String): List<PrescriptionItem> {
        if (text.isBlank()) return emptyList()
        
        val lines = text.split("\n").filter { it.isNotBlank() }
        val items = mutableListOf<PrescriptionItem>()
        
        // الگوهای تشخیص داروها
        val drugPatterns = listOf(
            Regex("""(ADULT COLD PREPARATIONS|VITAMIN D3|VITAMIN B|PENICILLIN|ANTIHISTAMINE|DECONGESTANT)""", RegexOption.IGNORE_CASE),
            Regex("""(\d+\s*mg|\d+\s*[IU])""", RegexOption.IGNORE_CASE),
            Regex("""(یک عدد|دو عدد|سه بار در روز|هر ۲۴ ساعت|طبق دستور)""")
        )
        
        var currentDrug = ""
        var currentQuantity = ""
        
        for (line in lines) {
            val trimmed = line.trim()
            
            // تشخیص داروها با الگوهای مشخص
            when {
                // داروهای شناسایی شده در لیست
                drugPatterns[0].containsMatchIn(trimmed) -> {
                    if (currentDrug.isNotBlank()) {
                        // داروی قبلی را ذخیره کن
                        val item = createDrugItem(currentDrug, currentQuantity)
                        if (item != null) items.add(item)
                    }
                    currentDrug = trimmed
                    currentQuantity = ""
                }
                // تشخیص مقدار
                drugPatterns[1].containsMatchIn(trimmed) || 
                trimmed.contains("مقدار") || 
                trimmed.contains("تعداد") -> {
                    currentQuantity = trimmed
                }
                // اطلاعات اضافی را نادیده بگیر
                else -> {
                    // اگر خط جدیدی شروع شده که به نظر دارو می‌رسد
                    if (trimmed.length > 3 && !trimmed.contains("ردیف") && !trimmed.contains("عنوان")) {
                        // احتمالاً این هم یک دارو است
                        if (currentDrug.isNotBlank()) {
                            val item = createDrugItem(currentDrug, currentQuantity)
                            if (item != null) items.add(item)
                        }
                        currentDrug = trimmed
                        currentQuantity = ""
                    }
                }
            }
        }
        
        // آخرین دارو را اضافه کن
        if (currentDrug.isNotBlank()) {
            val item = createDrugItem(currentDrug, currentQuantity)
            if (item != null) items.add(item)
        }
        
        return items.distinctBy { it.name }.take(10) // حداکثر 10 دارو
    }
    
    private fun createDrugItem(name: String, quantity: String): PrescriptionItem? {
        if (name.length < 3) return null
        
        // تشخیص ساعت مصرف بر اساس متن
        val times = when {
            name.contains("هر ۲۴ ساعت", ignoreCase = true) || 
            name.contains("یک بار در روز", ignoreCase = true) -> 
                listOf(LocalTime.of(8, 0))
            
            name.contains("سه بار در روز", ignoreCase = true) -> 
                listOf(LocalTime.of(8, 0), LocalTime.of(14, 0), LocalTime.of(20, 0))
            
            name.contains("هر دو هفته", ignoreCase = true) -> 
                listOf(LocalTime.of(8, 0))
            
            else -> listOf(LocalTime.of(8, 0), LocalTime.of(20, 0))
        }
        
        // تشخیص مقدار مصرف
        val dose = when {
            quantity.contains("یک عدد", ignoreCase = true) -> 1.0
            quantity.contains("دو عدد", ignoreCase = true) -> 2.0
            quantity.contains("نصف", ignoreCase = true) -> 0.5
            else -> 1.0
        }
        
        // تشخیص رابطه با غذا
        val rule = DrugKnowledgeBase.findRule(name)
        
        return PrescriptionItem(
            name = name.replace(Regex("""\d+\s*mg|\d+\s*[IU]|\([^)]*\)"""), "").trim(),
            quantity = quantity.toIntOrNull(),
            suggestedTimesOfDay = times,
            suggestedDoseAmount = dose,
            recognizedRule = rule
        )
    }
}
