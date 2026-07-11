package com.example.pillreminder.util

import java.time.LocalTime

data class ParsedPrescriptionItem(
    val rawLine: String,
    val name: String,
    val formHint: String?,
    val quantity: Int?,
    val suggestedDoseAmount: Double,
    val suggestedTimesOfDay: List<LocalTime>,
    val recognizedRule: DrugRuleSuggestion?
)

object PrescriptionParser {

    private val formWords = listOf("آمپول", "شربت", "سرم", "قرص", "کپسول", "پماد", "قطره", "اسپری", "شیاف")

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

    fun parse(rawText: String, words: List<OcrWord> = emptyList()): List<ParsedPrescriptionItem> {
        // اگر کلمات با مختصات داریم، از پارسر جدول استفاده کن
        if (words.isNotEmpty()) {
            val tableResult = TablePrescriptionParser.parse(words)
            if (tableResult != null && tableResult.isNotEmpty()) {
                return tableResult
            }
        }
        
        // در غیر این صورت از پارسر خطی معمولی استفاده کن
        return parseTextLines(rawText)
    }
    
    private fun parseTextLines(rawText: String): List<ParsedPrescriptionItem> {
        val lines = rawText
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        return lines.mapNotNull { parseLine(it) }
    }

    private fun parseLine(originalLine: String): ParsedPrescriptionItem? {
        var line = originalLine
            .replace(Regex("^[\\d۰-۹]+[\\-.\\)]\\s*"), "")
            .trim()
        if (line.length < 2) return null

        val qtyMatch = Regex("تعداد\\s*([\\d۰-۹]+)\\s*(عدد|واحد)?").find(line)
        val quantity = qtyMatch?.groupValues?.get(1)?.let {
            TimeParseUtils.normalizeDigits(it).toIntOrNull()
        }
        if (qtyMatch != null) line = line.removeRange(qtyMatch.range).trim()

        val parenMatch = Regex("\\(([^)]+)\\)").find(line)
        val altName = parenMatch?.groupValues?.get(1)?.trim()
        if (parenMatch != null) line = line.removeRange(parenMatch.range).trim()

        val formHint = formWords.firstOrNull { line.startsWith(it) }
        if (formHint != null) line = line.removePrefix(formHint).trim()

        val cleanedName = line
            .replace(Regex("[\\d۰-۹/.\\-–]+"), " ")
            .replace(Regex("میلی\\s*گرم|میلی\\s*لیتر|گرم|لیتری|نیم|cc|mg|ml|عدد|واحد"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

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
        val startMinutes = 8 * 60
        val numTimes = (24 / intervalHours).coerceAtLeast(1)
        return (0 until numTimes).map { i ->
            val m = (startMinutes + i * intervalHours * 60) % 1440
            LocalTime.of(m / 60, m % 60)
        }.distinct().sorted()
    }
}
