package com.asptechinc.daymark.repository

import com.asptechinc.daymark.data.ActivityDao
import com.asptechinc.daymark.models.Activity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

class ActivityRepository(
    private val activityDao: ActivityDao,
) {
    val allActivities: Flow<List<Activity>> = activityDao.getAllActivities()

    suspend fun add(activity: Activity) {
        activityDao.insert(activity)
    }

    suspend fun addAll(activities: List<Activity>) {
        activityDao.insertAll(activities)
    }

    suspend fun update(activity: Activity) {
        activityDao.update(activity)
    }

    suspend fun remove(activity: Activity) {
        activityDao.delete(activity)
    }

    suspend fun archive(activity: Activity) {
        val updated = activity.copy(archived = true)
        activityDao.update(updated)
    }

    suspend fun reset(activity: Activity) {
        val updated =
            activity.copy(
                startDateTime = LocalDateTime.now(),
                endDateTime = null,
            )
        activityDao.update(updated)
    }

    suspend fun clear() {
        activityDao.deleteAll()
    }

    suspend fun isEmpty(): Boolean = activityDao.getCount() == 0
}
