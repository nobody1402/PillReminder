package com.example.pillreminder.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.getSystemService

/**
 * فقط مسئول «صدا و ویبره‌ی مداوم» است، مثل یک ساعت زنگ‌دار واقعی؛ کاملاً مستقل از
 * این‌که این زنگ برای کدام دارو (یا چند دارو هم‌زمان) است. نوتیفیکیشن‌های واقعی هر
 * دارو جدا و مستقیم توسط AlarmReceiver ارسال می‌شوند تا وقتی دو دارو هم‌زمان زنگ
 * می‌خورند، هیچ‌کدام نوتیف/زنگ دیگری را حذف نکند.
 */
class AlarmRingService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var stopRunnable: Runnable? = null

    companion object {
        private const val MAX_RING_MS = 120_000L // حداکثر ۲ دقیقه زنگ می‌زند، بعدش خودکار خاموش می‌شود

        fun start(context: Context) {
            val intent = Intent(context, AlarmRingService::class.java)
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent) else context.startService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AlarmRingService::class.java))
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NotificationHelper.RINGING_FOREGROUND_ID, NotificationHelper.buildRingingForegroundNotification(this))
        beginRinging()
        armSafetyTimeout()
        return START_NOT_STICKY
    }

    private fun armSafetyTimeout() {
        stopRunnable?.let { handler.removeCallbacks(it) }
        stopRunnable = Runnable { stopSelf() }
        handler.postDelayed(stopRunnable!!, MAX_RING_MS)
    }

    private fun beginRinging() {
        stopRinging()
        try {
            val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@AlarmRingService, uri)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            // اگر پخش صدا شکست خورد، حداقل ویبره ادامه پیدا می‌کند تا کاربر متوجه شود
        }
        try {
            val vibrator = getSystemService<Vibrator>()
            val pattern = longArrayOf(0, 700, 400)
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0)) // ۰ یعنی از ابتدای الگو بی‌نهایت تکرار شود
        } catch (_: Exception) {
        }
    }

    private fun stopRinging() {
        mediaPlayer?.let {
            try { it.stop() } catch (_: Exception) {}
            try { it.release() } catch (_: Exception) {}
        }
        mediaPlayer = null
        try {
            getSystemService<Vibrator>()?.cancel()
        } catch (_: Exception) {
        }
    }

    override fun onDestroy() {
        stopRinging()
        stopRunnable?.let { handler.removeCallbacks(it) }
        super.onDestroy()
    }
}
