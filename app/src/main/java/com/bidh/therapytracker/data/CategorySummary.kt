package com.bidh.therapytracker.data

data class CategorySummary(
    val id: Long,
    val name: String,
    val targetCount: Int?,
    val completedCount: Int,
    val nextUpcomingMillis: Long?
)
