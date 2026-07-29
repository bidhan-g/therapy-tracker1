package com.bidh.therapytracker.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface SessionDao {

    @Query("SELECT * FROM sessions ORDER BY dateTimeMillis DESC")
    fun observeAll(): LiveData<List<Session>>

    @Query("SELECT * FROM sessions WHERE status = 'SCHEDULED' AND dateTimeMillis >= :nowMillis ORDER BY dateTimeMillis ASC LIMIT 1")
    fun observeNextUpcoming(nowMillis: Long): LiveData<Session?>

    @Query("SELECT COUNT(*) FROM sessions WHERE status = 'COMPLETED'")
    fun observeCompletedCount(): LiveData<Int>

    @Query("SELECT * FROM sessions WHERE status = 'SCHEDULED' AND dateTimeMillis >= :nowMillis")
    suspend fun getFutureScheduled(nowMillis: Long): List<Session>

    @Insert
    suspend fun insert(session: Session): Long

    @Update
    suspend fun update(session: Session)

    @Delete
    suspend fun delete(session: Session)
}
