package com.example.pillreminder.util

import com.example.pillreminder.data.FoodRelation
import java.time.LocalTime
import kotlin.math.ceil

/** Shared, drug-agnostic vocabulary for Iranian e-prescription OCR parsing. */
object PrescriptionLexicon {
    val formWords = listOf(
        "قرص", "کپسول", "شربت", "سوسپانسیون", "قطره", "اسپری", "آمپول", "ویال", "سرنگ",
        "پماد", "کرم", "ژل", "شیاف", "سرم", "محلول", "ساشه", "پودر", "لوسیون",
        "tablet", "tab", "capsule", "cap", "syrup", "drop", "spray", "ampoule", "amp",
        "vial", "ointment", "cream", "suppository", "solution", "sachet"
    )

    val doseUnits = listOf(
        "میلی گرم", "میلی‌گرم", "میکروگرم", "گرم", "واحد", "یونیت", "میلی لیتر", "میلی‌لیتر",
        "قطره", "پاف", "قاشق", "سی سی", "درصد", "mg", "mcg", "g", "ml", "cc", "iu", "%"
    )

    private val routeWords = listOf("خوراکی", "تزریقی", "موضعی", "چشمی", "گوشی", "بینی", "استنشاقی", "واژینال", "رکتال")
    private val adminWords = listOf("مصرف", "شود", "گردد", "میل", "تزریق", "چکانده", "استعمال", "مالیده", "هر", "روزی", "روزانه", "بار")

    fun norm(value: String): String = TimeParseUtils.normalizeDigits(value)
        .trim()
        .lowercase()
        .replace("ي", "ی")
        .replace("ك", "ک")
        .replace("‌", " ")
        .replace("：", ":")
        .replace(Regex("\\s+"), " ")
        .trim()

    fun findForm(text: String): String? {
        val n = norm(text)
        return formWords.firstOrNull { Regex("(^|\\s)${Regex.escape(norm(it))}(\\s|$)").containsMatchIn(n) }
    }

    fun stripNonNameInstruction(text: String): String {
        var result = norm(text)
        val unitPattern = doseUnits.joinToString("|") { Regex.escape(norm(it)) }
        result = result.replace(Regex("\\b[0-9]+(?:[./][0-9]+)?\\s*(?:$unitPattern)?\\b"), " ")
        (formWords + routeWords + adminWords + doseUnits).forEach { word ->
            result = result.replace(Regex("(^|\\s)${Regex.escape(norm(word))}(?=\\s|$)"), " ")
        }
        return result.replace(Regex("[،,:;|()\\[\\]\\-–]+"), " ").replace(Regex("\\s+"), " ").trim()
    }
}

data class PrescriptionInstruction(
    val doseAmount: Double = 1.0,
    val dosageText: String? = null,
    val times: List<LocalTime> = emptyList(),
    val fixedIntervalHours: Int? = null,
    val durationDays: Int? = null,
    val foodRelation: FoodRelation = FoodRelation.NO_RELATION,
    val needsManualTiming: Boolean = false,
    val note: String? = null
)

/** Parses dosage, frequency, duration and meal relation without looking at the drug identity. */
object PrescriptionInstructionParser {
    fun parse(vararg parts: String): PrescriptionInstruction {
        val original = parts.filter { it.isNotBlank() }.joinToString(" ")
        val n = PrescriptionLexicon.norm(original)
        val dose = detectDose(n)
        val interval = Regex("هر\\s*([0-9]{1,2})\\s*ساعت").find(n)?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it in 1..24 }
        val dailyCount = interval?.let { 24 / it } ?: detectDailyCount(n)
        val times = when {
            hasClockTimes(n) -> clockTimes(n)
            interval != null -> timesEvery(interval)
            dailyCount != null -> timesForDailyCount(dailyCount)
            containsPartOfDay(n) -> timesForPartsOfDay(n)
            else -> emptyList()
        }
        val manual = n.contains("طبق دستور") || n.contains("در صورت نیاز") || n.contains("هفته") || (times.isEmpty() && n.isNotBlank())
        return PrescriptionInstruction(
            doseAmount = dose.first,
            dosageText = dose.second,
            times = times.ifEmpty { listOf(LocalTime.of(9, 0)) },
            fixedIntervalHours = interval,
            durationDays = detectDurationDays(n),
            foodRelation = detectFoodRelation(n),
            needsManualTiming = manual,
            note = original.takeIf { it.isNotBlank() }
        )
    }

    fun treatmentDaysFromInventory(quantity: Int?, doseAmount: Double, times: List<LocalTime>): Int? {
        if (quantity == null || quantity <= 0 || doseAmount <= 0.0 || times.isEmpty()) return null
        return ceil(quantity / (doseAmount * times.size)).toInt().coerceAtLeast(1)
    }

    private fun detectDose(n: String): Pair<Double, String?> {
        val fractional = when {
            n.contains("نصف") || n.contains("1/2") -> 0.5
            n.contains("ربع") || n.contains("1/4") -> 0.25
            n.contains("دو") && (n.contains("عدد") || n.contains("قرص") || n.contains("کپسول")) -> 2.0
            else -> null
        }
        val amount = Regex("([0-9]+(?:\\.[0-9]+)?|[0-9]+/[0-9]+)\\s*(${PrescriptionLexicon.doseUnits.joinToString("|") { Regex.escape(PrescriptionLexicon.norm(it)) }})?").find(n)
        val numeric = amount?.groupValues?.get(1)?.let { token ->
            if (token.contains('/')) token.split('/').let { it[0].toDoubleOrNull()?.div(it[1].toDoubleOrNull() ?: 1.0) } else token.toDoubleOrNull()
        }
        val value = fractional ?: numeric ?: 1.0
        return value to amount?.value
    }

    private fun detectDailyCount(n: String): Int? = when {
        n.contains("سه بار") || n.contains("روزی سه") || n.contains("روزانه سه") -> 3
        n.contains("دو بار") || n.contains("روزی دو") || n.contains("روزانه دو") -> 2
        n.contains("یک بار") || n.contains("روزی یک") || n.contains("روزانه یک") -> 1
        n.contains("چهار بار") || n.contains("روزی چهار") -> 4
        else -> Regex("(?:روزی|روزانه)\\s*([1-6])\\s*بار?").find(n)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun detectDurationDays(n: String): Int? = Regex("(?:به مدت|مدت|تا)\\s*([0-9]{1,3})\\s*(روز|هفته|ماه)").find(n)?.let {
        val count = it.groupValues[1].toIntOrNull() ?: return@let null
        when (it.groupValues[2]) { "هفته" -> count * 7; "ماه" -> count * 30; else -> count }
    }

    private fun detectFoodRelation(n: String): FoodRelation = when {
        n.contains("معده خالی") || n.contains("ناشتا") || n.contains("قبل غذا") || n.contains("قبل از غذا") -> FoodRelation.BEFORE_FOOD
        n.contains("همراه غذا") || n.contains("حین غذا") || n.contains("با غذا") -> FoodRelation.WITH_FOOD
        n.contains("بعد غذا") || n.contains("بعد از غذا") || n.contains("پس از غذا") -> FoodRelation.AFTER_FOOD
        else -> FoodRelation.NO_RELATION
    }

    private fun hasClockTimes(n: String) = Regex("(?<!\\d)([01]?\\d|2[0-3])\\s*:\\s*([0-5]?\\d)(?!\\d)").containsMatchIn(n)
    private fun clockTimes(n: String) = Regex("(?<!\\d)([01]?\\d|2[0-3])\\s*:\\s*([0-5]?\\d)(?!\\d)").findAll(n).mapNotNull { TimeParseUtils.safeParse("${it.groupValues[1]}:${it.groupValues[2]}") }.distinct().sorted().toList()
    private fun containsPartOfDay(n: String) = listOf("صبح", "ظهر", "عصر", "شب").any { n.contains(it) }
    private fun timesForPartsOfDay(n: String) = listOfNotNull(LocalTime.of(8, 0).takeIf { n.contains("صبح") }, LocalTime.of(14, 0).takeIf { n.contains("ظهر") }, LocalTime.of(18, 0).takeIf { n.contains("عصر") }, LocalTime.of(20, 0).takeIf { n.contains("شب") })
    private fun timesForDailyCount(count: Int) = when (count) { 1 -> listOf(LocalTime.of(8, 0)); 2 -> timesEvery(12); 3 -> timesEvery(8); 4 -> timesEvery(6); 5, 6 -> timesEvery(24 / count); else -> listOf(LocalTime.of(9, 0)) }
    private fun timesEvery(intervalHours: Int) = (0 until (24 / intervalHours).coerceAtLeast(1)).map { LocalTime.of(8, 0).plusHours((it * intervalHours).toLong()) }.distinct().sorted()
}
