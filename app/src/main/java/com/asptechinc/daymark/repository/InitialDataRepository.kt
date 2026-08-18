package com.asptechinc.daymark.repository

import android.content.Context
import com.asptechinc.daymark.R
import com.asptechinc.daymark.models.Activity
import com.asptechinc.daymark.models.Category
import com.asptechinc.daymark.models.Tag
import com.asptechinc.daymark.utils.i18n
import java.time.LocalDateTime

/**
 * Initial data loaded on first app launch
 */
fun initialActivities(context: Context): MutableList<Activity> {
    val now = LocalDateTime.now()

    return mutableListOf(
        Activity(
            activityName = context.i18n(R.string.default_entry_title1),
            notes = context.i18n(R.string.default_entry_note1),
            startDateTime = now.minusWeeks(5),
        ),
        Activity(
            activityName = context.i18n(R.string.default_entry_title2),
            notes = context.i18n(R.string.default_entry_note2),
            startDateTime = now.minusDays(1),
            endDateTime = now.plusMonths(3),
        ),
        Activity(
            activityName = context.i18n(R.string.default_entry_title3),
            notes = context.i18n(R.string.default_entry_note3),
            startDateTime = now.plusYears(2),
        ),
        Activity(
            activityName = context.i18n(R.string.default_entry_title4),
            notes = context.i18n(R.string.default_entry_note4),
            startDateTime = now.minusDays(1),
            endDateTime = now.plusMonths(3),
        ),
        Activity(
            activityName = context.i18n(R.string.default_entry_title5),
            notes = context.i18n(R.string.default_entry_note5),
            startDateTime = now.minusDays(1),
        ),
        Activity(
            activityName = context.i18n(R.string.default_entry_title6),
            notes = context.i18n(R.string.default_entry_note6),
            startDateTime = now.minusDays(1),
            endDateTime = now.plusMonths(3),
        ),
        Activity(
            activityName = context.i18n(R.string.default_entry_title7),
            notes = context.i18n(R.string.default_entry_note7),
            startDateTime = now.minusMonths(2),
        ),
        Activity(
            activityName = context.i18n(R.string.default_entry_title8),
            notes = context.i18n(R.string.default_entry_note8),
            startDateTime = now.minusYears(1),
            endDateTime = now.plusMonths(3),
        ),
        Activity(
            activityName = context.i18n(R.string.default_entry_title9),
            notes = context.i18n(R.string.default_entry_note9),
            startDateTime = now.minusDays(1),
        ),
        Activity(
            activityName = context.i18n(R.string.default_entry_title10),
            notes = context.i18n(R.string.default_entry_note10),
            startDateTime = now.minusHours(1),
            endDateTime = now.plusMinutes(5),
        ),
    )
}

fun initialTags(): MutableList<Tag> =
    mutableListOf(
        Tag(1, "maintenance"),
        Tag(2, "urgent"),
        Tag(3, "appliance"),
        Tag(4, "yearly"),
    )

fun initialCategories(): MutableList<Category> =
    mutableListOf(
        Category(1, "Vehicle"),
        Category(2, "Health"),
        Category(3, "Home improvement"),
    )
