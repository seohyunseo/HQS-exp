package com.uvr.hqs_phone.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [LifelogEntity::class],
    version = 2,
    exportSchema = false
)
abstract class LifelogDatabase : RoomDatabase() {

    abstract fun lifelogDao(): LifelogDao

    companion object {
        @Volatile private var INSTANCE: LifelogDatabase? = null

        /**
         * Migration 1→2: adds the isSynced column (INTEGER, default 0 = false).
         * Uses ALTER TABLE so existing data is preserved.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE lifelog ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): LifelogDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    LifelogDatabase::class.java,
                    "lifelog_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
