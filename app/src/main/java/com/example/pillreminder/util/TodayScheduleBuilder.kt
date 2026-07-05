package com.example.pillreminder.util

import com.example.pillreminder.data.DoseLog
import com.example.pillreminder.data.DoseStatus
import com.example.pillreminder.data.Pill
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class TodayDoseItem(
    val pillId: Long,
    val pillName: String,
    val timeText: String,
    val scheduledAtMillis: Long,
    val doseText: String,
    val status: DoseStatus,
    val isOverdue: Boolean
)

object TodayScheduleBuilder {

    fun build(pills: List<Pill>, logsToday: List<DoseLog>): List<TodayDoseItem> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val now = System.currentTimeMillis()
        val items = mutableListOf<TodayDoseItem>()

        for (pill in pills) {
            val times = TimeParseUtils.safeParseList(pill.timesOfDay)
            for (time in times) {
                val scheduledAt = today.atTime(time).atZone(zone).toInstant().toEpochMilli()
                val log = logsToday.find { it.pillId == pill.id && it.scheduledAtMillis == scheduledAt }
                val status = log?.status ?: DoseStatus.PENDING
                items.add(
                    TodayDoseItem(
                        pillId = pill.id,
                        pillName = pill.name,
                        timeText = TimeParseUtils.formatTime(time),
                        scheduledAtMillis = scheduledAt,
                        doseText = PillTextFormatter.doseAmountText(pill.doseAmount),
                        status = status,
                        isOverdue = status == DoseStatus.PENDING && scheduledAt < now
                    )
                )
            }
        }
        return items.sortedBy { it.scheduledAtMillis }
    }
}
