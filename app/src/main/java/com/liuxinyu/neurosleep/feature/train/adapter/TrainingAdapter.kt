package com.liuxinyu.neurosleep.feature.train.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.liuxinyu.neurosleep.R
import com.liuxinyu.neurosleep.feature.train.model.MediaType
import com.liuxinyu.neurosleep.feature.train.model.TrainingItem

class TrainingAdapter(
    private var items: List<TrainingItem>,
    private val onItemClick: (TrainingItem) -> Unit
) : RecyclerView.Adapter<TrainingAdapter.TrainingViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrainingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_training, parent, false)
        return TrainingViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrainingViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<TrainingItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    inner class TrainingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageTraining: ImageView = itemView.findViewById(R.id.imageTraining)
        private val textTitle: TextView = itemView.findViewById(R.id.textTitle)
        private val textDescription: TextView = itemView.findViewById(R.id.textDescription)
        private val textDuration: TextView = itemView.findViewById(R.id.textDuration)
        private val imageMediaType: ImageView = itemView.findViewById(R.id.imageMediaType)

        fun bind(item: TrainingItem) {
            imageTraining.setImageResource(item.imageResId)
            textTitle.text = item.title
            textDescription.text = item.description
            textDuration.text = "${item.duration} min"

            // 根据媒体类型设置不同的图标
            when (item.mediaType) {
                MediaType.AUDIO -> {
                    imageMediaType.setImageResource(android.R.drawable.ic_media_play)
                    imageMediaType.contentDescription = "音频"
                }
                MediaType.VIDEO -> {
                    imageMediaType.setImageResource(R.drawable.ic_media_video)
                    imageMediaType.contentDescription = "视频"
                }
            }

            itemView.setOnClickListener {
                onItemClick(item)
            }
        }
    }
} 