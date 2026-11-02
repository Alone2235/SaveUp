package com.eta.saveup.data.repo

import com.eta.saveup.data.db.AppDatabase
import com.eta.saveup.data.db.DailyStat
import com.eta.saveup.data.db.Deposit
import com.eta.saveup.data.db.Goal
import kotlinx.coroutines.flow.Flow
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

class SaveRepository(private val db: AppDatabase) {

    private val goalDao = db.goalDao()
    private val depositDao = db.depositDao()
    private val dailyDao = db.dailyDao()

    val goals: Flow<List<Goal>> = goalDao.observeAll()
    val allDeposits: Flow<List<Deposit>> = depositDao.observeAll()
    val totalSaved: Flow<Long> = depositDao.observeTotalSaved().map { it ?: 0L }
    val inboxCount: Flow<Int> = depositDao.observeInboxCount()
    val recentDaily: Flow<List<DailyStat>> = dailyDao.observeRecent()

    fun goalViews(): Flow<List<GoalView>> =
        kotlinx.coroutines.flow.combine(goals, depositDao.observeSumsByGoal()) { goals, sums ->
            val map = sums.associate { it.goalId to it.sum }
            goals.map { GoalView(it, map[it.id] ?: 0L) }
        }

    fun depositsOf(goalId: Long): Flow<List<Deposit>> = depositDao.observeByGoal(goalId)

    // ---- 写操作 ----

    suspend fun createGoal(goal: Goal): Long = goalDao.insert(goal)

    suspend fun updateGoal(goal: Goal) = goalDao.update(goal)

    suspend fun deleteGoal(id: Long) {
        // 目标删掉后，其流水退回收件箱，不丢数据
        depositDao.getByGoal(id).forEach {
            depositDao.update(it.copy(goalId = null))
        }
        goalDao.delete(id)
    }

    suspend fun archiveGoal(goal: Goal) =
        goalDao.update(goal.copy(status = Goal.ARCHIVED))

    suspend fun restoreGoal(goal: Goal) =
        goalDao.update(goal.copy(status = Goal.ACTIVE))

    /**
     * 记一笔攒钱。会同步更新当日统计用于 streak。
     */
    suspend fun addDeposit(goalId: Long?, amountCent: Long, note: String = "") {
        if (amountCent <= 0) return
        depositDao.insert(
            Deposit(goalId = goalId, amountCent = amountCent, note = note.trim())
        )
        val day = todayKey()
        val cur = dailyDao.get(day) ?: DailyStat(day)
        dailyDao.upsert(
            cur.copy(
                savedCent = cur.savedCent + amountCent,
                savedCount = cur.savedCount + 1
            )
        )
        // 目标状态重算（攒满自动完成，多退少补都靠这一处）
        goalId?.let { gid -> refreshGoalStatus(gid) }
    }

    /** 破戒了：记一笔花销，从目标里扣 */
    suspend fun addSpent(goalId: Long?, amountCent: Long, note: String = "") {
        if (amountCent <= 0) return
        depositDao.insert(
            Deposit(goalId = goalId, amountCent = amountCent, note = note.trim(), status = Deposit.SPENT)
        )
        val day = todayKey()
        val cur = dailyDao.get(day) ?: DailyStat(day)
        dailyDao.upsert(cur.copy(spentCent = cur.spentCent + amountCent))
    }

    suspend fun deleteDeposit(id: Long) {
        val d = depositDao.get(id) ?: return
        depositDao.delete(id)

        // 回滚当日统计
        val day = dayKey(d.createdAt)
        val cur = dailyDao.get(day)
        if (cur != null) {
            dailyDao.upsert(
                if (d.status == Deposit.SPENT)
                    cur.copy(spentCent = (cur.spentCent - d.amountCent).coerceAtLeast(0))
                else
                    cur.copy(
                        savedCent = (cur.savedCent - d.amountCent).coerceAtLeast(0),
                        savedCount = (cur.savedCount - 1).coerceAtLeast(0)
                    )
            )
        }

        // 目标状态无条件重算：删掉任何一笔都可能让它不再达标
        d.goalId?.let { gid -> refreshGoalStatus(gid) }
    }

    /**
     * 按当前流水重算目标完成状态。
     * 增、删、改金额后都要调，避免状态与实际金额脱节。
     */
    suspend fun refreshGoalStatus(goalId: Long) {
        val goal = goalDao.get(goalId) ?: return
        if (goal.status == Goal.ARCHIVED) return

        val sum = depositDao.getByGoal(goalId)
            .filter { it.status == Deposit.SAVED }
            .sumOf { it.amountCent }

        val shouldBeDone = sum >= goal.targetCent
        val newStatus = if (shouldBeDone) Goal.DONE else Goal.ACTIVE
        if (goal.status != newStatus) {
            goalDao.update(goal.copy(status = newStatus))
        }
    }

    suspend fun moveDeposit(id: Long, goalId: Long?) {
        val d = depositDao.get(id) ?: return
        depositDao.update(d.copy(goalId = goalId))
    }

    companion object {
        fun todayKey(): Int {
            val c = Calendar.getInstance()
            return c.get(Calendar.YEAR) * 10000 + (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DAY_OF_MONTH)
        }

        fun dayKey(millis: Long): Int {
            val c = Calendar.getInstance().apply { timeInMillis = millis }
            return c.get(Calendar.YEAR) * 10000 + (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DAY_OF_MONTH)
        }

        /** 连续有攒钱记录的天数（今天没记但昨天记了也算，断一天就归零） */
        fun streak(daily: List<DailyStat>, today: Int = todayKey()): Int {
            val map = daily.associateBy { it.day }
            var cursor = today
            // 今天还没攒，从昨天开始算，避免早上一看 streak 就断
            if (map[cursor]?.savedCount?.let { it > 0 } != true) {
                cursor = prevDay(cursor)
                if (map[cursor]?.savedCount?.let { it > 0 } != true) return 0
            }
            var n = 0
            while (true) {
                val s = map[cursor] ?: break
                if ((s.savedCount) <= 0) break
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
            return c.get(Calendar.YEAR) * 10000 + (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DAY_OF_MONTH)
        }
    }
}
