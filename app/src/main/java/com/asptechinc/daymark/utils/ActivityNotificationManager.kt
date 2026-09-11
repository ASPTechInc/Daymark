package com.asptechinc.daymark.utils

import android.content.Context
import androidx.core.content.edit
import com.asptechinc.daymark.config.AppConfig

object ActivityNotificationManager {
    private const val ACTIVITY_START_NOTIFICATION_INDEX_KEY = "activity_start_notification_index"

    fun getSavedNotificationIndex(context: Context): Int =
        context
            .getSharedPreferences(AppConfig.SETTINGS_PREFS, Context.MODE_PRIVATE)
            .getInt(ACTIVITY_START_NOTIFICATION_INDEX_KEY, 0) // Default 0 is None

    fun setNotificationIndex(
        context: Context,
        index: Int,
    ) {
        context
            .getSharedPreferences(AppConfig.SETTINGS_PREFS, Context.MODE_PRIVATE)
            .edit { putInt(ACTIVITY_START_NOTIFICATION_INDEX_KEY, index) }
    }
}
