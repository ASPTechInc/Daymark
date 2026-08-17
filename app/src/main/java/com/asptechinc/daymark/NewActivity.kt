package com.asptechinc.daymark

import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.IntentCompat
import com.asptechinc.daymark.utils.ThemeManager
import com.asptechinc.daymark.utils.datePicked
import com.asptechinc.daymark.utils.formatDate
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.mcxiaoke.koi.ext.find
import org.joda.time.DateTime

class NewActivity : AppCompatActivity() {
    lateinit var startDateButton: Button
    lateinit var startDateTime: DateTime

    lateinit var endDateButton: Button
    lateinit var endDateTime: DateTime

    private lateinit var archivedCheckBox: MaterialCheckBox
    private lateinit var categoryDropdown: AutoCompleteTextView
    private lateinit var tagChipGroup: ChipGroup
    private lateinit var activityNameEditText: EditText
    private lateinit var notesEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        super.onCreate(savedInstanceState)

        val isEditing = intent.getBooleanExtra("isEditing", false)

        activityInit(isEditing)

        // Activity name
        activityNameEditText = find<EditText>(R.id.activityName)
        activityNameEditText.setText(intent.getStringExtra("activityName") ?: "")
        activityNameEditText.addTextChangedListener(TextChangedListener())

        // Notes
        notesEditText = find<EditText>(R.id.notes)
        notesEditText.setText(intent.getStringExtra("notes") ?: "")
        notesEditText.addTextChangedListener(TextChangedListener())

        // null along with other values if we're creating a new entry.
        // else it'll be the existing data that we're editing

        // Start date
        // It is required.
        startDateTime =
            requireNotNull(
                IntentCompat.getSerializableExtra(
                    intent,
                    "startDateTime",
                    DateTime::class.java,
                ),
            ) { "startDateTime is required" }

        val startDateDialog =
            DatePickerDialog(
                this,
                OnDateSetListener { datePicker, year, month, day ->
                    startDateTime = datePicked(datePicker, year, month + 1, day)
                    startDateButton.text = formatDate(startDateTime)
                },
                startDateTime.year,
                startDateTime.monthOfYear - 1,
                startDateTime.dayOfMonth,
            )

        startDateButton =
            find<Button>(R.id.startDateButton).apply {
                setOnClickListener {
                    startDateDialog.show()
                }

                text = formatDate(startDateTime)
            }

        // End date
        // It is optional and its default value is the current date.
        endDateTime = IntentCompat.getSerializableExtra(intent, "endDateTime", DateTime::class.java)
            ?: DateTime.now() // .minusDays(5)

        val endDateDialog =
            DatePickerDialog(
                this,
                OnDateSetListener { datePicker, year, month, day ->
                    endDateTime = datePicked(datePicker, year, month + 1, day)
                    endDateButton.text = formatDate(endDateTime)
                },
                endDateTime.year,
                endDateTime.monthOfYear - 1,
                endDateTime.dayOfMonth,
            )

        endDateButton =
            find<Button>(R.id.endDateButton).apply {
                setOnClickListener {
                    endDateDialog.show()
                }

                text = formatDate(endDateTime)
            }

        archivedCheckBox = find<MaterialCheckBox>(R.id.archived_checkbox)
        archivedCheckBox.isChecked = intent.getBooleanExtra("archived", false)

        categoryDropdown = find<AutoCompleteTextView>(R.id.category_dropdown)
        tagChipGroup = find<ChipGroup>(R.id.tag_chip_group)

        val availableCategoryNames = intent.getStringArrayListExtra("availableCategoryNames")
        val availableCategoryIds = intent.getIntegerArrayListExtra("availableCategoryIds")
        val availableTagNames = intent.getStringArrayListExtra("availableTagNames")
        val availableTagIds = intent.getIntegerArrayListExtra("availableTagIds")
        val selectedCategoryId = intent.getIntExtra("categoryId", -1).takeIf { it != -1 }
        val selectedTagIds = intent.getIntegerArrayListExtra("tagIds") ?: arrayListOf()

        if (!availableCategoryNames.isNullOrEmpty() && !availableCategoryIds.isNullOrEmpty()) {
            val categoryNames = availableCategoryNames.toList()
            val categoryAdapter =
                ArrayAdapter(
                    this,
                    android.R.layout.simple_dropdown_item_1line,
                    categoryNames,
                )
            categoryDropdown.setAdapter(categoryAdapter)

            val selectedIndex =
                availableCategoryIds.indexOfFirst { it == selectedCategoryId }
            if (selectedIndex >= 0) {
                categoryDropdown.setText(categoryNames[selectedIndex], false)
            }
        }

        if (!availableTagNames.isNullOrEmpty() && !availableTagIds.isNullOrEmpty()) {
            for (index in availableTagNames.indices) {
                val chip =
                    Chip(this).apply {
                        text = availableTagNames[index]
                        isCheckable = true
                        isChecked = selectedTagIds.contains(availableTagIds[index])
                        tag = availableTagIds[index]
                    }
                tagChipGroup.addView(chip)
            }
        }

        // Save button
        val saveButton = find<Button>(R.id.saveButton)
        saveButton.setOnClickListener {
            val selectedCategoryIdValue =
                categoryDropdown.text?.toString()?.let { categoryName ->
                    val index = availableCategoryNames?.indexOf(categoryName) ?: -1
                    if (index >= 0) availableCategoryIds?.get(index) else null
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
        val cancelButton = find<Button>(R.id.cancel)
        cancelButton.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    inner class TextChangedListener : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            val saveButton = this@NewActivity.find<Button>(R.id.saveButton)
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
        val toolbar = find<MaterialToolbar>(R.id.toolbar)

        setSupportActionBar(toolbar)

        // Replace ellipsis in toolbar with a tune icon
        toolbar.overflowIcon =
            AppCompatResources.getDrawable(
                this,
                R.drawable.ic_tune,
            )
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.settingsButton -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }

                else -> false
            }
        }
        toolbar.inflateMenu(R.menu.menu_toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Set title displayed in toolbar
        supportActionBar?.title =
            if (isEditing) {
                getString(R.string.screen_edit_activity)
            } else {
                getString(R.string.screen_new_activity)
            }
    }
}
