package com.asptechinc.daymark.repository

import com.asptechinc.daymark.data.ActivityDao
import com.asptechinc.daymark.models.Activity
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.time.LocalDateTime

class ActivityRepositoryTest {
    private lateinit var activityDao: ActivityDao
    private lateinit var repository: ActivityRepository

    @Before
    fun setup() {
        activityDao = mock()
        repository = ActivityRepository(activityDao)
    }

    @Test
    fun `add activity should call dao insert`() =
        runTest {
            val activity = Activity(activityName = "Test Activity", notes = "Notes", startDateTime = LocalDateTime.now())
            repository.add(activity)
            verify(activityDao).insert(activity)
        }

    @Test
    fun `edit activity should call dao update`() =
        runTest {
            val activity = Activity(activityName = "Original", notes = "Notes", startDateTime = LocalDateTime.now())
            repository.update(activity)
            verify(activityDao).update(activity)
        }

    @Test
    fun `delete activity should call dao delete`() =
        runTest {
            val activity = Activity(activityName = "To Delete", notes = "Notes", startDateTime = LocalDateTime.now())
            repository.remove(activity)
            verify(activityDao).delete(activity)
        }

    @Test
    fun `archive activity should set archived flag to true and update`() =
        runTest {
            val activity = Activity(activityName = "Test", notes = "Notes", startDateTime = LocalDateTime.now(), archived = false)
            repository.archive(activity)
            verify(activityDao).update(activity.copy(archived = true))
        }

    @Test
    fun `clear should call dao deleteAll`() =
        runTest {
            repository.clear()
            verify(activityDao).deleteAll()
        }
}
