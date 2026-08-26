package com.asptechinc.daymark

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.lifecycle.lifecycleScope
import com.asptechinc.daymark.data.AppDatabase
import com.asptechinc.daymark.utils.ThemeManager
import com.asptechinc.daymark.utils.datePicked
import com.asptechinc.daymark.utils.formatDate
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class NewActivity : AppCompatActivity() {
    lateinit var startDateButton: Button
    lateinit var startDateTime: LocalDateTime

    lateinit var endDateButton: Button
    lateinit var endDateTime: LocalDateTime

    private lateinit var archivedCheckBox: MaterialCheckBox
    private lateinit var categoryDropdown: AutoCompleteTextView
    private lateinit var tagChipGroup: ChipGroup
    private lateinit var tagChipExample: Chip
    private lateinit var activityNameEditText: EditText
    private lateinit var notesEditText: EditText

    private var availableCategoryNames = ArrayList<String>()
    private var availableCategoryIds = ArrayList<Int>()

    private var selectedCategoryId: Int? = null
    private var selectedTagIds = mutableListOf<Int>()

    override fun onResume() {
        super.onResume()
        // Refresh categories and tags in case they were modified in Settings
        loadInitialDataFromDb(selectedCategoryId, selectedTagIds)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        super.onCreate(savedInstanceState)

        val isEditing = intent.getBooleanExtra("isEditing", false)
        Log.i("NewActivity", "Creating activity screen (isEditing: $isEditing)")

        activityInit(isEditing)

        // Activity name
        activityNameEditText = findViewById(R.id.activityName)
        activityNameEditText.setText(intent.getStringExtra("activityName") ?: "")
        activityNameEditText.addTextChangedListener(TextChangedListener())

        // Notes
        notesEditText = findViewById(R.id.notes)
        notesEditText.setText(intent.getStringExtra("notes") ?: "")
        notesEditText.addTextChangedListener(TextChangedListener())

        // Start date
        startDateTime =
            IntentCompat.getSerializableExtra(
                intent,
                "startDateTime",
                LocalDateTime::class.java,
            ) ?: LocalDateTime.now()

        val startDateDialog =
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    startDateTime = datePicked(year, month + 1, day)
                    startDateButton.text = formatDate(startDateTime)
                },
                startDateTime.year,
                startDateTime.monthValue - 1,
                startDateTime.dayOfMonth,
            )

        startDateButton =
            findViewById<Button>(R.id.startDateButton).apply {
                setOnClickListener {
                    startDateDialog.show()
                }

                text = formatDate(startDateTime)
            }

        // End date
        endDateTime =
            IntentCompat.getSerializableExtra(intent, "endDateTime", LocalDateTime::class.java)
                ?: LocalDateTime.now()

        val endDateDialog =
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    endDateTime = datePicked(year, month + 1, day)
                    endDateButton.text = formatDate(endDateTime)
                },
                endDateTime.year,
                endDateTime.monthValue - 1,
                endDateTime.dayOfMonth,
            )

        endDateButton =
            findViewById<Button>(R.id.endDateButton).apply {
                setOnClickListener {
                    endDateDialog.show()
                }

                text = formatDate(endDateTime)
            }

        archivedCheckBox = findViewById(R.id.archived_checkbox)
        archivedCheckBox.isChecked = intent.getBooleanExtra("archived", false)

        categoryDropdown = findViewById(R.id.category_dropdown)
        tagChipGroup = findViewById(R.id.tag_chip_group)
        tagChipExample = findViewById(R.id.tag_chip_example)

        selectedCategoryId = intent.getIntExtra("categoryId", -1).takeIf { it != -1 }
        selectedTagIds = intent.getIntegerArrayListExtra("tagIds") ?: mutableListOf()

        findViewById<View>(R.id.btn_manage_categories).setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java).apply {
                putExtra("action", "manage_categories")
            }
            startActivity(intent)
        }

        findViewById<View>(R.id.btn_manage_tags).setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java).apply {
                putExtra("action", "manage_tags")
            }
            startActivity(intent)
        }

        // Save button
        val saveButton = findViewById<Button>(R.id.saveButton)
        saveButton.setOnClickListener {
            val selectedCategoryIdValue =
                categoryDropdown.text?.toString()?.let { categoryName ->
                    val index = availableCategoryNames.indexOf(categoryName)
                    if (index >= 0) availableCategoryIds[index] else null
                } ?: -1

            val selectedTagIdsValue =
                mutableListOf<Int>().apply {
                    for (index in 0 until tagChipGroup.childCount) {
                        val child = tagChipGroup.getChildAt(index)
                        if (child is Chip && child.isChecked) {
                            (child.tag as? Int)?.let(::add)
                        }
                    }
                }

            val intent = Intent()
            intent.putExtra("isEditing", true)
            intent.putExtra("activityName", activityNameEditText.text.toString())
            intent.putExtra("notes", notesEditText.text.toString())
            intent.putExtra("startDateTime", startDateTime)
            intent.putExtra("endDateTime", endDateTime)
            intent.putExtra("archived", archivedCheckBox.isChecked)
            intent.putExtra("categoryId", selectedCategoryIdValue)
            intent.putExtra("tagIds", ArrayList(selectedTagIdsValue))

            setResult(RESULT_OK, intent)
            finish()
        }

        // Cancel button
        val cancelButton = findViewById<Button>(R.id.cancel)
        cancelButton.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun loadInitialDataFromDb(selectedCategoryId: Int?, selectedTagIds: List<Int>) {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@NewActivity)
            val categories = db.categoryDao().getAll()
            val tags = db.tagDao().getAll()

            populateSpinners(
                ArrayList(categories.map { it.name }),
                ArrayList(categories.map { it.id }),
                ArrayList(tags.map { it.name }),
                ArrayList(tags.map { it.id }),
                selectedCategoryId,
                selectedTagIds
            )
        }
    }

    private fun populateSpinners(
        categoryNames: ArrayList<String>,
        categoryIds: ArrayList<Int>,
        tagNames: ArrayList<String>,
        tagIds: ArrayList<Int>,
        selectedCategoryId: Int?,
        selectedTagIds: List<Int>
    ) {
        this.availableCategoryNames = categoryNames
        this.availableCategoryIds = categoryIds

        val categoryAdapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                categoryNames,
            )
        categoryDropdown.setAdapter(categoryAdapter)

        val selectedIndex =
            categoryIds.indexOfFirst { it == selectedCategoryId }
        if (selectedIndex >= 0) {
            categoryDropdown.setText(categoryNames[selectedIndex], false)
        }

        tagChipGroup.removeAllViews()
        for (index in tagNames.indices) {
            val chip =
                Chip(this).apply {
                    text = tagNames[index]
                    isCheckable = true
                    isChecked = selectedTagIds.contains(tagIds[index])
                    tag = tagIds[index]
                }
            tagChipGroup.addView(chip)
        }
        
        tagChipExample.visibility = if (tagNames.isEmpty()) View.VISIBLE else View.GONE
    }

    inner class TextChangedListener : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            val saveButton = this@NewActivity.findViewById<Button>(R.id.saveButton)
            saveButton.isEnabled = !s.isNullOrEmpty()
        }

        override fun beforeTextChanged(
            s: CharSequence?,
            start: Int,
            count: Int,
            after: Int,
        ) = Unit

        override fun onTextChanged(
            s: CharSequence?,
            start: Int,
            before: Int,
            count: Int,
        ) = Unit
    }

    private fun activityInit(isEditing: Boolean) {
        setContentView(R.layout.activity_new)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)

        setSupportActionBar(toolbar)

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.settingsButton -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }

                else -> false
            }
        }
        toolbar.inflateMenu(R.menu.activity_new_menu)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Set title displayed in toolbar
        supportActionBar?.title =
            if (isEditing) {
                getString(R.string.screen_edit_activity)
            } else {
                getString(R.string.screen_new_activity)
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
