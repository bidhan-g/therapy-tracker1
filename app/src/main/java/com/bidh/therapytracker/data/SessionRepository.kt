package com.bidh.therapytracker.data

import android.content.Context
import androidx.lifecycle.LiveData

class SessionRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).sessionDao()

    fun observeAll(): LiveData<List<Session>> = dao.observeAll()

    fun observeCompletedCount(): LiveData<Int> = dao.observeCompletedCount()

    fun observeNextUpcoming(): LiveData<Session?> = dao.observeNextUpcoming(System.currentTimeMillis())

    suspend fun getFutureScheduled(): List<Session> = dao.getFutureScheduled(System.currentTimeMillis())

    suspend fun getAll(): List<Session> = dao.getAll()

    suspend fun insert(session: Session): Long = dao.insert(session)

    suspend fun update(session: Session) = dao.update(session)

    suspend fun delete(session: Session) = dao.delete(session)

    suspend fun deleteAll() = dao.deleteAll()
}
