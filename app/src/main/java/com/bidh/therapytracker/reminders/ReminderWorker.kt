package com.bidh.therapytracker.reminders

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bidh.therapytracker.R
import com.bidh.therapytracker.ui.MainActivity

class ReminderWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    companion object {
        const val KEY_SESSION_ID = "session_id"
        const val KEY_CATEGORY_ID = "category_id"
        const val KEY_TITLE = "title"
        const val KEY_MESSAGE = "message"
        const val CHANNEL_ID = "appointment_reminders"
    }

    override suspend fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE) ?: "Appointment reminder"
        val message = inputData.getString(KEY_MESSAGE) ?: "You have an upcoming appointment."
        val sessionId = inputData.getLong(KEY_SESSION_ID, -1L)
        val categoryId = inputData.getLong(KEY_CATEGORY_ID, -1L)

        showNotification(title, message, sessionId, categoryId)
        return Result.success()
    }

    private fun showNotification(title: String, message: String, sessionId: Long, categoryId: Long) {
        val context = applicationContext

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ActivityCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_CATEGORY_ID, categoryId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            sessionId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(sessionId.toInt(), notification)
    }
}
