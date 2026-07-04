package com.example.pillreminder.util

import java.time.LocalTime

data class QuickEntryResult(
    val name: String?,
    val intervalHours: Int?,
    val doseAmount: Double?,
    val startTime: LocalTime?,
    val inventoryCount: Double?,
    val timesOfDay: List<LocalTime>,
    val missingFields: List<String>
)

/**
 * جمله‌هایی مثل «استامینوفن هر هشت ساعت دوتا شروع از هشت صبح موجودی سی قرص» را
 * بدون نیاز به هوش مصنوعی یا اینترنت، صرفاً با تشخیص کلیدواژه تجزیه می‌کند.
 */
object QuickEntryParser {

    private val onesMap = mapOf(
        "صفر" to 0, "یک" to 1, "دو" to 2, "سه" to 3, "چهار" to 4, "پنج" to 5,
        "شش" to 6, "هفت" to 7, "هشت" to 8, "نه" to 9
    )
    private val teensMap = mapOf(
        "ده" to 10, "یازده" to 11, "دوازده" to 12, "سیزده" to 13, "چهارده" to 14,
        "پانزده" to 15, "شانزده" to 16, "هفده" to 17, "هجده" to 18, "نوزده" to 19
    )
    private val tensMap = mapOf(
        "بیست" to 20, "سی" to 30, "چهل" to 40, "پنجاه" to 50,
        "شصت" to 60, "هفتاد" to 70, "هشتاد" to 80, "نود" to 90
    )
    private val hundredsMap = mapOf(
        "صد" to 100, "دویست" to 200, "سیصد" to 300, "چهارصد" to 400, "پانصد" to 500,
        "ششصد" to 600, "هفتصد" to 700, "هشتصد" to 800, "نهصد" to 900
    )

    fun parsePersianNumber(raw: String): Int? {
        val normalized = TimeParseUtils.normalizeDigits(raw.trim())
        Regex("\\d+").find(normalized)?.let { return it.value.toIntOrNull() }

        val words = normalized.split(Regex("\\s+")).filter { it.isNotBlank() }
        var total = 0
        var found = false
        for (w in words) {
            when {
                hundredsMap.containsKey(w) -> { total += hundredsMap.getValue(w); found = true }
                teensMap.containsKey(w) -> { total += teensMap.getValue(w); found = true }
                tensMap.containsKey(w) -> { total += tensMap.getValue(w); found = true }
                onesMap.containsKey(w) -> { total += onesMap.getValue(w); found = true }
            }
        }
        return if (found) total else null
    }

    fun parse(text: String): QuickEntryResult {
        val missing = mutableListOf<String>()
        val t = text.trim()

        // نام دارو: هر چی قبل از "هر " بیاد
        val herIndex = t.indexOf("هر ")
        val name = if (herIndex > 0) t.substring(0, herIndex).trim() else null
        if (name.isNullOrBlank()) missing.add("نام دارو")

        // فاصله‌ی زمانی: بین "هر" و "ساعت"
        var intervalHours: Int? = null
        if (herIndex >= 0) {
            val afterHer = t.substring(herIndex + 3)
            val saatIndex = afterHer.indexOf("ساعت")
            if (saatIndex >= 0) {
                intervalHours = parsePersianNumber(afterHer.substring(0, saatIndex))
            }
        }
        if (intervalHours == null) missing.add("فاصله زمانی (هر چند ساعت)")

        // دوز: بین "ساعت" و "شروع"
        val saatGlobalIndex = t.indexOf("ساعت")
        val shorooIndex = t.indexOf("شروع")
        val doseSegment = if (saatGlobalIndex >= 0) {
            val end = if (shorooIndex > saatGlobalIndex) shorooIndex else t.length
            t.substring((saatGlobalIndex + 4).coerceAtMost(t.length), end).trim()
        } else ""
        val doseAmount = parseDose(doseSegment)
        if (doseAmount == null) missing.add("میزان مصرف")

        // ساعت شروع: بین "شروع" و "موجودی"
        var startTime: LocalTime? = null
        val moojudiIndex = t.indexOf("موجودی")
        if (shorooIndex >= 0) {
            val end = if (moojudiIndex > shorooIndex) moojudiIndex else t.length
            startTime = parseStartTime(t.substring(shorooIndex, end))
        }
        if (startTime == null) missing.add("ساعت شروع")

        // موجودی: بعد از "موجودی" (اختیاری است، فیلد missing نمی‌سازیم)
        var inventory: Double? = null
        if (moojudiIndex >= 0) {
            val invSegment = t.substring((moojudiIndex + 6).coerceAtMost(t.length))
            inventory = parsePersianNumber(invSegment)?.toDouble()
        }

        val times = if (intervalHours != null && intervalHours > 0 && startTime != null) {
            val numTimes = Math.round(24.0 / intervalHours).toInt().coerceAtLeast(1)
            val startMinutes = startTime.hour * 60 + startTime.minute
            (0 until numTimes).map { i ->
                val m = (startMinutes + i * intervalHours * 60) % 1440
                LocalTime.of(m / 60, m % 60)
            }.distinct().sorted()
        } else emptyList()

        return QuickEntryResult(name, intervalHours, doseAmount, startTime, inventory, times, missing)
    }

    private fun parseDose(segment: String): Double? {
        val s = segment.trim()
        if (s.isEmpty()) return null
        return when {
            s.contains("نصف") -> 0.5
            s.contains("یک‌چهارم") || s.contains("یک چهارم") || s.contains("ربع") -> 0.25
            s.contains("سه‌چهارم") || s.contains("سه چهارم") -> 0.75
            s.contains("یکی") || Regex("(^|\\s)یک(\\s|$)").containsMatchIn(s) -> 1.0
            s.contains("دوتا") || s.contains("دو تا") || Regex("(^|\\s)دو(\\s|$)").containsMatchIn(s) -> 2.0
            s.contains("سه‌تا") || s.contains("سه تا") -> 3.0
            else -> parsePersianNumber(s)?.toDouble()
        }
    }

    private fun parseStartTime(segment: String): LocalTime? {
        TimeParseUtils.safeParse(segment)?.let { return it }

        val normalized = TimeParseUtils.normalizeDigits(segment)
        var hour = parsePersianNumber(normalized) ?: return null
        val isMorning = normalized.contains("صبح")
        val isNoon = normalized.contains("ظهر")
        val isAfternoon = normalized.contains("عصر") || normalized.contains("بعدازظهر")
        val isNight = normalized.contains("شب")

        hour = when {
            isNoon -> 12
            isAfternoon -> if (hour in 1..11) hour + 12 else hour
            isNight -> if (hour in 1..11) hour + 12 else if (hour == 12) 0 else hour
            isMorning -> if (hour == 12) 0 else hour
            else -> hour
        }
        if (hour !in 0..23) return null
        return LocalTime.of(hour, 0)
    }
}
