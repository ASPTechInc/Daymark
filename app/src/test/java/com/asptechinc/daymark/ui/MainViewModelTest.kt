package com.asptechinc.daymark.ui

import android.app.Application
import com.asptechinc.daymark.repository.ActivityRepository
import com.asptechinc.daymark.repository.MetadataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var application: Application

    @Mock
    private lateinit var activityRepository: ActivityRepository

    @Mock
    private lateinit var metadataRepository: MetadataRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        // Mock strings for initialActivities
        `when`(application.getString(anyInt())).thenReturn("mock_string")

        // Mock flows to avoid null pointer exceptions when ViewModel initializes StateFlows
        `when`(activityRepository.allActivities).thenReturn(flowOf(emptyList()))
        `when`(metadataRepository.allCategories).thenReturn(flowOf(emptyList()))
        `when`(metadataRepository.allTags).thenReturn(flowOf(emptyList()))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init populates initial data when database is empty`() =
        runTest {
            // Given the database is empty
            `when`(activityRepository.isEmpty()).thenReturn(true)

            // When ViewModel is initialized
            MainViewModel(application, activityRepository, metadataRepository)

            // Then initial data should be added
            advanceUntilIdle()
            verify(activityRepository).addAll(any())
            verify(metadataRepository).addAllCategories(any())
            verify(metadataRepository).addAllTags(any())
        }

    @Test
    fun `init does not populate data when database is not empty`() =
        runTest {
            // Given the database is not empty
            `when`(activityRepository.isEmpty()).thenReturn(false)

            // When ViewModel is initialized
            MainViewModel(application, activityRepository, metadataRepository)

            // Then no data should be added
            advanceUntilIdle()
            // verify no addAll calls
            verify(activityRepository, never()).addAll(any())
        }
}
