package com.asptechinc.daymark.repository

import android.content.Context
import com.asptechinc.daymark.R
import com.asptechinc.daymark.models.Activity
import com.asptechinc.daymark.models.Category
import com.asptechinc.daymark.models.Tag
import com.asptechinc.daymark.utils.i18n
import org.joda.time.DateTime

/**
 * Initial data loaded on first app launch
 */
fun initialActivities(context: Context): MutableList<Activity> {
    val now = DateTime.now()

    return mutableListOf(
        Activity(
            context.i18n(R.string.default_entry_title1),
            context.i18n(R.string.default_entry_note1),
            now.minusWeeks(5),
        ),
        Activity(
            context.i18n(R.string.default_entry_title2),
            context.i18n(R.string.default_entry_note2),
            now.minusDays(1),
            now.plusMonths(3),
        ),
        Activity(
            context.i18n(R.string.default_entry_title3),
            context.i18n(R.string.default_entry_note3),
            now.plusYears(2),
        ),
        Activity(
            context.i18n(R.string.default_entry_title4),
            context.i18n(R.string.default_entry_note4),
            now.minusDays(1),
            now.plusMonths(3),
        ),
        Activity(
            context.i18n(R.string.default_entry_title5),
            context.i18n(R.string.default_entry_note5),
            now.minusDays(1),
        ),
        Activity(
            context.i18n(R.string.default_entry_title6),
            context.i18n(R.string.default_entry_note6),
            now.minusDays(1),
            now.plusMonths(3),
        ),
        Activity(
            context.i18n(R.string.default_entry_title7),
            context.i18n(R.string.default_entry_note7),
            now.minusMonths(2),
        ),
        Activity(
            context.i18n(R.string.default_entry_title8),
            context.i18n(R.string.default_entry_note8),
            now.minusYears(1),
            now.plusMonths(3),
        ),
        Activity(
            context.i18n(R.string.default_entry_title9),
            context.i18n(R.string.default_entry_note9),
            now.minusDays(1),
        ),
        Activity(
            context.i18n(R.string.default_entry_title10),
            context.i18n(R.string.default_entry_note10),
            now.minusHours(1),
            now.plusMinutes(5),
        ),
        Activity(
            context.i18n(R.string.default_entry_title1),
            context.i18n(R.string.default_entry_note1),
            now.minusMinutes(45),
        ),
        Activity(
            context.i18n(R.string.default_entry_title1),
            context.i18n(R.string.default_entry_note1),
            now.minusDays(1),
        ),
        Activity(
            context.i18n(R.string.default_entry_title1),
            context.i18n(R.string.default_entry_note1),
            now.minusDays(1),
            now.minusHours(6),
        ),
        Activity(
            context.i18n(R.string.default_entry_title1),
            context.i18n(R.string.default_entry_note1),
            now.minusDays(6),
            now.plusMonths(3),
        ),
        Activity(
            context.i18n(R.string.default_entry_title1),
            context.i18n(R.string.default_entry_note1),
            now.plusDays(9),
        ),
        Activity(
            context.i18n(R.string.default_entry_title1),
            context.i18n(R.string.default_entry_note1),
            now.minusHours(7),
            now.plusYears(3),
        ),
        Activity(
            context.i18n(R.string.default_entry_title1),
            context.i18n(R.string.default_entry_note1),
            now.minusMonths(4),
        ),
        Activity(
            context.i18n(R.string.default_entry_title1),
            context.i18n(R.string.default_entry_note1),
            now.minusYears(3),
            now.plusWeeks(2),
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
