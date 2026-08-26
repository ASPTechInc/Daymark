package com.asptechinc.daymark.ui.settings

import androidx.preference.PreferenceFragmentCompat
import com.asptechinc.daymark.R
import com.asptechinc.daymark.utils.AlarmHelper
import com.asptechinc.daymark.utils.LayoutManager
import com.asptechinc.daymark.utils.ThemeManager
import com.asptechinc.daymark.utils.TimeUnitManager
import com.asptechinc.daymark.utils.showStyled
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class GeneralSettingsHandler(
    private val fragment: PreferenceFragmentCompat,
) {
    fun showThemeSelectionDialogue() {
        val context = fragment.requireContext()
        val options = context.resources.getStringArray(R.array.app_theme_options)
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

    fun showLayoutModeSelectionDialogue() {
        val context = fragment.requireContext()
        val options = context.resources.getStringArray(R.array.app_layout_mode)
        val selected = LayoutManager.getSavedLayoutIndex(context)

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.settings_label_app_layout)
            .setSingleChoiceItems(options, selected) { dialogue, which ->
                LayoutManager.setLayoutByIndex(context, which)
                dialogue.dismiss()
                fragment.requireActivity().recreate()
            }.setNegativeButton(R.string.btn_cancel, null)
            .showStyled()
    }

    fun showTimeUnitSelectionDialogue() {
        val context = fragment.requireContext()
        val options = context.resources.getStringArray(R.array.app_time_period)
        val selected = TimeUnitManager.getSavedTimeUnitIndex(context)

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.settings_label_time_unit)
            .setSingleChoiceItems(options, selected) { dialogue, which ->
                TimeUnitManager.setTimeUnitByIndex(context, which)
                dialogue.dismiss()
                fragment.requireActivity().recreate()
            }.setNegativeButton(R.string.btn_cancel, null)
            .showStyled()
    }

    fun toggleNotifications(isEnabled: Boolean) {
        val context = fragment.requireContext()
        if (isEnabled) {
            AlarmHelper.rescheduleAllAlarms(context)
        } else {
            AlarmHelper.cancelAllAlarms(context)
        }
    }
}
