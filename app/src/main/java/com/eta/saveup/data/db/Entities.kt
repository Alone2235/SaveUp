package com.eta.saveup.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 攒钱目标。一张卡 = 一个想买的东西。
 * 金额一律用 Long 存「分」。
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
 * 一次「决定」。
 *
 * 这是整个 App 的核心记录：每次面对一笔想花的钱，用户做出选择，
 * 就产生一条 entry。它同时承担流水和决策日志两个角色。
 *
 * 金额一律保留正数，方向由 [decision] 决定。
 */
@Entity(
    tableName = "entries",
    indices = [Index("goalId"), Index("createdAt")]
)
data class Entry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** 归属目标；为空表示还没分配 */
    val goalId: Long? = null,
    /** 这笔钱的金额（正数，单位分） */
    val amountCent: Long,
    /** 商品名，用户自己填的，可以为空 */
    val title: String = "",
    /** 0=留着（计入攒钱） 1=买了（不计入） */
    val decision: Int = 0,
    /** 0=未落地（钱还在手上） 1=已锁定（钱已转走） */
    val settleState: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val KEPT = 0
        const val BOUGHT = 1

        const val UNSETTLED = 0
        const val SETTLED = 1
    }
}

/** 历史表结构，保留以兼容旧库迁移；新逻辑不再写入 */
@Entity(tableName = "deposits")
data class Deposit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long? = null,
    val amountCent: Long,
    val note: String = "",
    val status: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val SAVED = 0
        const val SPENT = 1
        const val REFUNDED = 2
    }
}

@Entity(tableName = "daily_stats")
data class DailyStat(
    @PrimaryKey val day: Int,
    val savedCent: Long = 0,
    val savedCount: Int = 0,
    val spentCent: Long = 0
)
