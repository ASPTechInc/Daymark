package com.asptechinc.daymark.models

import org.joda.time.DateTime

data class Activity(
    var activityName: String,
    var notes: String,
    var startDateTime: DateTime,
    // Optional field: defaults to null when not explicitly set
    var endDateTime: DateTime? = null,
    // Optional field: defaults to false when not explicitly set.
    var archived: Boolean? = false,
    // Optional field: defaults to null when no category is selected.
    var categoryId: Int? = null,
    // Optional field: defaults to an empty list when no tags are selected.
    var tagIds: MutableList<Int> = mutableListOf(),
)
