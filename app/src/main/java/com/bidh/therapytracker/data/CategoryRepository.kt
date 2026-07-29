package com.bidh.therapytracker.data

import android.content.Context
import androidx.lifecycle.LiveData

class CategoryRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).categoryDao()

    fun observeSummaries(): LiveData<List<CategorySummary>> =
        dao.observeCategorySummaries(System.currentTimeMillis())

    suspend fun getById(id: Long): Category? = dao.getById(id)

    suspend fun insert(category: Category): Long = dao.insert(category)

    suspend fun update(category: Category) = dao.update(category)

    suspend fun delete(category: Category) = dao.delete(category)
}
