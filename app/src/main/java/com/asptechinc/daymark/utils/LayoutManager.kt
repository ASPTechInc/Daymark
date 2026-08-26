package com.asptechinc.daymark.utils

import android.content.Context
import androidx.core.content.edit
import com.asptechinc.daymark.config.AppConfig

object LayoutManager {
    private const val LAYOUT_MODE_INDEX_KEY = "layout_mode_index"

    fun getSavedLayoutIndex(context: Context): Int =
        context
            .getSharedPreferences(AppConfig.SETTINGS_PREFS, Context.MODE_PRIVATE)
            .getInt(LAYOUT_MODE_INDEX_KEY, 0) // Default 0 is List

    fun setLayoutByIndex(
        context: Context,
        index: Int,
    ) {
        context
            .getSharedPreferences(AppConfig.SETTINGS_PREFS, Context.MODE_PRIVATE)
            .edit { putInt(LAYOUT_MODE_INDEX_KEY, index) }
    }

    fun isGridLayout(context: Context): Boolean = getSavedLayoutIndex(context) == 1
}
