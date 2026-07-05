package com.example.pillreminder.util

import com.example.pillreminder.data.FoodRelation

object PillTextFormatter {

    fun doseAmountText(amount: Double): String = when (amount) {
        1.0 -> "یک واحد"
        0.5 -> "نصف واحد"
        0.25 -> "یک‌چهارم واحد"
        0.75 -> "سه‌چهارم واحد"
        2.0 -> "دو واحد"
        3.0 -> "سه واحد"
        else -> {
            val trimmed = if (amount == amount.toLong().toDouble()) amount.toLong().toString() else amount.toString()
            "$trimmed واحد"
        }
    }

    fun foodRelationText(relation: FoodRelation, waitAfterMinutes: Int): String = when (relation) {
        FoodRelation.BEFORE_FOOD -> "با معده خالی" + if (waitAfterMinutes > 0) " — تا $waitAfterMinutes دقیقه چیزی نخور" else ""
        FoodRelation.AFTER_FOOD -> "بعد از غذا"
        FoodRelation.WITH_FOOD -> "همراه غذا"
        FoodRelation.NO_RELATION -> ""
    }
}
