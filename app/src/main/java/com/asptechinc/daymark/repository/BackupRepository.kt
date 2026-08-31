package com.asptechinc.daymark.repository

import android.util.Log
import com.asptechinc.daymark.BuildConfig
import com.asptechinc.daymark.config.AppConfig
import com.asptechinc.daymark.data.ActivityDao
import com.asptechinc.daymark.data.CategoryDao
import com.asptechinc.daymark.data.TagDao
import com.asptechinc.daymark.models.Activity
import com.asptechinc.daymark.models.Category
import com.asptechinc.daymark.models.Tag
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.time.LocalDateTime

/**
 * Data class representing the full state of the application for backup/restore purposes.
 *
 * @property formatVersion Used to handle schema changes in the JSON export over time.
 */
@Serializable
data class BackupState(
    var activities: MutableList<Activity>,
    var categories: MutableList<Category>,
    var tags: MutableList<Tag>,
    var formatVersion: Int = AppConfig.BACKUP_FORMAT_VERSION,
)

/**
 * Repository responsible for importing/exporting app data as JSON and managing backup versioning.
 */
class BackupRepository(
    private val activityDao: ActivityDao,
    private val categoryDao: CategoryDao,
    private val tagDao: TagDao,
) {
    private val json =
        Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }

    suspend fun loadBackupState(): BackupState {
        val activities = activityDao.getAll().toMutableList()
        val categories = categoryDao.getAll().toMutableList()
        val tags = tagDao.getAll().toMutableList()

        return BackupState(activities, categories, tags)
    }

    suspend fun saveBackupState(state: BackupState) {
        // Clear existing data to ensure that the state matches exactly (important for imports/resets)
        activityDao.deleteAll()
        categoryDao.deleteAll()
        tagDao.deleteAll()

        activityDao.insertAll(state.activities)
        categoryDao.insertAll(state.categories)
        tagDao.insertAll(state.tags)
    }

    suspend fun exportStateToJson(): String {
        val state = loadBackupState()
        return buildJsonObject {
            put("formatVersion", AppConfig.BACKUP_FORMAT_VERSION)
            put("appVersion", BuildConfig.VERSION_NAME)
            put("backupDate", LocalDateTime.now().toString())
            put("activities", json.encodeToJsonElement(state.activities))
            put("tags", json.encodeToJsonElement(state.tags))
            put("categories", json.encodeToJsonElement(state.categories))
        }.toString()
    }

    fun parseStateFromJson(jsonString: String): BackupState? =
        try {
            val root = json.parseToJsonElement(jsonString).jsonObject
            val version = root["formatVersion"]?.jsonPrimitive?.intOrNull ?: 1

            val activities =
                root["activities"]?.let { json.decodeFromJsonElement<MutableList<Activity>>(it) }
                    ?: mutableListOf()
            val categories =
                root["categories"]?.let { json.decodeFromJsonElement<MutableList<Category>>(it) }
                    ?: mutableListOf()
            val tags =
                root["tags"]?.let { json.decodeFromJsonElement<MutableList<Tag>>(it) }
                    ?: mutableListOf()

            BackupState(activities, categories, tags, version)
        } catch (e: Exception) {
            Log.e("BackupRepository", "Error parsing settings state", e)
            null
        }
}

class UnsupportedBackupVersionException : Exception("The backup file version is not supported.")
