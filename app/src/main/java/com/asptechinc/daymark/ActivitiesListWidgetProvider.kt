package com.asptechinc.daymark

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.asptechinc.daymark.data.AppDatabase
import com.asptechinc.daymark.utils.TimeUnitManager
import com.asptechinc.daymark.utils.getActivityRelativeText
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime

class ActivitiesListWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        super.onReceive(context, intent)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, ActivitiesListWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // For API 31+, we must re-set the RemoteCollectionItems to refresh data
                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }
            } else {
                // Legacy way for older devices
                @Suppress("DEPRECATION")
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.activities_list)
            }
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
    ) {
        val views = RemoteViews(context.packageName, R.layout.view_widget_activities_list)

        // RemoteViews doesn't support app:tint, so we set it in code
        views.setInt(
            R.id.btn_refresh,
            "setColorFilter",
            ContextCompat.getColor(context, R.color.on_menu_container),
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Non-deprecated path for API 31+
            views.setRemoteAdapter(R.id.activities_list, buildRemoteCollectionItems(context))
        } else {
            // Legacy path for API 26-30
            // Set up the RemoteViewsService to provide data for the ListView (Legacy)
            val intent =
                Intent(context, ActivitiesListRemoteViewsService::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    data = Uri.parse(this.toUri(Intent.URI_INTENT_SCHEME))
                }
            @Suppress("DEPRECATION")
            views.setRemoteAdapter(R.id.activities_list, intent)
        }

        views.setEmptyView(R.id.activities_list, R.id.empty_view)

        // Template for list item clicks
        val clickIntent = Intent(context, MainActivity::class.java)
        val clickPendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        views.setPendingIntentTemplate(R.id.activities_list, clickPendingIntent)

        // Refresh button
        val refreshIntent =
            Intent(context, ActivitiesListWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
        val refreshPendingIntent =
            PendingIntent.getBroadcast(
                context,
                0,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        views.setOnClickPendingIntent(R.id.btn_refresh, refreshPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun buildRemoteCollectionItems(context: Context): RemoteViews.RemoteCollectionItems {
        val activities =
            runBlocking {
                AppDatabase.getDatabase(context).activityDao().getAll()
            }

        val currentDate = LocalDateTime.now()
        val timeUnitIndex = TimeUnitManager.getSavedTimeUnitIndex(context)

        val builder = RemoteViews.RemoteCollectionItems.Builder()
        for (activity in activities) {
            val itemViews = RemoteViews(context.packageName, R.layout.item_widget_activity)
            itemViews.setTextViewText(R.id.activity_name, activity.activityName)

            val relativeText =
                getActivityRelativeText(
                    activity.startDateTime,
                    activity.endDateTime,
                    currentDate,
                    timeUnitIndex,
                )
            itemViews.setTextViewText(R.id.activity_relative_date, relativeText)

            val fillInIntent =
                Intent().apply {
                    putExtra("ACTIVITY_ID", activity.id)
                }
            itemViews.setOnClickFillInIntent(R.id.widget_item_container, fillInIntent)

            builder.addItem(activity.id.toLong(), itemViews)
        }

        return builder.setViewTypeCount(1).build()
    }
}
