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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.core.content.edit
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.asptechinc.daymark.config.AppConfig
import com.asptechinc.daymark.models.Activity
import com.asptechinc.daymark.models.Category
import com.asptechinc.daymark.models.ListOptions
import com.asptechinc.daymark.models.Tag
import com.asptechinc.daymark.repository.ActivityRepository
import com.asptechinc.daymark.repository.initialActivities
import com.asptechinc.daymark.repository.initialCategories
import com.asptechinc.daymark.repository.initialTags
import com.asptechinc.daymark.utils.ActivityPersistence
import com.asptechinc.daymark.utils.ThemeManager
import com.asptechinc.daymark.utils.i18n
import com.asptechinc.daymark.utils.styleDialogue
import com.asptechinc.daymark.utils.toOrdinalDateString
import com.fatboyindustrial.gsonjodatime.Converters
import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.jsonObject
import com.github.salomonbrys.kotson.registerTypeAdapter
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.stream.JsonToken
import com.mcxiaoke.koi.ext.dateNow
import com.mcxiaoke.koi.ext.find
import com.mcxiaoke.koi.ext.onClick
import org.joda.time.DateTime
import java.text.DateFormatSymbols

class MainActivity : AppCompatActivity() {
    companion object {
        const val APP_PIN_UNLOCKED_ONCE_KEY = "app_pin_unlocked_once"
    }

    private val listOptions = ListOptions()
    private val activityRepository = ActivityRepository()

    var editingIndex = -1
    private var selectedCategoryId: Int? = null
    private var selectedMonth: Int? = null
    private var selectedYear: Int? = null
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

    val deprecatedGson = Converters.registerDateTime(GsonBuilder()).create()!!
    val gson =
        GsonBuilder()
            .registerTypeAdapter<Activity> {
                write {
                    beginArray()
                    value(it.activityName)
                    value(it.notes)
                    value(it.startDateTime.toString())
                    value(it.endDateTime?.toString())
                    value(it.archived.toString())
                    value(it.categoryId.toString())
                    value(it.tagIds.toString())
                    endArray()
                }

                read {
                    beginArray()
                    val activityName = nextString()
                    val notes = nextString()
                    val startDateTime = nextString()
                    val endDateTime =
                        if (peek() == JsonToken.NULL) {
                            nextNull()
                            null
                        } else {
                            DateTime(nextString())
                        }
                    val archived = nextString()
                    val category = nextString()
                    val tags = nextString()
                    endArray()

                    Activity(
                        activityName = activityName,
                        notes = notes,
                        startDateTime = DateTime(startDateTime),
                        endDateTime = endDateTime?.let { DateTime(it) },
                        archived = archived.toBooleanStrictOrNull() ?: archived.toBoolean(),
                        categoryId = ActivityPersistence.parseCategoryId(category),
                        tagIds = ActivityPersistence.parseTagIds(tags),
                    )
                }
            }.create()

    lateinit var adapter: ActivityAdapter
    val listView by lazy { find<RecyclerView>(R.id.listview) }
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
    }

    private fun showMainContent() {
        Log.i(this::class.java.simpleName, "mainactivity getting created")

        setContentView(R.layout.activity_main)

        val toolbar = find<MaterialToolbar>(R.id.toolbar)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        toolbar.setOnMenuItemClickListener(::onOptionsItemSelected)

        // Floating add button
        find<FloatingActionButton>(R.id.btn_add_activity).apply {
            onClick { onCreateNewFabClick() }
        }

        listView.addItemDecoration(Divider(this, LinearLayoutManager.VERTICAL))

        prefs.registerOnSharedPreferenceChangeListener(prefsChangeListener)

        adapter =
            ActivityAdapter(this, { position, menuId ->
                when (menuId) {
                    // Edit menu
                    R.id.edit -> editCounter(position)

                    // Duplicate menu
                    R.id.duplicate -> duplicateCounter(position)

                    // Share menu
                    R.id.share -> shareCounter(position)

                    // Archive menu
                    R.id.archive -> archiveCounter(position)

                    // Delete menu
                    R.id.delete -> deleteCounter(position)
                }
            }, { viewHolder ->
                itemTouchHelper?.startDrag(viewHolder)
            })

        listView.apply {
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
                        val fromPosition = viewHolder.bindingAdapterPosition
                        val toPosition = target.bindingAdapterPosition
                        if (fromPosition == RecyclerView.NO_POSITION || toPosition == RecyclerView.NO_POSITION) {
                            return false
                        }

                        val visibleItems = adapter.activities.toMutableList()
                        if (fromPosition !in visibleItems.indices || toPosition !in visibleItems.indices) {
                            return false
                        }

                        val movedItem = visibleItems.removeAt(fromPosition)
                        visibleItems.add(toPosition, movedItem)

                        adapter.activities.clear()
                        adapter.activities.addAll(visibleItems)
                        adapter.notifyDataSetChanged()

                        val remainingItems =
                            activityRepository.activities.filterNot { item ->
                                visibleItems.any { candidate -> candidate === item }
                            }

                        activityRepository.setAll(visibleItems + remainingItems)
                        refreshVisibleActivities()
                        saveData()
                        return true
                    }

                    override fun onSwiped(
                        viewHolder: RecyclerView.ViewHolder,
                        direction: Int,
                    ) = Unit
                },
            )
        itemTouchHelper?.attachToRecyclerView(listView)

        initialiseSavedData(applicationContext)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    private fun onCreateNewFabClick() {
        val intent =
            Intent(this@MainActivity, NewActivity::class.java).apply {
                putExtra("isEditing", false)
                putExtra("startDateTime", DateTime.now())
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
    private fun matchesCurrentFilters(counter: Activity): Boolean {
        if (listOptions.searchText.isNotBlank() &&
            !counter.activityName.contains(listOptions.searchText, ignoreCase = true)
        ) {
            return false
        }

        listOptions.categoryId?.let { categoryId ->
            if (counter.categoryId != categoryId) return false
        }

        listOptions.month?.let { month ->
            if (counter.startDateTime.monthOfYear != month) return false
        }

        listOptions.year?.let { year ->
            if (counter.startDateTime.year != year) return false
        }

        return true
    }

    private fun refreshVisibleActivities() {
        val visibleActivities =
            activityRepository.activities
                .filter(::matchesCurrentFilters)
                .toMutableList()

        if (listOptions.sortByName) {
            visibleActivities.sortBy { it.activityName.lowercase() }
        }

        adapter.activities.clear()
        adapter.activities.addAll(visibleActivities)
        adapter.notifyDataSetChanged()
    }

    private fun applyFilters(
        categoryId: Int?,
        month: Int?,
        year: Int?,
    ) {
        listOptions.categoryId = categoryId
        listOptions.month = month
        listOptions.year = year
        selectedCategoryId = categoryId
        selectedMonth = month
        selectedYear = year
        refreshVisibleActivities()
    }

    private fun showFilter() {
        val dialogueView = LayoutInflater.from(this).inflate(R.layout.dialogue_filter, null)

        val categoryInput = dialogueView.findViewById<AutoCompleteTextView>(R.id.category_input)
        val monthInput = dialogueView.findViewById<AutoCompleteTextView>(R.id.month_input)
        val yearInput = dialogueView.findViewById<EditText>(R.id.year_input)

        val categoryOptions =
            listOf(getString(R.string.filter_all_categories) to null) +
                adapter.categories.map { it.name to it.id }
        val selectedCategoryLabel =
            categoryOptions.firstOrNull { it.second == selectedCategoryId }?.first
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
            monthOptions.firstOrNull { it.second == selectedMonth }?.first
                ?: getString(R.string.filter_any_month)

        monthInput.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                monthOptions.map { it.first },
            ),
        )
        monthInput.setText(selectedMonthLabel, false)
        yearInput.setText(selectedYear?.toString().orEmpty())

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
        listOptions.searchText = text
        searchText = text
        refreshVisibleActivities()
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
    private fun applyNameSort() {
        listOptions.sortByName = true
        refreshVisibleActivities()
    }

    private fun applyStartDateSort() {
        listOptions.sortByName = false
        refreshVisibleActivities()
    }

    private fun showSort() {
        val nextSortByName = !listOptions.sortByName
        if (nextSortByName) {
            applyNameSort()
        } else {
            applyStartDateSort()
        }
    }

    private fun clearFilters() {
        listOptions.searchText = ""
        listOptions.categoryId = null
        listOptions.month = null
        listOptions.year = null
        listOptions.sortByName = false
        selectedCategoryId = null
        selectedMonth = null
        selectedYear = null
        searchText = ""

        refreshVisibleActivities()
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
        refreshVisibleActivities()
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
            activityRepository.clear()
            adapter.activities.clear()
            adapter.notifyDataSetChanged()

            saveData()

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

    private fun duplicateCounter(position: Int) {
        val counter = adapter.activities[position]

        val duplicate = counter.copy()

        val sourceIndex = activityRepository.indexOf(counter)
        if (sourceIndex >= 0) {
            activityRepository.addAt(sourceIndex + 1, duplicate)
        } else {
            activityRepository.add(duplicate)
        }
        refreshVisibleActivities()

        saveData()
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

    private fun archiveCounter(position: Int) {
        val counter = adapter.activities[position]
        val sourceIndex = activityRepository.indexOf(counter)
        if (sourceIndex >= 0) {
            activityRepository.archive(sourceIndex)
        }

        refreshVisibleActivities()
        saveData()
    }

    private fun deleteCounter(position: Int) {
        val counterToDelete = adapter.activities.removeAt(position)
        activityRepository.remove(counterToDelete)
        refreshVisibleActivities()
        saveData()
    }

    private fun resetCounter(position: Int) {
        val counter = adapter.activities[position]
        val sourceIndex = activityRepository.indexOf(counter)
        if (sourceIndex >= 0) {
            activityRepository.reset(sourceIndex)
        }

        refreshVisibleActivities()
        saveData()
    }

    private fun createListItemFinished(data: Intent) {
        // Add a new activity
        val activityName = data.getStringExtra("activityName") ?: return
        val notes = data.getStringExtra("notes") ?: return
        val startDateTime =
            IntentCompat.getSerializableExtra(data, "startDateTime", DateTime::class.java) ?: return
        val endDateTime =
            IntentCompat.getSerializableExtra(data, "endDateTime", DateTime::class.java)

        val archived = data.getBooleanExtra("archived", false)
        val categoryId = data.getIntExtra("categoryId", -1).takeIf { it != -1 }
        val tagIds =
            data.getIntegerArrayListExtra("tagIds")?.toMutableList() ?: mutableListOf()

        val newCounter =
            Activity(
                activityName = activityName,
                notes = notes,
                startDateTime = startDateTime,
                endDateTime = endDateTime?.let { DateTime(it) },
                archived = archived,
                categoryId = categoryId,
                tagIds = tagIds.toMutableList(),
            )

        activityRepository.add(newCounter)
        refreshVisibleActivities()

        saveData()
    }

    fun editListItemFinished(data: Intent) {
        val counterToUpdate = adapter.activities[editingIndex]
        val sourceIndex = activityRepository.indexOf(counterToUpdate)

        val newActivityName = data.getStringExtra("activityName") ?: return
        val newNotes = data.getStringExtra("notes") ?: return
        val newStartDateTime =
            IntentCompat.getSerializableExtra(data, "startDateTime", DateTime::class.java) ?: return
        val newEndDateTime =
            IntentCompat.getSerializableExtra(data, "endDateTime", DateTime::class.java) ?: return

        val newArchived = data.getBooleanExtra("archived", false)
        val newCategoryId = data.getIntExtra("categoryId", -1).takeIf { it != -1 }
        val newTagIds = data.getIntegerArrayListExtra("tagIds")?.toMutableList() ?: mutableListOf()

        if (sourceIndex >= 0) {
            val updated =
                counterToUpdate.copy(
                    activityName = newActivityName,
                    notes = newNotes,
                    startDateTime = newStartDateTime,
                    endDateTime = newEndDateTime,
                    archived = newArchived,
                    categoryId = newCategoryId,
                    tagIds = newTagIds,
                )
            activityRepository.update(sourceIndex, updated)
        }

        refreshVisibleActivities()
        saveData()
    }

    fun saveData() {
        Log.i(
            this::class.java.simpleName,
            "Settings are being saved in JSON format using SharedPreferences.)",
        )

        val settingsJson: JsonObject =
            jsonObject(
                "saveFormatVersion" to AppConfig.SAVE_FILE_VERSION,
                "savedWithAppVersion" to BuildConfig.VERSION_NAME,
                "savedOnDate" to dateNow(),
                "activities" to gson.toJsonTree(activityRepository.activities),
                "tags" to gson.toJsonTree(adapter.tags),
                "categories" to gson.toJsonTree(adapter.categories),
            )

        prefs.edit().apply {
            putString(AppConfig.SETTINGS_JSON_KEY, settingsJson.toString())
            apply()
        }

        Log.i(
            this::class.java.simpleName,
            "Settings have been saved in JSON format using SharedPreferences.",
        )
    }

    private fun initialiseSavedData(context: Context) {
        // Loads saved data from shared preferences
        // If none exists, initialises with sample data

        Log.i(this::class.java.simpleName, "Loading saved data...")
        // we do not yet ever clear sharedprefs..it's our backup for now, in
        // case a rollout screws things up
        // todo in the future, delete this..maybe after a few versions
        // once we know we're in the clear
        val deprecatedPrefs = getPreferences(MODE_PRIVATE)
        val deprecatedJson = deprecatedPrefs.getString("counter-list-json", null)

        // conversion from 1.0 data format when we stored it in default sharedprefs
        // so convert it to JSON and clear it, write out to new sharedprefs JSON format
        if (deprecatedJson != null) {
            Log.i(this::class.java.simpleName, "upgrading settings data from 1.0")
            // we only perform the 1.0 -> 1.1 upgrade if the JSON output doesn't exist
            // (so it only runs once)
            // since shared prefs is migrated from after this first run, and kept until we
            // decide to (versions later), delete them safely.
            upgradeSavedEntriesFromV1(deprecatedJson, deprecatedPrefs)
        }

        loadSavedData(context)
    }

    /**
     * version 1.0, we stored data in default shared preferences and used
     * gson jodatime deserialization. we can't use kotson here from what
     * I could tell...seems like the output JSON isn't that great from version
     * 1.0...
     */
    private fun upgradeSavedEntriesFromV1(
        deprecatedJson: String,
        deprecatedPrefs: SharedPreferences,
    ) {
        setActivities(deprecatedGson.fromJson<MutableList<Activity>>(deprecatedJson))

        deprecatedPrefs.edit().apply {
            clear()
            apply()
        }

        saveData()
    }

    private fun setActivities(newActivities: List<Activity>) {
        activityRepository.setAll(newActivities)
        refreshVisibleActivities()
    }

    private fun loadSavedData(context: Context) {
        val settingsJsonValue = prefs.getString(AppConfig.SETTINGS_JSON_KEY, null)
        if (settingsJsonValue == null) {
            setActivities(initialActivities(context))
            adapter.categories = initialCategories()
            adapter.tags = initialTags()
            return
        }

        val fileJsonElement = JsonParser.parseString(settingsJsonValue).asJsonObject

        setActivities(gson.fromJson<MutableList<Activity>>(fileJsonElement["activities"]))
        adapter.categories = gson.fromJson<MutableList<Category>>(fileJsonElement["categories"])
        adapter.tags = gson.fromJson<MutableList<Tag>>(fileJsonElement["tags"])

        Log.i(
            this::class.java.simpleName,
            "Settings were loaded. Activities count size: ${activityRepository.activities.size}",
        )
    }
}
