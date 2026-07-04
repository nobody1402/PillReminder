package com.example.pillreminder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DoseStatus { PENDING, TAKEN, SKIPPED, SNOOZED }

@Entity(tableName = "dose_logs")
data class DoseLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pillId: Long,
    // زمان برنامه‌ریزی‌شده اصلی، به میلی‌ثانیه epoch
    val scheduledAtMillis: Long,
    val status: DoseStatus = DoseStatus.PENDING,
    // زمانی که واقعا مصرف/رد شد
    val actionAtMillis: Long? = null
)
