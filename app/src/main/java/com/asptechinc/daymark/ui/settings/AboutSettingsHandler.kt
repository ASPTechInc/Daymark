package com.asptechinc.daymark.ui.settings

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.edit
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
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AboutSettingsHandler(
    internal val fragment: PreferenceFragmentCompat,
) {
    fun showVersionDialogue() {
        MaterialAlertDialogBuilder(fragment.requireContext())
            .setTitle(R.string.settings_label_app_version)
            .setMessage("App name: ${AppConfig.APP_NAME}\nVersion: ${BuildConfig.VERSION_NAME}\nBuild time: ${BuildConfig.BUILD_TIME}")
            .setPositiveButton(R.string.btn_okay, null)
            .showStyled()
    }

    fun showAppUpdaterDialogue() {
        val context = fragment.requireContext()
        val prefs = context.getSharedPreferences(AppConfig.SETTINGS_PREFS, Context.MODE_PRIVATE)
        val prefKey = context.i18n(R.string.settings_key_auto_app_updater)

        val container = FrameLayout(context)
        val switch =
            MaterialSwitch(context).apply {
                text = context.getString(R.string.updater_btn_check)
                isChecked = prefs.getBoolean(prefKey, false)

                // Define the states for the switch
                val states =
                    arrayOf(
                        intArrayOf(android.R.attr.state_checked), // Checked state
                        intArrayOf(-android.R.attr.state_checked), // Unchecked state
                    )

                // Set the Thumb colour (i.e. the moving circle)
                val thumbColours =
                    intArrayOf(
                        MaterialColors.getColor(
                            this,
                            com.google.android.material.R.attr.colorOnTertiary,
                        ), // Checked colour
                        MaterialColors.getColor(
                            this,
                            com.google.android.material.R.attr.colorSurfaceVariant,
                        ), // Unchecked colour
                    )
                thumbTintList = ColorStateList(states, thumbColours)

                // Set the Track colour (i.e. the background bar)
                val trackColours =
                    intArrayOf(
                        MaterialColors.getColor(
                            this,
                            com.google.android.material.R.attr.colorPrimaryContainer,
                        ), // Checked colour
                        MaterialColors.getColor(
                            this,
                            R.attr.colorCheckboxUnchecked,
                        ), // Unchecked colour
                    )
                trackTintList = ColorStateList(states, trackColours)
            }
        val margin = context.resources.getDimensionPixelSize(R.dimen.layout_dimension_24)
        val params =
            FrameLayout
                .LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    setMargins(margin, margin / 3, margin, margin / 3)
                }
        container.addView(switch, params)

        val dialogue =
            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.settings_label_app_updater)
                .setView(container)
                .setPositiveButton(R.string.btn_close, null)
                .create()

        var updateJob: Job? = null

        fun performUpdateCheck() {
            updateJob?.cancel()
            dialogue.setMessage(context.getString(R.string.updater_status_checking))
            updateJob =
                fragment.lifecycleScope.launch {
                    val latestRelease = AppUpdater.checkForUpdate(BuildConfig.VERSION_NAME)
                    if (latestRelease != null) {
                        val apkAsset = latestRelease.assets.find { it.name.endsWith(".apk") }
                        if (apkAsset != null) {
                            dialogue.setMessage(
                                context.getString(
                                    R.string.updater_status_update_available,
                                    latestRelease.tagName,
                                ),
                            )
                            dialogue.setButton(
                                androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE,
                                context.getString(R.string.updater_btn_update),
                            ) { _, _ ->
                                Toast
                                    .makeText(
                                        context,
                                        context.i18n(R.string.updater_downloading),
                                        Toast.LENGTH_LONG,
                                    ).show()
                                AppUpdater.downloadAndInstall(
                                    context,
                                    apkAsset.browserDownloadUrl,
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

        switch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit { putBoolean(prefKey, isChecked) }
            if (isChecked) {
                performUpdateCheck()
            } else {
                updateJob?.cancel()
                dialogue.setMessage(null)
                dialogue.setButton(
                    androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE,
                    context.getString(R.string.btn_okay),
                ) { _, _ -> }
            }
        }

        dialogue.setOnShowListener {
            styleDialogue(dialogue)
        }

        if (switch.isChecked) {
            performUpdateCheck()
        }
        dialogue.show()
    }

    fun showChangelogDialogue() {
        val context = fragment.requireContext()
        val versions = ChangelogParser.parse(context)
        val view = fragment.layoutInflater.inflate(R.layout.dialogue_changelog, null)
        val recyclerView = view.findViewById<RecyclerView>(R.id.changelog_recycler)
        recyclerView.adapter = ChangelogAdapter(versions)

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.settings_label_app_changelog)
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
                Toast
                    .makeText(
                        context,
                        context.i18n(R.string.toast_app_lock_unavailable_app),
                        Toast.LENGTH_LONG,
                    ).show()
            }
        }
    }

    fun showOssLicencesDialogue() {
        val context = fragment.requireContext()
        val apacheUrl = context.getString(R.string.oss_licence_apache_url)
        val mitUrl = context.getString(R.string.oss_licence_mit_url)
        val eplUrl = context.getString(R.string.oss_licence_epl_url)

        val message =
            SpannableStringBuilder().apply {
                append("Daymark is built using the following open source libraries:\n\n")

                fun appendLibrary(
                    name: String,
                    license: String,
                    url: String,
                ) {
                    append("• $name (")
                    val start = length
                    append(license)
                    setSpan(
                        object : ClickableSpan() {
                            override fun onClick(widget: View) {
                                openExternalUrl(url)
                            }
                        },
                        start,
                        length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                    )
                    append(")\n")
                }

                appendLibrary("Android Jetpack", "Apache 2.0", apacheUrl)
                appendLibrary("Material Components", "Apache 2.0", apacheUrl)
                appendLibrary("Room Persistence Library", "Apache 2.0", apacheUrl)
                appendLibrary("Kotlin Coroutines", "Apache 2.0", apacheUrl)
                appendLibrary("Kotlinx Serialization", "Apache 2.0", apacheUrl)
                appendLibrary("Mockito", "MIT", mitUrl)
                appendLibrary("JUnit", "EPL 1.0", eplUrl)
            }

        val dialogue =
            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.settings_label_oss_licences)
                .setMessage(message)
                .setPositiveButton(R.string.btn_okay, null)
                .create()

        dialogue.setOnShowListener {
            styleDialogue(dialogue)
            dialogue.findViewById<TextView>(android.R.id.message)?.movementMethod =
                LinkMovementMethod.getInstance()
        }
        dialogue.show()
    }
}
