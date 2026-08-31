package com.asptechinc.daymark

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.content.withStyledAttributes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class Divider(
    context: Context,
    orientation: Int,
) : RecyclerView.ItemDecoration() {
    private lateinit var divider: Drawable

    private var orientation = 0

    init {
        context.withStyledAttributes(null, ATTRS) {
            divider = requireNotNull(getDrawable(0)) { "listDivider attribute is required" }
        }
        setOrientation(orientation)
    }

    private fun setOrientation(orientation: Int) {
        if (orientation != HORIZONTAL_LIST && orientation != VERTICAL_LIST) {
            throw IllegalArgumentException("invalid orientation")
        }
        this.orientation = orientation
    }

    override fun onDrawOver(
        c: Canvas,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        when (orientation) {
            VERTICAL_LIST -> drawVertical(c, parent)
            else -> drawHorizontal(c, parent)
        }
    }

    private fun drawVertical(
        c: Canvas,
        parent: RecyclerView,
    ) {
        val left = parent.paddingLeft
        val right = parent.width - parent.paddingRight

        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)

            val params = child.layoutParams as RecyclerView.LayoutParams
            val top = child.bottom + params.bottomMargin
            val bottom = top + divider.intrinsicHeight

            divider.setBounds(left, top, right, bottom)
            divider.draw(c)
        }
    }

    private fun drawHorizontal(
        c: Canvas,
        parent: RecyclerView,
    ) {
        val top = parent.paddingTop
        val bottom = parent.height - parent.paddingBottom

        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)

            val params = child.layoutParams as RecyclerView.LayoutParams
            val left = child.right + params.rightMargin
            val right = left + divider.intrinsicHeight

            divider.setBounds(left, top, right, bottom)
            divider.draw(c)
        }
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        when (orientation) {
            VERTICAL_LIST -> outRect.set(0, 0, 0, divider.intrinsicHeight)
            else -> outRect.set(0, 0, divider.intrinsicWidth, 0)
        }
    }

    companion object {
        private val ATTRS = intArrayOf(android.R.attr.listDivider)

        const val HORIZONTAL_LIST = LinearLayoutManager.HORIZONTAL
        const val VERTICAL_LIST = LinearLayoutManager.VERTICAL
    }
}
