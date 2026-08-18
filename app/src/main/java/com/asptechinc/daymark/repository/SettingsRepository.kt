package com.asptechinc.daymark.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.asptechinc.daymark.BuildConfig
import com.asptechinc.daymark.config.AppConfig
import com.asptechinc.daymark.models.Activity
import com.asptechinc.daymark.models.Category
import com.asptechinc.daymark.models.Tag
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import java.time.LocalDateTime

@Serializable
data class SettingsState(
    val activities: MutableList<Activity>,
    var categories: MutableList<Category>,
    var tags: MutableList<Tag>,
)

class SettingsRepository(
    private val context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val json =
        Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }

    fun loadSettingsState(): SettingsState {
        val settingsJsonValue =
            prefs.getString(AppConfig.SETTINGS_JSON_KEY, null) ?: return SettingsState(
                activities = initialActivities(context),
                categories = initialCategories(),
                tags = initialTags(),
            )

        return try {
            val root = json.parseToJsonElement(settingsJsonValue).jsonObject
            val activities = root["activities"]?.let { json.decodeFromJsonElement<MutableList<Activity>>(it) } ?: mutableListOf()
            val categories = root["categories"]?.let { json.decodeFromJsonElement<MutableList<Category>>(it) } ?: mutableListOf()
            val tags = root["tags"]?.let { json.decodeFromJsonElement<MutableList<Tag>>(it) } ?: mutableListOf()

            SettingsState(activities, categories, tags)
        } catch (e: Exception) {
            Log.e("SettingsRepository", "Error loading settings state", e)
            SettingsState(
                activities = initialActivities(context),
                categories = initialCategories(),
                tags = initialTags(),
            )
        }
    }

    fun saveSettingsState(state: SettingsState) {
        val settingsJson =
            buildJsonObject {
                put("saveFormatVersion", AppConfig.SAVE_FILE_VERSION)
                put("savedWithAppVersion", BuildConfig.VERSION_NAME)
                put("savedOnDate", LocalDateTime.now().toString())
                put("activities", json.encodeToJsonElement(state.activities))
                put("tags", json.encodeToJsonElement(state.tags))
                put("categories", json.encodeToJsonElement(state.categories))
            }

        prefs.edit { putString(AppConfig.SETTINGS_JSON_KEY, settingsJson.toString()) }
    }
}
