package com.eta.saveup.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 攒钱目标。一张卡片 = 一个想买的东西。
 * 金额一律用 Long 存「分」，绝不用 Float/Double。
 */
@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val emoji: String = "🎯",
    val targetCent: Long,
    val deadlineMillis: Long? = null,
    /** 解锁图片的本地 URI；为空则退回环形进度 */
    val coverUri: String? = null,
    /** 揭示方向：0=自下而上 1=自左向右 2=径向 */
    val revealMode: Int = 0,
    /** 0=进行中 1=已完成 2=已归档 */
    val status: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0
) {
    companion object {
        const val ACTIVE = 0
        const val DONE = 1
        const val ARCHIVED = 2
    }
}

/**
 * 一笔攒钱记录（钱是「忍住没花」省下来的，或真的从别处挪进来的）。
 * goalId 为 null 表示还没分配到目标（收件箱）。
 */
@Entity(tableName = "deposits", indices = [androidx.room.Index("goalId"), androidx.room.Index("createdAt")])
data class Deposit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long? = null,
    val amountCent: Long,
    val note: String = "",
    /** 0=已攒下 1=已花掉（破戒） 2=已退回 */
    val status: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val SAVED = 0
        const val SPENT = 1
        const val REFUNDED = 2
    }
}

/** 每日打卡统计，用于 streak。day 用 yyyyMMdd 整数 */
@Entity(tableName = "daily_stats")
data class DailyStat(
    @PrimaryKey val day: Int,
    val savedCent: Long = 0,
    val savedCount: Int = 0,
    val spentCent: Long = 0
)
