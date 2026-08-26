package com.asptechinc.daymark

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.asptechinc.daymark.config.AppConfig
import com.asptechinc.daymark.data.AppDatabase
import com.asptechinc.daymark.models.Activity
import com.asptechinc.daymark.repository.BackupRepository
import com.asptechinc.daymark.ui.settings.AboutSettingsHandler
import com.asptechinc.daymark.ui.settings.BackupSettingsHandler
import com.asptechinc.daymark.ui.settings.CalculatorSettingsHandler
import com.asptechinc.daymark.ui.settings.DataManagementHandler
import com.asptechinc.daymark.ui.settings.GeneralSettingsHandler
import com.asptechinc.daymark.ui.settings.SecuritySettingsHandler
import com.asptechinc.daymark.ui.settings.SupportSettingsHandler
import com.asptechinc.daymark.utils.ThemeManager
import com.asptechinc.daymark.utils.i18n
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
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
        private val repository by lazy {
            val database = AppDatabase.getDatabase(requireContext())
            BackupRepository(
                requireContext(),
                database.activityDao(),
                database.categoryDao(),
                database.tagDao(),
            )
        }

        private val importLauncher =
            registerForActivityResult(StartActivityForResult()) { result ->
                if (result.resultCode != RESULT_OK || result.data?.data == null) return@registerForActivityResult
                val uri = result.data!!.data!!
                Log.i(javaClass.simpleName, "Starting backup import")
                val stream =
                    requireContext().contentResolver.openInputStream(uri)
                        ?: return@registerForActivityResult
                backupHandler.handleImport(stream)
            }

        private val exportLauncher =
            registerForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
                if (uri == null) return@registerForActivityResult
                lifecycleScope.launch {
                    try {
                        val json = repository.exportStateToJson()
                        requireContext()
                            .contentResolver
                            .openOutputStream(uri)
                            ?.use { outputStream ->
                                outputStream.write(json.toByteArray())
                            }
                        Toast
                            .makeText(
                                requireContext(),
                                i18n(R.string.toast_settings_exported),
                                Toast.LENGTH_LONG,
                            ).show()
                    } catch (e: Exception) {
                        Log.e(
                            javaClass.simpleName,
                            i18n(R.string.toast_app_lock_backup_save_failed),
                            e,
                        )
                        Toast
                            .makeText(
                                requireContext(),
                                "${getString(R.string.toast_app_lock_backup_save_failed)}: ${e.localizedMessage}",
                                Toast.LENGTH_LONG,
                            ).show()
                    }
                }
            }

        private val exportCsvLauncher =
            registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
                if (uri == null) return@registerForActivityResult
                lifecycleScope.launch {
                    try {
                        val state = repository.loadBackupState()
                        val csv = generateActivitiesCsv(state.activities)
                        requireContext()
                            .contentResolver
                            .openOutputStream(uri)
                            ?.use { outputStream ->
                                outputStream.write(csv.toByteArray())
                            }
                        Toast
                            .makeText(
                                requireContext(),
                                i18n(R.string.toast_csv_exported),
                                Toast.LENGTH_LONG,
                            ).show()
                    } catch (e: Exception) {
                        Log.e(javaClass.simpleName, "CSV export failed", e)
                        Toast
                            .makeText(
                                requireContext(),
                                "CSV export failed: ${e.localizedMessage}",
                                Toast.LENGTH_LONG,
                            ).show()
                    }
                }
            }

        private fun generateActivitiesCsv(activities: List<Activity>): String {
            val builder = StringBuilder()
            builder.append("Name,Notes,Start Date,End Date,Archived\n")
            activities.forEach { activity ->
                val name = escapeCsvField(activity.activityName)
                val notes = escapeCsvField(activity.notes)
                val start = activity.startDateTime.toString()
                val end = activity.endDateTime?.toString() ?: ""
                val archived = activity.archived ?: false
                builder.append("$name,$notes,$start,$end,$archived\n")
            }
            return builder.toString()
        }

        private fun escapeCsvField(field: String): String =
            if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
                "\"" + field.replace("\"", "\"\"") + "\""
            } else {
                field
            }

        private val generalHandler by lazy { GeneralSettingsHandler(this) }
        private val calculatorHandler by lazy { CalculatorSettingsHandler(this) }
        private val dataHandler by lazy { DataManagementHandler(this, repository) }
        private val securityHandler by lazy { SecuritySettingsHandler(this) }
        private val backupHandler: BackupSettingsHandler by lazy {
            BackupSettingsHandler(this, repository, importLauncher, exportLauncher)
        }
        private val aboutHandler by lazy { AboutSettingsHandler(this) }
        private val supportHandler by lazy { SupportSettingsHandler(aboutHandler) }

        override fun onCreatePreferences(
            savedInstanceState: Bundle?,
            rootKey: String?,
        ) {
            addPreferencesFromResource(R.xml.settings_main)

            // General
            findPreference<Preference>(getString(R.string.settings_key_theme_config))
                ?.setOnPreferenceClickListener {
                    generalHandler.showThemeSelectionDialogue()
                    true
                }
            findPreference<Preference>(getString(R.string.settings_key_layout_mode))
                ?.setOnPreferenceClickListener {
                    generalHandler.showLayoutModeSelectionDialogue()
                    true
                }
            findPreference<Preference>(getString(R.string.settings_key_time_unit))
                ?.setOnPreferenceClickListener {
                    generalHandler.showTimeUnitSelectionDialogue()
                    true
                }
            findPreference<SwitchPreferenceCompat>(getString(R.string.settings_key_notifications))
                ?.setOnPreferenceChangeListener { _, newValue ->
                    generalHandler.toggleNotifications(newValue as Boolean)
                    true
                }

            // Calculators
            findPreference<Preference>(getString(R.string.settings_key_days_calculator))
                ?.setOnPreferenceClickListener {
                    calculatorHandler.showDaysCalculatorDialogue()
                    true
                }
            findPreference<Preference>(getString(R.string.settings_key_date_calculator))
                ?.setOnPreferenceClickListener {
                    calculatorHandler.showDateCalculatorDialogue()
                    true
                }

            // Data Management
            findPreference<Preference>(getString(R.string.settings_key_clear_all))
                ?.setOnPreferenceClickListener {
                    dataHandler.confirmClearAllActivities()
                    true
                }
            findPreference<Preference>(getString(R.string.settings_key_category))
                ?.setOnPreferenceClickListener {
                    dataHandler.showManageCategoriesDialogue()
                    true
                }
            findPreference<Preference>(getString(R.string.settings_key_tags))
                ?.setOnPreferenceClickListener {
                    dataHandler.showManageTagsDialogue()
                    true
                }
            findPreference<Preference>(getString(R.string.settings_key_app_storage))
                ?.setOnPreferenceClickListener {
                    dataHandler.showAppStorageDialogue()
                    true
                }
            findPreference<Preference>(getString(R.string.settings_key_reset))
                ?.setOnPreferenceClickListener {
                    dataHandler.confirmFullReset()
                    true
                }

            // Security
            val appPinPreference =
                findPreference<SwitchPreferenceCompat>(getString(R.string.settings_key_manage_app_pin))

            appPinPreference
                ?.setOnPreferenceClickListener {
                    securityHandler.handleAppLock(appPinPreference)
                    true
                }

            // Backup
            findPreference<Preference>(getString(R.string.settings_key_import_backup))
                ?.setOnPreferenceClickListener {
                    backupHandler.importBackupIntent()
                    true
                }
            findPreference<Preference>(getString(R.string.settings_key_export_backup))
                ?.setOnPreferenceClickListener {
                    backupHandler.exportBackupIntent()
                    true
                }
            findPreference<Preference>(getString(R.string.settings_key_export_csv))
                ?.setOnPreferenceClickListener {
                    val timestamp =
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
                    val fileName =
                        "${AppConfig.APP_NAME.lowercase()}-activities-$timestamp.csv"
                    exportCsvLauncher.launch(fileName)
                    true
                }

            // About
            findPreference<Preference>(getString(R.string.settings_key_privacy_policy))
                ?.setOnPreferenceClickListener {
                    aboutHandler.openExternalUrl(getString(R.string.about_privacy_policy_url))
                    true
                }
            findPreference<Preference>(getString(R.string.settings_key_licence))
                ?.setOnPreferenceClickListener {
                    aboutHandler.openExternalUrl(getString(R.string.about_licence_url))
                    true
                }
            findPreference<Preference>(getString(R.string.settings_key_oss_licences))
                ?.setOnPreferenceClickListener {
                    aboutHandler.showOssLicencesDialogue()
                    true
                }
            findPreference<Preference>(getString(R.string.settings_key_app_version))
                ?.setOnPreferenceClickListener {
                    aboutHandler.showVersionDialogue()
                    true
                }
            findPreference<Preference>(getString(R.string.settings_key_app_updater))
                ?.setOnPreferenceClickListener {
                    aboutHandler.showAppUpdaterDialogue()
                    true
                }
            findPreference<Preference>(getString(R.string.settings_key_app_changelog))
                ?.setOnPreferenceClickListener {
                    aboutHandler.showChangelogDialogue()
                    true
                }

            // Support
            findPreference<Preference>(getString(R.string.settings_key_donate))
                ?.setOnPreferenceClickListener {
                    supportHandler.openDonateUrl()
                    true
                }
            findPreference<Preference>(getString(R.string.settings_key_app_source_code))
                ?.setOnPreferenceClickListener {
                    supportHandler.openSourceCodeUrl()
                    true
                }

            handleIntentAction()
        }

        private fun handleIntentAction() {
            val action = requireActivity().intent.getStringExtra("action") ?: return
            requireActivity().intent.removeExtra("action") // Only handle it once

            when (action) {
                "manage_categories" -> dataHandler.showManageCategoriesDialogue()
                "manage_tags" -> dataHandler.showManageTagsDialogue()
            }
        }

        override fun onViewCreated(
            view: android.view.View,
            savedInstanceState: Bundle?,
        ) {
            super.onViewCreated(view, savedInstanceState)
            setDivider(AppCompatResources.getDrawable(requireContext(), R.drawable.divider))
            setDividerHeight(resources.getDimensionPixelSize(R.dimen.layout_dimension_1))
        }
    }
}
