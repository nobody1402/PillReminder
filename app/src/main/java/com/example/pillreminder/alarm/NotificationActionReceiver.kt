package com.example.pillreminder.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.pillreminder.data.AppDatabase
import com.example.pillreminder.data.DoseLog
import com.example.pillreminder.data.DoseStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pillId = intent.getLongExtra(AlarmScheduler.EXTRA_PILL_ID, -1)
        val timeIndex = intent.getIntExtra(AlarmScheduler.EXTRA_TIME_INDEX, 0)
        val scheduledAt = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, 0)
        val notifId = intent.getIntExtra("notif_id", 0)

        // در هر حالتی (هر دکمه، کشیدن نوتیف، یا دکمه‌ی توقف زنگ عمومی) همیشه اول صدا/ویبره را قطع کن،
        // فارغ از این‌که این اکشن به یک داروی خاص مربوط است یا نه.
        AlarmRingService.stop(context)

        when (intent.action) {
            NotificationHelper.ACTION_STOP_TEST -> {
                NotificationHelper.cancel(context, NotificationHelper.TEST_NOTIF_ID)
                return
            }
            NotificationHelper.ACTION_STOP_RING -> {
                // فقط زنگ سراسری را خاموش کن؛ نوتیف‌های تک‌تک داروها دست‌نخورده می‌مانند
                return
            }
            NotificationHelper.ACTION_MUTE, NotificationHelper.ACTION_DISMISS -> {
                // فقط صدا/ویبره قطع می‌شود؛ نوتیف همین دارو دست‌نخورده باقی می‌ماند تا کاربر بعداً بخواندش
                return
            }
        }

        if (pillId < 0) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val doseLogDao = db.doseLogDao()
                val pillDao = db.pillDao()
                val existing = doseLogDao.findByPillAndTime(pillId, scheduledAt)
                    ?: DoseLog(pillId = pillId, scheduledAtMillis = scheduledAt)

                when (intent.action) {
                    NotificationHelper.ACTION_TAKEN -> {
                        val updated = existing.copy(status = DoseStatus.TAKEN, actionAtMillis = System.currentTimeMillis())
                        if (existing.id == 0L) doseLogDao.insert(updated) else doseLogDao.update(updated)

                        // موجودی را کم کن و در صورت نیاز هشدار اتمام دارو بده
                        val pill = pillDao.getById(pillId)
                        if (pill != null && pill.inventoryCount != null) {
                            val newCount = (pill.inventoryCount - pill.doseAmount).coerceAtLeast(0.0)
                            pillDao.updateInventory(pillId, newCount)
                            InventoryChecker.checkAndNotify(context, pill.copy(inventoryCount = newCount))
                        }
                        NotificationHelper.cancel(context, notifId)
                    }

                    NotificationHelper.ACTION_SKIP -> {
                        val updated = existing.copy(status = DoseStatus.SKIPPED, actionAtMillis = System.currentTimeMillis())
                        if (existing.id == 0L) doseLogDao.insert(updated) else doseLogDao.update(updated)
                        NotificationHelper.cancel(context, notifId)
                    }

                    NotificationHelper.ACTION_SNOOZE -> {
                        val updated = existing.copy(status = DoseStatus.SNOOZED)
                        if (existing.id == 0L) doseLogDao.insert(updated) else doseLogDao.update(updated)
                        NotificationHelper.cancel(context, notifId)
                        AlarmScheduler.scheduleSnooze(context, pillId, timeIndex, scheduledAt)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
