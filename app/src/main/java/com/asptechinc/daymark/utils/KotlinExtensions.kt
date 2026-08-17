@file:Suppress("DEPRECATION")

package com.asptechinc.daymark.utils

import android.app.Fragment
import android.content.Context
import androidx.fragment.app.Fragment as AndroidXFragment

fun Context.i18n(resourceId: Int) = getString(resourceId)

fun Fragment.i18n(resourceId: Int) = getString(resourceId)

fun AndroidXFragment.i18n(resourceId: Int) = getString(resourceId)

/**
 * calls the given block and then calls a 'finalising' function.
 * basically like Closeable. Use, but can be anything you want
 * @sample
a.finish(a::finalize) {
a.start()
println("mid run")
}

 */
inline fun <T> T.finish(
    finalizer: () -> Unit,
    block: T.() -> Unit,
): T {
    block()
    finalizer()
    return this
}
