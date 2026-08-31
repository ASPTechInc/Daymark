package com.asptechinc.daymark.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.asptechinc.daymark.R
import com.asptechinc.daymark.models.Tag
import com.google.android.material.checkbox.MaterialCheckBox

/**
 * Adapter for selecting tags in a searchable list.
 *
 * This adapter uses [ListAdapter] for efficient list updates and supports filtering tags
 * based on a search query. It maintains its own state of selected tag IDs.
 *
 * @param allTags The full list of available tags to choose from.
 * @param initiallySelectedIds A set of IDs for tags that should be selected by default.
 */
class TagSelectionAdapter(
    private val allTags: List<Tag>,
    initiallySelectedIds: Set<Int>,
) : ListAdapter<Tag, TagSelectionAdapter.TagViewHolder>(TagDiffCallback()) {
    private val selectedIds = initiallySelectedIds.toMutableSet()

    init {
        submitList(allTags)
    }

    /**
     * Filters the displayed tags based on the provided [query].
     *
     * If the query is empty, all tags are shown. Otherwise, only tags whose names
     * contain the query (case-insensitive) are displayed.
     *
     * @param query The search string to filter by.
     */
    fun filter(query: String) {
        val filteredList =
            if (query.isEmpty()) {
                allTags
            } else {
                allTags.filter { it.name.contains(query, ignoreCase = true) }
            }
        submitList(filteredList)
    }

    /**
     * Returns the list of IDs for all tags currently selected in the adapter.
     */
    fun getSelectedIds(): List<Int> = selectedIds.toList()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): TagViewHolder {
        val view =
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.item_tag_selection, parent, false)
        return TagViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: TagViewHolder,
        position: Int,
    ) {
        val tag = getItem(position)
        holder.bind(tag)
    }

    inner class TagViewHolder(
        itemView: View,
    ) : RecyclerView.ViewHolder(itemView) {
        private val checkbox: MaterialCheckBox = itemView.findViewById(R.id.tag_checkbox)
        private val tagName: TextView = itemView.findViewById(R.id.tag_name_text)

        fun bind(tag: Tag) {
            tagName.text = tag.name
            checkbox.isChecked = selectedIds.contains(tag.id)

            itemView.setOnClickListener {
                checkbox.isChecked = !checkbox.isChecked
                toggleSelection(tag.id, checkbox.isChecked)
            }

            checkbox.setOnClickListener {
                toggleSelection(tag.id, checkbox.isChecked)
            }
        }

        private fun toggleSelection(
            id: Int,
            isSelected: Boolean,
        ) {
            if (isSelected) {
                selectedIds.add(id)
            } else {
                selectedIds.remove(id)
            }
        }
    }

    class TagDiffCallback : DiffUtil.ItemCallback<Tag>() {
        override fun areItemsTheSame(
            oldItem: Tag,
            newItem: Tag,
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: Tag,
            newItem: Tag,
        ): Boolean = oldItem == newItem
    }
}
