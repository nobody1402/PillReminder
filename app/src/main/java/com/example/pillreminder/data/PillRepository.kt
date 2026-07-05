package com.example.pillreminder.data

import android.content.Context
import com.example.pillreminder.alarm.AlarmScheduler
import com.example.pillreminder.util.DrugKnowledgeBase
import com.example.pillreminder.util.InteractionChecker
import com.example.pillreminder.util.InteractionWarning
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class PillRepository(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val pillDao = db.pillDao()
    private val doseLogDao = db.doseLogDao()
    private val ruleDao = db.interactionRuleDao()
    private val drugHistoryDao = db.drugHistoryDao()

    fun observePills(): Flow<List<Pill>> = pillDao.getAllActive()
    fun observeRules(): Flow<List<InteractionRule>> = ruleDao.getAll()
    fun observeLogsForDay(dayStartMillis: Long, dayEndMillis: Long): Flow<List<DoseLog>> =
        doseLogDao.getLogsForDay(dayStartMillis, dayEndMillis)

    /** ثبت قرص جدید، بررسی تداخل زمانی، و برنامه‌ریزی آلارم‌ها را با هم انجام می‌دهد */
    suspend fun addOrUpdatePill(pill: Pill, allPills: List<Pill>, rules: List<InteractionRule>): List<InteractionWarning> {
        val id = if (pill.id == 0L) pillDao.insert(pill) else { pillDao.update(pill); pill.id }
        val saved = pill.copy(id = id)

        AlarmScheduler.scheduleAllForPill(context, saved)
        saveDrugHistory(saved)

        val relevantRules = rules.filter { it.pillAId == id || it.pillBId == id }
        return InteractionChecker.checkPill(saved, allPills + saved, relevantRules)
    }

    /** تنظیمات این دارو را برای دفعه بعد ذخیره می‌کند (مثلا استامینوفن یک سال دیگر دوباره ثبت شود) */
    suspend fun saveDrugHistory(pill: Pill) {
        if (pill.name.isBlank()) return
        drugHistoryDao.upsert(
            DrugHistory(
                normalizedName = DrugKnowledgeBase.normalize(pill.name),
                displayName = pill.name,
                doseAmount = pill.doseAmount,
                foodRelation = pill.foodRelation,
                waitAfterMinutes = pill.waitAfterMinutes,
                timesOfDay = pill.timesOfDay,
                treatmentDurationDays = pill.treatmentDurationDays,
                lowStockThresholdDays = pill.lowStockThresholdDays,
                lastUsedEpochMillis = System.currentTimeMillis()
            )
        )
    }

    /** اگر این دارو قبلا ثبت شده، آخرین تنظیماتش را برمی‌گرداند تا کاربر لازم نباشد دوباره وارد کند */
    suspend fun findDrugHistory(name: String): DrugHistory? {
        if (name.isBlank()) return null
        return drugHistoryDao.findByName(DrugKnowledgeBase.normalize(name))
    }

    suspend fun deletePill(pill: Pill) {
        AlarmScheduler.cancelAllForPill(context, pill)
        pillDao.delete(pill.id)
    }

    /** دوره درمان همین دارو رو با همون تعداد روز قبلی، از امروز دوباره شروع می‌کنه */
    suspend fun renewPill(pill: Pill, extraDays: Int, allPills: List<Pill>, rules: List<InteractionRule>): List<InteractionWarning> {
        val renewed = pill.copy(
            startDateEpochDay = java.time.LocalDate.now().toEpochDay(),
            treatmentDurationDays = extraDays
        )
        return addOrUpdatePill(renewed, allPills, rules)
    }

    /** دارو رو غیرفعال می‌کنه (آلارم‌هاش لغو می‌شن) بدون حذف تاریخچه‌ی مصرف قبلیش */
    suspend fun deactivatePill(pill: Pill) {
        AlarmScheduler.cancelAllForPill(context, pill)
        pillDao.update(pill.copy(isActive = false))
    }

    suspend fun addRule(rule: InteractionRule) = ruleDao.insert(rule)
    suspend fun deleteRule(id: Long) = ruleDao.delete(id)
    suspend fun getAllPillsSnapshot(): List<Pill> = pillDao.getAllActive().first()
}
