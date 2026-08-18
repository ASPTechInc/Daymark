package com.asptechinc.daymark.ui.settings

import androidx.preference.PreferenceFragmentCompat
import com.asptechinc.daymark.R
import com.asptechinc.daymark.utils.ThemeManager
import com.asptechinc.daymark.utils.showStyled
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class GeneralSettingsHandler(
    private val fragment: PreferenceFragmentCompat,
) {
    fun showThemeSelectionDialogue() {
        val context = fragment.requireContext()
        val options = context.resources.getStringArray(R.array.settings_theme_options)
        val selected = ThemeManager.getSavedThemeIndex(context)

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.settings_label_theme)
            .setSingleChoiceItems(options, selected) { dialogue, which ->
                ThemeManager.setThemeByIndex(context, which)
                dialogue.dismiss()
                fragment.requireActivity().recreate()
            }.setNegativeButton(R.string.btn_cancel, null)
            .showStyled()
    }
}
