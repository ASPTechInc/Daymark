package com.asptechinc.daymark.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.asptechinc.daymark.models.Activity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activities ORDER BY position ASC")
    fun getAllActivities(): Flow<List<Activity>>

    @Query("SELECT * FROM activities ORDER BY position ASC")
    suspend fun getAll(): List<Activity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(activity: Activity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(activities: List<Activity>): List<Long>

    @Update
    suspend fun update(activity: Activity)

    @Delete
    suspend fun delete(activity: Activity)

    @Query("DELETE FROM activities")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM activities")
    suspend fun getCount(): Int
}
