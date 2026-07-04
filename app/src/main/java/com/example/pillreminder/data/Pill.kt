package com.example.pillreminder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class FoodRelation { BEFORE_FOOD, AFTER_FOOD, WITH_FOOD, NO_RELATION }

/**
 * doseAmount is stored as a fraction of a whole pill (e.g. 1.0, 0.5, 0.25, 2.0)
 * so "نصف قرص" / "یک‌چهارم قرص" / "دو عدد" همه با یک فیلد پوشش داده می‌شن.
 */
@Entity(tableName = "pills")
data class Pill(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val photoUri: String? = null,
    val colorHex: String = "#4CAF50",
    val doseAmount: Double = 1.0,
    val foodRelation: FoodRelation = FoodRelation.NO_RELATION,
    // دقیقه‌هایی که باید بعد از "قبل غذا" چیزی نخورد (مثل ۳۰ دقیقه لووتیروکسین)
    val waitAfterMinutes: Int = 0,
    // ساعات مصرف روزانه به فرمت "HH:mm" جدا شده با کاما، مثلا "08:00,20:00"
    val timesOfDay: String,
    val startDateEpochDay: Long,
    // null یعنی درمان مادام‌العمر / نامحدود
    val treatmentDurationDays: Int? = null,
    // موجودی فعلی به تعداد قرص کامل (نه دوز)، null یعنی موجودی پیگیری نمی‌شود
    val inventoryCount: Double? = null,
    val lowStockThresholdDays: Int = 3,
    val isActive: Boolean = true
)
