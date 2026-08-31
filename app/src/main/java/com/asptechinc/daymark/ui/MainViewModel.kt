package com.asptechinc.daymark.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.asptechinc.daymark.models.Activity
import com.asptechinc.daymark.models.Category
import com.asptechinc.daymark.models.ListOptions
import com.asptechinc.daymark.models.SortOrder
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
        combine(allActivities, listOptions, categories) { activities, options, categories ->
            filterAndSort(activities, options, categories)
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
     * This logic filters activities by name, category, tags, and date/status. It then
     * sorts the resulting list based on the selected [SortOrder].
     *
     * For date sorting, activities with null end dates are treated as:
     * - [LocalDateTime.MAX] for Ascending (putting them at the bottom)
     * - [LocalDateTime.MIN] for Descending (putting them at the bottom)
     *
     * This logic is executed in a background thread via [combine] and [stateIn].
     *
     * @param activities The raw list of all activities.
     * @param options The user's selected filtering and sorting options.
     * @param categories The current list of categories (needed for category name sorting).
     * @return A filtered and sorted list of activities ready for display.
     */
    private fun filterAndSort(
        activities: List<Activity>,
        options: ListOptions,
        categories: List<Category>,
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

                options.tagId?.let { tagId ->
                    if (!counter.tagIds.contains(tagId)) return@filter false
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
                when (options.sortOrder) {
                    SortOrder.NAME_ASC -> filtered.sortedBy { it.activityName.lowercase() }
                    SortOrder.NAME_DESC -> filtered.sortedByDescending { it.activityName.lowercase() }
                    SortOrder.CATEGORY_ASC -> {
                        filtered.sortedBy { activity ->
                            categories.find { it.id == activity.categoryId }?.name?.lowercase()
                                ?: ""
                        }
                    }

                    SortOrder.CATEGORY_DESC -> {
                        filtered.sortedByDescending { activity ->
                            categories.find { it.id == activity.categoryId }?.name?.lowercase()
                                ?: ""
                        }
                    }

                    SortOrder.START_DATE_ASC -> filtered.sortedBy { it.startDateTime }
                    SortOrder.START_DATE_DESC -> filtered.sortedByDescending { it.startDateTime }
                    SortOrder.END_DATE_ASC -> filtered.sortedBy { it.endDateTime ?: LocalDateTime.MAX }
                    SortOrder.END_DATE_DESC -> {
                        filtered.sortedByDescending {
                            it.endDateTime ?: LocalDateTime.MIN
                        }
                    }

                    SortOrder.POSITION -> filtered
                }
            }
    }

    fun updateSearchText(text: String) {
        listOptions.update { it.copy(searchText = text) }
    }

    /**
     * Updates the current activity filters.
     *
     * @param categoryId Filter by a specific category ID, or null for all categories.
     * @param tagId Filter by a specific tag ID (activity must contain this tag), or null for all tags.
     * @param month Filter by start month (1-12), or null for any month.
     * @param year Filter by start year, or null for any year.
     * @param showArchived true to show only archived, false for only unarchived, or null for both.
     * @param showCompleted true for completed only, false for ongoing only, or null for both.
     */
    fun updateFilters(
        categoryId: Int?,
        tagId: Int?,
        month: Int?,
        year: Int?,
        showArchived: Boolean?,
        showCompleted: Boolean?,
    ) {
        listOptions.update {
            it.copy(
                categoryId = categoryId,
                tagId = tagId,
                month = month,
                year = year,
                showArchived = showArchived,
                showCompleted = showCompleted,
            )
        }
    }

    fun setSortOrder(order: SortOrder) {
        listOptions.update { it.copy(sortOrder = order) }
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
                    endTime,
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
                    endTime,
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
