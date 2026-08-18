package com.asptechinc.daymark.ui.settings

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.preference.PreferenceFragmentCompat
import com.asptechinc.daymark.R
import com.asptechinc.daymark.config.AppConfig
import com.asptechinc.daymark.utils.i18n
import java.io.InputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class BackupSettingsHandler(
    private val fragment: PreferenceFragmentCompat,
    private val importLauncher: ActivityResultLauncher<Intent>,
    private val exportLauncher: ActivityResultLauncher<String>,
) {
    fun importBackupIntent() {
        importLauncher.launch(
            Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            },
        )
    }

    fun exportBackupIntent() {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
        val fileName = "${AppConfig.APP_NAME.lowercase()}-backup-$timestamp.backup"
        exportLauncher.launch(fileName)
    }

    fun handleImport(inputStream: InputStream) {
        val context = fragment.requireContext()
        val prefs = context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
        try {
            val json = inputStream.bufferedReader().use { it.readText() }
            Log.i("BackupSettingsHandler", "Importing backup: $json")
            prefs.edit().putString(AppConfig.SETTINGS_JSON_KEY, json).apply()
            Toast.makeText(context, context.i18n(R.string.toast_settings_imported), Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e("BackupSettingsHandler", "Backup import failed", e)
        }
    }
}
