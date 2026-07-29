package com.bidh.therapytracker.reminders

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.bidh.therapytracker.data.SecurePrefs
import com.bidh.therapytracker.data.Session
import java.util.Calendar
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    private fun morningWorkName(sessionId: Long) = "reminder_morning_$sessionId"
    private fun hourWorkName(sessionId: Long) = "reminder_hour_$sessionId"

    fun schedule(context: Context, session: Session) {
        cancel(context, session.id)

        val morningHour = SecurePrefs.getMorningHour(context)
        val morningMinute = SecurePrefs.getMorningMinute(context)
        val morningTrigger = Calendar.getInstance().apply {
            timeInMillis = session.dateTimeMillis
            set(Calendar.HOUR_OF_DAY, morningHour)
            set(Calendar.MINUTE, morningMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val hourBeforeTrigger = Calendar.getInstance().apply {
            timeInMillis = session.dateTimeMillis
            add(Calendar.HOUR_OF_DAY, -1)
        }

        val now = System.currentTimeMillis()
        val workManager = WorkManager.getInstance(context)

        if (morningTrigger.timeInMillis > now) {
            val delay = morningTrigger.timeInMillis - now
            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(
                    workDataOf(
                        ReminderWorker.KEY_SESSION_ID to session.id,
                        ReminderWorker.KEY_TITLE to "Therapy session today",
                        ReminderWorker.KEY_MESSAGE to "You have a therapy session scheduled today."
                    )
                )
                .build()
            workManager.enqueueUniqueWork(morningWorkName(session.id), ExistingWorkPolicy.REPLACE, request)
        }

        if (hourBeforeTrigger.timeInMillis > now) {
            val delay = hourBeforeTrigger.timeInMillis - now
            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(
                    workDataOf(
                        ReminderWorker.KEY_SESSION_ID to session.id,
                        ReminderWorker.KEY_TITLE to "Therapy session in 1 hour",
                        ReminderWorker.KEY_MESSAGE to "Your therapy session starts in about an hour."
                    )
                )
                .build()
            workManager.enqueueUniqueWork(hourWorkName(session.id), ExistingWorkPolicy.REPLACE, request)
        }
    }

    fun cancel(context: Context, sessionId: Long) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(morningWorkName(sessionId))
        workManager.cancelUniqueWork(hourWorkName(sessionId))
    }
}
