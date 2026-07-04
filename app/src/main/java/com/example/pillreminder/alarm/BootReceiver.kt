package com.example.pillreminder.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.pillreminder.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                // چون بعد از ری‌استارت همه آلارم‌های سیستم پاک می‌شوند،
                // برای هر قرص فعال دوباره برنامه‌ریزی می‌کنیم.
                val pills = db.pillDao().getAllActive().first()
                pills.forEach { AlarmScheduler.scheduleAllForPill(context, it) }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
