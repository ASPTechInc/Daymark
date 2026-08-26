package com.asptechinc.daymark.ui.settings

import android.content.Context
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.edit
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.asptechinc.daymark.R
import com.asptechinc.daymark.config.AppConfig
import com.asptechinc.daymark.utils.i18n
import com.asptechinc.daymark.utils.styleDialogue
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SecuritySettingsHandler(
    private val fragment: PreferenceFragmentCompat,
) {
    private fun showAppLockSetupDialogue(
        allowRemove: Boolean = true,
        onPinConfigured: ((Boolean) -> Unit)? = null,
    ) {
        val context = fragment.requireContext()
        val prefs = context.getSharedPreferences(AppConfig.SETTINGS_PREFS, Context.MODE_PRIVATE)
        val currentPin = prefs.getString(context.i18n(R.string.app_lock_pin_key), null)
        val firstInput = EditText(context)
        val confirmInput = EditText(context)

        firstInput.setHint(
            if (currentPin ==
                null
            ) {
                R.string.dialogue_app_pin_input_hint_new_pin
            } else {
                R.string.dialogue_app_pin_input_hint_change_pin
            },
        )
        firstInput.setHintTextColor(
            MaterialColors.getColor(
                context,
                com.google.android.material.R.attr.colorOnTertiary,
                context.getColor(R.color.on_surface_variant),
            ),
        )

        confirmInput.setHint(R.string.dialogue_app_pin_confirm_pin_hint)
        confirmInput.setHintTextColor(
            MaterialColors.getColor(
                context,
                com.google.android.material.R.attr.colorOnTertiary,
                context.getColor(R.color.on_surface_variant),
            ),
        )
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
            MaterialAlertDialogBuilder(context)
                .setTitle(if (currentPin == null) R.string.settings_label_set_app_pin else R.string.settings_label_change_app_pin)
                .setView(
                    android.widget.LinearLayout(context).apply {
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
                            prefs.edit { remove(context.i18n(R.string.app_lock_pin_key)) }
                            Toast
                                .makeText(
                                    context,
                                    context.i18n(R.string.toast_app_lock_pin_removal),
                                    Toast.LENGTH_LONG,
                                ).show()
                            onPinConfigured?.invoke(false)
                        }

                        !allowRemove && (pin.isBlank() || confirmedPin.isBlank()) -> {
                            Toast
                                .makeText(
                                    context,
                                    context.i18n(R.string.toast_app_lock_pin_required),
                                    Toast.LENGTH_LONG,
                                ).show()
                            onPinConfigured?.invoke(
                                !prefs
                                    .getString(context.i18n(R.string.app_lock_pin_key), null)
                                    .isNullOrBlank(),
                            )
                        }

                        pin != confirmedPin -> {
                            Toast
                                .makeText(
                                    context,
                                    context.i18n(R.string.toast_app_lock_pin_mismatch),
                                    Toast.LENGTH_LONG,
                                ).show()
                            onPinConfigured?.invoke(
                                !prefs
                                    .getString(context.i18n(R.string.app_lock_pin_key), null)
                                    .isNullOrBlank(),
                            )
                        }

                        pin.length !in 4..16 || !pin.all { it.isDigit() } -> {
                            Toast
                                .makeText(
                                    context,
                                    context.i18n(R.string.toast_app_lock_pin_action),
                                    Toast.LENGTH_LONG,
                                ).show()
                            onPinConfigured?.invoke(
                                !prefs
                                    .getString(context.i18n(R.string.app_lock_pin_key), null)
                                    .isNullOrBlank(),
                            )
                        }

                        else -> {
                            prefs.edit {
                                putString(
                                    context.i18n(R.string.app_lock_pin_key),
                                    pin,
                                )
                            }
                            Toast
                                .makeText(
                                    context,
                                    context.i18n(R.string.toast_app_lock_pin_set_success),
                                    Toast.LENGTH_LONG,
                                ).show()
                            onPinConfigured?.invoke(true)
                        }
                    }
                }.setNegativeButton(R.string.btn_cancel) { _, _ ->
                    onPinConfigured?.invoke(
                        !prefs
                            .getString(context.i18n(R.string.app_lock_pin_key), null)
                            .isNullOrBlank(),
                    )
                }.create()
        dialogue.setOnShowListener { styleDialogue(dialogue) }
        dialogue.show()
    }

    fun handleAppLock(appPinPreference: SwitchPreferenceCompat?) {
        val context = fragment.requireContext()
        val prefs = context.getSharedPreferences(AppConfig.SETTINGS_PREFS, Context.MODE_PRIVATE)
        val currentPin = prefs.getString(context.i18n(R.string.app_lock_pin_key), null)

        appPinPreference?.isChecked = !currentPin.isNullOrBlank()
        appPinPreference?.title =
            if (currentPin.isNullOrBlank()) {
                context.i18n(R.string.settings_label_set_app_pin)
            } else {
                context.i18n(R.string.settings_label_change_app_pin)
            }

        appPinPreference?.setOnPreferenceChangeListener { _, newValue ->
            val enablePin = newValue as Boolean
            if (enablePin) {
                showAppLockSetupDialogue(allowRemove = false) { pinSet ->
                    appPinPreference.isChecked = pinSet
                    val updatedPin = prefs.getString(context.i18n(R.string.app_lock_pin_key), null)
                    appPinPreference.title =
                        if (updatedPin.isNullOrBlank()) {
                            context.i18n(R.string.settings_label_set_app_pin)
                        } else {
                            context.i18n(R.string.settings_label_change_app_pin)
                        }
                }
                false
            } else {
                prefs.edit { remove(context.i18n(R.string.app_lock_pin_key)) }
                Toast
                    .makeText(
                        context,
                        context.i18n(R.string.toast_app_lock_pin_removal),
                        Toast.LENGTH_LONG,
                    ).show()
                appPinPreference.title = context.i18n(R.string.settings_label_set_app_pin)
                true
            }
        }

        appPinPreference?.setOnPreferenceClickListener {
            if (appPinPreference.isChecked) {
                showAppLockSetupDialogue(allowRemove = false) { pinSet ->
                    appPinPreference.isChecked = pinSet
                    val updatedPin = prefs.getString(context.i18n(R.string.app_lock_pin_key), null)
                    appPinPreference.title =
                        if (updatedPin.isNullOrBlank()) {
                            context.i18n(R.string.settings_label_set_app_pin)
                        } else {
                            context.i18n(R.string.settings_label_change_app_pin)
                        }
                }
            }
            true
        }
    }
}
