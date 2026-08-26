package com.asptechinc.daymark.repository

import android.util.Log
import com.asptechinc.daymark.data.ActivityDao
import com.asptechinc.daymark.models.Activity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

class ActivityRepository(
    private val activityDao: ActivityDao,
) {
    val allActivities: Flow<List<Activity>> = activityDao.getAllActivities()

    suspend fun add(activity: Activity): Int {
        Log.i("ActivityRepository", "Adding new activity")
        return activityDao.insert(activity).toInt()
    }

    suspend fun addAll(activities: List<Activity>): List<Int> {
        Log.i("ActivityRepository", "Adding ${activities.size} activities")
        return activityDao.insertAll(activities).map { it.toInt() }
    }

    suspend fun update(activity: Activity) {
        Log.i("ActivityRepository", "Updating activity (ID: ${activity.id})")
        activityDao.update(activity)
    }

    suspend fun remove(activity: Activity) {
        Log.i("ActivityRepository", "Removing activity (ID: ${activity.id})")
        activityDao.delete(activity)
    }

    suspend fun archive(activity: Activity) {
        Log.i("ActivityRepository", "Archiving activity (ID: ${activity.id})")
        val updated = activity.copy(archived = true)
        activityDao.update(updated)
    }

    suspend fun reset(activity: Activity) {
        Log.i("ActivityRepository", "Resetting activity (ID: ${activity.id})")
        val updated =
            activity.copy(
                startDateTime = LocalDateTime.now(),
                endDateTime = null,
            )
        activityDao.update(updated)
    }

    suspend fun clear() {
        Log.i("ActivityRepository", "Clearing all activities")
        activityDao.deleteAll()
    }

    suspend fun isEmpty(): Boolean = activityDao.getCount() == 0
}
