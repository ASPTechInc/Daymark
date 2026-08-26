package com.asptechinc.daymark

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.asptechinc.daymark.config.AppConfig
import com.asptechinc.daymark.data.AppDatabase
import com.asptechinc.daymark.models.Activity
import com.asptechinc.daymark.repository.ActivityRepository
import com.asptechinc.daymark.repository.MetadataRepository
import com.asptechinc.daymark.ui.MainViewModel
import com.asptechinc.daymark.utils.NotificationHelper
import com.asptechinc.daymark.utils.ThemeManager
import com.asptechinc.daymark.utils.i18n
import com.asptechinc.daymark.utils.styleDialogue
import com.asptechinc.daymark.utils.toOrdinalDateString
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.DateFormatSymbols
import java.time.LocalDateTime
import com.asptechinc.daymark.utils.LayoutManager as AppLayoutManager

class MainActivity : AppCompatActivity() {
    companion object {
        const val APP_PIN_UNLOCKED_ONCE_KEY = "app_pin_unlocked_once"
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Log.i("MainActivity", "Notification permission granted")
            } else {
                Log.i("MainActivity", "Notification permission denied")
            }
        }

    private val viewModel: MainViewModel by viewModels {
        val database = AppDatabase.getDatabase(application)
        val activityRepository = ActivityRepository(database.activityDao())
        val metadataRepository = MetadataRepository(database.categoryDao(), database.tagDao())
        MainViewModel.Factory(application, activityRepository, metadataRepository)
    }

    var editingIndex = -1
    private var searchText: String = ""

    private val createListItemLauncher =
        registerForActivityResult(StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.let(::createListItemFinished)
            }
        }

    private val editListItemLauncher =
        registerForActivityResult(StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.let(::editListItemFinished)
            }
        }

    lateinit var adapter: ActivityAdapter
    private var listView: RecyclerView? = null
    private var emptyStateContainer: android.view.View? = null
    private var itemTouchHelper: ItemTouchHelper? = null

    val prefs by lazy { getSharedPreferences(AppConfig.SETTINGS_PREFS, MODE_PRIVATE)!! }

    val prefsChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            sharedPreferencesChanged(this@MainActivity, key)
        }

    override fun onResume() {
        super.onResume()

        if (::adapter.isInitialized) {
            updateLayoutConfiguration()
            adapter.notifyDataSetChanged()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        ThemeManager.applySavedTheme(this)
        super.onCreate(savedInstanceState)

        val savedPin = prefs.getString(i18n(R.string.app_lock_pin_key), null)
        val unlockedForThisLaunch = prefs.getBoolean(APP_PIN_UNLOCKED_ONCE_KEY, false)

        if (!savedPin.isNullOrBlank() && !unlockedForThisLaunch) {
            startActivity(Intent(this, AppLockActivity::class.java))
            finish()
            return
        }

        if (unlockedForThisLaunch) {
            prefs.edit { putBoolean(APP_PIN_UNLOCKED_ONCE_KEY, false) }
        }

        NotificationHelper.createNotificationChannel(this)
        checkNotificationPermission()

        showMainContent()
        observeViewModel()

        handleIntent(intent)
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    /**
     * Handles deep-links and widget intents, such as the Quick Add action.
     */
    private fun handleIntent(intent: Intent?) {
        if (intent?.action == "com.asptechinc.daymark.ACTION_QUICK_ADD") {
            intent.action = null // Clear action so it doesn't trigger again on rotation
            onCreateNewFabClick()
        }
    }

    /**
     * Observes the ViewModel's state flows and updates the UI accordingly.
     * Uses lifecycle-aware collection to prevent resource leaks.
     */
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.visibleActivities.collectLatest { activities ->
                        adapter.activities.clear()
                        adapter.activities.addAll(activities)
                        adapter.notifyDataSetChanged()

                        val isEmpty = activities.isEmpty()
                        listView?.visibility =
                            if (isEmpty) android.view.View.GONE else android.view.View.VISIBLE
                        emptyStateContainer?.visibility =
                            if (isEmpty) android.view.View.VISIBLE else android.view.View.GONE
                    }
                }
                launch {
                    viewModel.categories.collect { categories ->
                        adapter.categories = categories.toMutableList()
                        adapter.notifyDataSetChanged()
                    }
                }
                launch {
                    viewModel.tags.collect { tags ->
                        adapter.tags = tags.toMutableList()
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    private fun showMainContent() {
        Log.i(this::class.java.simpleName, "Loading MainActivity")

        setContentView(R.layout.activity_main)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        listView = findViewById(R.id.listview)
        emptyStateContainer = findViewById(R.id.empty_state_container)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        toolbar.setOnMenuItemClickListener(::onOptionsItemSelected)

        // Floating add button
        findViewById<FloatingActionButton>(R.id.btn_add_activity).apply {
            setOnClickListener { onCreateNewFabClick() }
        }

        val isGrid = AppLayoutManager.isGridLayout(this)
        if (!isGrid) {
            listView?.addItemDecoration(
                Divider(
                    this,
                    LinearLayoutManager.VERTICAL,
                ),
            )
        }

        prefs.registerOnSharedPreferenceChangeListener(prefsChangeListener)

        adapter =
            ActivityAdapter({ position, menuId ->
                val activity = adapter.activities[position]
                when (menuId) {
                    R.id.edit -> editCounter(position)
                    R.id.duplicate ->
                        viewModel.addActivity(
                            activity.copy(
                                id = 0,
                                position = adapter.itemCount,
                            ),
                        )

                    R.id.share -> shareCounter(position)
                    R.id.archive -> viewModel.archiveActivity(activity)
                    R.id.delete -> viewModel.deleteActivity(activity)
                }
            }, { viewHolder ->
                itemTouchHelper?.startDrag(viewHolder)
            })

        listView?.apply {
            adapter = this@MainActivity.adapter
            itemAnimator = DefaultItemAnimator()
        }

        updateLayoutConfiguration()

        itemTouchHelper =
            ItemTouchHelper(
                object : ItemTouchHelper.SimpleCallback(
                    ItemTouchHelper.UP or ItemTouchHelper.DOWN,
                    0,
                ) {
                    override fun isLongPressDragEnabled(): Boolean = false

                    override fun onMove(
                        recyclerView: RecyclerView,
                        viewHolder: RecyclerView.ViewHolder,
                        target: RecyclerView.ViewHolder,
                    ): Boolean {
                        val from = viewHolder.bindingAdapterPosition
                        val to = target.bindingAdapterPosition
                        if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) return false

                        // Don't allow reordering if user has manual sorting (by name) or filters active?
                        // For now, let's allow it as requested.
                        val reordered =
                            com.asptechinc.daymark.utils.ListReorderer.moveItem(
                                adapter.activities,
                                from,
                                to,
                            )
                        adapter.activities.clear()
                        adapter.activities.addAll(reordered)
                        adapter.notifyItemMoved(from, to)
                        return true
                    }

                    override fun clearView(
                        recyclerView: RecyclerView,
                        viewHolder: RecyclerView.ViewHolder,
                    ) {
                        super.clearView(recyclerView, viewHolder)
                        // Reordering finished, persist to DB
                        viewModel.reorderActivities(adapter.activities)
                    }

                    override fun onSwiped(
                        viewHolder: RecyclerView.ViewHolder,
                        direction: Int,
                    ) = Unit
                },
            )
        itemTouchHelper?.attachToRecyclerView(listView!!)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_main_menu, menu)
        return true
    }

    private fun onCreateNewFabClick() {
        val intent =
            Intent(this@MainActivity, NewActivity::class.java).apply {
                putExtra("isEditing", false)
                putExtra("startDateTime", LocalDateTime.now())
                putStringArrayListExtra(
                    "availableCategoryNames",
                    ArrayList(adapter.categories.map { it.name }),
                )
                putIntegerArrayListExtra(
                    "availableCategoryIds",
                    ArrayList(adapter.categories.map { it.id }),
                )
                putStringArrayListExtra(
                    "availableTagNames",
                    ArrayList(adapter.tags.map { it.name }),
                )
                putIntegerArrayListExtra(
                    "availableTagIds",
                    ArrayList(adapter.tags.map { it.id }),
                )
            }

        createListItemLauncher.launch(intent)
    }

    private fun updateLayoutConfiguration() {
        val isGrid = AppLayoutManager.isGridLayout(this)

        // Update adapter
        if (::adapter.isInitialized) {
            adapter.isGridLayout = isGrid
        }

        // Update LayoutManager
        listView?.apply {
            recycledViewPool.clear()
            layoutManager =
                if (isGrid) {
                    GridLayoutManager(applicationContext, 2)
                } else {
                    LinearLayoutManager(applicationContext)
                }
        }

        // Update Item Decoration (Dividers)
        listView?.apply {
            // Remove existing dividers to avoid duplicates or wrong dividers for grid
            repeat(itemDecorationCount) {
                removeItemDecorationAt(0)
            }

            if (!isGrid) {
                addItemDecoration(
                    Divider(
                        this@MainActivity,
                        LinearLayoutManager.VERTICAL,
                    ),
                )
            }
        }
    }

    private fun sharedPreferencesChanged(
        context: Context,
        key: String?,
    ) {
        if (key == "layout_mode_index") {
            updateLayoutConfiguration()
        }

        adapter.notifyDataSetChanged()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.searchButton -> {
                showSearchDialogue()
                true
            }

            R.id.filterButton -> {
                showFilterDialogue()
                true
            }

            R.id.sortButton -> {
                showSortDialogue()
                true
            }

            R.id.settingsButton -> {
                showSettingsScreen()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }

    // Filter
    private fun applyFilters(
        categoryId: Int?,
        month: Int?,
        year: Int?,
        showArchived: Boolean?,
        showCompleted: Boolean?,
    ) {
        viewModel.updateFilters(categoryId, month, year, showArchived, showCompleted)
    }

    private fun showFilterDialogue() {
        val dialogueView = LayoutInflater.from(this).inflate(R.layout.dialogue_filter, null)

        val categoryInput = dialogueView.findViewById<AutoCompleteTextView>(R.id.category_input)
        val monthInput = dialogueView.findViewById<AutoCompleteTextView>(R.id.month_input)
        val yearInput = dialogueView.findViewById<EditText>(R.id.year_input)
        val archiveCheckbox = dialogueView.findViewById<MaterialCheckBox>(R.id.archived_checkbox)
        val completedCheckbox = dialogueView.findViewById<MaterialCheckBox>(R.id.completed_checkbox)

        val categoryOptions =
            listOf(getString(R.string.filter_all_categories) to null) +
                    adapter.categories.map { it.name to it.id }

        val currentOptions = viewModel.listOptions.value
        val selectedCategoryLabel =
            categoryOptions.firstOrNull { it.second == currentOptions.categoryId }?.first
                ?: getString(R.string.filter_all_categories)

        categoryInput.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                categoryOptions.map { it.first },
            ),
        )
        categoryInput.setText(selectedCategoryLabel, false)

        val monthNames = DateFormatSymbols.getInstance().months.take(12)
        val monthOptions =
            listOf(getString(R.string.filter_any_month) to null) +
                    monthNames.mapIndexed { index, monthName -> monthName to (index + 1) }
        val selectedMonthLabel =
            monthOptions.firstOrNull { it.second == currentOptions.month }?.first
                ?: getString(R.string.filter_any_month)

        monthInput.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                monthOptions.map { it.first },
            ),
        )
        monthInput.setText(selectedMonthLabel, false)
        yearInput.setText(currentOptions.year?.toString().orEmpty())

        archiveCheckbox.checkedState =
            when (currentOptions.showArchived) {
                true -> MaterialCheckBox.STATE_CHECKED
                false -> MaterialCheckBox.STATE_UNCHECKED
                null -> MaterialCheckBox.STATE_INDETERMINATE
            }

        completedCheckbox.checkedState =
            when (currentOptions.showCompleted) {
                true -> MaterialCheckBox.STATE_CHECKED
                false -> MaterialCheckBox.STATE_UNCHECKED
                null -> MaterialCheckBox.STATE_INDETERMINATE
            }

        val dialogue =
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.menu_filter)
                .setView(dialogueView)
                .setPositiveButton(R.string.dialogue_action_apply, null)
                .setNeutralButton(R.string.dialogue_action_clear) { _, _ -> clearFilters() }
                .setNegativeButton(R.string.btn_cancel, null)
                .create()

        dialogue.setOnShowListener {
            styleDialogue(dialogue)
            dialogue
                .getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {
                    val selectedCategory =
                        categoryOptions.firstOrNull { it.first == categoryInput.text.toString() }?.second
                    val selectedMonthValue =
                        monthOptions.firstOrNull { it.first == monthInput.text.toString() }?.second

                    val selectedShowArchived =
                        when (archiveCheckbox.checkedState) {
                            MaterialCheckBox.STATE_CHECKED -> true
                            MaterialCheckBox.STATE_UNCHECKED -> false
                            else -> null
                        }

                    val selectedShowCompleted =
                        when (completedCheckbox.checkedState) {
                            MaterialCheckBox.STATE_CHECKED -> true
                            MaterialCheckBox.STATE_UNCHECKED -> false
                            else -> null
                        }

                    val typedYear =
                        yearInput.text
                            ?.toString()
                            ?.trim()
                            .orEmpty()

                    val selectedYearValue =
                        if (typedYear.isBlank()) {
                            null
                        } else {
                            typedYear.toIntOrNull()
                        }

                    if (typedYear.isNotBlank()) {
                        if (typedYear.length != 4 || selectedYearValue == null) {
                            yearInput.error = getString(R.string.filter_year_invalid)
                            return@setOnClickListener
                        }
                    }

                    yearInput.error = null

                    applyFilters(
                        selectedCategory,
                        selectedMonthValue,
                        selectedYearValue,
                        selectedShowArchived,
                        selectedShowCompleted,
                    )
                    dialogue.dismiss()
                }
        }

        dialogue.show()
    }

    // Search
    private fun applySearch(text: String) {
        viewModel.updateSearchText(text)
        searchText = text
    }

    private fun showSearchDialogue() {
        val dialogueView = LayoutInflater.from(this).inflate(R.layout.dialogue_search, null)
        val searchInput = dialogueView.findViewById<EditText>(R.id.search_input)
        searchInput.setText(searchText)
        searchInput.setSelection(searchInput.text?.length ?: 0)

        val dialogue =
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.menu_search)
                .setView(dialogueView)
                .setPositiveButton(R.string.dialogue_action_apply) { _, _ ->
                    applySearch(
                        searchInput.text
                            ?.toString()
                            ?.trim()
                            .orEmpty(),
                    )
                }.setNeutralButton(R.string.dialogue_action_clear) { _, _ ->
                    applySearch("")
                }.setNegativeButton(R.string.btn_cancel, null)
                .create()

        dialogue.setOnShowListener {
            styleDialogue(dialogue)
        }
        dialogue.show()
    }

    // Sort
    private fun showSortDialogue() {
        viewModel.toggleSort()
    }

    private fun clearFilters() {
        viewModel.clearFilters()
        searchText = ""
    }

    private fun showSettingsScreen() {
        val intent = Intent(this@MainActivity, SettingsActivity::class.java)
        startActivity(intent)
    }

    // Menu options - list
    private fun editCounter(position: Int) {
        editingIndex = position

        val counter = adapter.activities[position]

        val intent =
            Intent(this, NewActivity::class.java).apply {
                putExtra("isEditing", true)
                putExtra("activityName", counter.activityName)
                putExtra("notes", counter.notes)
                putExtra("startDateTime", counter.startDateTime)
                putExtra("endDateTime", counter.endDateTime)
                putExtra("archived", counter.archived)
                putExtra("categoryId", counter.categoryId ?: -1)
                putExtra("tagIds", ArrayList(counter.tagIds))
                putStringArrayListExtra(
                    "availableCategoryNames",
                    ArrayList(adapter.categories.map { it.name }),
                )
                putIntegerArrayListExtra(
                    "availableCategoryIds",
                    ArrayList(adapter.categories.map { it.id }),
                )
                putStringArrayListExtra(
                    "availableTagNames",
                    ArrayList(adapter.tags.map { it.name }),
                )
                putIntegerArrayListExtra(
                    "availableTagIds",
                    ArrayList(adapter.tags.map { it.id }),
                )
            }

        editListItemLauncher.launch(intent)
    }

    private fun shareCounter(position: Int) {
        val counter = adapter.activities[position]

        val text =
            buildString {
                appendLine(counter.activityName)

                if (counter.notes.isNotBlank()) {
                    appendLine()
                    appendLine(counter.notes)
                }

                appendLine()
                appendLine("Started: ${counter.startDateTime.toOrdinalDateString()}")

                counter.endDateTime?.let {
                    appendLine("Ends: ${it.toOrdinalDateString()}")
                }
            }

        val intent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }

        startActivity(Intent.createChooser(intent, null))
    }

    private fun createListItemFinished(data: Intent) {
        // Add a new activity
        val activityName = data.getStringExtra("activityName") ?: return
        val notes = data.getStringExtra("notes") ?: return
        val startDateTime =
            IntentCompat.getSerializableExtra(data, "startDateTime", LocalDateTime::class.java)
                ?: return
        val endDateTime =
            IntentCompat.getSerializableExtra(data, "endDateTime", LocalDateTime::class.java)

        val archived = data.getBooleanExtra("archived", false)
        val categoryId = data.getIntExtra("categoryId", -1).takeIf { it != -1 }
        val tagIds =
            data.getIntegerArrayListExtra("tagIds")?.toMutableList() ?: mutableListOf()

        val newActivity =
            Activity(
                activityName = activityName,
                notes = notes,
                startDateTime = startDateTime,
                endDateTime = endDateTime,
                archived = archived,
                categoryId = categoryId,
                tagIds = tagIds.toMutableList(),
                position = adapter.itemCount,
            )

        viewModel.addActivity(newActivity)
    }

    fun editListItemFinished(data: Intent) {
        val activityToUpdate = adapter.activities[editingIndex]

        val newActivityName = data.getStringExtra("activityName") ?: return
        val newNotes = data.getStringExtra("notes") ?: return
        val newStartDateTime =
            IntentCompat.getSerializableExtra(data, "startDateTime", LocalDateTime::class.java)
                ?: return
        val newEndDateTime =
            IntentCompat.getSerializableExtra(data, "endDateTime", LocalDateTime::class.java)

        val newArchived = data.getBooleanExtra("archived", false)
        val newCategoryId = data.getIntExtra("categoryId", -1).takeIf { it != -1 }
        val newTagIds = data.getIntegerArrayListExtra("tagIds")?.toMutableList() ?: mutableListOf()

        val updated =
            activityToUpdate.copy(
                activityName = newActivityName,
                notes = newNotes,
                startDateTime = newStartDateTime,
                endDateTime = newEndDateTime,
                archived = newArchived,
                categoryId = newCategoryId,
                tagIds = newTagIds,
            )
        viewModel.updateActivity(updated)
    }
}
