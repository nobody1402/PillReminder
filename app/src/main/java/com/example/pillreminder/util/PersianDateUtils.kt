package com.example.pillreminder.util

import android.icu.text.SimpleDateFormat
import android.icu.util.ULocale
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

/**
 * تبدیل تاریخ میلادی به شمسی، با استفاده از تقویم فارسیِ داخلی اندروید (ICU) —
 * بدون نیاز به هیچ کتابخانه‌ی خارجی یا الگوریتم دستی تبدیل.
 */
object PersianDateUtils {
    private val locale = ULocale("fa_IR@calendar=persian")

    private fun epochDayToMillis(epochDay: Long): Long =
        LocalDate.ofEpochDay(epochDay).atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    /** مثلاً «۱۵ مهر ۱۴۰۳» */
    fun formatFull(epochDay: Long): String =
        SimpleDateFormat("d MMMM yyyy", locale).format(Date(epochDayToMillis(epochDay)))

    fun formatFull(date: LocalDate): String = formatFull(date.toEpochDay())

    /** مثلاً «۱۵ مهر» — بدون سال، برای جاهایی که فشرده‌تر لازمه */
    fun formatShort(epochDay: Long): String =
        SimpleDateFormat("d MMMM", locale).format(Date(epochDayToMillis(epochDay)))
}
