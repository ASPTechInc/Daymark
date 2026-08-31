package com.asptechinc.daymark.ui.settings

import com.asptechinc.daymark.R

class SupportSettingsHandler(
    private val aboutSettingsHandler: AboutSettingsHandler,
) {
    fun openDonateUrl() {
        val url = aboutSettingsHandler.fragment.getString(R.string.about_donate_url)
        aboutSettingsHandler.openExternalUrl(url)
    }

    fun openSourceCodeUrl() {
        val url = aboutSettingsHandler.fragment.getString(R.string.about_source_code_url)
        aboutSettingsHandler.openExternalUrl(url)
    }
}
