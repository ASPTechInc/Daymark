package com.asptechinc.daymark.repository

import com.asptechinc.daymark.models.Activity
import org.joda.time.DateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityRepositoryTest {
    @Test
    fun `add activity should increase list size`() {
        val repository = ActivityRepository()
        val activity = Activity("Test Activity", "Notes", DateTime.now())

        repository.add(activity)

        assertEquals(1, repository.activities.size)
        assertEquals("Test Activity", repository.activities[0].activityName)
    }

    @Test
    fun `edit activity should update details`() {
        val repository = ActivityRepository()
        val activity = Activity("Original", "Notes", DateTime.now())
        repository.add(activity)

        val updated = activity.copy(activityName = "Updated", notes = "New Notes")
        repository.update(0, updated)

        assertEquals("Updated", repository.activities[0].activityName)
        assertEquals("New Notes", repository.activities[0].notes)
    }

    @Test
    fun `rename activity should only change name`() {
        val repository = ActivityRepository()
        val activity = Activity("Old Name", "Notes", DateTime.now())
        repository.add(activity)

        repository.rename(0, "New Name")

        assertEquals("New Name", repository.activities[0].activityName)
        assertEquals("Notes", repository.activities[0].notes)
    }

    @Test
    fun `delete activity should remove it from list`() {
        val repository = ActivityRepository()
        val activity = Activity("To Delete", "Notes", DateTime.now())
        repository.add(activity)

        repository.removeAt(0)

        assertTrue(repository.activities.isEmpty())
    }

    @Test
    fun `archive activity should set archived flag to true`() {
        val repository = ActivityRepository()
        val activity = Activity("Test", "Notes", DateTime.now(), archived = false)
        repository.add(activity)

        repository.archive(0)

        assertEquals(true, repository.activities[0].archived)
    }

    @Test
    fun `reset activity should update start date and clear end date`() {
        val repository = ActivityRepository()
        val oldDate = DateTime.now().minusDays(5)
        val endDate = DateTime.now().plusDays(2)
        val activity = Activity("Test", "Notes", oldDate, endDateTime = endDate)
        repository.add(activity)

        repository.reset(0)

        assertNotEquals(oldDate, repository.activities[0].startDateTime)
        assertNull(repository.activities[0].endDateTime)
    }

    @Test
    fun `clear should empty the repository`() {
        val repository =
            ActivityRepository(
                listOf(
                    Activity("A", "Notes", DateTime.now()),
                    Activity("B", "Notes", DateTime.now()),
                ),
            )

        repository.clear()

        assertTrue(repository.activities.isEmpty())
    }
}
