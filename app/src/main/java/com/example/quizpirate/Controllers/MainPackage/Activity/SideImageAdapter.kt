package com.example.quizpirate.Controllers.MainPackage.Activity

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.RecyclerView

import com.example.quizpirate.R

class SideImageAdapter(
    private val items: List<Int>,
    private val onSelected: (position: Int) -> Unit
) : RecyclerView.Adapter<SideImageAdapter.VH>() {

    private var selectedIndex: Int = RecyclerView.NO_POSITION

    fun setSelectedIndex(index: Int) {
        if (index == selectedIndex) return
        val old = selectedIndex
        selectedIndex = index
        if (old != RecyclerView.NO_POSITION) notifyItemChanged(old)
        if (selectedIndex != RecyclerView.NO_POSITION) notifyItemChanged(selectedIndex)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_side_image, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position], position == selectedIndex)
        holder.itemView.setOnClickListener {
            setSelectedIndex(holder.bindingAdapterPosition)
            onSelected(holder.bindingAdapterPosition)
        }
    }

    override fun getItemCount() = items.size

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val img: ImageView = view.findViewById(R.id.img)
        fun bind(@DrawableRes resId: Int, isSelected: Boolean) {
            img.setImageResource(resId)          // juste pour avoir une vignette
            img.isSelected = isSelected          // pilote le selector XML
        }
    }
}
