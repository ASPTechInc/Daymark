package com.asptechinc.daymark

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.asptechinc.daymark.config.AppConfig
import com.asptechinc.daymark.utils.ThemeManager
import com.asptechinc.daymark.utils.i18n

class AppLockActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences
    private lateinit var pinDisplay: TextView
    private val enteredPin = StringBuilder()
    private val maxPinLength = 16

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        ThemeManager.applySavedTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_lock)

        prefs = getSharedPreferences(AppConfig.SETTINGS_PREFS, MODE_PRIVATE)
        pinDisplay = findViewById(R.id.pin_display)

        val clearButton = findViewById<Button>(R.id.btn_clear)
        clearButton.setOnClickListener { clearPin() }

        val enterButton = findViewById<Button>(R.id.btn_enter)
        enterButton.setOnClickListener {
            if (enteredPin.isNotEmpty()) {
                validatePin()
            }
        }

        val keypadButtons =
            listOf(
                R.id.btn_0 to "0",
                R.id.btn_1 to "1",
                R.id.btn_2 to "2",
                R.id.btn_3 to "3",
                R.id.btn_4 to "4",
                R.id.btn_5 to "5",
                R.id.btn_6 to "6",
                R.id.btn_7 to "7",
                R.id.btn_8 to "8",
                R.id.btn_9 to "9",
            )

        keypadButtons.forEach { (id, value) ->
            findViewById<Button>(id).setOnClickListener {
                if (enteredPin.length < maxPinLength) {
                    enteredPin.append(value)
                    updatePinDisplay()
                }
            }
        }
    }

    private fun updatePinDisplay() {
        pinDisplay.text = "●".repeat(enteredPin.length)
    }

    private fun clearPin() {
        enteredPin.setLength(0)
        updatePinDisplay()
    }

    private fun validatePin() {
        val savedPin = prefs.getString(i18n(R.string.app_lock_pin_key), null)
        if (savedPin != null && enteredPin.toString() == savedPin) {
            Log.i("AppLockActivity", "PIN validation successful")
            prefs.edit { putBoolean(MainActivity.APP_PIN_UNLOCKED_ONCE_KEY, true) }
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            Log.i("AppLockActivity", i18n(R.string.toast_app_lock_pin_invalid))
            Toast
                .makeText(
                    this,
                    i18n(R.string.toast_app_lock_pin_invalid),
                    Toast.LENGTH_LONG,
                ).show()
            clearPin()
        }
    }
}
