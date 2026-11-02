package com.eta.saveup.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Goal::class, Entry::class, Deposit::class, DailyStat::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun goalDao(): GoalDao
    abstract fun entryDao(): EntryDao
    abstract fun depositDao(): DepositDao
    abstract fun dailyDao(): DailyDao

    companion object {
        /** v1 → v2：目标表增加解锁封面字段 */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE goals ADD COLUMN coverUri TEXT")
                db.execSQL("ALTER TABLE goals ADD COLUMN revealMode INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * v2 → v3：引入 entries（决策+流水合一）。
         * 把旧的 deposits 迁移过来：SAVED → KEPT+SETTLED，SPENT → BOUGHT+SETTLED。
         * 旧数据默认为已落地，避免历史金额凭空消失。
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS entries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        goalId INTEGER,
                        amountCent INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        decision INTEGER NOT NULL,
                        settleState INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_entries_goalId ON entries(goalId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_entries_createdAt ON entries(createdAt)")

                // 确保 deposits 旧索引存在（实体仍声明了索引，Room 升级后会校验）
                db.execSQL("CREATE INDEX IF NOT EXISTS index_deposits_goalId ON deposits(goalId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_deposits_createdAt ON deposits(createdAt)")

                // 旧记录迁移：SAVED(0) → KEPT(0)+SETTLED(1)；SPENT(1) → BOUGHT(1)+SETTLED(1)
                db.execSQL(
                    """
                    INSERT INTO entries (goalId, amountCent, title, decision, settleState, createdAt)
                    SELECT goalId, amountCent, note,
                           CASE WHEN status = 0 THEN 0 ELSE 1 END,
                           1,
                           createdAt
                    FROM deposits
                    """.trimIndent()
                )
            }
        }

        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: build(context.applicationContext).also { INSTANCE = it }
        }

        private fun build(ctx: Context): AppDatabase =
            Room.databaseBuilder(ctx, AppDatabase::class.java, "saveup.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // 首装放一张示例卡，方便上手（普通数据，随时可改可删）
                        CoroutineScope(Dispatchers.IO).launch {
                            get(ctx).goalDao().run {
                                if (count() == 0) {
                                    insert(
                                        Goal(
                                            title = "我的第一个目标",
                                            emoji = "🎯",
                                            targetCent = 100000L // ¥1000
                                        )
                                    )
                                }
                            }
                        }
                    }
                })
                .build()
    }
}
