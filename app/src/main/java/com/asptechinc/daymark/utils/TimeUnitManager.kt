package com.asptechinc.daymark.utils

import android.content.Context
import androidx.core.content.edit
import com.asptechinc.daymark.config.AppConfig

object TimeUnitManager {
    private const val TIME_UNIT_PREFERENCE_INDEX_KEY = "time_unit_preference_index"

    fun getSavedTimeUnitIndex(context: Context): Int =
        context
            .getSharedPreferences(AppConfig.SETTINGS_PREFS, Context.MODE_PRIVATE)
            .getInt(TIME_UNIT_PREFERENCE_INDEX_KEY, 0) // Default 0 is Year, month, weeks, days

    fun setTimeUnitByIndex(
        context: Context,
        index: Int,
    ) {
        context
            .getSharedPreferences(AppConfig.SETTINGS_PREFS, Context.MODE_PRIVATE)
            .edit { putInt(TIME_UNIT_PREFERENCE_INDEX_KEY, index) }
    }
}
