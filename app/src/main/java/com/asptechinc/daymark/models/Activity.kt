package com.asptechinc.daymark.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
@Entity(tableName = "activities")
data class Activity(
    var activityName: String,
    var notes: String,
    var startDateTime:
        @Serializable(with = LocalDateTimeSerializer::class)
        LocalDateTime,
    // Optional field: defaults to null when not explicitly set
    var endDateTime:
        @Serializable(with = LocalDateTimeSerializer::class)
        LocalDateTime? = null,
    // Optional field: defaults to false when not explicitly set.
    var archived: Boolean? = false,
    // Optional field: defaults to null when no category is selected.
    var categoryId: Int? = null,
    // Optional field: defaults to an empty list when no tags are selected.
    var tagIds: MutableList<Int> = mutableListOf(),
    var position: Int = 0,
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
)
