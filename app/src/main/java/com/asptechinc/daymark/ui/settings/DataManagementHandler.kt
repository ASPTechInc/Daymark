package com.asptechinc.daymark.ui.settings

import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceFragmentCompat
import com.asptechinc.daymark.R
import com.asptechinc.daymark.models.Category
import com.asptechinc.daymark.models.Tag
import com.asptechinc.daymark.repository.BackupRepository
import com.asptechinc.daymark.repository.BackupState
import com.asptechinc.daymark.repository.initialActivities
import com.asptechinc.daymark.repository.initialCategories
import com.asptechinc.daymark.repository.initialTags
import com.asptechinc.daymark.utils.AlarmHelper
import com.asptechinc.daymark.utils.dpToPx
import com.asptechinc.daymark.utils.i18n
import com.asptechinc.daymark.utils.showStyled
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class DataManagementHandler(
    private val fragment: PreferenceFragmentCompat,
    private val repository: BackupRepository,
) {
    fun confirmClearAllActivities() {
        val context = fragment.requireContext()
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.settings_label_clear_all)
            .setMessage(R.string.settings_clear_all_confirm)
            .setPositiveButton(R.string.btn_clear) { _, _ ->
                fragment.lifecycleScope.launch {
                    val state = repository.loadBackupState()
                    state.activities.clear()
                    repository.saveBackupState(state)
                    AlarmHelper.rescheduleAllAlarms(context)
                    Toast
                        .makeText(
                            context,
                            context.i18n(R.string.settings_clear_all_done),
                            Toast.LENGTH_LONG,
                        ).show()
                }
            }.setNegativeButton(R.string.btn_cancel, null)
            .showStyled()
    }

    fun showAppStorageDialogue() {
        fragment.lifecycleScope.launch {
            val state = repository.loadBackupState()
            val categoryCount = state.categories.size
            val tagsCount = state.tags.size
            val archiveCountChecked =
                state.activities.count { it.archived == true }
            val archiveCountUnchecked =
                state.activities.count { it.archived == false }
            val activitiesCount = state.activities.size
            val context = fragment.requireContext()

            val dialogView =
                fragment.layoutInflater.inflate(
                    R.layout.dialogue_storage,
                    null,
                )
            dialogView.findViewById<TextView>(R.id.categories_count).text =
                categoryCount.toString()

            dialogView.findViewById<TextView>(R.id.tags_count).text =
                tagsCount.toString()

            dialogView.findViewById<TextView>(R.id.archived_count).text =
                archiveCountChecked.toString()

            dialogView.findViewById<TextView>(R.id.unarchived_count).text =
                archiveCountUnchecked.toString()

            dialogView.findViewById<TextView>(R.id.total_activities_count).text =
                activitiesCount.toString()

            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.settings_view_app_data)
                .setView(dialogView)
                .setNegativeButton(R.string.btn_close, null)
                .showStyled()
        }
    }

    fun confirmFullReset() {
        val context = fragment.requireContext()
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.settings_label_reset)
            .setMessage(R.string.settings_reset_confirm)
            .setPositiveButton(R.string.btn_yes) { _, _ ->
                fragment.lifecycleScope.launch {
                    val resetState =
                        BackupState(
                            activities = initialActivities(context),
                            categories = initialCategories(),
                            tags = initialTags(),
                        )
                    repository.saveBackupState(resetState)
                    AlarmHelper.rescheduleAllAlarms(context)
                    Toast
                        .makeText(
                            context,
                            context.i18n(R.string.settings_reset_done),
                            Toast.LENGTH_LONG,
                        ).show()
                }
            }.setNegativeButton(R.string.btn_no, null)
            .showStyled()
    }

    fun showManageCategoriesDialogue() {
        fragment.lifecycleScope.launch {
            val state = repository.loadBackupState()
            val categoryNames = state.categories.map { it.name }.toTypedArray()
            val context = fragment.requireContext()

            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.settings_manage_categories)
                .setMessage(
                    if (categoryNames.isEmpty()) {
                        context.i18n(R.string.settings_manage_categories_empty)
                    } else {
                        null
                    },
                ).setItems(categoryNames) { _, which ->
                    showCategoryActionDialogue(state, state.categories[which].id)
                }.setPositiveButton(R.string.btn_add) { _, _ ->
                    showAddCategoryDialogue(state)
                }.setNegativeButton(R.string.btn_close, null)
                .showStyled()
        }
    }

    fun showManageTagsDialogue() {
        fragment.lifecycleScope.launch {
            val state = repository.loadBackupState()
            val tagNames = state.tags.map { it.name }.toTypedArray()
            val context = fragment.requireContext()

            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.settings_manage_tags)
                .setMessage(
                    if (tagNames.isEmpty()) {
                        context.i18n(R.string.settings_manage_tags_empty)
                    } else {
                        null
                    },
                ).setItems(tagNames) { _, which ->
                    showTagActionDialogue(state, state.tags[which].id)
                }.setPositiveButton(R.string.btn_add) { _, _ ->
                    showAddTagDialogue(state)
                }.setNegativeButton(R.string.btn_close, null)
                .showStyled()
        }
    }

    private fun showCategoryActionDialogue(
        state: BackupState,
        categoryId: Int,
    ) {
        val category = state.categories.firstOrNull { it.id == categoryId } ?: return
        val context = fragment.requireContext()

        val view = fragment.layoutInflater.inflate(R.layout.dialogue_actions, null)

        val explanation = view.findViewById<TextView>(R.id.explanation)
        val renameAction = view.findViewById<TextView>(R.id.renameAction)
        val deleteAction = view.findViewById<TextView>(R.id.deleteAction)

        explanation.text = context.i18n(R.string.dialogue_action_prompt)

        val textColour =
            MaterialColors.getColor(
                context,
                com.google.android.material.R.attr.colorOnSurface,
                context.getColor(R.color.on_surface),
            )

        explanation.setTextColor(
            MaterialColors.getColor(
                context,
                com.google.android.material.R.attr.colorOnTertiary,
                context.getColor(R.color.on_surface_variant),
            ),
        )

        renameAction.setTextColor(textColour)
        deleteAction.setTextColor(textColour)

        renameAction.setOnClickListener {
            showRenameCategoryDialogue(state, categoryId)
        }

        deleteAction.setOnClickListener {
            confirmDeleteCategory(state, categoryId)
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(category.name)
            .setView(view)
            .setNegativeButton(R.string.btn_cancel, null)
            .showStyled()
    }

    private fun showTagActionDialogue(
        state: BackupState,
        tagId: Int,
    ) {
        val tag = state.tags.firstOrNull { it.id == tagId } ?: return
        val context = fragment.requireContext()
        val view = fragment.layoutInflater.inflate(R.layout.dialogue_actions, null)

        val explanation = view.findViewById<TextView>(R.id.explanation)
        val renameAction = view.findViewById<TextView>(R.id.renameAction)
        val deleteAction = view.findViewById<TextView>(R.id.deleteAction)

        explanation.text = context.i18n(R.string.dialogue_action_prompt)

        val textColour =
            MaterialColors.getColor(
                context,
                com.google.android.material.R.attr.colorOnSurface,
                context.getColor(R.color.on_surface),
            )

        explanation.setTextColor(
            MaterialColors.getColor(
                context,
                com.google.android.material.R.attr.colorOnTertiary,
                context.getColor(R.color.on_surface_variant),
            ),
        )

        renameAction.setTextColor(textColour)
        deleteAction.setTextColor(textColour)

        renameAction.setOnClickListener {
            showRenameTagDialogue(state, tagId)
        }

        deleteAction.setOnClickListener {
            confirmDeleteTag(state, tagId)
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(tag.name)
            .setView(view)
            .setNegativeButton(R.string.btn_cancel, null)
            .showStyled()
    }

    private fun showAddCategoryDialogue(state: BackupState) {
        val context = fragment.requireContext()
        val input = EditText(context).apply { hint = context.i18n(R.string.hint_new_item_name) }

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.settings_manage_categories)
            .setView(input)
            .setPositiveButton(R.string.btn_add) { _, _ ->
                val name =
                    input.text
                        ?.toString()
                        ?.trim()
                        .orEmpty()
                if (name.isBlank()) {
                    Toast
                        .makeText(
                            context,
                            context.i18n(R.string.error_empty_item_name),
                            Toast.LENGTH_LONG,
                        ).show()
                    return@setPositiveButton
                }

                val exists = state.categories.any { it.name.equals(name, ignoreCase = true) }
                if (exists) {
                    Toast
                        .makeText(
                            context,
                            context.i18n(R.string.error_item_name_exists),
                            Toast.LENGTH_LONG,
                        ).show()
                    return@setPositiveButton
                }

                val nextId = (state.categories.maxOfOrNull { it.id } ?: 0) + 1
                state.categories.add(Category(nextId, name))
                fragment.lifecycleScope.launch {
                    repository.saveBackupState(state)
                    Toast
                        .makeText(
                            context,
                            context.i18n(R.string.toast_item_saved),
                            Toast.LENGTH_LONG,
                        ).show()
                }
            }.setNegativeButton(R.string.btn_cancel, null)
            .showStyled()
    }

    private fun showAddTagDialogue(state: BackupState) {
        val context = fragment.requireContext()
        val input = EditText(context).apply { hint = context.i18n(R.string.hint_new_item_name) }

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.settings_manage_tags)
            .setView(input)
            .setPositiveButton(R.string.btn_add) { _, _ ->
                val name =
                    input.text
                        ?.toString()
                        ?.trim()
                        .orEmpty()
                if (name.isBlank()) {
                    Toast
                        .makeText(
                            context,
                            context.i18n(R.string.error_empty_item_name),
                            Toast.LENGTH_LONG,
                        ).show()
                    return@setPositiveButton
                }

                val exists = state.tags.any { it.name.equals(name, ignoreCase = true) }
                if (exists) {
                    Toast
                        .makeText(
                            context,
                            context.i18n(R.string.error_item_name_exists),
                            Toast.LENGTH_LONG,
                        ).show()
                    return@setPositiveButton
                }

                val nextId = (state.tags.maxOfOrNull { it.id } ?: 0) + 1
                state.tags.add(Tag(nextId, name))
                fragment.lifecycleScope.launch {
                    repository.saveBackupState(state)
                    Toast
                        .makeText(
                            context,
                            context.i18n(R.string.toast_item_saved),
                            Toast.LENGTH_LONG,
                        ).show()
                }
            }.setNegativeButton(R.string.btn_cancel, null)
            .showStyled()
    }

    private fun showRenameCategoryDialogue(
        state: BackupState,
        categoryId: Int,
    ) {
        val category = state.categories.firstOrNull { it.id == categoryId } ?: return
        val context = fragment.requireContext()
        val input =
            EditText(context).apply {
                hint = context.i18n(R.string.hint_new_item_name)
                setText(category.name)
                setSelection(category.name.length)

                val leftPadding = 16f.dpToPx(context).toInt()

                setPadding(
                    leftPadding,
                    paddingTop,
                    paddingRight,
                    paddingBottom,
                )
            }

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.settings_rename_category)
            .setView(input)
            .setPositiveButton(R.string.btn_rename) { _, _ ->
                val newName =
                    input.text
                        ?.toString()
                        ?.trim()
                        .orEmpty()
                if (newName.isBlank()) {
                    Toast
                        .makeText(
                            context,
                            context.i18n(R.string.error_empty_item_name),
                            Toast.LENGTH_LONG,
                        ).show()
                    return@setPositiveButton
                }

                val exists =
                    state.categories.any {
                        it.id != categoryId && it.name.equals(newName, ignoreCase = true)
                    }
                if (exists) {
                    Toast
                        .makeText(
                            context,
                            context.i18n(R.string.error_item_name_exists),
                            Toast.LENGTH_LONG,
                        ).show()
                    return@setPositiveButton
                }

                category.name = newName
                fragment.lifecycleScope.launch {
                    repository.saveBackupState(state)
                    Toast
                        .makeText(
                            context,
                            context.i18n(R.string.toast_item_saved),
                            Toast.LENGTH_LONG,
                        ).show()
                }
            }.setNegativeButton(R.string.btn_cancel, null)
            .showStyled()
    }

    private fun showRenameTagDialogue(
        state: BackupState,
        tagId: Int,
    ) {
        val tag = state.tags.firstOrNull { it.id == tagId } ?: return
        val context = fragment.requireContext()
        val input =
            EditText(context).apply {
                hint = context.i18n(R.string.hint_new_item_name)
                setText(tag.name)
                setSelection(tag.name.length)

                val leftPadding = 16f.dpToPx(context).toInt()

                setPadding(
                    leftPadding,
                    paddingTop,
                    paddingRight,
                    paddingBottom,
                )
            }

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.settings_rename_tag)
            .setView(input)
            .setPositiveButton(R.string.btn_rename) { _, _ ->
                val newName =
                    input.text
                        ?.toString()
                        ?.trim()
                        .orEmpty()
                if (newName.isBlank()) {
                    Toast
                        .makeText(
                            context,
                            context.i18n(R.string.error_empty_item_name),
                            Toast.LENGTH_LONG,
                        ).show()
                    return@setPositiveButton
                }

                val exists =
                    state.tags.any {
                        it.id != tagId && it.name.equals(newName, ignoreCase = true)
                    }
                if (exists) {
                    Toast
                        .makeText(
                            context,
                            context.i18n(R.string.error_item_name_exists),
                            Toast.LENGTH_LONG,
                        ).show()
                    return@setPositiveButton
                }

                tag.name = newName
                fragment.lifecycleScope.launch {
                    repository.saveBackupState(state)
                    Toast
                        .makeText(
                            context,
                            context.i18n(R.string.toast_item_saved),
                            Toast.LENGTH_LONG,
                        ).show()
                }
            }.setNegativeButton(R.string.btn_cancel, null)
            .showStyled()
    }

    private fun confirmDeleteCategory(
        state: BackupState,
        categoryId: Int,
    ) {
        val category = state.categories.firstOrNull { it.id == categoryId } ?: return
        val context = fragment.requireContext()

        val message = context.getString(R.string.confirm_dialogue_delete_prompt, category.name)
        val start = message.indexOf(category.name)
        val end = start + category.name.length

        val spannableMessage =
            SpannableString(message).apply {
                setSpan(
                    StyleSpan(Typeface.BOLD),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.settings_delete_category)
            .setMessage(spannableMessage)
            .setPositiveButton(R.string.btn_yes) { _, _ ->
                state.categories.removeAll { it.id == categoryId }
                state.activities.forEach { activity ->
                    if (activity.categoryId == categoryId) {
                        activity.categoryId = null
                    }
                }
                fragment.lifecycleScope.launch {
                    repository.saveBackupState(state)
                    Toast
                        .makeText(
                            context,
                            context.i18n(R.string.toast_item_deleted),
                            Toast.LENGTH_LONG,
                        ).show()
                }
            }.setNegativeButton(R.string.btn_no, null)
            .showStyled()
    }

    private fun confirmDeleteTag(
        state: BackupState,
        tagId: Int,
    ) {
        val tag = state.tags.firstOrNull { it.id == tagId } ?: return
        val context = fragment.requireContext()

        val message = context.getString(R.string.confirm_dialogue_delete_prompt, tag.name)
        val start = message.indexOf(tag.name)
        val end = start + tag.name.length

        val spannableMessage =
            SpannableString(message).apply {
                setSpan(
                    StyleSpan(Typeface.BOLD),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.settings_delete_tag)
            .setMessage(spannableMessage)
            .setPositiveButton(R.string.btn_yes) { _, _ ->
                state.tags.removeAll { it.id == tagId }
                state.activities.forEach { activity ->
                    activity.tagIds = activity.tagIds.filterNot { it == tagId }.toMutableList()
                }
                fragment.lifecycleScope.launch {
                    repository.saveBackupState(state)
                    Toast
                        .makeText(
                            context,
                            context.i18n(R.string.toast_item_deleted),
                            Toast.LENGTH_LONG,
                        ).show()
                }
            }.setNegativeButton(R.string.btn_no, null)
            .showStyled()
    }
}
