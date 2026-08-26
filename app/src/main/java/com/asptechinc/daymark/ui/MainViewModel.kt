package com.asptechinc.daymark.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.asptechinc.daymark.models.Activity
import com.asptechinc.daymark.models.Category
import com.asptechinc.daymark.models.ListOptions
import com.asptechinc.daymark.models.Tag
import com.asptechinc.daymark.repository.ActivityRepository
import com.asptechinc.daymark.repository.MetadataRepository
import com.asptechinc.daymark.repository.initialActivities
import com.asptechinc.daymark.repository.initialCategories
import com.asptechinc.daymark.repository.initialTags
import com.asptechinc.daymark.utils.AlarmHelper
import com.asptechinc.daymark.utils.WidgetUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/**
 * Central ViewModel for managing the main activity list, filtering, and sorting.
 *
 * It coordinates data between [ActivityRepository] and [MetadataRepository] (categories/tags).
 */
class MainViewModel(
    application: Application,
    private val repository: ActivityRepository,
    private val metadataRepository: MetadataRepository,
) : AndroidViewModel(application) {
    val listOptions = MutableStateFlow(ListOptions())

    private val allActivities = repository.allActivities
    val categories: StateFlow<List<Category>> =
        metadataRepository.allCategories
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val tags: StateFlow<List<Tag>> =
        metadataRepository.allTags
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val visibleActivities: StateFlow<List<Activity>> =
        combine(allActivities, listOptions) { activities, options ->
            filterAndSort(activities, options)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        checkInitialData()
    }

    private fun checkInitialData() {
        viewModelScope.launch {
            if (repository.isEmpty()) {
                repository.addAll(initialActivities(getApplication()))
                metadataRepository.addAllCategories(initialCategories())
                metadataRepository.addAllTags(initialTags())
            }
        }
    }

    /**
     * Applies the current [ListOptions] (search, filter, sort) to the list of activities.
     *
     * This logic is executed in a background thread via [combine] and [stateIn].
     */
    private fun filterAndSort(
        activities: List<Activity>,
        options: ListOptions,
    ): List<Activity> {
        val now = LocalDateTime.now()
        return activities
            .filter { counter ->
                if (options.searchText.isNotBlank() &&
                    !counter.activityName.contains(options.searchText, ignoreCase = true)
                ) {
                    return@filter false
                }

                options.categoryId?.let { categoryId ->
                    if (counter.categoryId != categoryId) return@filter false
                }

                options.month?.let { month ->
                    if (counter.startDateTime.monthValue != month) return@filter false
                }

                options.year?.let { year ->
                    if (counter.startDateTime.year != year) return@filter false
                }

                options.showArchived?.let { showArchived ->
                    if ((counter.archived == true) != showArchived) {
                        return@filter false
                    }
                }

                options.showCompleted?.let { showCompleted ->
                    val isCompleted =
                        counter.endDateTime != null && counter.endDateTime!!.isBefore(now)
                    if (isCompleted != showCompleted) return@filter false
                }

                true
            }.let { filtered ->
                if (options.sortByName) {
                    filtered.sortedBy { it.activityName.lowercase() }
                } else {
                    filtered // Default sorting by Room if needed, or implement here
                }
            }
    }

    fun updateSearchText(text: String) {
        listOptions.update { it.copy(searchText = text) }
    }

    fun updateFilters(
        categoryId: Int?,
        month: Int?,
        year: Int?,
        showArchived: Boolean?,
        showCompleted: Boolean?,
    ) {
        listOptions.update {
            it.copy(
                categoryId = categoryId,
                month = month,
                year = year,
                showArchived = showArchived,
                showCompleted = showCompleted,
            )
        }
    }

    fun toggleSort() {
        listOptions.update { it.copy(sortByName = !it.sortByName) }
    }

    fun clearFilters() {
        listOptions.value = ListOptions()
    }

    fun addActivity(activity: Activity) =
        viewModelScope.launch {
            val id = repository.add(activity)
            activity.endDateTime?.let { endTime ->
                AlarmHelper.scheduleActivityEndAlarm(
                    getApplication(),
                    id,
                    activity.activityName,
                    endTime
                )
            }
            WidgetUtils.updateAllWidgets(getApplication())
        }

    fun updateActivity(activity: Activity) =
        viewModelScope.launch {
            repository.update(activity)
            AlarmHelper.cancelActivityEndAlarm(getApplication(), activity.id)
            activity.endDateTime?.let { endTime ->
                AlarmHelper.scheduleActivityEndAlarm(
                    getApplication(),
                    activity.id,
                    activity.activityName,
                    endTime
                )
            }
            WidgetUtils.updateAllWidgets(getApplication())
        }

    fun deleteActivity(activity: Activity) =
        viewModelScope.launch {
            repository.remove(activity)
            AlarmHelper.cancelActivityEndAlarm(getApplication(), activity.id)
            WidgetUtils.updateAllWidgets(getApplication())
        }

    fun archiveActivity(activity: Activity) =
        viewModelScope.launch {
            repository.archive(activity)
            AlarmHelper.cancelActivityEndAlarm(getApplication(), activity.id)
            WidgetUtils.updateAllWidgets(getApplication())
        }

    fun reorderActivities(activities: List<Activity>) =
        viewModelScope.launch {
            val updatedActivities =
                activities.mapIndexed { index, activity ->
                    activity.copy(position = index)
                }
            repository.addAll(updatedActivities)
            WidgetUtils.updateAllWidgets(getApplication())
        }

    class Factory(
        private val application: Application,
        private val repository: ActivityRepository,
        private val metadataRepository: MetadataRepository,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(application, repository, metadataRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
