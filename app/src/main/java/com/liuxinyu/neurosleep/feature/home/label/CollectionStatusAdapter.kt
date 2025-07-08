package com.liuxinyu.neurosleep.feature.home.label

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.liuxinyu.neurosleep.R

class CollectionStatusAdapter(
    private var list: List<CollectionStatus>, // 改为可变
    private val onItemClick: (Int) -> Unit    // 添加点击回调
) : RecyclerView.Adapter<CollectionStatusAdapter.StatusViewHolder>() {

    private var selectedPosition = -1

    fun getSelectedPosition(): Int = selectedPosition

    fun updateList(newList: List<CollectionStatus>) {
        list = newList
        notifyDataSetChanged()
    }

    class StatusViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val stateText: TextView = view.findViewById(R.id.state_text)
        val startTimeText: TextView = view.findViewById(R.id.start_time_text)
        val endTimeText: TextView = view.findViewById(R.id.end_time_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatusViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_collection_status, parent, false)
        return StatusViewHolder(view)
    }

    override fun onBindViewHolder(holder: StatusViewHolder, position: Int) {
        // 添加点击效果反馈
        holder.itemView.isClickable = true
        holder.itemView.setOnClickListener {
            // 增加点击动画
            it.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction {
                it.animate().scaleX(1f).scaleY(1f).duration = 100
            }
            // 更新选中位置
            selectedPosition = position
            // 传递位置
            onItemClick(position)
        }

        // 设置选中状态的背景色
        holder.itemView.setBackgroundColor(
            if (position == selectedPosition) android.graphics.Color.LTGRAY
            else android.graphics.Color.TRANSPARENT
        )

        val item = list[position]
        holder.stateText.text = item.state
        holder.startTimeText.text = item.startTime
        holder.endTimeText.text = item.endTime
    }

    override fun getItemCount(): Int = list.size
}
