package com.bidh.therapytracker.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import java.util.concurrent.TimeUnit

// Kicks off Drive sync in the background. All calls are safe to make even
// when the user isn't connected - they just no-op in that case.
object DriveSyncScheduler {

    private const val IMMEDIATE_WORK_NAME = "drive_sync_immediate"
    private const val PERIODIC_WORK_NAME = "drive_sync_periodic"

    private fun networkConstraints() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    private fun isConnected(context: Context) =
        GoogleSignIn.getLastSignedInAccount(context) != null

    // Call after any data change (add/edit/delete a category or appointment).
    fun triggerSyncSoon(context: Context) {
        if (!isConnected(context)) return
        val request = OneTimeWorkRequestBuilder<DriveSyncWorker>()
            .setConstraints(networkConstraints())
            .setInitialDelay(10, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(IMMEDIATE_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    // Call right after a successful sign-in, to sync everything immediately.
    fun triggerImmediateFullSync(context: Context) {
        val request = OneTimeWorkRequestBuilder<DriveSyncWorker>()
            .setConstraints(networkConstraints())
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(IMMEDIATE_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    // Call on app start (if already connected) and right after sign-in, so
    // data keeps syncing even if the app isn't opened for a while.
    fun ensurePeriodicSyncScheduled(context: Context) {
        if (!isConnected(context)) return
        val request = PeriodicWorkRequestBuilder<DriveSyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(networkConstraints())
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancelPeriodicSync(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
    }
}
