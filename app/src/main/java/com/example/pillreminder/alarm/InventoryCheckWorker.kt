package com.example.pillreminder.alarm

import android.content.Context
import androidx.work.*
import com.example.pillreminder.data.AppDatabase
import java.util.concurrent.TimeUnit

class InventoryCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(applicationContext)
        val pills = db.pillDao().getAllWithInventoryTracking()
        pills.forEach { InventoryChecker.checkAndNotify(applicationContext, it) }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "daily_inventory_check"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<InventoryCheckWorker>(24, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request
            )
        }
    }
}
