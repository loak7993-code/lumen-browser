package com.lwbrowser

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lwbrowser.databinding.ItemLinkBinding

class LinkAdapter(
    private val items: List<LinkItem>,
    private val iconRes: Int = R.drawable.ic_globe,
    private val onClick: (LinkItem) -> Unit,
    private val onDelete: (LinkItem) -> Unit
) : RecyclerView.Adapter<LinkAdapter.VH>() {

    class VH(val b: ItemLinkBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemLinkBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.b.itemIcon.setImageResource(iconRes)
        holder.b.itemTitle.text = item.title.ifEmpty { item.url }
        holder.b.itemUrl.text = item.url
        holder.b.root.setOnClickListener { onClick(item) }
        holder.b.itemDelete.setOnClickListener { onDelete(item) }
    }

    override fun getItemCount(): Int = items.size
}
