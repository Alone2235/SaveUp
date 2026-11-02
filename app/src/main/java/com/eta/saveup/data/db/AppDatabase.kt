package com.eta.saveup.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Goal::class, Deposit::class, DailyStat::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun goalDao(): GoalDao
    abstract fun depositDao(): DepositDao
    abstract fun dailyDao(): DailyDao

    companion object {
        /** v1 → v2：目标表增加解锁封面字段 */
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE goals ADD COLUMN coverUri TEXT")
                db.execSQL("ALTER TABLE goals ADD COLUMN revealMode INTEGER NOT NULL DEFAULT 0")
            }
        }

        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: build(context.applicationContext).also { INSTANCE = it }
        }

        private fun build(ctx: Context): AppDatabase =
            Room.databaseBuilder(ctx, AppDatabase::class.java, "saveup.db")
                .addMigrations(MIGRATION_1_2)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // 首装放一张示例卡，方便上手（只是一条普通数据，随时可改可删）
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
