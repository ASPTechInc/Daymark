package com.asptechinc.daymark.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.asptechinc.daymark.R
import com.asptechinc.daymark.models.ChangelogVersion

class ChangelogAdapter(
    private val versions: List<ChangelogVersion>,
) : RecyclerView.Adapter<ChangelogAdapter.ViewHolder>() {
    class ViewHolder(
        view: View,
    ) : RecyclerView.ViewHolder(view) {
        val versionName: TextView = view.findViewById(R.id.version_name)
        val versionDate: TextView = view.findViewById(R.id.version_date)
        val versionDescription: TextView = view.findViewById(R.id.version_description)
        val sectionsContainer: LinearLayout = view.findViewById(R.id.sections_container)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        val view =
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.item_changelog_version, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        val version = versions[position]
        holder.versionName.text = version.version
        holder.versionDate.text = version.date
        holder.versionDate.visibility = if (version.date != null) View.VISIBLE else View.GONE

        holder.versionDescription.text = version.description
        holder.versionDescription.visibility = if (version.description != null) View.VISIBLE else View.GONE

        holder.sectionsContainer.removeAllViews()
        val inflater = LayoutInflater.from(holder.itemView.context)

        version.sections.forEach { section ->
            val sectionView = inflater.inflate(R.layout.item_changelog_section, holder.sectionsContainer, false)
            val sectionTitle = sectionView.findViewById<TextView>(R.id.section_title)
            val itemsContainer = sectionView.findViewById<LinearLayout>(R.id.items_container)

            sectionTitle.text = section.title

            section.items.forEach { item ->
                val itemView =
                    TextView(holder.itemView.context).apply {
                        text = "• $item"
                        androidx.core.widget.TextViewCompat.setTextAppearance(
                            this,
                            com.google.android.material.R.style.TextAppearance_Material3_BodyMedium,
                        )
                        setPadding(0, 4, 0, 4)
                    }
                itemsContainer.addView(itemView)
            }

            holder.sectionsContainer.addView(sectionView)
        }
    }

    override fun getItemCount() = versions.size
}
