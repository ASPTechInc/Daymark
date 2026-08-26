package com.asptechinc.daymark.data

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Converters {
    @TypeConverter
    fun fromTimestamp(value: String?): LocalDateTime? = value?.let { LocalDateTime.parse(it, DateTimeFormatter.ISO_LOCAL_DATE_TIME) }

    @TypeConverter
    fun dateToTimestamp(date: LocalDateTime?): String? = date?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

    @TypeConverter
    fun fromIntList(value: String): MutableList<Int> = Json.decodeFromString(value)

    @TypeConverter
    fun toIntList(list: MutableList<Int>): String = Json.encodeToString(list)
}
