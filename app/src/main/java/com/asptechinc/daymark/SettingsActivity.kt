package com.asptechinc.daymark

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import androidx.recyclerview.widget.RecyclerView
import com.asptechinc.daymark.config.AppConfig
import com.asptechinc.daymark.models.Activity
import com.asptechinc.daymark.models.Category
import com.asptechinc.daymark.models.Tag
import com.asptechinc.daymark.repository.initialActivities
import com.asptechinc.daymark.repository.initialCategories
import com.asptechinc.daymark.repository.initialTags
import com.asptechinc.daymark.ui.ChangelogAdapter
import com.asptechinc.daymark.utils.AppUpdater
import com.asptechinc.daymark.utils.ChangelogParser
import com.asptechinc.daymark.utils.ThemeManager
import com.asptechinc.daymark.utils.dpToPx
import com.asptechinc.daymark.utils.i18n
import com.asptechinc.daymark.utils.showStyled
import com.asptechinc.daymark.utils.styleDialogue
import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.jsonObject
import com.github.salomonbrys.kotson.registerTypeAdapter
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.stream.JsonToken
import com.mcxiaoke.koi.ext.dateNow
import com.mcxiaoke.koi.ext.longToast
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import java.io.InputStream

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)

        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, MainSettingsFragment())
                .commit()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    class MainSettingsFragment : PreferenceFragmentCompat() {
        private val sourceCodeUrl by lazy { getString(R.string.about_source_code_url) }
        private val licenceUrl by lazy { getString(R.string.about_licence_url) }
        private val privacyPolicyUrl by lazy { getString(R.string.about_privacy_policy_url) }
        private val donateUrl by lazy { getString(R.string.about_donate_url) }

        private val appContext by lazy { requireActivity().applicationContext }
        private val prefs by lazy {
            requireActivity().getSharedPreferences("settings", MODE_PRIVATE)
        }
        private val gson =
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
                            categoryId = category.toIntOrNull(),
                            tagIds =
                                tags
                                    .split(",")
                                    .mapNotNull { it.toIntOrNull() }
                                    .toMutableList(),
                        )
                    }
                }.create()

        private data class SettingsState(
            val activities: MutableList<Activity>,
            val categories: MutableList<Category>,
            val tags: MutableList<Tag>,
        )

        private val importLauncher =
            registerForActivityResult(StartActivityForResult()) { result ->
                if (result.resultCode != RESULT_OK || result.data?.data == null) return@registerForActivityResult
                val uri = result.data!!.data!!
                Log.i(javaClass.simpleName, "Backup import attempt, uri: $uri")
                val stream =
                    appContext.contentResolver.openInputStream(uri)
                        ?: return@registerForActivityResult
                importBackup(stream)
            }

        private val exportLauncher =
            registerForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
                if (uri == null) return@registerForActivityResult
                val json =
                    prefs.getString(AppConfig.SETTINGS_JSON_KEY, null)
                        ?: return@registerForActivityResult
                try {
                    appContext.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(json.toByteArray())
                    }
                    requireContext().longToast(i18n(R.string.toast_settings_exported))
                } catch (e: Exception) {
                    Log.e(javaClass.simpleName, i18n(R.string.toast_app_lock_backup_save_failed), e)
                    requireContext().longToast("${R.string.toast_app_lock_backup_save_failed}: ${e.localizedMessage}")
                }
            }

        override fun onCreatePreferences(
            savedInstanceState: Bundle?,
            rootKey: String?,
        ) {
            addPreferencesFromResource(R.xml.settings_main)

            findPreference<Preference>(getString(R.string.settings_key_theme_config))
                ?.setOnPreferenceClickListener {
                    showThemeSelectionDialogue()
                    true
                }
            findPreference<Preference>(getString(R.string.settings_key_clear_all))
                ?.setOnPreferenceClickListener {
                    confirmClearAllActivities()
                    true
                }
            findPreference<Preference>(getString(R.string.settings_key_category))
                ?.setOnPreferenceClickListener {
                    showManageCategoriesDialogue()
                    true
                }
            findPreference<Preference>(getString(R.string.settings_key_tags))
                ?.setOnPreferenceClickListener {
                    showManageTagsDialogue()
                    true
                }
            findPreference<Preference>(getString(R.string.settings_key_reset))
                ?.setOnPreferenceClickListener {
                    confirmFullReset()
                    true
                }
            val appPinPreference =
                findPreference<SwitchPreferenceCompat>(getString(R.string.settings_key_app_pin))

            appPinPreference?.isChecked =
                !prefs.getString(i18n(R.string.backup_key_app_pin), null).isNullOrBlank()

            appPinPreference?.setOnPreferenceChangeListener { _, newValue ->
                val enablePin = newValue as Boolean

                if (enablePin) {
                    showAppLockSetupDialogue(allowRemove = false) { pinSet ->
                        appPinPreference.isChecked = pinSet
                    }
                    false
                } else {
                    prefs.edit { remove(i18n(R.string.backup_key_app_pin)) }
                    requireContext().longToast(i18n(R.string.toast_app_lock_pin_removal))
                    true
                }
            }

            appPinPreference?.setOnPreferenceClickListener {
                if (appPinPreference.isChecked) {
                    showAppLockSetupDialogue(allowRemove = false) { pinSet ->
                        appPinPreference.isChecked = pinSet
                    }
                }
                true
            }
            findPreference<Preference>(getString(R.string.settings_key_import_backup))
                ?.setOnPreferenceClickListener {
                    importBackupIntent()
                    true
                }
            findPreference<Preference>(getString(R.string.settings_key_export_backup))
                ?.setOnPreferenceClickListener {
                    exportBackupIntent()
                    true
                }
            findPreference<Preference>(getString(R.string.settings_key_app_source_code))
                ?.setOnPreferenceClickListener {
                    openExternalUrl(sourceCodeUrl)
                    true
                }
            findPreference<Preference>(getString(R.string.settings_key_licence))
                ?.setOnPreferenceClickListener {
                    openExternalUrl(licenceUrl)
                    true
                }
            findPreference<Preference>(getString(R.string.settings_key_privacy_policy))
                ?.setOnPreferenceClickListener {
                    openExternalUrl(privacyPolicyUrl)
                    true
                }
            findPreference<Preference>(getString(R.string.settings_key_donate))
                ?.setOnPreferenceClickListener {
                    openExternalUrl(donateUrl)
                    true
                }
            findPreference<Preference>(getString(R.string.settings_key_app_version))
                ?.setOnPreferenceClickListener {
                    showVersionDialogue()
                    true
                }
            findPreference<Preference>(getString(R.string.settings_key_app_updater))
                ?.setOnPreferenceClickListener {
                    showAppUpdaterDialogue()
                    true
                }
            findPreference<Preference>(getString(R.string.settings_key_app_changelog))
                ?.setOnPreferenceClickListener {
                    showChangelogDialogue()
                    true
                }
        }

        override fun onViewCreated(
            view: View,
            savedInstanceState: Bundle?,
        ) {
            super.onViewCreated(view, savedInstanceState)
            setDivider(AppCompatResources.getDrawable(requireContext(), R.drawable.divider))
            setDividerHeight(resources.getDimensionPixelSize(R.dimen.divider_height_1))
        }

        private fun importBackupIntent() {
            importLauncher.launch(
                Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "*/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                },
            )
        }

        private fun showThemeSelectionDialogue() {
            val options = resources.getStringArray(R.array.settings_theme_options)
            val selected = ThemeManager.getSavedThemeIndex(requireContext())

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_label_theme)
                .setSingleChoiceItems(options, selected) { dialogue, which ->
                    ThemeManager.setThemeByIndex(requireContext(), which)
                    dialogue.dismiss()
                    requireActivity().recreate()
                }.setNegativeButton(R.string.btn_cancel, null)
                .showStyled()
        }

        private fun openExternalUrl(url: String) {
            val uri = url.toUri()
            val customTabsIntent = CustomTabsIntent.Builder().build()

            try {
                customTabsIntent.launchUrl(requireContext(), uri)
            } catch (_: Exception) {
                val fallbackIntent = Intent(Intent.ACTION_VIEW, uri)
                val canHandle =
                    fallbackIntent.resolveActivity(requireContext().packageManager) != null
                if (canHandle) {
                    startActivity(fallbackIntent)
                } else {
                    requireContext().longToast(i18n(R.string.toast_app_lock_unavailable_app))
                }
            }
        }

        private fun showVersionDialogue() {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("App version")
                .setMessage("App name: ${AppConfig.APP_NAME}\nVersion: ${BuildConfig.VERSION_NAME}\nBuild: ${BuildConfig.BUILD_TIME}")
                .setPositiveButton(R.string.btn_okay, null)
                .showStyled()
        }

        private fun showAppLockSetupDialogue(
            allowRemove: Boolean = true,
            onPinConfigured: ((Boolean) -> Unit)? = null,
        ) {
            val currentPin = prefs.getString(i18n(R.string.backup_key_app_pin), null)
            val firstInput = EditText(requireContext())
            val confirmInput = EditText(requireContext())
            firstInput.setHint(if (currentPin == null) "Enter a PIN (4-16 digits)" else "Enter a new PIN (4-16 digits)")
            confirmInput.setHint("Confirm PIN")
            firstInput.inputType =
                android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            confirmInput.inputType =
                android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            firstInput.transformationMethod =
                android.text.method.PasswordTransformationMethod
                    .getInstance()
            confirmInput.transformationMethod =
                android.text.method.PasswordTransformationMethod
                    .getInstance()
            firstInput.filters = arrayOf(android.text.InputFilter.LengthFilter(16))
            confirmInput.filters = arrayOf(android.text.InputFilter.LengthFilter(16))

            val dialogue =
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(if (currentPin == null) "Set app PIN" else "Change app PIN")
                    .setView(
                        android.widget.LinearLayout(requireContext()).apply {
                            orientation = android.widget.LinearLayout.VERTICAL
                            setPadding(48, 24, 48, 0)
                            addView(firstInput)
                            addView(confirmInput)
                        },
                    ).setPositiveButton(R.string.btn_okay) { _, _ ->
                        val pin = firstInput.text?.toString().orEmpty()
                        val confirmedPin = confirmInput.text?.toString().orEmpty()
                        when {
                            allowRemove && pin.isBlank() && confirmedPin.isBlank() -> {
                                prefs.edit { remove(i18n(R.string.backup_key_app_pin)) }
                                requireContext().longToast(i18n(R.string.toast_app_lock_pin_removal))
                                onPinConfigured?.invoke(false)
                            }

                            !allowRemove && (pin.isBlank() || confirmedPin.isBlank()) -> {
                                requireContext().longToast(i18n(R.string.toast_app_lock_pin_required))
                                onPinConfigured?.invoke(
                                    !prefs
                                        .getString(i18n(R.string.backup_key_app_pin), null)
                                        .isNullOrBlank(),
                                )
                            }

                            pin != confirmedPin -> {
                                requireContext().longToast(i18n(R.string.toast_app_lock_pin_mismatch))
                                onPinConfigured?.invoke(
                                    !prefs
                                        .getString(i18n(R.string.backup_key_app_pin), null)
                                        .isNullOrBlank(),
                                )
                            }

                            pin.length !in 4..16 || !pin.all { it.isDigit() } -> {
                                requireContext().longToast(i18n(R.string.toast_app_lock_pin_action))
                                onPinConfigured?.invoke(
                                    !prefs
                                        .getString(i18n(R.string.backup_key_app_pin), null)
                                        .isNullOrBlank(),
                                )
                            }

                            else -> {
                                prefs.edit { putString(i18n(R.string.backup_key_app_pin), pin) }
                                requireContext().longToast(i18n(R.string.toast_app_lock_pin_set_success))
                                onPinConfigured?.invoke(true)
                            }
                        }
                    }.setNegativeButton(R.string.btn_cancel) { _, _ ->
                        onPinConfigured?.invoke(
                            !prefs
                                .getString(i18n(R.string.backup_key_app_pin), null)
                                .isNullOrBlank(),
                        )
                    }.create()
            dialogue.setOnShowListener { styleDialogue(dialogue) }
            dialogue.show()
        }

        private fun showAppUpdaterDialogue() {
            val dialogue =
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.settings_label_app_updater)
                    .setMessage(R.string.updater_status_checking)
                    .setPositiveButton(R.string.btn_okay, null)
                    .create()

            dialogue.setOnShowListener {
                styleDialogue(dialogue)
            }
            dialogue.show()

            lifecycleScope.launch {
                val latestRelease = AppUpdater.checkForUpdate(BuildConfig.VERSION_NAME)
                if (latestRelease != null) {
                    val apkAsset = latestRelease.assets.find { it.name.endsWith(".apk") }
                    if (apkAsset != null) {
                        dialogue.setMessage(
                            getString(
                                R.string.updater_status_update_available,
                                latestRelease.tag_name,
                            ),
                        )
                        dialogue.setButton(
                            androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE,
                            getString(R.string.updater_btn_update),
                        ) { _, _ ->
                            requireContext().longToast(i18n(R.string.updater_downloading))
                            AppUpdater.downloadAndInstall(
                                requireContext(),
                                apkAsset.browser_download_url,
                                apkAsset.name,
                            )
                        }
                    } else {
                        dialogue.setMessage(i18n(R.string.updater_error_no_apk))
                    }
                } else {
                    dialogue.setMessage(i18n(R.string.updater_status_up_to_date))
                }
            }
        }

        private fun showChangelogDialogue() {
            val versions = ChangelogParser.parse(requireContext())
            val view = layoutInflater.inflate(R.layout.dialogue_changelog, null)
            val recyclerView = view.findViewById<RecyclerView>(R.id.changelog_recycler)
            recyclerView.adapter = ChangelogAdapter(versions)

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Changelog")
                .setView(view)
                .setPositiveButton(R.string.btn_okay, null)
                .showStyled()
        }

        private fun InputStream.readText() = readBytes().toString(Charsets.UTF_8)

        private fun importBackup(input: InputStream) {
            val json = input.readText()
            Log.i(javaClass.simpleName, "importing backup: $json")
            prefs.edit { putString(AppConfig.SETTINGS_JSON_KEY, json) }
            requireContext().longToast(i18n(R.string.toast_settings_imported))
        }

        private fun exportBackupIntent() {
            val timestamp = DateTime.now().toString("yyyyMMdd-HHmmss")
            val fileName = "${AppConfig.APP_NAME.lowercase()}-backup-$timestamp.backup"
            exportLauncher.launch(fileName)
        }

        private fun loadSettingsState(): SettingsState {
            val settingsJsonValue =
                prefs.getString(AppConfig.SETTINGS_JSON_KEY, null) ?: return SettingsState(
                    activities = initialActivities(requireContext()),
                    categories = initialCategories(),
                    tags = initialTags(),
                )

            val root = JsonParser.parseString(settingsJsonValue).asJsonObject
            val activitiesElement = root["activities"]
            val categoriesElement = root["categories"]
            val tagsElement = root["tags"]

            val activities =
                if (activitiesElement != null) {
                    gson.fromJson<MutableList<Activity>>(activitiesElement)
                } else {
                    mutableListOf()
                }

            val categories =
                if (categoriesElement != null) {
                    gson.fromJson<MutableList<Category>>(categoriesElement)
                } else {
                    mutableListOf()
                }

            val tags =
                if (tagsElement != null) {
                    gson.fromJson<MutableList<Tag>>(tagsElement)
                } else {
                    mutableListOf()
                }

            return SettingsState(
                activities = activities,
                categories = categories,
                tags = tags,
            )
        }

        private fun saveSettingsState(state: SettingsState) {
            val settingsJson: JsonObject =
                jsonObject(
                    "saveFormatVersion" to AppConfig.SAVE_FILE_VERSION,
                    "savedWithAppVersion" to BuildConfig.VERSION_NAME,
                    "savedOnDate" to dateNow(),
                    "activities" to gson.toJsonTree(state.activities),
                    "tags" to gson.toJsonTree(state.tags),
                    "categories" to gson.toJsonTree(state.categories),
                )

            prefs.edit { putString(AppConfig.SETTINGS_JSON_KEY, settingsJson.toString()) }
        }

        private fun confirmClearAllActivities() {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_label_clear_all)
                .setMessage(R.string.settings_clear_all_confirm)
                .setPositiveButton(R.string.btn_clear) { _, _ ->
                    val state = loadSettingsState()
                    state.activities.clear()
                    saveSettingsState(state)
                    requireContext().longToast(i18n(R.string.settings_clear_all_done))
                }.setNegativeButton(R.string.btn_cancel, null)
                .showStyled()
        }

        private fun confirmFullReset() {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_reset)
                .setMessage(R.string.settings_reset_confirm)
                .setPositiveButton(R.string.confirm_dialogue_yes) { _, _ ->
                    val resetState =
                        SettingsState(
                            activities = initialActivities(requireContext()),
                            categories = initialCategories(),
                            tags = initialTags(),
                        )
                    saveSettingsState(resetState)
                    requireContext().longToast(i18n(R.string.settings_reset_done))
                }.setNegativeButton(R.string.confirm_dialogue_no, null)
                .showStyled()
        }

        private fun showManageCategoriesDialogue() {
            val state = loadSettingsState()
            val categoryNames = state.categories.map { it.name }.toTypedArray()

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_manage_categories)
                .setMessage(
                    if (categoryNames.isEmpty()) {
                        i18n(R.string.settings_manage_categories_empty)
                    } else {
                        null
                    },
                ).setItems(categoryNames) { _, which ->
                    showCategoryActionDialogue(state, state.categories[which].id)
                }.setPositiveButton(R.string.settings_add) { _, _ ->
                    showAddCategoryDialogue(state)
                }.setNegativeButton(R.string.settings_close, null)
                .showStyled()
        }

        private fun showManageTagsDialogue() {
            val state = loadSettingsState()
            val tagNames = state.tags.map { it.name }.toTypedArray()

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_manage_tags)
                .setMessage(
                    if (tagNames.isEmpty()) {
                        i18n(R.string.settings_manage_tags_empty)
                    } else {
                        null
                    },
                ).setItems(tagNames) { _, which ->
                    showTagActionDialogue(state, state.tags[which].id)
                }.setPositiveButton(R.string.settings_add) { _, _ ->
                    showAddTagDialogue(state)
                }.setNegativeButton(R.string.settings_close, null)
                .showStyled()
        }

        private fun showCategoryActionDialogue(
            state: SettingsState,
            categoryId: Int,
        ) {
            val category = state.categories.firstOrNull { it.id == categoryId } ?: return

            val view =
                layoutInflater.inflate(
                    R.layout.dialogue_actions,
                    null,
                )

            val explanation = view.findViewById<TextView>(R.id.explanation)
            val renameAction = view.findViewById<TextView>(R.id.renameAction)
            val deleteAction = view.findViewById<TextView>(R.id.deleteAction)

            explanation.text = i18n(R.string.settings_manage_action_title)

            val textColour =
                MaterialColors.getColor(
                    requireContext(),
                    com.google.android.material.R.attr.colorOnSurface,
                    requireContext().getColor(R.color.on_surface),
                )

            explanation.setTextColor(
                MaterialColors.getColor(
                    requireContext(),
                    com.google.android.material.R.attr.colorOnTertiary,
                    requireContext().getColor(R.color.on_surface_variant),
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

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(category.name)
                .setView(view)
                .setNegativeButton(R.string.btn_cancel, null)
                .showStyled()
        }

        private fun showTagActionDialogue(
            state: SettingsState,
            tagId: Int,
        ) {
            val tag = state.tags.firstOrNull { it.id == tagId } ?: return
            val view =
                layoutInflater.inflate(
                    R.layout.dialogue_actions,
                    null,
                )

            val explanation = view.findViewById<TextView>(R.id.explanation)
            val renameAction = view.findViewById<TextView>(R.id.renameAction)
            val deleteAction = view.findViewById<TextView>(R.id.deleteAction)

            explanation.text = i18n(R.string.settings_manage_action_title)

            val textColour =
                MaterialColors.getColor(
                    requireContext(),
                    com.google.android.material.R.attr.colorOnSurface,
                    requireContext().getColor(R.color.on_surface),
                )

            explanation.setTextColor(
                MaterialColors.getColor(
                    requireContext(),
                    com.google.android.material.R.attr.colorOnTertiary,
                    requireContext().getColor(R.color.on_surface_variant),
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

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(tag.name)
                .setView(view)
                .setNegativeButton(R.string.btn_cancel, null)
                .showStyled()
        }

        private fun showAddCategoryDialogue(state: SettingsState) {
            val input =
                EditText(requireContext()).apply { hint = i18n(R.string.settings_name_hint) }

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_manage_categories)
                .setView(input)
                .setPositiveButton(R.string.settings_add) { _, _ ->
                    val name =
                        input.text
                            ?.toString()
                            ?.trim()
                            .orEmpty()
                    if (name.isBlank()) {
                        requireContext().longToast(i18n(R.string.settings_empty_name_error))
                        return@setPositiveButton
                    }

                    val exists = state.categories.any { it.name.equals(name, ignoreCase = true) }
                    if (exists) {
                        requireContext().longToast(i18n(R.string.settings_name_exists_error))
                        return@setPositiveButton
                    }

                    val nextId = (state.categories.maxOfOrNull { it.id } ?: 0) + 1
                    state.categories.add(Category(nextId, name))
                    saveSettingsState(state)
                    requireContext().longToast(i18n(R.string.settings_item_saved))
                }.setNegativeButton(R.string.btn_cancel, null)
                .showStyled()
        }

        private fun showAddTagDialogue(state: SettingsState) {
            val input =
                EditText(requireContext()).apply { hint = i18n(R.string.settings_name_hint) }

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_manage_tags)
                .setView(input)
                .setPositiveButton(R.string.settings_add) { _, _ ->
                    val name =
                        input.text
                            ?.toString()
                            ?.trim()
                            .orEmpty()
                    if (name.isBlank()) {
                        requireContext().longToast(i18n(R.string.settings_empty_name_error))
                        return@setPositiveButton
                    }

                    val exists = state.tags.any { it.name.equals(name, ignoreCase = true) }
                    if (exists) {
                        requireContext().longToast(i18n(R.string.settings_name_exists_error))
                        return@setPositiveButton
                    }

                    val nextId = (state.tags.maxOfOrNull { it.id } ?: 0) + 1
                    state.tags.add(Tag(nextId, name))
                    saveSettingsState(state)
                    requireContext().longToast(i18n(R.string.settings_item_saved))
                }.setNegativeButton(R.string.btn_cancel, null)
                .showStyled()
        }

        private fun showRenameCategoryDialogue(
            state: SettingsState,
            categoryId: Int,
        ) {
            val category = state.categories.firstOrNull { it.id == categoryId } ?: return
            val input =
                EditText(requireContext()).apply {
                    hint = i18n(R.string.settings_name_hint)
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

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_rename_tag)
                .setView(input)
                .setPositiveButton(R.string.settings_action_rename) { _, _ ->
                    val newName =
                        input.text
                            ?.toString()
                            ?.trim()
                            .orEmpty()
                    if (newName.isBlank()) {
                        requireContext().longToast(i18n(R.string.settings_empty_name_error))
                        return@setPositiveButton
                    }

                    val exists =
                        state.categories.any {
                            it.id != categoryId && it.name.equals(newName, ignoreCase = true)
                        }
                    if (exists) {
                        requireContext().longToast(i18n(R.string.settings_name_exists_error))
                        return@setPositiveButton
                    }

                    category.name = newName
                    saveSettingsState(state)
                    requireContext().longToast(i18n(R.string.settings_item_saved))
                }.setNegativeButton(R.string.btn_cancel, null)
                .showStyled()
        }

        private fun showRenameTagDialogue(
            state: SettingsState,
            tagId: Int,
        ) {
            val tag = state.tags.firstOrNull { it.id == tagId } ?: return
            val input =
                EditText(requireContext()).apply {
                    hint = i18n(R.string.settings_name_hint)
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

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_rename_tag)
                .setView(input)
                .setPositiveButton(R.string.settings_action_rename) { _, _ ->
                    val newName =
                        input.text
                            ?.toString()
                            ?.trim()
                            .orEmpty()
                    if (newName.isBlank()) {
                        requireContext().longToast(i18n(R.string.settings_empty_name_error))
                        return@setPositiveButton
                    }

                    val exists =
                        state.tags.any {
                            it.id != tagId && it.name.equals(newName, ignoreCase = true)
                        }
                    if (exists) {
                        requireContext().longToast(i18n(R.string.settings_name_exists_error))
                        return@setPositiveButton
                    }

                    tag.name = newName
                    saveSettingsState(state)
                    requireContext().longToast(i18n(R.string.settings_item_saved))
                }.setNegativeButton(R.string.btn_cancel, null)
                .showStyled()
        }

        private fun confirmDeleteCategory(
            state: SettingsState,
            categoryId: Int,
        ) {
            val category = state.categories.firstOrNull { it.id == categoryId } ?: return

            val message = getString(R.string.settings_confirm_delete, category.name)
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

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_delete_category)
                .setMessage(spannableMessage)
                .setPositiveButton(R.string.btn_yes) { _, _ ->
                    state.categories.removeAll { it.id == categoryId }
                    state.activities.forEach { activity ->
                        if (activity.categoryId == categoryId) {
                            activity.categoryId = null
                        }
                    }
                    saveSettingsState(state)
                    requireContext().longToast(i18n(R.string.settings_item_deleted))
                }.setNegativeButton(R.string.btn_no, null)
                .showStyled()
        }

        private fun confirmDeleteTag(
            state: SettingsState,
            tagId: Int,
        ) {
            val tag = state.tags.firstOrNull { it.id == tagId } ?: return

            val message = getString(R.string.settings_confirm_delete, tag.name)
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

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_delete_tag)
                .setMessage(spannableMessage)
                .setPositiveButton(R.string.btn_yes) { _, _ ->
                    state.tags.removeAll { it.id == tagId }
                    state.activities.forEach { activity ->
                        activity.tagIds = activity.tagIds.filterNot { it == tagId }.toMutableList()
                    }
                    saveSettingsState(state)
                    requireContext().longToast(i18n(R.string.settings_item_deleted))
                }.setNegativeButton(R.string.btn_no, null)
                .showStyled()
        }
    }
}
