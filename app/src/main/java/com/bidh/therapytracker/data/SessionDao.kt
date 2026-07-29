package com.bidh.therapytracker.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface SessionDao {

    @Query("SELECT * FROM sessions WHERE categoryId = :categoryId ORDER BY dateTimeMillis DESC")
    fun observeAllForCategory(categoryId: Long): LiveData<List<Session>>

    @Query("SELECT * FROM sessions WHERE categoryId = :categoryId AND status = 'SCHEDULED' AND dateTimeMillis >= :nowMillis ORDER BY dateTimeMillis ASC LIMIT 1")
    fun observeNextUpcomingForCategory(categoryId: Long, nowMillis: Long): LiveData<Session?>

    @Query("SELECT COUNT(*) FROM sessions WHERE categoryId = :categoryId AND status = 'COMPLETED'")
    fun observeCompletedCountForCategory(categoryId: Long): LiveData<Int>

    @Query("SELECT * FROM sessions WHERE categoryId = :categoryId")
    suspend fun getAllForCategory(categoryId: Long): List<Session>

    @Query(
        """
        SELECT s.id as id, s.dateTimeMillis as dateTimeMillis, s.status as status, s.createdAtMillis as createdAtMillis, s.categoryId as categoryId, c.name as categoryName
        FROM sessions s
        JOIN categories c ON c.id = s.categoryId
        WHERE s.status = 'SCHEDULED' AND s.dateTimeMillis >= :nowMillis
        """
    )
    suspend fun getAllFutureScheduledWithCategory(nowMillis: Long): List<ScheduledSessionWithCategory>

    @Insert
    suspend fun insert(session: Session): Long

    @Update
    suspend fun update(session: Session)

    @Delete
    suspend fun delete(session: Session)

    @Query("DELETE FROM sessions WHERE categoryId = :categoryId")
    suspend fun deleteAllForCategory(categoryId: Long)
}
