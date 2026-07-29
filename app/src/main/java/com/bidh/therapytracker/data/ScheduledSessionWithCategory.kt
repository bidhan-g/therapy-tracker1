package com.bidh.therapytracker.data

data class ScheduledSessionWithCategory(
    val id: Long,
    val dateTimeMillis: Long,
    val status: SessionStatus,
    val createdAtMillis: Long,
    val categoryId: Long,
    val categoryName: String
)
