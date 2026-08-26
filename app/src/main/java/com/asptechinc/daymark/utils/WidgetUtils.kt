package com.asptechinc.daymark.utils

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.util.Log
import com.asptechinc.daymark.ActivitiesListWidgetProvider

object WidgetUtils {
    fun updateAllWidgets(context: Context) {
        Log.i("WidgetUtils", "Updating all app widgets")

        // Broadcast update intent to refresh widget providers and their collection data
        val updateIntent =
            Intent(context, ActivitiesListWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
        context.sendBroadcast(updateIntent)
    }
}
