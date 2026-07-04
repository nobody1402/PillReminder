package com.example.pillreminder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * آخرین تنظیماتی که کاربر برای یک نام دارو (نرمال‌شده) ثبت کرده است، تا دفعه بعد که
 * همین دارو دوباره تایپ شد (مثلا یک سال دیگر، یک دوره جدید استامینوفن)، لازم نباشد
 * جزییات (دوز، ساعت‌ها، رابطه با غذا و ...) دوباره از صفر وارد شود.
 */
@Entity(tableName = "drug_history")
data class DrugHistory(
    @PrimaryKey val normalizedName: String,
    val displayName: String,
    val doseAmount: Double,
    val foodRelation: FoodRelation,
    val waitAfterMinutes: Int,
    val timesOfDay: String,
    val treatmentDurationDays: Int?,
    val lowStockThresholdDays: Int,
    val lastUsedEpochMillis: Long
)
