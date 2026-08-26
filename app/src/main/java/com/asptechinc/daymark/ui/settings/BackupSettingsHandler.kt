package com.asptechinc.daymark.ui.settings

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceFragmentCompat
import com.asptechinc.daymark.R
import com.asptechinc.daymark.config.AppConfig
import com.asptechinc.daymark.repository.BackupRepository
import com.asptechinc.daymark.repository.UnsupportedBackupVersionException
import com.asptechinc.daymark.utils.AlarmHelper
import com.asptechinc.daymark.utils.i18n
import kotlinx.coroutines.launch
import java.io.InputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class BackupSettingsHandler(
    private val fragment: PreferenceFragmentCompat,
    private val repository: BackupRepository,
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
        val fileName =
            "${AppConfig.APP_NAME.lowercase()}-backup-$timestamp.backup"
        exportLauncher.launch(fileName)
    }

    fun handleImport(inputStream: InputStream) {
        val context = fragment.requireContext()
        fragment.lifecycleScope.launch {
            try {
                val json = inputStream.bufferedReader().use { it.readText() }
                val state = repository.parseStateFromJson(json)

                if (state != null) {
                    // Handle imports from different backup formats so that migrations
                    // and data from older app versions can be processed correctly.
                    when (state.formatVersion) {
                        1 -> repository.saveBackupState(state)
                        else -> throw UnsupportedBackupVersionException()
                    }
                    AlarmHelper.rescheduleAllAlarms(context)
                    Log.i("BackupSettingsHandler", "Backup import completed successfully")
                    Toast
                        .makeText(
                            context,
                            context.i18n(R.string.toast_settings_imported),
                            Toast.LENGTH_LONG,
                        ).show()
                } else {
                    Toast.makeText(context, "Invalid backup file", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("BackupSettingsHandler", "Backup import failed", e)
                Toast
                    .makeText(context, "Import failed: ${e.localizedMessage}", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }
}
