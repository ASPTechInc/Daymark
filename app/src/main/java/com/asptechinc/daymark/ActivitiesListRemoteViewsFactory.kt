package com.asptechinc.daymark

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.asptechinc.daymark.data.AppDatabase
import com.asptechinc.daymark.models.Activity
import com.asptechinc.daymark.utils.TimeUnitManager
import com.asptechinc.daymark.utils.getActivityRelativeText
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime

/**
 * Factory class that acts as the data adapter for the 4x4 App Widget ListView.
 *
 * Since widgets run in a separate process, this class retrieves data from Room
 * and maps it to [RemoteViews].
 */
class ActivitiesListRemoteViewsFactory(
    private val context: Context,
) : RemoteViewsService.RemoteViewsFactory {
    private var activities: List<Activity> = emptyList()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        // Fetch data from Room database synchronously as required by RemoteViewsFactory
        runBlocking {
            val db = AppDatabase.getDatabase(context)
            activities = db.activityDao().getAll()
        }
    }

    override fun onDestroy() {}

    override fun getCount(): Int = activities.size

    override fun getViewAt(position: Int): RemoteViews? {
        if (position < 0 || position >= activities.size) return null

        val activity = activities[position]
        val views = RemoteViews(context.packageName, R.layout.item_widget_activity)

        val currentDate = LocalDateTime.now()
        val timeUnitIndex = TimeUnitManager.getSavedTimeUnitIndex(context)

        views.setTextViewText(R.id.activity_name, activity.activityName)

        val relativeText =
            getActivityRelativeText(
                activity.startDateTime,
                activity.endDateTime,
                currentDate,
                timeUnitIndex,
            )
        views.setTextViewText(R.id.activity_relative_date, relativeText)

        // Fill in the intent template
        val fillInIntent =
            Intent().apply {
                putExtra("ACTIVITY_ID", activity.id)
            }
        views.setOnClickFillInIntent(R.id.widget_item_container, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = activities[position].id.toLong()

    override fun hasStableIds(): Boolean = true
}
