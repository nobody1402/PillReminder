package com.example.pillreminder.util

import java.time.LocalTime

object TimeParseUtils {

    private val persianDigits = "۰۱۲۳۴۵۶۷۸۹"
    private val arabicDigits = "٠١٢٣٤٥٦٧٨٩"

    /** ارقام فارسی/عربی را به ارقام انگلیسی تبدیل می‌کند تا LocalTime.parse بتواند آن‌ها را بخواند */
    fun normalizeDigits(input: String): String {
        val sb = StringBuilder()
        for (c in input) {
            val pIndex = persianDigits.indexOf(c)
            val aIndex = arabicDigits.indexOf(c)
            when {
                pIndex >= 0 -> sb.append(pIndex)
                aIndex >= 0 -> sb.append(aIndex)
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    /**
     * پارس امن یک ساعت به فرمت HH:mm. ارقام فارسی را نرمال می‌کند، تک‌رقمی‌ها را صفر می‌گذارد
     * (مثلا "8:0" -> "08:00")، و در صورت نامعتبر بودن null برمی‌گرداند (کرش نمی‌کند).
     */
    fun safeParse(raw: String): LocalTime? {
        val normalized = normalizeDigits(raw.trim())
        if (normalized.isBlank()) return null
        val parts = normalized.split(":")
        if (parts.size < 2) return null
        val hour = parts[0].trim().toIntOrNull() ?: return null
        val minute = parts[1].trim().toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return runCatching { LocalTime.of(hour, minute) }.getOrNull()
    }

    /** یک رشته‌ی جدا شده با کاما را به لیست ساعات معتبر تبدیل می‌کند و مقادیر خراب را نادیده می‌گیرد */
    fun safeParseList(csv: String): List<LocalTime> =
        csv.split(",").mapNotNull { safeParse(it) }

    fun formatTime(time: LocalTime): String =
        "%02d:%02d".format(time.hour, time.minute)
}
