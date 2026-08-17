package com.asptechinc.daymark

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class RecyclerTouchListener(
    context: Context,
    recyclerView: RecyclerView,
    private val clickListener: ClickListener,
) : RecyclerView.OnItemTouchListener {
    interface ClickListener {
        fun onClick(
            view: View,
            position: Int,
        )

        fun onLongClick(
            view: View,
            position: Int,
        )
    }

    private val gestureDetector: GestureDetector
    private var isLongPress = false

    init {
        gestureDetector =
            GestureDetector(
                context,
                object : GestureDetector.SimpleOnGestureListener() {
                    override fun onSingleTapUp(e: MotionEvent) = true

                    override fun onLongPress(e: MotionEvent) {
                        val child = recyclerView.findChildViewUnder(e.x, e.y)
                        if (child != null) {
                            isLongPress = true
                            clickListener.onLongClick(
                                child,
                                recyclerView.getChildAdapterPosition(child),
                            )
                        }
                    }
                },
            )
    }

    override fun onInterceptTouchEvent(
        view: RecyclerView,
        e: MotionEvent,
    ): Boolean {
        val child = view.findChildViewUnder(e.x, e.y)
        if (child == null) {
            return false
        }

        val itemMenuButton = child.findViewById<View?>(R.id.item_menu_button)
        if (itemMenuButton != null && itemMenuButton.isPressed) {
            return false
        }

        val handled = gestureDetector.onTouchEvent(e)
        if (isLongPress) {
            isLongPress = false
            return true
        }

        if (handled) {
            clickListener.onClick(child, view.getChildAdapterPosition(child))
            return true
        }

        return false
    }

    override fun onTouchEvent(
        view: RecyclerView,
        e: MotionEvent,
    ) = Unit

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) = Unit
}
