package com.lwbrowser

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class Suggestion(
    val text: String,
    val url: String,
    val type: SuggestionType
)

enum class SuggestionType {
    SEARCH, HISTORY, BOOKMARK
}

class SuggestionsAdapter(
    private val onClick: (Suggestion) -> Unit
) : RecyclerView.Adapter<SuggestionsAdapter.VH>() {

    private val items = mutableListOf<Suggestion>()

    fun update(newItems: List<Suggestion>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun clear() {
        items.clear()
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_suggestion, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.text.text = item.text
        holder.icon.setImageResource(
            when (item.type) {
                SuggestionType.SEARCH -> R.drawable.ic_search
                SuggestionType.HISTORY -> R.drawable.ic_history
                SuggestionType.BOOKMARK -> R.drawable.ic_bookmark
            }
        )
        holder.source.text = when (item.type) {
            SuggestionType.SEARCH -> "Search"
            SuggestionType.HISTORY -> "History"
            SuggestionType.BOOKMARK -> "Bookmark"
        }
        holder.source.visibility = View.VISIBLE
        holder.itemView.setOnClickListener { onClick(item) }
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.suggestionIcon)
        val text: TextView = view.findViewById(R.id.suggestionText)
        val source: TextView = view.findViewById(R.id.suggestionSource)
    }
}
