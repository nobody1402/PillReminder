package com.example.pillreminder.alarm

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.example.pillreminder.data.Pill

object InventoryChecker {

    /**
     * تقریب تعداد روزهای باقی‌مانده = موجودی تقسیم بر مجموع دوز روزانه.
     * اگر کمتر یا مساوی آستانه هشدار (پیش‌فرض ۳ روز) شد، اعلان بده.
     */
    fun checkAndNotify(context: Context, pill: Pill) {
        val inventory = pill.inventoryCount ?: return
        val dailyDoseCount = pill.timesOfDay.split(",").count { it.isNotBlank() }
        if (dailyDoseCount == 0) return

        val dailyUsage = pill.doseAmount * dailyDoseCount
        if (dailyUsage <= 0) return

        val daysRemaining = (inventory / dailyUsage)
        if (daysRemaining <= pill.lowStockThresholdDays) {
            val roundedDays = kotlin.math.floor(daysRemaining).toInt().coerceAtLeast(0)
            NotificationHelper.ensureChannel(context)
            val text = if (roundedDays <= 0)
                "داروی «${pill.name}» تمام شده یا برای امروز کافی نیست."
            else
                "فقط برای $roundedDays روز دیگر داروی «${pill.name}» دارید."

            val builder = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("⚠️ موجودی دارو کم است")
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            // شناسه ثابت بر اساس pillId تا هر بار اعلان تکراری روی هم بازنویسی شود نه انباشته
            nm.notify(("inv_" + pill.id).hashCode(), builder.build())
        }
    }
}
