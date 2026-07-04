package com.example.pillreminder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "interaction_rules")
data class InteractionRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pillAId: Long,
    val pillBId: Long,
    val minGapMinutes: Int
)
