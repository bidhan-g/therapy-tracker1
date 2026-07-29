package com.bidh.therapytracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import net.sqlcipher.database.SupportFactory

@Database(entities = [Session::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: build(context).also { INSTANCE = it }
            }
        }

        private fun build(context: Context): AppDatabase {
            val passphrase = SecurePrefs.getOrCreateDbPassphrase(context)
            val factory = SupportFactory(passphrase)
            return Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "sessions.db")
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
