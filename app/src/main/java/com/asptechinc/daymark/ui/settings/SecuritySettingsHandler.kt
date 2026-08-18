package com.asptechinc.daymark.ui.settings

import android.content.Context
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.edit
import androidx.preference.PreferenceFragmentCompat
import com.asptechinc.daymark.R
import com.asptechinc.daymark.utils.i18n
import com.asptechinc.daymark.utils.styleDialogue
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SecuritySettingsHandler(
    private val fragment: PreferenceFragmentCompat,
) {
    fun showAppLockSetupDialogue(
        allowRemove: Boolean = true,
        onPinConfigured: ((Boolean) -> Unit)? = null,
    ) {
        val context = fragment.requireContext()
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val currentPin = prefs.getString(context.i18n(R.string.backup_key_app_pin), null)
        val firstInput = EditText(context)
        val confirmInput = EditText(context)
        firstInput.setHint(if (currentPin == null) "Enter a PIN (4-16 digits)" else "Enter a new PIN (4-16 digits)")
        confirmInput.setHint("Confirm PIN")
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
                .setTitle(if (currentPin == null) "Set app PIN" else "Change app PIN")
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
                            prefs.edit { remove(context.i18n(R.string.backup_key_app_pin)) }
                            Toast.makeText(context, context.i18n(R.string.toast_app_lock_pin_removal), Toast.LENGTH_LONG).show()
                            onPinConfigured?.invoke(false)
                        }

                        !allowRemove && (pin.isBlank() || confirmedPin.isBlank()) -> {
                            Toast.makeText(context, context.i18n(R.string.toast_app_lock_pin_required), Toast.LENGTH_LONG).show()
                            onPinConfigured?.invoke(
                                !prefs
                                    .getString(context.i18n(R.string.backup_key_app_pin), null)
                                    .isNullOrBlank(),
                            )
                        }

                        pin != confirmedPin -> {
                            Toast.makeText(context, context.i18n(R.string.toast_app_lock_pin_mismatch), Toast.LENGTH_LONG).show()
                            onPinConfigured?.invoke(
                                !prefs
                                    .getString(context.i18n(R.string.backup_key_app_pin), null)
                                    .isNullOrBlank(),
                            )
                        }

                        pin.length !in 4..16 || !pin.all { it.isDigit() } -> {
                            Toast.makeText(context, context.i18n(R.string.toast_app_lock_pin_action), Toast.LENGTH_LONG).show()
                            onPinConfigured?.invoke(
                                !prefs
                                    .getString(context.i18n(R.string.backup_key_app_pin), null)
                                    .isNullOrBlank(),
                            )
                        }

                        else -> {
                            prefs.edit { putString(context.i18n(R.string.backup_key_app_pin), pin) }
                            Toast.makeText(context, context.i18n(R.string.toast_app_lock_pin_set_success), Toast.LENGTH_LONG).show()
                            onPinConfigured?.invoke(true)
                        }
                    }
                }.setNegativeButton(R.string.btn_cancel) { _, _ ->
                    onPinConfigured?.invoke(
                        !prefs
                            .getString(context.i18n(R.string.backup_key_app_pin), null)
                            .isNullOrBlank(),
                    )
                }.create()
        dialogue.setOnShowListener { styleDialogue(dialogue) }
        dialogue.show()
    }
}
