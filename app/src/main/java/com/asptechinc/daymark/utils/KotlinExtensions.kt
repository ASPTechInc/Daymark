@file:Suppress("DEPRECATION")

package com.asptechinc.daymark.utils

import android.app.Fragment
import android.content.Context
import androidx.fragment.app.Fragment as AndroidXFragment

fun Context.i18n(resourceId: Int) = getString(resourceId)

fun Fragment.i18n(resourceId: Int) = getString(resourceId)

fun AndroidXFragment.i18n(resourceId: Int) = getString(resourceId)
