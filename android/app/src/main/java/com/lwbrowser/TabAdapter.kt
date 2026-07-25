package com.lwbrowser

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.lwbrowser.databinding.ItemTabBinding

class TabAdapter(
    private val tabs: List<Tab>,
    private val selectedIndex: Int,
    private val onClick: (Int) -> Unit,
    private val onClose: (Int) -> Unit
) : RecyclerView.Adapter<TabAdapter.VH>() {

    class VH(val b: ItemTabBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemTabBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val tab = tabs[position]
        holder.b.tabTitle.text = tab.title.ifEmpty { UrlUtils.host(tab.url) }
        holder.b.tabUrl.text = tab.url
        holder.b.root.setOnClickListener { onClick(position) }
        holder.b.btnCloseTab.setOnClickListener { onClose(position) }
        val selected = position == selectedIndex
        val card = holder.b.root as MaterialCardView
        card.strokeWidth = if (selected) 3 else 1
        card.strokeColor = ContextCompat.getColor(
            card.context,
            if (selected) R.color.brand_primary else R.color.divider_light
        )
    }

    override fun getItemCount(): Int = tabs.size
}
