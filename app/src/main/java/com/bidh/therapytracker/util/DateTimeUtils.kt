package com.bidh.therapytracker.util

import java.text.SimpleDateFormat
import java.util.Locale

object DateTimeUtils {
    private val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("EEE, MMM d, yyyy 'at' h:mm a", Locale.getDefault())

    fun formatDate(millis: Long): String = dateFormat.format(millis)
    fun formatTime(millis: Long): String = timeFormat.format(millis)
    fun formatDateTime(millis: Long): String = dateTimeFormat.format(millis)
}
