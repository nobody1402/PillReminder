package com.example.pillreminder.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.pillreminder.data.AppDatabase
import com.example.pillreminder.data.DoseLog
import com.example.pillreminder.data.DoseStatus
import com.example.pillreminder.util.SettingsStore
import com.example.pillreminder.util.TtsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val TEST_PILL_ID = -999L
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pillId = intent.getLongExtra(AlarmScheduler.EXTRA_PILL_ID, -1)
        val timeIndex = intent.getIntExtra(AlarmScheduler.EXTRA_TIME_INDEX, 0)
        val scheduledAt = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, System.currentTimeMillis())
        val isSnooze = intent.getBooleanExtra("is_snooze", false)

        // مسیر تست: برای بررسی این‌که کل زنجیره (AlarmManager -> Receiver -> صدا + نوتیف) کار می‌کند یا نه
        if (pillId == TEST_PILL_ID) {
            try {
                AlarmRingService.start(context)
                NotificationHelper.showTest(context)
            } catch (e: Throwable) {
                NotificationHelper.showError(context, e)
            }
            return
        }
        if (pillId < 0) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val pill = db.pillDao().getById(pillId)
                if (pill != null && pill.isActive) {
                    // اگر برای این دوز هنوز رکوردی ثبت نشده، به عنوان PENDING بساز
                    val existing = db.doseLogDao().findByPillAndTime(pillId, scheduledAt)
                    if (existing == null) {
                        db.doseLogDao().insert(DoseLog(pillId = pillId, scheduledAtMillis = scheduledAt, status = DoseStatus.PENDING))
                    }

                    val elderlyMode = SettingsStore.isElderlyMode(context)

                    // این دو مورد کاملاً مستقل از هم هستند: حتی اگر چند دارو دقیقاً هم‌زمان زنگ بخورند،
                    // هر کدام نوتیف مخصوص به خودش را می‌گیرد؛ فقط صدا/ویبره مشترک است.
                    AlarmRingService.start(context)
                    NotificationHelper.show(context, pill, timeIndex, scheduledAt, elderlyMode)

                    if (elderlyMode) {
                        TtsHelper.speakReminder(context, pill.name)
                    }

                    // فقط در فراخوانی اصلی (نه در فراخوانی ناشی از دکمه "۱۰ دقیقه بعد")
                    // آلارم فردا را برنامه‌ریزی کن
                    if (!isSnooze) {
                        AlarmScheduler.scheduleNextDay(context, pillId, timeIndex, scheduledAt)
                    }
                }
            } catch (e: Throwable) {
                NotificationHelper.showError(context, e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
