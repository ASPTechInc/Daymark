package com.asptechinc.daymark.ui.settings

import android.content.Intent
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import com.asptechinc.daymark.BuildConfig
import com.asptechinc.daymark.R
import com.asptechinc.daymark.config.AppConfig
import com.asptechinc.daymark.ui.ChangelogAdapter
import com.asptechinc.daymark.utils.AppUpdater
import com.asptechinc.daymark.utils.ChangelogParser
import com.asptechinc.daymark.utils.i18n
import com.asptechinc.daymark.utils.showStyled
import com.asptechinc.daymark.utils.styleDialogue
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class AboutSettingsHandler(
    internal val fragment: PreferenceFragmentCompat,
) {
    fun showVersionDialogue() {
        MaterialAlertDialogBuilder(fragment.requireContext())
            .setTitle(R.string.settings_label_app_version)
            .setMessage("App name: ${AppConfig.APP_NAME}\nVersion: ${BuildConfig.VERSION_NAME}\nBuild: ${BuildConfig.BUILD_TIME}")
            .setPositiveButton(R.string.btn_okay, null)
            .showStyled()
    }

    fun showAppUpdaterDialogue() {
        val context = fragment.requireContext()
        val dialogue =
            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.settings_label_app_updater)
                .setMessage(R.string.updater_status_checking)
                .setPositiveButton(R.string.btn_okay, null)
                .create()

        dialogue.setOnShowListener {
            styleDialogue(dialogue)
        }
        dialogue.show()

        fragment.lifecycleScope.launch {
            val latestRelease = AppUpdater.checkForUpdate(BuildConfig.VERSION_NAME)
            if (latestRelease != null) {
                val apkAsset = latestRelease.assets.find { it.name.endsWith(".apk") }
                if (apkAsset != null) {
                    dialogue.setMessage(
                        context.getString(
                            R.string.updater_status_update_available,
                            latestRelease.tag_name,
                        ),
                    )
                    dialogue.setButton(
                        androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE,
                        context.getString(R.string.updater_btn_update),
                    ) { _, _ ->
                        Toast.makeText(context, context.i18n(R.string.updater_downloading), Toast.LENGTH_LONG).show()
                        AppUpdater.downloadAndInstall(
                            context,
                            apkAsset.browser_download_url,
                            apkAsset.name,
                        )
                    }
                } else {
                    dialogue.setMessage(context.i18n(R.string.updater_error_no_apk))
                }
            } else {
                dialogue.setMessage(context.i18n(R.string.updater_status_up_to_date))
            }
        }
    }

    fun showChangelogDialogue() {
        val context = fragment.requireContext()
        val versions = ChangelogParser.parse(context)
        val view = fragment.layoutInflater.inflate(R.layout.dialogue_changelog, null)
        val recyclerView = view.findViewById<RecyclerView>(R.id.changelog_recycler)
        recyclerView.adapter = ChangelogAdapter(versions)

        MaterialAlertDialogBuilder(context)
            .setTitle("Changelog")
            .setView(view)
            .setPositiveButton(R.string.btn_okay, null)
            .showStyled()
    }

    fun openExternalUrl(url: String) {
        val context = fragment.requireContext()
        val uri = url.toUri()
        val customTabsIntent = CustomTabsIntent.Builder().build()

        try {
            customTabsIntent.launchUrl(context, uri)
        } catch (_: Exception) {
            val fallbackIntent = Intent(Intent.ACTION_VIEW, uri)
            val canHandle =
                fallbackIntent.resolveActivity(context.packageManager) != null
            if (canHandle) {
                fragment.startActivity(fallbackIntent)
            } else {
                Toast.makeText(context, context.i18n(R.string.toast_app_lock_unavailable_app), Toast.LENGTH_LONG).show()
            }
        }
    }
}
