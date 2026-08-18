package com.asptechinc.daymark.ui.settings

import android.app.DatePickerDialog
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.preference.PreferenceFragmentCompat
import com.asptechinc.daymark.R
import com.asptechinc.daymark.utils.i18n
import com.asptechinc.daymark.utils.styleDialogue
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class CalculatorSettingsHandler(
    private val fragment: PreferenceFragmentCompat,
) {
    fun showDateCalculatorDialogue() {
        val context = fragment.requireContext()
        val dialogueView = fragment.layoutInflater.inflate(R.layout.dialogue_date_calculator, null)

        val startDay = dialogueView.findViewById<EditText>(R.id.start_day)
        val startMonth = dialogueView.findViewById<EditText>(R.id.start_month)
        val startYear = dialogueView.findViewById<EditText>(R.id.start_year)
        val btnPickStart = dialogueView.findViewById<ImageButton>(R.id.btn_pick_start)

        val opDropdown = dialogueView.findViewById<AutoCompleteTextView>(R.id.op_dropdown)
        val inputYears = dialogueView.findViewById<EditText>(R.id.input_years)
        val inputMonths = dialogueView.findViewById<EditText>(R.id.input_months)
        val inputWeeks = dialogueView.findViewById<EditText>(R.id.input_weeks)
        val inputDays = dialogueView.findViewById<EditText>(R.id.input_days)

        val btnCalculate = dialogueView.findViewById<MaterialButton>(R.id.btn_calculate)
        val resultContainer = dialogueView.findViewById<LinearLayout>(R.id.result_container)
        val resultText = dialogueView.findViewById<TextView>(R.id.result_text)

        // Setup dropdown
        val ops =
            listOf(
                context.getString(R.string.calculator_op_add),
                context.getString(R.string.calculator_op_subtract),
            )
        val adapter =
            ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, ops)
        opDropdown.setAdapter(adapter)
        opDropdown.setText(ops[0], false)

        // Set current date as default
        val now = LocalDateTime.now()
        startDay.setText(now.dayOfMonth.toString())
        startMonth.setText(now.monthValue.toString())
        startYear.setText(now.year.toString())

        btnPickStart.setOnClickListener {
            showDatePicker { date ->
                startDay.setText(date.dayOfMonth.toString())
                startMonth.setText(date.monthValue.toString())
                startYear.setText(date.year.toString())
            }
        }

        btnCalculate.setOnClickListener {
            val start = parseDate(startDay, startMonth, startYear)
            if (start == null) {
                Toast.makeText(context, context.i18n(R.string.calculator_error_invalid_date), Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val years = inputYears.text.toString().toLongOrNull() ?: 0L
            val months = inputMonths.text.toString().toLongOrNull() ?: 0L
            val weeks = inputWeeks.text.toString().toLongOrNull() ?: 0L
            val days = inputDays.text.toString().toLongOrNull() ?: 0L

            val isAdd = opDropdown.text.toString() == ops[0]
            val resultDate =
                if (isAdd) {
                    start
                        .plusYears(years)
                        .plusMonths(months)
                        .plusWeeks(weeks)
                        .plusDays(days)
                } else {
                    start
                        .minusYears(years)
                        .minusMonths(months)
                        .minusWeeks(weeks)
                        .minusDays(days)
                }

            val formattedDate = resultDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy"))
            resultText.text = formattedDate
            resultContainer.visibility = View.VISIBLE
        }

        val dialogue =
            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.settings_label_date_calculator)
                .setView(dialogueView)
                .setNegativeButton(R.string.settings_close, null)
                .create()

        dialogue.setOnShowListener { styleDialogue(dialogue) }
        dialogue.show()
    }

    fun showDaysCalculatorDialogue() {
        val context = fragment.requireContext()
        val dialogueView = fragment.layoutInflater.inflate(R.layout.dialogue_days_calculator, null)

        val startDay = dialogueView.findViewById<EditText>(R.id.start_day)
        val startMonth = dialogueView.findViewById<EditText>(R.id.start_month)
        val startYear = dialogueView.findViewById<EditText>(R.id.start_year)
        val btnPickStart = dialogueView.findViewById<ImageButton>(R.id.btn_pick_start)

        val endDay = dialogueView.findViewById<EditText>(R.id.end_day)
        val endMonth = dialogueView.findViewById<EditText>(R.id.end_month)
        val endYear = dialogueView.findViewById<EditText>(R.id.end_year)
        val btnPickEnd = dialogueView.findViewById<ImageButton>(R.id.btn_pick_end)

        val cbIncludeEndDate =
            dialogueView.findViewById<MaterialCheckBox>(R.id.cb_include_end_date)
        val btnCalculate = dialogueView.findViewById<MaterialButton>(R.id.btn_calculate)

        val resultsContainer = dialogueView.findViewById<LinearLayout>(R.id.results_container)
        val resultBreakdown = dialogueView.findViewById<TextView>(R.id.result_breakdown)
        val resultWeeks = dialogueView.findViewById<TextView>(R.id.result_weeks)
        val resultBusinessDays = dialogueView.findViewById<TextView>(R.id.result_business_days)
        val resultWeekends = dialogueView.findViewById<TextView>(R.id.result_weekends)
        val resultTotalDays = dialogueView.findViewById<TextView>(R.id.result_total_days)

        // Set current date as default
        val now = LocalDateTime.now()
        startDay.setText(now.dayOfMonth.toString())
        startMonth.setText(now.monthValue.toString())
        startYear.setText(now.year.toString())

        endDay.setText(now.dayOfMonth.toString())
        endMonth.setText(now.monthValue.toString())
        endYear.setText(now.year.toString())

        btnPickStart.setOnClickListener {
            showDatePicker { date ->
                startDay.setText(date.dayOfMonth.toString())
                startMonth.setText(date.monthValue.toString())
                startYear.setText(date.year.toString())
            }
        }

        btnPickEnd.setOnClickListener {
            showDatePicker { date ->
                endDay.setText(date.dayOfMonth.toString())
                endMonth.setText(date.monthValue.toString())
                endYear.setText(date.year.toString())
            }
        }

        btnCalculate.setOnClickListener {
            val start = parseDate(startDay, startMonth, startYear)
            val end = parseDate(endDay, endMonth, endYear)

            if (start == null || end == null) {
                Toast.makeText(context, context.i18n(R.string.calculator_error_invalid_date), Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (end.isBefore(start)) {
                Toast.makeText(context, context.i18n(R.string.calculator_error_end_before_start), Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            calculateAndShowResults(
                start,
                end,
                cbIncludeEndDate.isChecked,
                resultsContainer,
                resultBreakdown,
                resultWeeks,
                resultBusinessDays,
                resultWeekends,
                resultTotalDays,
            )
        }

        val dialogue =
            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.settings_label_days_calculator)
                .setView(dialogueView)
                .setNegativeButton(R.string.settings_close, null)
                .create()

        dialogue.setOnShowListener { styleDialogue(dialogue) }
        dialogue.show()
    }

    private fun showDatePicker(onDateSet: (LocalDateTime) -> Unit) {
        val now = LocalDateTime.now()
        DatePickerDialog(
            fragment.requireContext(),
            { _, year, month, day ->
                onDateSet(LocalDateTime.of(year, month + 1, day, 0, 0))
            },
            now.year,
            now.monthValue - 1,
            now.dayOfMonth,
        ).show()
    }

    private fun parseDate(
        dayEt: EditText,
        monthEt: EditText,
        yearEt: EditText,
    ): LocalDateTime? =
        try {
            val d = dayEt.text.toString().toInt()
            val m = monthEt.text.toString().toInt()
            val y = yearEt.text.toString().toInt()
            LocalDateTime.of(y, m, d, 0, 0)
        } catch (_: Exception) {
            null
        }

    private fun calculateAndShowResults(
        start: LocalDateTime,
        end: LocalDateTime,
        includeEndDate: Boolean,
        container: View,
        breakdownTv: TextView,
        weeksTv: TextView,
        businessDaysTv: TextView,
        weekendsTv: TextView,
        totalDaysTv: TextView,
    ) {
        var actualEnd = end
        if (includeEndDate) {
            actualEnd = end.plusDays(1)
        }

        val startDate = start.toLocalDate()
        val endDate = actualEnd.toLocalDate()
        val period = Period.between(startDate, endDate)

        val parts = mutableListOf<String>()
        if (period.years != 0) {
            parts += if (period.years == 1) "1 year" else "${period.years} years"
        }
        if (period.months != 0) {
            parts += if (period.months == 1) "1 month" else "${period.months} months"
        }

        val totalDays = ChronoUnit.DAYS.between(startDate, endDate).toInt()
        val weeksInPeriod = (totalDays - (period.years * 365) - (period.months * 30)) / 7 // Rough approximation for breakdown
        // Actually java.time.Period.between is good for Y/M/D.
        // If we want weeks in the breakdown, it's tricky because Period doesn't have them.

        // Let's just use days for the last part of breakdown if it's less than a month.
        if (period.days != 0) {
            val w = period.days / 7
            val d = period.days % 7
            if (w != 0) {
                parts += if (w == 1) "1 week" else "$w weeks"
            }
            if (d != 0) {
                parts += if (d == 1) "1 day" else "$d days"
            }
        }

        val breakdown = if (parts.isEmpty()) "0 days" else parts.joinToString(", ")

        val context = fragment.requireContext()
        breakdownTv.text = context.getString(R.string.calculator_result_breakdown, breakdown)

        val totalWeeks = totalDays / 7
        weeksTv.text = context.getString(R.string.calculator_result_weeks, totalWeeks)

        // Business days, Sat, Sun
        var businessDays = 0
        var saturdays = 0
        var sundays = 0

        var current = startDate
        while (current.isBefore(endDate)) {
            val dayOfWeek = current.dayOfWeek
            if (dayOfWeek == DayOfWeek.SATURDAY) {
                saturdays++
            } else if (dayOfWeek == DayOfWeek.SUNDAY) {
                sundays++
            } else {
                businessDays++
            }
            current = current.plusDays(1)
        }

        businessDaysTv.text = context.getString(R.string.calculator_result_business_days, businessDays)
        weekendsTv.text =
            context.getString(
                R.string.calculator_result_weekends,
                saturdays + sundays,
                saturdays,
                sundays,
            )
        totalDaysTv.text = context.getString(R.string.calculator_result_total_days, totalDays)

        container.visibility = View.VISIBLE
    }
}
