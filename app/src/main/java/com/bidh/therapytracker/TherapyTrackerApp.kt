package com.bidh.therapytracker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.bidh.therapytracker.data.AppDatabase
import com.bidh.therapytracker.data.SecurePrefs
import com.bidh.therapytracker.sync.DriveSyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TherapyTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        migrateLegacyTargetIfNeeded()
        DriveSyncScheduler.ensurePeriodicSyncScheduled(this)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "appointment_reminders",
                "Appointment Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for upcoming appointments"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    // One-time backfill: the pre-categories version of this app stored a single
    // "total sessions" target in SecurePrefs. After upgrading, that number becomes
    // the target for the default "Therapy" category so nothing is lost.
    private fun migrateLegacyTargetIfNeeded() {
        if (SecurePrefs.isLegacyTargetMigrated(this)) return
        val legacyTarget = SecurePrefs.getLegacyTargetSessions(this)
        CoroutineScope(Dispatchers.IO).launch {
            if (legacyTarget > 0) {
                val dao = AppDatabase.getInstance(this@TherapyTrackerApp).categoryDao()
                val category = dao.getById(1)
                if (category != null && category.targetCount == null) {
                    dao.update(category.copy(targetCount = legacyTarget))
                }
            }
            SecurePrefs.setLegacyTargetMigrated(this@TherapyTrackerApp)
        }
    }
}
