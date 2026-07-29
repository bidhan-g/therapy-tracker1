package com.bidh.therapytracker.data

import android.content.Context
import androidx.lifecycle.LiveData

class SessionRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).sessionDao()

    fun observeAllForCategory(categoryId: Long): LiveData<List<Session>> = dao.observeAllForCategory(categoryId)

    fun observeCompletedCountForCategory(categoryId: Long): LiveData<Int> = dao.observeCompletedCountForCategory(categoryId)

    fun observeNextUpcomingForCategory(categoryId: Long): LiveData<Session?> =
        dao.observeNextUpcomingForCategory(categoryId, System.currentTimeMillis())

    suspend fun getAllForCategory(categoryId: Long): List<Session> = dao.getAllForCategory(categoryId)

    suspend fun getAllFutureScheduledWithCategory(): List<ScheduledSessionWithCategory> =
        dao.getAllFutureScheduledWithCategory(System.currentTimeMillis())

    suspend fun insert(session: Session): Long = dao.insert(session)

    suspend fun update(session: Session) = dao.update(session)

    suspend fun delete(session: Session) = dao.delete(session)

    suspend fun deleteAllForCategory(categoryId: Long) = dao.deleteAllForCategory(categoryId)
}
