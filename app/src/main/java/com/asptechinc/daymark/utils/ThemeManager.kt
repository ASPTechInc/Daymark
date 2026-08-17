package com.asptechinc.daymark.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit

object ThemeManager {
    private const val SETTINGS_PREFS = "settings"
    private const val THEME_MODE_INDEX_KEY = "theme_mode_index"

    fun applySavedTheme(context: Context) {
        AppCompatDelegate.setDefaultNightMode(modeForIndex(getSavedThemeIndex(context)))
    }

    fun getSavedThemeIndex(context: Context): Int =
        context
            .getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
            .getInt(THEME_MODE_INDEX_KEY, 0)
            .coerceIn(0, 2)

    fun setThemeByIndex(
        context: Context,
        index: Int,
    ) {
        val normalizedIndex = index.coerceIn(0, 2)
        context
            .getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
            .edit { putInt(THEME_MODE_INDEX_KEY, normalizedIndex) }

        AppCompatDelegate.setDefaultNightMode(modeForIndex(normalizedIndex))
    }

    private fun modeForIndex(index: Int): Int =
        when (index) {
            1 -> AppCompatDelegate.MODE_NIGHT_YES
            2 -> AppCompatDelegate.MODE_NIGHT_NO
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
}
