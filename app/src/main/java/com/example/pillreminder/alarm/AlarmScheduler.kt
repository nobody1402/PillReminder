package com.example.pillreminder.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.pillreminder.data.Pill
import com.example.pillreminder.util.TimeParseUtils
import android.os.Build
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

object AlarmScheduler {

    const val EXTRA_PILL_ID = "extra_pill_id"
    const val EXTRA_TIME_INDEX = "extra_time_index"
    const val EXTRA_SCHEDULED_AT = "extra_scheduled_at"

    private fun requestCode(pillId: Long, timeIndex: Int): Int =
        (pillId * 100 + timeIndex).toInt()

    private fun pendingIntent(context: Context, pillId: Long, timeIndex: Int, scheduledAt: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_PILL_ID, pillId)
            putExtra(EXTRA_TIME_INDEX, timeIndex)
            putExtra(EXTRA_SCHEDULED_AT, scheduledAt)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode(pillId, timeIndex),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun canScheduleExact(am: AlarmManager): Boolean =
        Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()

    /** آلارم یک دوز خاص را دقیقا برای یک لحظه (زمان اپوک میلی‌ثانیه) تنظیم می‌کند */
    fun scheduleExactAt(context: Context, pillId: Long, timeIndex: Int, triggerAtMillis: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(context, pillId, timeIndex, triggerAtMillis)
        if (canScheduleExact(am)) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }
    }

    /** تمام ساعات مصرف امروز/فردا (هر کدام زودتر برسد) را برای یک قرص برنامه‌ریزی می‌کند */
    fun scheduleAllForPill(context: Context, pill: Pill) {
        cancelAllForPill(context, pill)
        if (!pill.isActive) return

        val zone = ZoneId.systemDefault()
        val times = TimeParseUtils.safeParseList(pill.timesOfDay)
        val now = System.currentTimeMillis()

        times.forEachIndexed { index, time ->
            var target = LocalDate.now(zone).atTime(time).atZone(zone).toInstant().toEpochMilli()
            if (target <= now) {
                target = LocalDate.now(zone).plusDays(1).atTime(time).atZone(zone).toInstant().toEpochMilli()
            }
            // اگر دوره درمان محدود است و این زمان بعد از پایان دوره است، برنامه‌ریزی نکن
            if (pill.treatmentDurationDays != null) {
                val startMillis = LocalDate.ofEpochDay(pill.startDateEpochDay).atStartOfDay(zone).toInstant().toEpochMilli()
                val endMillis = startMillis + pill.treatmentDurationDays.toLong() * 24 * 3600 * 1000
                if (target > endMillis) return@forEachIndexed
            }
            scheduleExactAt(context, pill.id, index, target)
        }
    }

    fun cancelAllForPill(context: Context, pill: Pill) {
        val times = pill.timesOfDay.split(",").filter { it.isNotBlank() }
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        times.indices.forEach { index ->
            val intent = Intent(context, AlarmReceiver::class.java)
            val pi = PendingIntent.getBroadcast(
                context, requestCode(pill.id, index), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            am.cancel(pi)
        }
    }

    /** بعد از فعال شدن یک آلارم، همان زمان را برای فردا دوباره برنامه‌ریزی می‌کند */
    fun scheduleNextDay(context: Context, pillId: Long, timeIndex: Int, previousTriggerMillis: Long) {
        val next = previousTriggerMillis + 24 * 3600 * 1000L
        scheduleExactAt(context, pillId, timeIndex, next)
    }

    /** یادآوری ۱۰ دقیقه بعد (دکمه "⏰ ۱۰ دقیقه بعد") */
    fun scheduleSnooze(context: Context, pillId: Long, timeIndex: Int, originalScheduledAt: Long, minutes: Int = 10) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = System.currentTimeMillis() + minutes * 60 * 1000L
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_PILL_ID, pillId)
            putExtra(EXTRA_TIME_INDEX, timeIndex)
            putExtra(EXTRA_SCHEDULED_AT, originalScheduledAt) // لاگ اصلی حفظ می‌شود
            putExtra("is_snooze", true)
        }
        // requestCode متفاوت تا آلارم اصلی فردا را override نکند
        val snoozeRequestCode = requestCode(pillId, timeIndex) + 500000
        val pi = PendingIntent.getBroadcast(
            context, snoozeRequestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (canScheduleExact(am)) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    /** یک آلارم واقعی (از طریق AlarmManager) چند ثانیه دیگر برای تست کل زنجیره برنامه‌ریزی می‌کند */
    fun scheduleTestAlarm(context: Context, secondsFromNow: Int = 10) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = System.currentTimeMillis() + secondsFromNow * 1000L
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_PILL_ID, AlarmReceiver.TEST_PILL_ID)
        }
        val pi = PendingIntent.getBroadcast(
            context, 777777, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (canScheduleExact(am)) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }
}
