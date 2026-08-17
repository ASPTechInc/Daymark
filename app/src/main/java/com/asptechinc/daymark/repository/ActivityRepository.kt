package com.asptechinc.daymark.repository

import com.asptechinc.daymark.models.Activity
import org.joda.time.DateTime

class ActivityRepository(
    initialActivities: List<Activity> = emptyList(),
) {
    private val _activities = initialActivities.toMutableList()

    val activities: List<Activity>
        get() = _activities.toList()

    fun add(activity: Activity) {
        _activities.add(activity)
    }

    fun addAt(
        index: Int,
        activity: Activity,
    ) {
        if (index in 0.._activities.size) {
            _activities.add(index, activity)
        } else {
            _activities.add(activity)
        }
    }

    fun update(
        index: Int,
        updatedActivity: Activity,
    ) {
        if (index in _activities.indices) {
            _activities[index] = updatedActivity
        }
    }

    fun removeAt(index: Int): Activity? =
        if (index in _activities.indices) {
            _activities.removeAt(index)
        } else {
            null
        }

    fun remove(activity: Activity): Boolean = _activities.remove(activity)

    fun archive(index: Int) {
        if (index in _activities.indices) {
            _activities[index].archived = true
        }
    }

    fun reset(index: Int) {
        if (index in _activities.indices) {
            val activity = _activities[index]
            activity.startDateTime = DateTime.now()
            activity.endDateTime = null
        }
    }

    fun rename(
        index: Int,
        newName: String,
    ) {
        if (index in _activities.indices) {
            _activities[index].activityName = newName
        }
    }

    fun clear() {
        _activities.clear()
    }

    fun setAll(newActivities: List<Activity>) {
        _activities.clear()
        _activities.addAll(newActivities)
    }

    fun indexOf(activity: Activity): Int = _activities.indexOfFirst { it === activity }
}
