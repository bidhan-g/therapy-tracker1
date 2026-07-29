package com.bidh.therapytracker.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromStatus(status: SessionStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): SessionStatus = SessionStatus.valueOf(value)
}
