package com.asptechinc.daymark

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.asptechinc.daymark.config.AppConfig
import com.asptechinc.daymark.data.AppDatabase
import com.asptechinc.daymark.models.Activity
import com.asptechinc.daymark.repository.ActivityRepository
import com.asptechinc.daymark.repository.MetadataRepository
import com.asptechinc.daymark.ui.MainViewModel
import com.asptechinc.daymark.utils.ThemeManager
import com.asptechinc.daymark.utils.i18n
import com.asptechinc.daymark.utils.styleDialogue
import com.asptechinc.daymark.utils.toOrdinalDateString
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.DateFormatSymbols
import java.time.LocalDateTime

class MainActivity : AppCompatActivity() {
    companion object {
        const val APP_PIN_UNLOCKED_ONCE_KEY = "app_pin_unlocked_once"
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

    val prefs by lazy { getSharedPreferences("settings", MODE_PRIVATE)!! }

    // fixme i ran into some issues with this using a lambda..it also didn't like it if i just passed
    // a method ref. i even tried holding a strong reference to it cuz android docs have it held in a weak hashmap
    val prefsChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            sharedPreferencesChanged(this@MainActivity, sharedPreferences, key)
        }

    override fun onResume() {
        super.onResume()

        if (::adapter.isInitialized) {
            loadSavedData(applicationContext)
            adapter.notifyDataSetChanged()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        super.onCreate(savedInstanceState)

        val savedPin = prefs.getString(i18n(R.string.backup_key_app_pin), null)
        val unlockedForThisLaunch = prefs.getBoolean(APP_PIN_UNLOCKED_ONCE_KEY, false)

        if (!savedPin.isNullOrBlank() && !unlockedForThisLaunch) {
            startActivity(Intent(this, AppLockActivity::class.java))
            finish()
            return
        }

        if (unlockedForThisLaunch) {
            prefs.edit { putBoolean(APP_PIN_UNLOCKED_ONCE_KEY, false) }
        }

        showMainContent()
        observeViewModel()
        checkMigration()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.visibleActivities.collectLatest { activities ->
                        adapter.activities.clear()
                        adapter.activities.addAll(activities)
                        adapter.notifyDataSetChanged()

                        val isEmpty = activities.isEmpty()
                        listView?.visibility = if (isEmpty) android.view.View.GONE else android.view.View.VISIBLE
                        emptyStateContainer?.visibility = if (isEmpty) android.view.View.VISIBLE else android.view.View.GONE
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

    private fun checkMigration() {
        val settingsJsonValue = prefs.getString(AppConfig.SETTINGS_JSON_KEY, null)
        if (settingsJsonValue != null) {
            try {
                // We previously had a format that crashed. Since the user said it's okay to clear data,
                // we check if it's the old format and remove it.
                // The crash was: Expected JsonObject, but had JsonArray as the serialized body of Activity at path: $.0
                // This indicates the whole JSON might be an array or the 'activities' field is an array of arrays.

                prefs.edit { remove(AppConfig.SETTINGS_JSON_KEY) }
                Log.i(this::class.java.simpleName, "Cleared legacy migration data as requested.")
            } catch (e: Exception) {
                Log.e(this::class.java.simpleName, "Failed to clear legacy data", e)
            }
        }
    }

    private fun showMainContent() {
        Log.i(this::class.java.simpleName, "mainactivity getting created")

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

        listView?.addItemDecoration(com.asptechinc.daymark.Divider(this, LinearLayoutManager.VERTICAL))

        prefs.registerOnSharedPreferenceChangeListener(prefsChangeListener)

        adapter =
            ActivityAdapter(this, { position, menuId ->
                val activity = adapter.activities[position]
                when (menuId) {
                    R.id.edit -> editCounter(position)
                    R.id.duplicate -> viewModel.addActivity(activity.copy(id = 0))
                    R.id.share -> shareCounter(position)
                    R.id.archive -> viewModel.archiveActivity(activity)
                    R.id.delete -> viewModel.deleteActivity(activity)
                }
            }, { viewHolder ->
                itemTouchHelper?.startDrag(viewHolder)
            })

        listView?.apply {
            adapter = this@MainActivity.adapter
            layoutManager = LinearLayoutManager(applicationContext)
            itemAnimator = DefaultItemAnimator()
        }

        itemTouchHelper =
            ItemTouchHelper(
                object : ItemTouchHelper.SimpleCallback(
                    ItemTouchHelper.UP or ItemTouchHelper.DOWN,
                    0,
                ) {
                    override fun onMove(
                        recyclerView: RecyclerView,
                        viewHolder: RecyclerView.ViewHolder,
                        target: RecyclerView.ViewHolder,
                    ): Boolean {
                        // Drag and drop is trickier with Room without an explicit order column
                        // For now, let's keep it as is in memory but it won't persist order
                        // unless we add a 'position' field to Activity.
                        return false
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
        menuInflater.inflate(R.menu.menu_main, menu)
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

    private fun sharedPreferencesChanged(
        context: Context,
        sharedPreferences: SharedPreferences,
        key: String?,
    ) {
        Log.i(
            this::class.java.simpleName,
            "SharedPreferences have changed. Reloading activities from settings",
        )

        // todo prolly add a "just saved don't reload" thing...
        loadSavedData(context)

        adapter.notifyDataSetChanged()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.searchButton -> {
                showSearch()
                true
            }

            R.id.filterButton -> {
                showFilter()
                true
            }

            R.id.sortButton -> {
                showSort()
                true
            }

            R.id.settingsButton -> {
                showSettings()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }

    // Filter
    private fun applyFilters(
        categoryId: Int?,
        month: Int?,
        year: Int?,
    ) {
        viewModel.updateFilters(categoryId, month, year)
    }

    private fun showFilter() {
        val dialogueView = LayoutInflater.from(this).inflate(R.layout.dialogue_filter, null)

        val categoryInput = dialogueView.findViewById<AutoCompleteTextView>(R.id.category_input)
        val monthInput = dialogueView.findViewById<AutoCompleteTextView>(R.id.month_input)
        val yearInput = dialogueView.findViewById<EditText>(R.id.year_input)

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

                    if (typedYear.isNotBlank() && selectedYearValue == null) {
                        yearInput.error = getString(R.string.filter_year_invalid)
                        return@setOnClickListener
                    }

                    if (selectedMonthValue == null && selectedYearValue != null) {
                        yearInput.error = getString(R.string.filter_year_requires_month)
                        return@setOnClickListener
                    }

                    yearInput.error = null
                    applyFilters(selectedCategory, selectedMonthValue, selectedYearValue)
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

    private fun showSearch() {
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
    private fun showSort() {
        viewModel.toggleSort()
    }

    private fun clearFilters() {
        viewModel.clearFilters()
        searchText = ""
    }

    private fun styleDialogueButtons(dialogue: androidx.appcompat.app.AlertDialog) {
        val positiveButton = dialogue.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
        val negativeButton = dialogue.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)
        val neutralButton = dialogue.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL)

        val positiveColour =
            MaterialColors.getColor(
                this,
                com.google.android.material.R.attr.colorOnTertiary,
                getColor(R.color.primary),
            )
        val negativeColour =
            MaterialColors.getColor(
                this,
                com.google.android.material.R.attr.colorSecondary,
                getColor(R.color.secondary),
            )
        val neutralColour =
            MaterialColors.getColor(
                this,
                com.google.android.material.R.attr.colorOnSurfaceVariant,
                getColor(R.color.on_surface_variant),
            )

        listOf(positiveButton, negativeButton, neutralButton).forEach { button ->
            button.setTypeface(Typeface.DEFAULT_BOLD)
            button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        }

        positiveButton.setTextColor(positiveColour)
        negativeButton.setTextColor(negativeColour)
        neutralButton.setTextColor(neutralColour)
    }

    private fun updateList() {
        // ViewModel observes changes
    }

    private fun showSettings() {
        val intent = Intent(this@MainActivity, SettingsActivity::class.java)
        startActivity(intent)
    }

    private fun clearCounters() {
        val builder = MaterialAlertDialogBuilder(this)

        builder.setTitle(i18n(R.string.confirm_dialogue_title))
        builder.setMessage(i18n(R.string.confirm_dialogue_prompt))

        builder.setPositiveButton(i18n(R.string.confirm_dialogue_yes)) { dialogue, _ ->
            viewModel.clearAllActivities()
            dialogue.dismiss()
        }

        builder.setNegativeButton(i18n(R.string.confirm_dialogue_no)) { dialogue, _ -> dialogue.dismiss() }

        val alert = builder.create()
        alert.show()
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

    private fun resetCounter(position: Int) {
        val counter = adapter.activities[position]
        viewModel.resetActivity(counter)
    }

    private fun createListItemFinished(data: Intent) {
        // Add a new activity
        val activityName = data.getStringExtra("activityName") ?: return
        val notes = data.getStringExtra("notes") ?: return
        val startDateTime =
            IntentCompat.getSerializableExtra(data, "startDateTime", LocalDateTime::class.java) ?: return
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
            )

        viewModel.addActivity(newActivity)
    }

    fun editListItemFinished(data: Intent) {
        val activityToUpdate = adapter.activities[editingIndex]

        val newActivityName = data.getStringExtra("activityName") ?: return
        val newNotes = data.getStringExtra("notes") ?: return
        val newStartDateTime =
            IntentCompat.getSerializableExtra(data, "startDateTime", LocalDateTime::class.java) ?: return
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

    private fun setActivities(newActivities: List<Activity>) {
        viewModel.importActivities(newActivities)
    }

    private fun loadSavedData(context: Context) {
        // Activities handled by ViewModel
    }
}
