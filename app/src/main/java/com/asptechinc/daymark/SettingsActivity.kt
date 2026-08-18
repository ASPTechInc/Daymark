package com.asptechinc.daymark

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.edit
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.asptechinc.daymark.config.AppConfig
import com.asptechinc.daymark.repository.SettingsRepository
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
        private val repository by lazy { SettingsRepository(requireContext()) }

        private val importLauncher =
            registerForActivityResult(StartActivityForResult()) { result ->
                if (result.resultCode != RESULT_OK || result.data?.data == null) return@registerForActivityResult
                val uri = result.data!!.data!!
                Log.i(javaClass.simpleName, "Backup import attempt, uri: $uri")
                val stream =
                    requireContext().contentResolver.openInputStream(uri)
                        ?: return@registerForActivityResult
                backupHandler.handleImport(stream)
            }

        private val exportLauncher =
            registerForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
                if (uri == null) return@registerForActivityResult
                val prefs = requireContext().getSharedPreferences("settings", MODE_PRIVATE)
                val json =
                    prefs.getString(AppConfig.SETTINGS_JSON_KEY, null)
                        ?: return@registerForActivityResult
                try {
                    requireContext().contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(json.toByteArray())
                    }
                    Toast.makeText(requireContext(), i18n(R.string.toast_settings_exported), Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Log.e(javaClass.simpleName, i18n(R.string.toast_app_lock_backup_save_failed), e)
                    Toast
                        .makeText(
                            requireContext(),
                            "${getString(R.string.toast_app_lock_backup_save_failed)}: ${e.localizedMessage}",
                            Toast.LENGTH_LONG,
                        ).show()
                }
            }

        private val generalHandler by lazy { GeneralSettingsHandler(this) }
        private val calculatorHandler by lazy { CalculatorSettingsHandler(this) }
        private val dataHandler by lazy { DataManagementHandler(this, repository) }
        private val securityHandler by lazy { SecuritySettingsHandler(this) }
        private val backupHandler: BackupSettingsHandler by lazy {
            BackupSettingsHandler(this, importLauncher, exportLauncher)
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
            findPreference<Preference>(getString(R.string.settings_key_reset))
                ?.setOnPreferenceClickListener {
                    dataHandler.confirmFullReset()
                    true
                }

            // Security
            val prefs = requireContext().getSharedPreferences("settings", MODE_PRIVATE)
            val appPinPreference =
                findPreference<SwitchPreferenceCompat>(getString(R.string.settings_key_app_pin))

            appPinPreference?.isChecked =
                !prefs.getString(i18n(R.string.backup_key_app_pin), null).isNullOrBlank()

            appPinPreference?.setOnPreferenceChangeListener { _, newValue ->
                val enablePin = newValue as Boolean
                if (enablePin) {
                    securityHandler.showAppLockSetupDialogue(allowRemove = false) { pinSet ->
                        appPinPreference.isChecked = pinSet
                    }
                    false
                } else {
                    prefs.edit { remove(i18n(R.string.backup_key_app_pin)) }
                    Toast.makeText(requireContext(), i18n(R.string.toast_app_lock_pin_removal), Toast.LENGTH_LONG).show()
                    true
                }
            }

            appPinPreference?.setOnPreferenceClickListener {
                if (appPinPreference.isChecked) {
                    securityHandler.showAppLockSetupDialogue(allowRemove = false) { pinSet ->
                        appPinPreference.isChecked = pinSet
                    }
                }
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
        }

        override fun onViewCreated(
            view: android.view.View,
            savedInstanceState: Bundle?,
        ) {
            super.onViewCreated(view, savedInstanceState)
            setDivider(AppCompatResources.getDrawable(requireContext(), R.drawable.divider))
            setDividerHeight(resources.getDimensionPixelSize(R.dimen.divider_height_1))
        }
    }
}
