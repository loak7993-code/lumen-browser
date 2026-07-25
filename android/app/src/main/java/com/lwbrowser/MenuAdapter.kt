package com.lwbrowser

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lwbrowser.databinding.ItemMenuBinding

data class MenuItem(val iconRes: Int, val text: String, val action: () -> Unit)

class MenuAdapter(
    private val items: List<MenuItem>,
    private val onDismiss: () -> Unit
) : RecyclerView.Adapter<MenuAdapter.VH>() {
    class VH(val b: ItemMenuBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemMenuBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.b.menuIcon.setImageResource(item.iconRes)
        holder.b.menuText.text = item.text
        holder.b.root.setOnClickListener {
            onDismiss()
            item.action()
        }
    }

    override fun getItemCount(): Int = items.size
}
