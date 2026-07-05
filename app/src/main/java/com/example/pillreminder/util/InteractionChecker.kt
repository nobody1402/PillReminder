package com.example.pillreminder.util

import com.example.pillreminder.data.InteractionRule
import com.example.pillreminder.data.Pill
import java.time.LocalTime
import kotlin.math.abs

data class InteractionWarning(val message: String)

object InteractionChecker {

    private fun parseTimes(csv: String): List<LocalTime> = TimeParseUtils.safeParseList(csv)

    /**
     * فاصله دو ساعت را با در نظر گرفتن چرخه ۲۴ ساعته حساب می‌کند
     * (مثلا ۲۳:۰۰ و ۰۱:۰۰ فقط ۲ ساعت فاصله دارند، نه ۲۲).
     */
    private fun minutesBetween(a: LocalTime, b: LocalTime): Int {
        val diff = abs(a.toSecondOfDay() - b.toSecondOfDay()) / 60
        return minOf(diff, 24 * 60 - diff)
    }

    /**
     * بررسی می‌کند آیا زمان‌بندی جدید pill با قرص‌هایی که با آن قاعده تداخل دارند
     * فاصله کافی را رعایت می‌کند یا نه. اگر نه، پیام هشدار برمی‌گرداند.
     */
    fun checkPill(
        pill: Pill,
        allPills: List<Pill>,
        rulesForPill: List<InteractionRule>
    ): List<InteractionWarning> {
        val warnings = mutableListOf<InteractionWarning>()
        val myTimes = parseTimes(pill.timesOfDay)

        for (rule in rulesForPill) {
            val otherId = if (rule.pillAId == pill.id) rule.pillBId else rule.pillAId
            val other = allPills.find { it.id == otherId } ?: continue
            val otherTimes = parseTimes(other.timesOfDay)

            for (t1 in myTimes) {
                for (t2 in otherTimes) {
                    val gap = minutesBetween(t1, t2)
                    if (gap < rule.minGapMinutes) {
                        val hours = rule.minGapMinutes / 60.0
                        warnings.add(
                            InteractionWarning(
                                "بین ${pill.name} و ${other.name} باید حداقل " +
                                    (if (hours == hours.toInt().toDouble()) "${hours.toInt()}" else "$hours") +
                                    " ساعت فاصله باشد."
                            )
                        )
                    }
                }
            }
        }
        return warnings
    }
}
