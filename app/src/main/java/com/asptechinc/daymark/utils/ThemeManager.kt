package com.asptechinc.daymark.utils

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import com.asptechinc.daymark.config.AppConfig

object ThemeManager {
    private const val THEME_MODE_INDEX_KEY = "theme_mode_index"

    fun applySavedTheme(context: Context) {
        val index = getSavedThemeIndex(context)
        Log.i("ThemeManager", "Applying saved theme (index: $index)")
        AppCompatDelegate.setDefaultNightMode(modeForIndex(index))
    }

    fun getSavedThemeIndex(context: Context): Int =
        context
            .getSharedPreferences(AppConfig.SETTINGS_PREFS, Context.MODE_PRIVATE)
            .getInt(THEME_MODE_INDEX_KEY, 0)
            .coerceIn(0, 2)

    fun setThemeByIndex(
        context: Context,
        index: Int,
    ) {
        val normalisedIndex = index.coerceIn(0, 2)
        Log.i("ThemeManager", "Setting theme to index: $normalisedIndex")
        context
            .getSharedPreferences(AppConfig.SETTINGS_PREFS, Context.MODE_PRIVATE)
            .edit { putInt(THEME_MODE_INDEX_KEY, normalisedIndex) }

        AppCompatDelegate.setDefaultNightMode(modeForIndex(normalisedIndex))
    }

    private fun modeForIndex(index: Int): Int =
        when (index) {
            1 -> AppCompatDelegate.MODE_NIGHT_YES
            2 -> AppCompatDelegate.MODE_NIGHT_NO
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
}
