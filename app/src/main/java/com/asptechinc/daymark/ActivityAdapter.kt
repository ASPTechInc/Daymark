package com.asptechinc.daymark

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.MenuCompat
import androidx.recyclerview.widget.RecyclerView
import com.asptechinc.daymark.models.Activity
import com.asptechinc.daymark.models.Category
import com.asptechinc.daymark.models.Tag
import com.asptechinc.daymark.utils.relativeDateText
import com.asptechinc.daymark.utils.toOrdinalDateString
import com.google.android.material.checkbox.MaterialCheckBox
import java.time.LocalDateTime

class ActivityAdapter(
    context: Context,
    private val onMenuClick: (Int, Int) -> Unit,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit,
) : RecyclerView.Adapter<ActivityAdapter.DayViewHolder>() {
    var activities = mutableListOf<Activity>()
    var tags = mutableListOf<Tag>()
    var categories = mutableListOf<Category>()
    var isDragModeEnabled = false

    fun toggleDragMode() {
        isDragModeEnabled = !isDragModeEnabled
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(
        holder: DayViewHolder,
        position: Int,
    ) {
        holder.bind(activities[position], categories, tags)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): DayViewHolder {
        val itemView =
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.activity_list, parent, false)

        return DayViewHolder(itemView, onMenuClick, onStartDrag)
    }

    override fun getItemCount() = activities.size

    override fun getItemId(position: Int) = position.toLong()

    inner class DayViewHolder(
        itemView: View,
        private val onMenuClick: (Int, Int) -> Unit,
        private val onStartDrag: (RecyclerView.ViewHolder) -> Unit,
    ) : RecyclerView.ViewHolder(itemView) {
        var activityNameView: TextView = itemView.findViewById(R.id.activity_name_text_view)
        var notesView: TextView = itemView.findViewById(R.id.notes_text_view)

        var relativeTextView: TextView = itemView.findViewById(R.id.relative_date_text_view)
        var startDateTextView: TextView = itemView.findViewById(R.id.start_date_text_view)
        var endDateTextView: TextView = itemView.findViewById(R.id.end_date_text_view)

        var archivedCheckBox: MaterialCheckBox = itemView.findViewById(R.id.archived_checkbox)
        var categoryTextView: TextView = itemView.findViewById(R.id.category_text_view)

        var tagTextView: TextView = itemView.findViewById(R.id.tag_text_view)
        var dragHandleView: ImageView = itemView.findViewById(R.id.drag_handle)

        var menuButton = itemView.findViewById<ImageButton>(R.id.item_menu_button)

        init {
            dragHandleView.visibility = View.GONE
            itemView.setOnLongClickListener {
                toggleDragMode()
                true
            }
            dragHandleView.setOnLongClickListener {
                if (isDragModeEnabled) {
                    onStartDrag(this@DayViewHolder)
                    true
                } else {
                    false
                }
            }
        }

        fun bind(
            counter: Activity,
            categories: List<Category>,
            tags: List<Tag>,
        ) {
            dragHandleView.visibility = if (isDragModeEnabled) View.VISIBLE else View.GONE
            dragHandleView.setImageResource(R.drawable.ic_menu)
            dragHandleView.setColorFilter(
                ContextCompat.getColor(
                    itemView.context,
                    if (isDragModeEnabled) R.color.primary else R.color.secondary,
                ),
            )
            activityNameView.text = counter.activityName
            notesView.text = counter.notes

            val currentDate = LocalDateTime.now()
            val endDateTime = counter.endDateTime
            val startDateTime = counter.startDateTime

            // Relative date
            val relativeText =
                endDateTime?.let {
                    if (it.isAfter(currentDate)) {
                        "Ends ${relativeDateText(it, currentDate)}"
                    } else {
                        "Ended ${relativeDateText(it, currentDate)}"
                    }
                } ?: relativeDateText(counter.startDateTime, currentDate)

            relativeTextView.text = relativeText

            // Start date
            startDateTextView.text =
                if (startDateTime.isAfter(currentDate)) {
                    "Starts: ${startDateTime.toOrdinalDateString()}"
                } else {
                    "Started: ${startDateTime.toOrdinalDateString()}"
                }

            // End date
            if (endDateTime != null) {
                endDateTextView.visibility = View.VISIBLE
                endDateTextView.text =
                    if (endDateTime.isAfter(currentDate)) {
                        "Ends: ${endDateTime.toOrdinalDateString()}"
                    } else {
                        "Ended: ${endDateTime.toOrdinalDateString()}"
                    }
            } else {
                endDateTextView.visibility = View.GONE
            }

            // Archived
            archivedCheckBox.checkedState =
                if (counter.archived == true) {
                    MaterialCheckBox.STATE_CHECKED
                } else {
                    MaterialCheckBox.STATE_UNCHECKED
                }

            // Category
            val category =
                categories.find {
                    it.id == counter.categoryId
                }

            categoryTextView.text = category?.name ?: "No category"

            // Tags
            val counterTags =
                tags.filter {
                    counter.tagIds.contains(it.id)
                }
            tagTextView.text =
                counterTags.joinToString(", ") {
                    it.name
                }

            // Menu button
            menuButton.setOnClickListener { view ->
                view.parent?.requestDisallowInterceptTouchEvent(true)

                val popup = PopupMenu(view.context, view)
                popup.setForceShowIcon(true)
                popup.menuInflater.inflate(
                    R.menu.menu_list,
                    popup.menu,
                )
                MenuCompat.setGroupDividerEnabled(popup.menu, true)
                popup.setOnMenuItemClickListener { menuItem ->

                    val position = bindingAdapterPosition

                    if (position == RecyclerView.NO_POSITION) {
                        return@setOnMenuItemClickListener false
                    }

                    when (menuItem.itemId) {
                        R.id.edit -> {
                            onMenuClick(position, R.id.edit)
                            true
                        }

                        R.id.duplicate -> {
                            onMenuClick(position, R.id.duplicate)
                            true
                        }

                        R.id.share -> {
                            onMenuClick(position, R.id.share)
                            true
                        }

                        R.id.archive -> {
                            onMenuClick(position, R.id.archive)
                            true
                        }

                        R.id.delete -> {
                            onMenuClick(position, R.id.delete)
                            true
                        }

                        else -> false
                    }
                }
                popup.show()
            }
        }
    }
}
