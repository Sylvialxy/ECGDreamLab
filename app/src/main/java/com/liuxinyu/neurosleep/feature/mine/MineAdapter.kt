package com.liuxinyu.neurosleep.feature.mine

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.liuxinyu.neurosleep.R

class MineAdapter(private val items: List<MineItem>) : RecyclerView.Adapter<MineAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_mine, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconImageView: ImageView = itemView.findViewById(R.id.setting_icon)
        private val titleTextView: TextView = itemView.findViewById(R.id.setting_title)
        private val arrowImageView: ImageView = itemView.findViewById(R.id.setting_arrow)

        fun bind(item: MineItem) {
            iconImageView.setImageResource(item.iconResId)
            titleTextView.text = item.title
            itemView.setOnClickListener { item.action() }
        }
    }
}
