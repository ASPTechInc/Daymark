package com.asptechinc.daymark.utils

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.CheckedTextView
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.asptechinc.daymark.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.time.LocalDateTime
import java.time.Period
import java.time.format.DateTimeFormatter

fun datePicked(
    year: Int,
    month: Int,
    day: Int,
) = LocalDateTime.of(year, month, day, 0, 0)

fun formatDate(dateTime: LocalDateTime): String = dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

fun relativeDateText(
    from: LocalDateTime,
    to: LocalDateTime,
): String {
    val isFuture = from.isAfter(to)

    val startDate = if (isFuture) to.toLocalDate() else from.toLocalDate()
    val endDate = if (isFuture) from.toLocalDate() else to.toLocalDate()

    val period = Period.between(startDate, endDate)

    val parts = mutableListOf<String>()

    if (period.years != 0) {
        parts += if (period.years == 1) "1 year" else "${period.years} years"
    }
    if (period.months != 0) {
        parts += if (period.months == 1) "1 month" else "${period.months} months"
    }
    val daysOnly = period.days % 7
    val weeks = period.days / 7

    if (weeks != 0) {
        parts += if (weeks == 1) "1 week" else "$weeks weeks"
    }
    if (daysOnly != 0) {
        parts += if (daysOnly == 1) "1 day" else "$daysOnly days"
    }

    val text = if (parts.isEmpty()) "0 days" else parts.joinToString(", ")

    return if (isFuture) "in $text" else "$text ago"
}

fun LocalDateTime.toOrdinalDateString(): String {
    val day = dayOfMonth
    val suffix =
        when {
            day in 11..13 -> "th"
            day % 10 == 1 -> "st"
            day % 10 == 2 -> "nd"
            day % 10 == 3 -> "rd"
            else -> "th"
        }

    return "$day$suffix ${format(DateTimeFormatter.ofPattern("MMMM, yyyy"))}"
}

fun Float.dpToPx(context: Context): Float =
    TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this,
        context.resources.displayMetrics,
    )

fun MaterialAlertDialogBuilder.showStyled(): AlertDialog {
    val dialogue = create()
    dialogue.setOnShowListener { styleDialogue(dialogue) }
    dialogue.show()
    return dialogue
}

fun styleDialogue(dialogue: AlertDialog) {
    val context = dialogue.context

    // Resolve colours from your current theme
    val titleColour =
        MaterialColors.getColor(
            context,
            com.google.android.material.R.attr.colorOnSurface,
            context.getColor(R.color.on_surface),
        )

    val bodyColour =
        MaterialColors.getColor(
            context,
            R.attr.colorOnMenuContainer,
            context.getColor(R.color.on_surface_variant),
        )

    val positiveColour =
        MaterialColors.getColor(
            context,
            R.attr.colorOnPrimaryContainer,
            context.getColor(R.color.primary),
        )

    val negativeColour =
        MaterialColors.getColor(
            context,
            R.attr.colorOnSecondaryContainer,
            context.getColor(R.color.error),
        )
    val negativeBgColour =
        MaterialColors.getColor(
            context,
            com.google.android.material.R.attr.colorSecondaryContainer,
            context.getColor(R.color.secondary_container),
        )
    val outlineColour =
        MaterialColors.getColor(
            context,
            com.google.android.material.R.attr.colorOutline,
            context.getColor(R.color.outline),
        )

    val neutralColour =
        MaterialColors.getColor(
            context,
            com.google.android.material.R.attr.colorOnTertiary,
            context.getColor(R.color.secondary),
        )

    val checkColour =
        MaterialColors.getColor(
            context,
            com.google.android.material.R.attr.colorOnPrimary,
            context.getColor(R.color.primary),
        )

    // Style title text
    dialogue.findViewById<TextView>(androidx.appcompat.R.id.alertTitle)?.apply {
        setTextColor(titleColour)
        setTypeface(typeface, Typeface.BOLD)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
    }

    // Style message/body text (if this dialogue has one)
    dialogue.findViewById<TextView>(android.R.id.message)?.apply {
        setTextColor(bodyColour)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        setLineSpacing(0f, 1.1f)
    }

    // Style list items
    dialogue.listView?.let { listView ->
        // Style currently visible/added views
        listView.post {
            for (i in 0 until listView.childCount) {
                listView
                    .getChildAt(i)
                    .recursivelyStyleDialogueItem(bodyColour, checkColour, outlineColour)
            }
        }

        // Style views added during scrolling/binding
        listView.setOnHierarchyChangeListener(
            object : ViewGroup.OnHierarchyChangeListener {
                override fun onChildViewAdded(
                    parent: View?,
                    child: View?,
                ) {
                    child?.recursivelyStyleDialogueItem(bodyColour, checkColour, outlineColour)
                }

                override fun onChildViewRemoved(
                    parent: View?,
                    child: View?,
                ) {
                }
            },
        )
    }

    // Style action buttons
    val positiveButton = dialogue.getButton(AlertDialog.BUTTON_POSITIVE)
    positiveButton?.apply {
        setTextColor(positiveColour)
        setTypeface(Typeface.DEFAULT_BOLD)
        isAllCaps = false
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)

        val primaryContainerColour =
            MaterialColors.getColor(
                context,
                com.google.android.material.R.attr.colorPrimaryContainer,
                context.getColor(R.color.primary_container),
            )

        this.backgroundTintList = ColorStateList.valueOf(primaryContainerColour)
        this.backgroundTintMode = android.graphics.PorterDuff.Mode.SRC_IN
    }

    val negativeButton = dialogue.getButton(AlertDialog.BUTTON_NEGATIVE)
    negativeButton?.apply {
        setTextColor(negativeColour) // cancel button
        setTypeface(Typeface.DEFAULT_BOLD)
        isAllCaps = false
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)

        val cornerPx = 4f.dpToPx(context)
        val strokePx = 2f.dpToPx(context).toInt()

        if (this is MaterialButton) {
            backgroundTintList = ColorStateList.valueOf(negativeBgColour)
            cornerRadius = cornerPx.toInt()
            strokeColor = ColorStateList.valueOf(outlineColour)
            strokeWidth = strokePx
        } else {
            val bg =
                GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = cornerPx
                    setColor(negativeBgColour)
                    setStroke(strokePx, outlineColour)
                }
            this.background = bg
        }
    }

    val neutralButton = dialogue.getButton(AlertDialog.BUTTON_NEUTRAL)
    neutralButton?.apply {
        setTextColor(neutralColour)
        setTypeface(Typeface.DEFAULT_BOLD)
        isAllCaps = false
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
    }
}

private fun View.recursivelyStyleDialogueItem(
    textColour: Int,
    checkColour: Int,
    outlineColour: Int,
) {
    when (this) {
        is CheckedTextView -> {
            setTextColor(textColour)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
//            checkMarkTintList = ColorStateList.valueOf(checkColour)
//            checkMarkTintMode = android.graphics.PorterDuff.Mode.SRC_IN

//            val drawable = checkMarkDrawable?.mutate()
//            drawable?.setTint(checkColour)
//            checkMarkDrawable = drawable

//            val outlineColour = MaterialColors.getColor(
//                context,
//                com.google.android.material.R.attr.colorOutline,
//                context.getColor(R.color.outline),
//            )
            val checkTint =
                ColorStateList(
                    arrayOf(
                        intArrayOf(android.R.attr.state_checked),
                        intArrayOf(-android.R.attr.state_checked),
                    ),
                    intArrayOf(
                        checkColour,
                        outlineColour,
                    ),
                )

            checkMarkDrawable?.mutate()?.let { drawable ->
                drawable.setTintList(checkTint)
                checkMarkDrawable = drawable
            }
        }

        is RadioButton -> {
            buttonTintList = ColorStateList.valueOf(checkColour)
            buttonTintMode = android.graphics.PorterDuff.Mode.SRC_IN
        }

        is TextView -> {
            setTextColor(textColour)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        }

        is ViewGroup -> {
            for (i in 0 until childCount) {
                getChildAt(i).recursivelyStyleDialogueItem(textColour, checkColour, outlineColour)
            }
        }
    }
}
