package com.bidh.therapytracker.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface CategoryDao {

    @Insert
    suspend fun insert(category: Category): Long

    @Update
    suspend fun update(category: Category)

    @Delete
    suspend fun delete(category: Category)

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): Category?

    @Query(
        """
        SELECT c.id as id, c.name as name, c.targetCount as targetCount,
        COUNT(CASE WHEN s.status = 'COMPLETED' THEN 1 END) as completedCount,
        MIN(CASE WHEN s.status = 'SCHEDULED' AND s.dateTimeMillis >= :nowMillis THEN s.dateTimeMillis END) as nextUpcomingMillis
        FROM categories c
        LEFT JOIN sessions s ON s.categoryId = c.id
        GROUP BY c.id
        ORDER BY c.name ASC
        """
    )
    fun observeCategorySummaries(nowMillis: Long): LiveData<List<CategorySummary>>
}
