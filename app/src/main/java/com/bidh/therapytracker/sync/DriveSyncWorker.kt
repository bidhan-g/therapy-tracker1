package com.bidh.therapytracker.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bidh.therapytracker.data.AppDatabase
import com.bidh.therapytracker.data.SecurePrefs
import com.google.android.gms.auth.api.signin.GoogleSignIn

class DriveSyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val account = GoogleSignIn.getLastSignedInAccount(applicationContext) ?: return Result.success()
        val androidAccount = account.account ?: return Result.success()

        return try {
            val drive = DriveServiceHelper.buildService(applicationContext, androidAccount)
            val cachedFolderId = SecurePrefs.getDriveFolderId(applicationContext)
            val folderId = DriveServiceHelper.getOrCreateAppFolder(drive, cachedFolderId)
            SecurePrefs.setDriveFolderId(applicationContext, folderId)

            val db = AppDatabase.getInstance(applicationContext)
            val categories = db.categoryDao().getAllOnce()
            categories.forEach { category ->
                val sessions = db.sessionDao().getAllForCategory(category.id)
                val fileName = CategoryExporter.buildFileName(category)
                val content = CategoryExporter.buildContent(category, sessions)
                DriveServiceHelper.uploadOrUpdateTextFile(drive, folderId, fileName, content)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
