package com.eta.saveup.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {

    @Query("SELECT * FROM goals ORDER BY status ASC, sortOrder ASC, createdAt DESC")
    fun observeAll(): Flow<List<Goal>>

    @Query("SELECT * FROM goals WHERE id = :id")
    fun observeOne(id: Long): Flow<Goal?>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun get(id: Long): Goal?

    @Insert
    suspend fun insert(goal: Goal): Long

    @Update
    suspend fun update(goal: Goal)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM goals")
    suspend fun count(): Int

    /** 一次性取出所有目标，供计算用 */
    @Query("SELECT * FROM goals")
    suspend fun getAll(): List<Goal>
}

@Dao
interface EntryDao {

    @Query("SELECT * FROM entries ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Entry>>

    @Query("SELECT * FROM entries WHERE goalId = :goalId ORDER BY createdAt DESC")
    fun observeByGoal(goalId: Long): Flow<List<Entry>>

    @Query("SELECT * FROM entries WHERE goalId = :goalId ORDER BY createdAt DESC")
    suspend fun getByGoal(goalId: Long): List<Entry>

    @Query("SELECT * FROM entries WHERE id = :id")
    suspend fun get(id: Long): Entry?

    @Insert
    suspend fun insert(entry: Entry): Long

    @Update
    suspend fun update(entry: Entry)

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE entries SET goalId = NULL WHERE goalId = :goalId")
    suspend fun detachFromGoal(goalId: Long)

    /**
     * 各目标「已攒下」的总额。
     * 只算 decision=KEPT 且 settleState=SETTLED 的，未落地的钱不计入。
     */
    @Query(
        """
        SELECT goalId AS goalId, SUM(amountCent) AS sum
        FROM entries
        WHERE decision = 0 AND settleState = 1 AND goalId IS NOT NULL
        GROUP BY goalId
        """
    )
    fun observeSettledByGoal(): Flow<List<GoalSum>>

    /** 未落地的总额，用于提示"有 X 笔待转入" */
    @Query("SELECT SUM(amountCent) FROM entries WHERE decision = 0 AND settleState = 0")
    fun observePendingSum(): Flow<Long?>

    @Query("SELECT COUNT(*) FROM entries WHERE decision = 0 AND settleState = 0")
    fun observePendingCount(): Flow<Int>

    /** 所有未落地的保留记录 id */
    @Query("SELECT id FROM entries WHERE decision = 0 AND settleState = 0")
    suspend fun pendingKeptIds(): List<Long>

    /** 全局累计已锁定金额 */
    @Query("SELECT SUM(amountCent) FROM entries WHERE decision = 0 AND settleState = 1")
    fun observeTotalSettled(): Flow<Long?>

    /**
     * 每日保留/购买汇总，直接从 entries 聚合。
     * day 用 SQLite 本地日期换算。
     */
    @Query(
        """
        SELECT
            CAST(strftime('%Y%m%d', createdAt / 1000, 'unixepoch', 'localtime') AS INTEGER) AS day,
            SUM(CASE WHEN decision = 0 THEN amountCent ELSE 0 END) AS savedCent,
            CAST(SUM(CASE WHEN decision = 0 THEN 1 ELSE 0 END) AS INTEGER) AS savedCount,
            SUM(CASE WHEN decision = 1 THEN amountCent ELSE 0 END) AS spentCent
        FROM entries
        WHERE createdAt >= :fromMillis
        GROUP BY day
        ORDER BY day DESC
        """
    )
    fun observeDailyStats(fromMillis: Long): Flow<List<DailyStat>>
}

data class GoalSum(val goalId: Long, val sum: Long)

@Dao
interface DailyDao {
    @Query("SELECT * FROM daily_stats ORDER BY day DESC LIMIT :limit")
    fun observeRecent(limit: Int = 90): Flow<List<DailyStat>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stat: DailyStat)

    @Query("DELETE FROM daily_stats")
    suspend fun clear()
}

/** 旧表 DAO，仅保留读取以兼容已有数据 */
@Dao
interface DepositDao {
    @Query("SELECT * FROM deposits ORDER BY createdAt DESC")
    suspend fun getAll(): List<Deposit>

    @Query("SELECT COUNT(*) FROM deposits")
    suspend fun count(): Int

    @Query("DELETE FROM deposits WHERE id = :id")
    suspend fun delete(id: Long)
}
