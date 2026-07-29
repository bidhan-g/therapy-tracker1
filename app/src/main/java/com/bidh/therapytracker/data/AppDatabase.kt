package com.bidh.therapytracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(entities = [Session::class, Category::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun sessionDao(): SessionDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Adds categories, and gives every pre-existing session a "Therapy" category
        // (id 1) so nobody's existing history disappears when this update installs.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `targetCount` INTEGER)"
                )
                db.execSQL("INSERT INTO categories (id, name, targetCount) VALUES (1, 'Therapy', NULL)")
                db.execSQL("ALTER TABLE sessions ADD COLUMN categoryId INTEGER NOT NULL DEFAULT 1")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: build(context).also { INSTANCE = it }
            }
        }

        private fun build(context: Context): AppDatabase {
            System.loadLibrary("sqlcipher")
            val passphrase = SecurePrefs.getOrCreateDbPassphrase(context)
            val factory = SupportOpenHelperFactory(passphrase)
            return Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "sessions.db")
                .openHelperFactory(factory)
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
