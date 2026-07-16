package com.example.pillreminder.util

import com.example.pillreminder.data.FoodRelation
import java.time.LocalTime

data class ParsedPrescriptionItem(
    val rawLine: String,
    val name: String,
    val formHint: String?,
    val quantity: Int?,
    val suggestedDoseAmount: Double,
    val suggestedTimesOfDay: List<LocalTime>,
    val recognizedRule: DrugRuleSuggestion?,
    val suggestedTreatmentDurationDays: Int? = null,
    val suggestedInventoryCount: Double? = quantity?.toDouble(),
    val strengthText: String? = null,
    val usageInstructionText: String? = null,
    val foodRelationText: String? = null
) {
    fun readableSummary(): String = listOf(
        "نام دارو: $name",
        "دوز: ${strengthText ?: UNKNOWN_TEXT}",
        "فرم: ${formHint ?: UNKNOWN_TEXT}",
        "تعداد: ${quantity?.toString() ?: UNKNOWN_TEXT}",
        "دستور: ${usageInstructionText ?: UNKNOWN_TEXT}",
        "همراه غذا: ${foodRelationText ?: foodText(recognizedRule?.foodRelation ?: FoodRelation.NO_RELATION)}"
    ).joinToString("\n")

    companion object {
        private const val UNKNOWN_TEXT = "نامشخص"

        fun foodText(relation: FoodRelation): String = when (relation) {
            FoodRelation.WITH_FOOD -> "بله"
            FoodRelation.BEFORE_FOOD -> "قبل غذا"
            FoodRelation.AFTER_FOOD -> "بعد غذا"
            FoodRelation.NO_RELATION -> "اطلاعی ثبت نشده"
        }
    }
}

/**
 * Offline, drug-agnostic prescription extraction pipeline.
 *
 * The important architectural point is that Iranian e-prescription OCR is usually a table whose visual columns are
 * emitted in mixed order. Therefore the fallback text parser does not assume one OCR line equals one medicine. It first
 * identifies medicine-title lines by pharmaceutical structure (Latin/Persian title + strength/form tokens), then attaches
 * neighboring non-title table cells as quantity/amount/frequency/food-route slots. No branch depends on a specific drug.
 */
object PrescriptionParser {

    fun parse(rawText: String): List<ParsedPrescriptionItem> = parseMixedTableText(rawText).ifEmpty {
        segmentRows(rawText).mapNotNull { parseLinearRow(it) }
    }.distinctBy { PrescriptionLexicon.norm(it.name) }

    private data class TextRecord(val title: String, val cells: List<String>)
    private data class DrugTitleParts(val name: String, val strength: String?, val form: String?)

    private fun parseMixedTableText(rawText: String): List<ParsedPrescriptionItem> {
        val lines = rawText.lines().map { PrescriptionLexicon.norm(it) }.filter { it.isNotBlank() }.filterNot { isAdministrativeLine(it) }
        val titleIndexes = lines.indices.filter { looksLikeDrugTitle(lines[it]) }
        if (titleIndexes.isEmpty()) return emptyList()
        return titleIndexes.mapIndexedNotNull { idx, titleIndex ->
            val previousTitle = titleIndexes.getOrNull(idx - 1) ?: -1
            val cells = lines.subList(previousTitle + 1, titleIndex).filterNot { isRowNumber(it) || looksLikeDrugTitle(it) }
            parseRecord(TextRecord(lines[titleIndex], cells))
        }
    }

    private fun parseRecord(record: TextRecord): ParsedPrescriptionItem? {
        val title = splitDrugTitle(record.title) ?: return null
        val quantity = record.cells.asReversed().firstNotNullOfOrNull { standaloneQuantity(it) }
        val instructionCells = record.cells.filterNot { standaloneQuantity(it) != null }
        val instruction = PrescriptionInstructionParser.parse(*instructionCells.toTypedArray())
        val food = instruction.foodRelation
        val usage = buildUsageText(instructionCells, instruction)
        val rule = DrugKnowledgeBase.findRule(title.name)
        val finalFood = if (food == FoodRelation.NO_RELATION) rule?.foodRelation ?: food else food
        val duration = instruction.durationDays ?: PrescriptionInstructionParser.treatmentDaysFromInventory(quantity, instruction.doseAmount, instruction.times)
        return ParsedPrescriptionItem(
            rawLine = (record.cells + record.title).joinToString(" | "),
            name = title.name,
            formHint = PrescriptionLexicon.displayForm(title.form),
            quantity = quantity,
            suggestedDoseAmount = instruction.doseAmount,
            suggestedTimesOfDay = instruction.times,
            suggestedTreatmentDurationDays = duration,
            suggestedInventoryCount = quantity?.toDouble(),
            strengthText = title.strength,
            usageInstructionText = usage,
            foodRelationText = ParsedPrescriptionItem.foodText(finalFood),
            recognizedRule = DrugRuleSuggestion(
                foodRelation = finalFood,
                waitAfterMinutes = rule?.waitAfterMinutes ?: 0,
                fixedIntervalHours = instruction.fixedIntervalHours ?: rule?.fixedIntervalHours,
                note = "طبق نسخه: ${instructionCells.joinToString("، ")}".takeIf { instructionCells.isNotEmpty() } ?: rule?.note.orEmpty(),
                englishName = rule?.englishName ?: title.name
            )
        )
    }

    private fun segmentRows(rawText: String): List<String> = rawText.lines().map { PrescriptionLexicon.norm(it) }.filter { it.isNotBlank() }.filterNot { isAdministrativeLine(it) }

    private fun parseLinearRow(row: String): ParsedPrescriptionItem? = parseRecord(TextRecord(row, emptyList()))

    private fun looksLikeDrugTitle(line: String): Boolean {
        val hasForm = PrescriptionLexicon.findForm(line) != null
        val hasDoseUnit = Regex("[0-9][0-9./\\s-]*(${PrescriptionLexicon.doseUnits.joinToString("|") { Regex.escape(PrescriptionLexicon.norm(it)) }})").containsMatchIn(line)
        val hasLatin = Regex("[a-z]").containsMatchIn(line)
        val notInstruction = PrescriptionInstructionParser.parse(line).foodRelation == FoodRelation.NO_RELATION && !listOf("هر", "روزی", "طبق دستور", "قبل", "بعد", "همراه", "عضلانی").any { line.contains(it) }
        return (hasForm || hasDoseUnit) && (hasLatin || notInstruction)
    }

    private fun splitDrugTitle(line: String): DrugTitleParts? {
        val form = PrescriptionLexicon.findForm(line)
        val unitPattern = PrescriptionLexicon.doseUnits.joinToString("|") { Regex.escape(PrescriptionLexicon.norm(it)) } + "|\\[?u\\]?"
        val strengthPattern = "[0-9]+(?:[./-][0-9]+)*(?:\\s*/\\s*[0-9]+)*\\s*(?:$unitPattern)(?:\\s*/\\s*[0-9]+\\s*(?:$unitPattern))*"
        val strength = Regex(strengthPattern).find(line)?.value?.replace(Regex("\\s+"), " ")
        var name = line
        strength?.let { name = name.replace(it, " ") }
        form?.let { name = name.replace(Regex("(^|\\s)${Regex.escape(PrescriptionLexicon.norm(it))}(\\s|$)"), " ") }
        name = name.replace(Regex("\\([^)]*\\)|\\[[^]]*]"), " ").replace(Regex("[,،].*"), " ").replace(Regex("\\s+"), " ").trim(' ', '/', '-', ',')
        return name.takeIf { it.length >= 2 }?.let { DrugTitleParts(it.uppercase(), strength, form) }
    }

    private fun standaloneQuantity(line: String): Int? = TimeParseUtils.normalizeDigits(line).let { n ->
        Regex("^(?:تعداد\\s*)?([0-9]{1,4})(?:\\s*(?:عدد|واحد))?$").find(n)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun isRowNumber(line: String): Boolean = Regex("^[0-9]{1,2}[-.)]?$|^ردیف$").matches(TimeParseUtils.normalizeDigits(line))

    private fun buildUsageText(cells: List<String>, instruction: PrescriptionInstruction): String? {
        val doseCell = cells.firstOrNull { Regex("یک|دو|سه|نصف|ربع|[0-9]").containsMatchIn(it) && (it.contains("عدد") || it.contains("قرص") || it.contains("کپسول")) }
        val freqCell = cells.firstOrNull { listOf("هر", "روزی", "روزانه", "بار", "طبق دستور").any(it::contains) }
        return listOfNotNull(freqCell, doseCell).distinct().joinToString("، ").ifBlank { instruction.note }
    }

    private fun isAdministrativeLine(line: String): Boolean = listOf("نام بیمار", "کد ملی", "پزشک", "بیمه", "تاریخ", "نسخه", "ردیف عنوان دارو").any { line.contains(it) }
}
