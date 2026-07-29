package com.bidh.therapytracker.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateTimeMillis: Long,
    val status: SessionStatus,
    val createdAtMillis: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "1")
    val categoryId: Long
)
