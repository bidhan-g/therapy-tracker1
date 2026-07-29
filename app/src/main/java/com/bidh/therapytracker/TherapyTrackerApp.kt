package com.bidh.therapytracker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class TherapyTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "appointment_reminders",
                "Appointment Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for upcoming therapy sessions"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
