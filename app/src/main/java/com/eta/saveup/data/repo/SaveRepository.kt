package com.eta.saveup.data.repo

import com.eta.saveup.data.db.AppDatabase
import com.eta.saveup.data.db.DailyStat
import com.eta.saveup.data.db.Entry
import com.eta.saveup.data.db.Goal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.Calendar

/** 一个目标的完整视图数据 */
data class GoalView(
    val goal: Goal,
    val savedCent: Long
) {
    val progress: Float
        get() = if (goal.targetCent <= 0) 0f
        else (savedCent.toFloat() / goal.targetCent).coerceIn(0f, 1f)

    val remainCent: Long get() = (goal.targetCent - savedCent).coerceAtLeast(0)
    val achieved: Boolean get() = savedCent >= goal.targetCent
}

/**
 * 仓库层。
 *
 * 核心原则：**entries 是唯一真相**。所有统计都从 entries 现算，
 * 不再维护任何累加计数器。
 */
class SaveRepository(private val db: AppDatabase) {

    private val goalDao = db.goalDao()
    private val entryDao = db.entryDao()

    val goals: Flow<List<Goal>> = goalDao.observeAll()
    val allEntries: Flow<List<Entry>> = entryDao.observeAll()
    val totalSettled: Flow<Long> = entryDao.observeTotalSettled().map { it ?: 0L }

    /** 最近 90 天每日汇总，直接从 entries 聚合 */
    val recentDaily: Flow<List<DailyStat>> =
        entryDao.observeDailyStats(System.currentTimeMillis() - 90L * 24 * 3600 * 1000)

    /** 目标 + 已锁定金额 */
    fun goalViews(): Flow<List<GoalView>> =
        combine(goals, entryDao.observeSettledByGoal()) { goals, sums ->
            val map = sums.associate { it.goalId to it.sum }
            goals.map { GoalView(it, map[it.id] ?: 0L) }
        }

    fun entriesOf(goalId: Long): Flow<List<Entry>> = entryDao.observeByGoal(goalId)

    // ---- 目标 ----

    suspend fun createGoal(goal: Goal): Long = goalDao.insert(goal)

    suspend fun updateGoal(goal: Goal) = goalDao.update(goal)

    suspend fun deleteGoal(id: Long) {
        // 目标删掉后，其记录退回未分配，不丢数据
        entryDao.detachFromGoal(id)
        goalDao.delete(id)
    }

    suspend fun archiveGoal(goal: Goal) =
        goalDao.update(goal.copy(status = Goal.ARCHIVED))

    suspend fun restoreGoal(goal: Goal) =
        goalDao.update(goal.copy(status = Goal.ACTIVE))

    // ---- 决定 ----

    /**
     * 记下一个决定。
     *
     * @param kept true = 留着（计入攒钱），false = 买了（不计入）
     * @param settled 这笔钱是否已经真的转走；默认未落地
     */
    suspend fun recordDecision(
        goalId: Long?,
        amountCent: Long,
        title: String,
        kept: Boolean,
        settled: Boolean = true
    ) {
        if (amountCent <= 0) return

        entryDao.insert(
            Entry(
                goalId = goalId,
                amountCent = amountCent,
                title = title.trim(),
                decision = if (kept) Entry.KEPT else Entry.BOUGHT,
                settleState = if (settled) Entry.SETTLED else Entry.UNSETTLED
            )
        )
        goalId?.let { refreshGoalStatus(it) }
    }

    suspend fun deleteEntry(id: Long) {
        val e = entryDao.get(id) ?: return
        entryDao.delete(id)
        e.goalId?.let { refreshGoalStatus(it) }
    }

    suspend fun updateEntry(entry: Entry) = entryDao.update(entry)

    /**
     * 按当前记录重算目标完成状态。
     * 只统计「保留 + 已锁定」的钱。
     */
    suspend fun refreshGoalStatus(goalId: Long) {
        val goal = goalDao.get(goalId) ?: return
        if (goal.status == Goal.ARCHIVED) return

        val sum = entryDao.getByGoal(goalId)
            .filter { it.decision == Entry.KEPT }
            .sumOf { it.amountCent }

        val newStatus = if (sum >= goal.targetCent) Goal.DONE else Goal.ACTIVE
        if (goal.status != newStatus) {
            goalDao.update(goal.copy(status = newStatus))
        }
    }

    companion object {
        fun todayKey(): Int {
            val c = Calendar.getInstance()
            return c.get(Calendar.YEAR) * 10000 +
                (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DAY_OF_MONTH)
        }

        fun dayKey(millis: Long): Int {
            val c = Calendar.getInstance().apply { timeInMillis = millis }
            return c.get(Calendar.YEAR) * 10000 +
                (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DAY_OF_MONTH)
        }

        /** 连续有「保留」记录的天数；今天没记就从昨天算，断一天归零 */
        fun streak(daily: List<DailyStat>, today: Int = todayKey()): Int {
            val map = daily.associateBy { it.day }
            var cursor = today
            if (map[cursor]?.savedCount?.let { it > 0 } != true) {
                cursor = prevDay(cursor)
                if (map[cursor]?.savedCount?.let { it > 0 } != true) return 0
            }
            var n = 0
            while (true) {
                val s = map[cursor] ?: break
                if (s.savedCount <= 0) break
                n++
                cursor = prevDay(cursor)
            }
            return n
        }

        fun prevDay(day: Int): Int {
            val c = Calendar.getInstance().apply {
                set(Calendar.YEAR, day / 10000)
                set(Calendar.MONTH, (day / 100) % 100 - 1)
                set(Calendar.DAY_OF_MONTH, day % 100)
                add(Calendar.DAY_OF_MONTH, -1)
            }
            return c.get(Calendar.YEAR) * 10000 +
                (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DAY_OF_MONTH)
        }
    }
}
