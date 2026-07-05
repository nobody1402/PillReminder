package com.example.pillreminder.util

import com.example.pillreminder.data.DoseLog
import com.example.pillreminder.data.DoseStatus
import com.example.pillreminder.data.Pill
import java.time.LocalDate
import java.time.ZoneId

data class DayAdherence(
    val date: LocalDate,
    val totalDue: Int,
    val taken: Int
) {
    val percent: Int get() = if (totalDue == 0) 100 else (taken * 100) / totalDue
}

data class DailyLogEntry(
    val pillName: String,
    val timeText: String,
    val status: DoseStatus,
    val hasLog: Boolean, // false یعنی اصلاً کاری ثبت نشده (نه مصرف نه رد)
    val scheduledAtMillis: Long
)

/**
 * آمار پایبندی رو از روی داروهای فعال و لاگ‌های واقعی مصرف (dose_logs) می‌سازه — نه یک
 * عدد ساختگی. چون لاگ فقط وقتی ساخته می‌شه که کاربر روی یکی از دکمه‌های نوتیفیکیشن بزنه،
 * «نبود لاگ» برای یک دوز گذشته به‌معنی مصرف‌نشدنشه (جزو مخرج حساب می‌شه ولی جزو صورت نه).
 */
object HistoryBuilder {
    private val zone = ZoneId.systemDefault()

    /** آمار روزانه‌ی N روز اخیر (پیش‌فرض ۷ روز، شامل امروز تا همین لحظه) */
    fun buildWeekly(pills: List<Pill>, logs: List<DoseLog>, daysBack: Int = 7): List<DayAdherence> {
        val today = LocalDate.now(zone)
        val now = System.currentTimeMillis()
        val result = mutableListOf<DayAdherence>()
        for (i in (daysBack - 1) downTo 0) {
            val day = today.minusDays(i.toLong())
            var due = 0
            var taken = 0
            for (pill in pills) {
                for (time in TimeParseUtils.safeParseList(pill.timesOfDay)) {
                    val scheduledAt = day.atTime(time).atZone(zone).toInstant().toEpochMilli()
                    if (scheduledAt > now) continue // هنوز نرسیده؛ جزو آمار پایبندی حساب نشه
                    due++
                    if (logs.any { it.pillId == pill.id && it.scheduledAtMillis == scheduledAt && it.status == DoseStatus.TAKEN }) {
                        taken++
                    }
                }
            }
            result.add(DayAdherence(day, due, taken))
        }
        return result
    }

    fun overallPercent(days: List<DayAdherence>): Int {
        val totalDue = days.sumOf { it.totalDue }
        val totalTaken = days.sumOf { it.taken }
        return if (totalDue == 0) 100 else (totalTaken * 100) / totalDue
    }

    /** گزارش دارو‌به‌دارو برای یک روز مشخص (فقط دوزهایی که زمانشون رسیده) */
    fun buildDailyLog(pills: List<Pill>, logs: List<DoseLog>, day: LocalDate): List<DailyLogEntry> {
        val now = System.currentTimeMillis()
        val entries = mutableListOf<DailyLogEntry>()
        for (pill in pills) {
            for (time in TimeParseUtils.safeParseList(pill.timesOfDay)) {
                val scheduledAt = day.atTime(time).atZone(zone).toInstant().toEpochMilli()
                if (scheduledAt > now) continue
                val log = logs.find { it.pillId == pill.id && it.scheduledAtMillis == scheduledAt }
                entries.add(
                    DailyLogEntry(
                        pillName = pill.name,
                        timeText = TimeParseUtils.formatTime(time),
                        status = log?.status ?: DoseStatus.PENDING,
                        hasLog = log != null,
                        scheduledAtMillis = scheduledAt
                    )
                )
            }
        }
        return entries.sortedBy { it.scheduledAtMillis }
    }
}
