package com.example.pillreminder.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PillDao {
    @Query("SELECT * FROM pills WHERE isActive = 1 ORDER BY name")
    fun getAllActive(): Flow<List<Pill>>

    @Query("SELECT * FROM pills WHERE id = :id")
    suspend fun getById(id: Long): Pill?

    @Insert
    suspend fun insert(pill: Pill): Long

    @Update
    suspend fun update(pill: Pill)

    @Query("DELETE FROM pills WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE pills SET inventoryCount = :count WHERE id = :id")
    suspend fun updateInventory(id: Long, count: Double)

    @Query("SELECT * FROM pills WHERE isActive = 1 AND inventoryCount IS NOT NULL")
    suspend fun getAllWithInventoryTracking(): List<Pill>
}

@Dao
interface DoseLogDao {
    @Insert
    suspend fun insert(log: DoseLog): Long

    @Update
    suspend fun update(log: DoseLog)

    @Query("SELECT * FROM dose_logs WHERE pillId = :pillId AND scheduledAtMillis = :scheduledAt LIMIT 1")
    suspend fun findByPillAndTime(pillId: Long, scheduledAt: Long): DoseLog?

    @Query("SELECT * FROM dose_logs WHERE scheduledAtMillis BETWEEN :dayStart AND :dayEnd")
    fun getLogsForDay(dayStart: Long, dayEnd: Long): Flow<List<DoseLog>>

    @Query("""
        SELECT * FROM dose_logs 
        WHERE pillId = :pillId AND status = 'TAKEN' 
        ORDER BY scheduledAtMillis DESC LIMIT 5
    """)
    suspend fun recentTakenForPill(pillId: Long): List<DoseLog>
}

@Dao
interface DrugHistoryDao {
    @Query("SELECT * FROM drug_history WHERE normalizedName = :normalizedName LIMIT 1")
    suspend fun findByName(normalizedName: String): DrugHistory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(history: DrugHistory)
}

@Dao
interface InteractionRuleDao {
    @Query("SELECT * FROM interaction_rules")
    fun getAll(): Flow<List<InteractionRule>>

    @Query("""
        SELECT * FROM interaction_rules 
        WHERE pillAId = :pillId OR pillBId = :pillId
    """)
    suspend fun getRulesForPill(pillId: Long): List<InteractionRule>

    @Insert
    suspend fun insert(rule: InteractionRule): Long

    @Query("DELETE FROM interaction_rules WHERE id = :id")
    suspend fun delete(id: Long)
}
