package com.example.pillreminder.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.example.pillreminder.data.Pill
import com.example.pillreminder.ui.MainActivity
import com.example.pillreminder.util.PillTextFormatter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object NotificationHelper {

    // نکته: چون تنظیمات کانال بعد از ساخته‌شدن دیگه با کد عوض نمیشه، هر بار که صدا/رفتار
    // کانال رو عوض می‌کنیم باید آیدی رو عوض کنیم تا اندروید مجبور بشه کانال رو از نو بسازه.
    const val CHANNEL_ID = "pill_reminders_alarm_v2"
    private const val OLD_CHANNEL_ID = "pill_reminders"
    const val ACTION_TAKEN = "com.example.pillreminder.ACTION_TAKEN"
    const val ACTION_SNOOZE = "com.example.pillreminder.ACTION_SNOOZE"
    const val ACTION_SKIP = "com.example.pillreminder.ACTION_SKIP"
    const val ACTION_MUTE = "com.example.pillreminder.ACTION_MUTE"
    const val ACTION_DISMISS = "com.example.pillreminder.ACTION_DISMISS"
    const val ACTION_STOP_TEST = "com.example.pillreminder.ACTION_STOP_TEST"
    const val ACTION_STOP_RING = "com.example.pillreminder.ACTION_STOP_RING"

    const val TEST_NOTIF_ID = 999999
    const val RINGING_FOREGROUND_ID = 555555

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // کانال قدیمی (با صدای پیش‌فرض نوتیفیکیشن) را حذف کن تا در تنظیمات تکراری نماند
        if (nm.getNotificationChannel(OLD_CHANNEL_ID) != null) {
            nm.deleteNotificationChannel(OLD_CHANNEL_ID)
        }

        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val alarmSound = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID, "یادآوری مصرف دارو", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "آلارم‌های یادآوری زمان مصرف دارو"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500, 250, 500)
                setSound(alarmSound, audioAttributes)
                setBypassDnd(false)
            }
            nm.createNotificationChannel(channel)
        }
    }

    private fun actionIntent(
        context: Context, action: String, pillId: Long, timeIndex: Int, scheduledAt: Long, notifId: Int
    ): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(AlarmScheduler.EXTRA_PILL_ID, pillId)
            putExtra(AlarmScheduler.EXTRA_TIME_INDEX, timeIndex)
            putExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, scheduledAt)
            putExtra("notif_id", notifId)
        }
        val requestCode = "$action$pillId$timeIndex$scheduledAt".hashCode()
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** یک PendingIntent عمومی برای دکمه‌های «توقف زنگ» که به داروی خاصی وابسته نیستند (تست/نوتیفیکیشن سرویس) */
    private fun genericStopIntent(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
        }
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun notifId(pillId: Long, timeIndex: Int, scheduledAt: Long): Int =
        "$pillId-$timeIndex-$scheduledAt".hashCode()

    /** باز کردن اپ با لمس نوتیفیکیشن، و همچنین برای نمایش فوری روی صفحه (حتی وقتی قفل/خاموش است) */
    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context, 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** نوتیفیکیشن دوز را می‌سازد و مستقیم ارسال می‌کند؛ کاملاً مستقل از سرویس زنگ است تا چند دارو هم‌زمان هر کدام نوتیف جدا بگیرند */
    fun buildDoseNotification(context: Context, pill: Pill, timeIndex: Int, scheduledAtMillis: Long, elderlyMode: Boolean): Pair<Int, android.app.Notification> {
        ensureChannel(context)

        val zone = ZoneId.systemDefault()
        val timeText = Instant.ofEpochMilli(scheduledAtMillis).atZone(zone)
            .format(DateTimeFormatter.ofPattern("HH:mm"))

        val doseText = PillTextFormatter.doseAmountText(pill.doseAmount)
        val foodText = PillTextFormatter.foodRelationText(pill.foodRelation, pill.waitAfterMinutes)

        val bodyLines = mutableListOf("🔔 ساعت $timeText", pill.name, "• $doseText")
        if (foodText.isNotBlank()) bodyLines.add("• $foodText")

        val bigText = bodyLines.joinToString("\n")
        val id = notifId(pill.id, timeIndex, scheduledAtMillis)

        val takenPi = actionIntent(context, ACTION_TAKEN, pill.id, timeIndex, scheduledAtMillis, id)
        val snoozePi = actionIntent(context, ACTION_SNOOZE, pill.id, timeIndex, scheduledAtMillis, id)
        val skipPi = actionIntent(context, ACTION_SKIP, pill.id, timeIndex, scheduledAtMillis, id)
        val mutePi = actionIntent(context, ACTION_MUTE, pill.id, timeIndex, scheduledAtMillis, id)
        val dismissPi = actionIntent(context, ACTION_DISMISS, pill.id, timeIndex, scheduledAtMillis, id)
        val openPi = openAppIntent(context)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("⏰ وقت داروی «${pill.name}»")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setContentText(bodyLines.joinToString(" • "))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setOngoing(elderlyMode) // در حالت ساده، تا لمس نشود کنار نمی‌رود
            .setContentIntent(openPi)
            .setFullScreenIntent(openPi, true) // تا حتی روی صفحه قفل/خاموش هم بلافاصله ظاهر شود
            .setDeleteIntent(dismissPi)
            .addAction(0, "✅ مصرف کردم", takenPi)
            .addAction(0, "⏰ ۱۰ دقیقه بعد", snoozePi)
            .addAction(0, "🔇 قطع صدا", mutePi)
            .addAction(0, "❌ رد شد", skipPi)

        pill.photoUri?.let { uri ->
            runCatching {
                val bmp = BitmapFactory.decodeFile(uri)
                if (bmp != null) {
                    builder.setLargeIcon(bmp)
                    builder.setStyle(
                        NotificationCompat.BigPictureStyle()
                            .bigPicture(bmp)
                            .setSummaryText(bigText)
                    )
                }
            }
        }

        return id to builder.build()
    }

    fun show(context: Context, pill: Pill, timeIndex: Int, scheduledAtMillis: Long, elderlyMode: Boolean) {
        val (id, notification) = buildDoseNotification(context, pill, timeIndex, scheduledAtMillis, elderlyMode)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(id, notification)
    }

    /** نوتیفیکیشن عمومیِ خودِ سرویس زنگ (برای الزام startForeground)، مستقل از داروی خاص */
    fun buildRingingForegroundNotification(context: Context): android.app.Notification {
        ensureChannel(context)
        val stopPi = genericStopIntent(context, ACTION_STOP_RING, 700001)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🔔 یادآور دارو در حال زنگ زدن است")
            .setContentText("برای جزئیات، نوتیفیکیشن زیر را ببین")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setContentIntent(openAppIntent(context))
            .addAction(0, "🔇 قطع صدا", stopPi)
            .build()
    }

    fun buildTestNotification(context: Context): android.app.Notification {
        ensureChannel(context)
        val stopPi = genericStopIntent(context, ACTION_STOP_TEST, 700002)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("✅ تست آلارم موفق بود")
            .setContentText("اگه این نوتیفیکیشن رو می‌بینی، آلارم‌های واقعی هم درست کار می‌کنن.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(openAppIntent(context))
            .setFullScreenIntent(openAppIntent(context), true)
            .setDeleteIntent(genericStopIntent(context, ACTION_STOP_TEST, 700003))
            .setAutoCancel(true)
            .addAction(0, "⏹ خاموش کردن زنگ", stopPi)
            .build()
    }

    fun showTest(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(TEST_NOTIF_ID, buildTestNotification(context))
    }

    fun showError(context: Context, e: Throwable) {
        try {
            ensureChannel(context)
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("❌ خطا در آلارم")
                .setStyle(NotificationCompat.BigTextStyle().bigText("${e.javaClass.simpleName}: ${e.message}"))
                .setContentText("${e.javaClass.simpleName}: ${e.message}")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(999998, builder.build())
        } catch (ignored: Throwable) {
            // اگر حتی نمایش خطا هم شکست بخورد، دیگر کاری نمی‌توان کرد؛ از کرش کردن جلوگیری می‌کنیم
        }
    }

    fun cancel(context: Context, id: Int) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(id)
    }
}
