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
}

@Dao
interface DepositDao {

    @Query("SELECT * FROM deposits ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Deposit>>

    @Query("SELECT * FROM deposits ORDER BY createdAt DESC")
    suspend fun getAll(): List<Deposit>

    @Query("SELECT * FROM deposits WHERE goalId = :goalId ORDER BY createdAt DESC")
    fun observeByGoal(goalId: Long): Flow<List<Deposit>>

    @Query("SELECT * FROM deposits WHERE goalId = :goalId ORDER BY createdAt DESC")
    suspend fun getByGoal(goalId: Long): List<Deposit>

    @Query("SELECT * FROM deposits WHERE id = :id")
    suspend fun get(id: Long): Deposit?

    @Insert
    suspend fun insert(deposit: Deposit): Long

    @Update
    suspend fun update(deposit: Deposit)

    @Query("DELETE FROM deposits WHERE id = :id")
    suspend fun delete(id: Long)

    /** 各目标已攒下（SAVED）的总额，用于进度计算 */
    @Query("SELECT goalId AS goalId, SUM(amountCent) AS sum FROM deposits WHERE status = 0 AND goalId IS NOT NULL GROUP BY goalId")
    fun observeSumsByGoal(): Flow<List<GoalSum>>

    @Query("SELECT SUM(amountCent) FROM deposits WHERE status = 0")
    fun observeTotalSaved(): Flow<Long?>

    @Query("SELECT COUNT(*) FROM deposits WHERE goalId IS NULL AND status = 0")
    fun observeInboxCount(): Flow<Int>

    /**
     * 直接从流水聚合每日数据。
     *
     * 不再依赖 daily_stats 这个累加计数器 —— 计数器一旦和流水错位（删除、
     * 异常退出）就会永久偏，且难以自愈。这里以流水为唯一真相。
     *
     * day 用 SQLite 的本地日期函数换算，与 createdDay 同口径。
     */
    @Query(
        """
        SELECT
            CAST(strftime('%Y%m%d', createdAt / 1000, 'unixepoch', 'localtime') AS INTEGER) AS day,
            SUM(CASE WHEN status = 0 THEN amountCent ELSE 0 END) AS savedCent,
            CAST(SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END) AS INTEGER) AS savedCount,
            SUM(CASE WHEN status = 1 THEN amountCent ELSE 0 END) AS spentCent
        FROM deposits
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

    @Query("SELECT * FROM daily_stats WHERE day = :day")
    suspend fun get(day: Int): DailyStat?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stat: DailyStat)

    @Query("DELETE FROM daily_stats")
    suspend fun clear()
}
