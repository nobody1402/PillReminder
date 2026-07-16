package com.example.pillreminder.util

import com.example.pillreminder.data.FoodRelation
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
 * Generic OCR-text prescription parser for Iranian electronic prescription images.
 *
 * Architecture:
 * 1) Normalize OCR noise/digits/Arabic variants.
 * 2) Segment text into likely medication rows using row numbers, table separators and form words.
 * 3) Extract generic slots (form, dosage, quantity, frequency, duration, meal relation) from instruction text.
 * 4) Treat remaining non-instruction tokens as the medication name.
 *
 * DrugKnowledgeBase is intentionally used only for optional display/clinical hints after parsing; it is not required
 * to recognize a medication and no drug-specific parsing branches live here.
 */
object PrescriptionParser {

    fun parse(rawText: String): List<ParsedPrescriptionItem> = segmentRows(rawText).mapNotNull { parseRow(it) }.distinctBy {
        PrescriptionLexicon.norm(it.name)
    }

    private fun segmentRows(rawText: String): List<String> {
        val normalized = rawText.lines()
            .map { PrescriptionLexicon.norm(it) }
            .filter { it.isNotBlank() }
            .filterNot { isAdministrativeLine(it) }
            .joinToString("\n")

        return normalized
            .replace(Regex("(?m)(^|\\n)\\s*[0-9]{1,2}\\s*[.)–-]\\s*"), "\n§")
            .split('\n', '§')
            .map { it.trim(' ', '|', '،', ';') }
            .filter { it.length >= 3 && looksLikeMedicationRow(it) }
    }

    private fun parseRow(row: String): ParsedPrescriptionItem? {
        var working = row
        val quantity = extractQuantity(working).also { result -> working = result.second }.first
        val form = PrescriptionLexicon.findForm(working)
        val instruction = PrescriptionInstructionParser.parse(working)
        val name = extractName(working, form) ?: return null
        val rule = DrugKnowledgeBase.findRule(name)
        val finalFoodRelation = when {
            instruction.foodRelation != FoodRelation.NO_RELATION -> instruction.foodRelation
            else -> rule?.foodRelation ?: FoodRelation.NO_RELATION
        }
        val duration = instruction.durationDays ?: PrescriptionInstructionParser.treatmentDaysFromInventory(quantity, instruction.doseAmount, instruction.times)
        val noteParts = listOfNotNull(
            instruction.note?.let { "طبق نسخه: $it" },
            "مدت درمان: ${duration ?: "نامشخص"} روز".takeIf { duration != null },
            "⚠️ زمان‌بندی نیاز به بازبینی دارد.".takeIf { instruction.needsManualTiming },
            rule?.note
        )

        return ParsedPrescriptionItem(
            rawLine = row,
            name = displayName(name, rule),
            formHint = form,
            quantity = quantity,
            suggestedDoseAmount = instruction.doseAmount,
            suggestedTimesOfDay = instruction.times,
            suggestedTreatmentDurationDays = duration,
            suggestedInventoryCount = quantity?.toDouble(),
            recognizedRule = DrugRuleSuggestion(
                foodRelation = finalFoodRelation,
                waitAfterMinutes = rule?.waitAfterMinutes ?: 0,
                fixedIntervalHours = instruction.fixedIntervalHours ?: rule?.fixedIntervalHours,
                note = noteParts.joinToString("\n"),
                englishName = rule?.englishName ?: name
            )
        )
    }

    private fun extractQuantity(text: String): Pair<Int?, String> {
        val match = Regex("(?:تعداد|عدد|qty|quantity)\\s*[:：]?\\s*([0-9]{1,4})").find(text)
            ?: Regex("(?:^|\\s)([0-9]{1,4})\\s*(?:عدد|واحد)(?:\\s|$)").find(text)
        val quantity = match?.groupValues?.get(1)?.toIntOrNull()
        val cleaned = if (match != null) text.removeRange(match.range).trim() else text
        return quantity to cleaned
    }

    private fun extractName(text: String, form: String?): String? {
        val beforeInstruction = text.split(Regex("\\b(?:هر|روزی|روزانه|صبح|ظهر|عصر|شب|قبل|بعد|همراه|ناشتا|به مدت|طبق دستور)\\b"), limit = 2).first()
        val candidate = PrescriptionLexicon.stripNonNameInstruction(beforeInstruction.ifBlank { text })
            .replace(Regex("\\b(?:تعداد|qty|quantity)\\b"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        return candidate.takeIf { it.length >= 2 && it != form }
    }

    private fun looksLikeMedicationRow(line: String): Boolean =
        PrescriptionLexicon.findForm(line) != null ||
            Regex("\\b[0-9]+\\s*(${PrescriptionLexicon.doseUnits.joinToString("|") { Regex.escape(PrescriptionLexicon.norm(it)) }})\\b").containsMatchIn(line) ||
            listOf("هر", "روزی", "روزانه", "قبل غذا", "بعد غذا", "همراه غذا", "تعداد").any { line.contains(it) }

    private fun isAdministrativeLine(line: String): Boolean = listOf("نام بیمار", "کد ملی", "پزشک", "بیمه", "تاریخ", "نسخه", "ردیف عنوان دارو").any { line.contains(it) }

    private fun displayName(name: String, rule: DrugRuleSuggestion?): String {
        val fa = DrugKnowledgeBase.persianNameFor(name)
        return if (rule != null && fa != null && !name.contains(fa, ignoreCase = true)) "$fa (${rule.englishName})" else name
    }
}
